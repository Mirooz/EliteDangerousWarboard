package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.commons.lib.models.commodities.CarrierCommodityResolver;
import be.mirooz.elitedangerous.commons.lib.models.commodities.ICommodity;
import be.mirooz.elitedangerous.dashboard.model.colonisation.CarrierTradeOrderEntry;
import be.mirooz.elitedangerous.dashboard.model.registries.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.model.registries.fleetcarrier.CarrierStatus;
import be.mirooz.elitedangerous.dashboard.service.listeners.ColonisationNotificationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Persistance JSON de {@link CarrierStatus} : sauvegarde complète à la fermeture de l’app (schéma 2),
 * rechargement au démarrage. Fichier {@code ~/.elite-warboard/fleet-carrier-journal-snapshot-&lt;FID&gt;.json}.
 * L’ancien format (schéma 1, stocks + ordres + lastModified seuls) reste pris en charge à la lecture.
 */
public final class CarrierStatusPersistence {

    private static final CarrierStatusPersistence INSTANCE = new CarrierStatusPersistence();

    private static final String DIR_NAME = ".elite-warboard";
    private static final String FILE_PREFIX = "fleet-carrier-journal-snapshot-";
    private static final String FILE_SUFFIX = ".json";
    private static final int SCHEMA_LEGACY = 1;
    private static final int SCHEMA_FULL_CARRIER_STATE = 2;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private CarrierStatusPersistence() {
    }

    public static CarrierStatusPersistence getInstance() {
        return INSTANCE;
    }

    /**
     * À la fermeture de l’application : écrit l’état courant du singleton {@link CarrierStatus} (FID connu).
     */
    public void saveOnShutdown() {
        Path path = snapshotPathForCurrentCommander();
        if (path == null) {
            return;
        }
        CarrierStatus cs = CarrierStatus.getInstance();
        try {
            Files.createDirectories(path.getParent());
            ObjectNode root = objectMapper.createObjectNode();
            root.put("schema", SCHEMA_FULL_CARRIER_STATE);
            String fid = CommanderStatus.getInstance().getFID();
            if (fid != null && !fid.isBlank()) {
                root.put("fid", fid);
            }
            Instant last = cs.getLastModifiedTime();
            if (last != null) {
                root.put("lastModified", last.toString());
            }

            ObjectNode carrier = root.putObject("carrier");
            carrier.put("carrierId", cs.getCarrierId());
            carrier.put("carrierType", nullToEmpty(cs.getCarrierType()));
            carrier.put("callsign", nullToEmpty(cs.getCallsign()));
            carrier.put("name", nullToEmpty(cs.getName()));
            carrier.put("totalCapacity", cs.getTotalCapacity());
            carrier.put("blackMarket", cs.isBlackMarket());
            carrier.put("carrierStatsInitialized", cs.isCarrierStatsInitialized());
            carrier.put("balance", cs.getBalance());
            carrier.put("fuel", cs.getFuel());
            carrier.put("operationalState", nullToEmpty(cs.getOperationalState()));
            if (cs.getPosition() != null) {
                carrier.put("positionStarSystem", nullToEmpty(cs.getPosition().getStarSystem()));
                carrier.put("positionBodyId", cs.getPosition().getBodyId());
            } else {
                carrier.put("positionStarSystem", "");
                carrier.put("positionBodyId", -1L);
            }

            ArrayNode stocksArr = root.putArray("stocks");
            for (Map.Entry<ICommodity, Integer> e : cs.getStocksByCommodity().entrySet()) {
                if (e.getKey() == null || e.getValue() == null || e.getValue() <= 0) {
                    continue;
                }
                if (CarrierStatus.isFleetStockExcludedDrone(e.getKey())) {
                    continue;
                }
                ObjectNode row = stocksArr.addObject();
                row.put("commodity", nullToEmpty(e.getKey().getCargoJsonName()));
                row.put("commodityLocalised", nullToEmpty(e.getKey().getInaraName()));
                row.put("tons", e.getValue());
            }

            ArrayNode ordersArr = root.putArray("orders");
            for (CarrierTradeOrderEntry tr : cs.getActiveTransactions()) {
                if (tr == null || tr.isCancelTrade()) {
                    continue;
                }
                ObjectNode o = ordersArr.addObject();
                o.put("timestamp", nullToEmpty(tr.getTimestamp()));
                o.put("carrierId", tr.getCarrierId());
                o.put("carrierType", nullToEmpty(tr.getCarrierType()));
                o.put("blackMarket", tr.isBlackMarket());
                if (tr.getCommodity() != null) {
                    o.put("commodity", nullToEmpty(tr.getCommodity().getCargoJsonName()));
                    o.put("commodityLocalised", nullToEmpty(persistLocalisedHint(tr.getCommodity())));
                } else {
                    o.put("commodity", "");
                    o.put("commodityLocalised", "");
                }
                o.put("purchaseOrder", tr.getPurchaseOrder());
                o.put("saleOrder", tr.getSaleOrder());
                o.put("cancelTrade", tr.isCancelTrade());
                o.put("price", tr.getPrice());
                o.put("stock", tr.getStock());
            }

            Files.writeString(path, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("CarrierStatus persistence (shutdown) save failed: " + e.getMessage());
        }
    }

    /**
     * Recharge depuis le fichier snapshot du commandant courant, si présent.
     *
     * @return {@code true} si un snapshot valide a été appliqué
     */
    public boolean restoreFromDisk(CarrierStatus carrierStatus) {
        Path path = snapshotPathForCurrentCommander();
        if (path == null || !Files.isRegularFile(path)) {
            return false;
        }
        try {
            JsonNode root = objectMapper.readTree(Files.readString(path, StandardCharsets.UTF_8));
            if (root == null || !root.isObject()) {
                return false;
            }
            int schema = root.path("schema").asInt(SCHEMA_LEGACY);
            if (schema >= SCHEMA_FULL_CARRIER_STATE) {
                applyFullStateFromJson(carrierStatus, root);
            } else {
                String lastMod = root.path("lastModified").asText("");
                Map<ICommodity, Integer> stocks = parseStocks(root.path("stocks"));
                List<CarrierTradeOrderEntry> orders = parseOrders(root.path("orders"));
                carrierStatus.restoreJournalPersistedSnapshot(orders, stocks, lastMod);
            }
            ColonisationNotificationService.getInstance().notifyColonisationDataChanged();
            return true;
        } catch (IOException e) {
            System.err.println("CarrierStatus persistence restore failed: " + e.getMessage());
            return false;
        }
    }

    private void applyFullStateFromJson(CarrierStatus cs, JsonNode root) {
        JsonNode c = root.path("carrier");
        String lastMod = root.path("lastModified").asText("");
        Instant lastInstant = null;
        if (lastMod != null && !lastMod.isBlank()) {
            try {
                lastInstant = Instant.parse(lastMod);
            } catch (Exception ignored) {
                lastInstant = null;
            }
        }
        Map<ICommodity, Integer> stocks = parseStocks(root.path("stocks"));
        List<CarrierTradeOrderEntry> orders = parseOrders(root.path("orders"));
        cs.applyFullPersistedSnapshot(
                c.path("carrierId").asLong(0L),
                c.path("carrierType").asText(""),
                c.path("callsign").asText(""),
                c.path("name").asText(""),
                c.path("totalCapacity").asInt(0),
                c.path("blackMarket").asBoolean(false),
                c.path("carrierStatsInitialized").asBoolean(false),
                c.path("balance").asLong(0L),
                c.path("fuel").asInt(0),
                c.path("operationalState").asText(""),
                c.path("positionStarSystem").asText(""),
                c.path("positionBodyId").asLong(-1L),
                lastInstant,
                stocks,
                orders);
    }

    private Map<ICommodity, Integer> parseStocks(JsonNode stocksNode) {
        Map<ICommodity, Integer> out = new LinkedHashMap<>();
        if (stocksNode == null || !stocksNode.isArray()) {
            return out;
        }
        for (JsonNode row : stocksNode) {
            String internal = row.path("commodity").asText("");
            String loc = row.path("commodityLocalised").asText("");
            int tons = row.path("tons").asInt(0);
            if (tons <= 0) {
                continue;
            }
            if (CarrierStatus.isFleetStockExcludedDrone(internal, loc)) {
                continue;
            }
            ICommodity key = CarrierCommodityResolver.resolve(internal, loc);
            if (key != null) {
                out.put(key, tons);
            }
        }
        return out;
    }

    private List<CarrierTradeOrderEntry> parseOrders(JsonNode ordersNode) {
        List<CarrierTradeOrderEntry> out = new ArrayList<>();
        if (ordersNode == null || !ordersNode.isArray()) {
            return out;
        }
        for (JsonNode o : ordersNode) {
            String internal = o.path("commodity").asText("");
            String loc = o.path("commodityLocalised").asText("");
            ICommodity commodity = CarrierCommodityResolver.resolve(internal, loc);
            if (commodity == null) {
                continue;
            }
            if (loc != null && !loc.isBlank()) {
                commodity.setLocalisedName(loc);
            }
            CarrierTradeOrderEntry e = new CarrierTradeOrderEntry(
                    o.path("timestamp").asText(null),
                    o.path("carrierId").asLong(0L),
                    o.path("carrierType").asText(""),
                    o.path("blackMarket").asBoolean(false),
                    commodity,
                    o.path("purchaseOrder").asInt(0),
                    o.path("saleOrder").asInt(0),
                    o.path("cancelTrade").asBoolean(false),
                    o.path("price").asLong(0L),
                    o.path("stock").asInt(0)
            );
            if (!e.isCancelTrade()) {
                out.add(e);
            }
        }
        return out;
    }

    private Path snapshotPathForCurrentCommander() {
        String fid = CommanderStatus.getInstance().getFID();
        if (fid == null || fid.isBlank()) {
            return null;
        }
        String safe = sanitizeFileSegment(fid);
        return Paths.get(System.getProperty("user.home"), DIR_NAME, FILE_PREFIX + safe + FILE_SUFFIX);
    }

    private static String sanitizeFileSegment(String fid) {
        StringBuilder b = new StringBuilder(fid.length());
        for (int i = 0; i < fid.length(); i++) {
            char ch = fid.charAt(i);
            if (Character.isLetterOrDigit(ch) || ch == '-' || ch == '_') {
                b.append(ch);
            } else {
                b.append('_');
            }
        }
        return b.toString();
    }

    private static String nullToEmpty(String s) {
        return s != null ? s : "";
    }

    private static String persistLocalisedHint(ICommodity c) {
        if (c == null) {
            return "";
        }
        String ln = c.getLocalisedName();
        if (ln != null && !ln.isBlank()) {
            return ln;
        }
        String vis = c.getVisibleName();
        if (vis != null && !vis.isBlank()) {
            return vis;
        }
        String inara = c.getInaraName();
        if (inara != null && !inara.isBlank()) {
            return inara;
        }
        return c.getCargoJsonName() != null ? c.getCargoJsonName() : "";
    }
}
