package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.commons.lib.models.commodities.ICommodity;
import be.mirooz.elitedangerous.commons.lib.models.commodities.CarrierCommodityResolver;
import be.mirooz.elitedangerous.dashboard.model.colonisation.CarrierTradeOrderEntry;
import be.mirooz.elitedangerous.dashboard.model.registries.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.model.registries.fleetcarrier.CarrierStatus;
import be.mirooz.elitedangerous.dashboard.service.listeners.ColonisationNotificationService;
import be.mirooz.elitedangerous.dashboard.view.common.context.DashboardContext;
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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Persistance de l'état carrier (stocks + ordres) mis à jour depuis le journal,
 * pour recharger cet état quand le sync CAPI fleet carrier est sauté.
 */
public final class FleetCarrierJournalSnapshotPersistence {

    private static final FleetCarrierJournalSnapshotPersistence INSTANCE = new FleetCarrierJournalSnapshotPersistence();
    private static final String DIR_NAME = ".elite-warboard";
    private static final String FILE_PREFIX = "fleet-carrier-journal-snapshot-";
    private static final String FILE_SUFFIX = ".json";
    private static final int SCHEMA = 1;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private volatile boolean dirtyDuringBatch;

    private FleetCarrierJournalSnapshotPersistence() {
    }

    public static FleetCarrierJournalSnapshotPersistence getInstance() {
        return INSTANCE;
    }

    public void notifyJournalMutation() {
        if (DashboardContext.getInstance().isBatchLoading()) {
            dirtyDuringBatch = true;
            return;
        }
        saveSnapshotNow();
    }

    public void flushAfterJournalBatchIfDirty() {
        if (dirtyDuringBatch) {
            dirtyDuringBatch = false;
            saveSnapshotNow();
        }
    }

    /**
     * @return {@code true} si un snapshot valide a été appliqué
     */
    public boolean restoreInto(CarrierStatus carrierStatus) {
        Path path = snapshotPathForCurrentCommander();
        if (path == null || !Files.isRegularFile(path)) {
            return false;
        }
        try {
            JsonNode root = objectMapper.readTree(Files.readString(path, StandardCharsets.UTF_8));
            if (root == null || !root.isObject()) {
                return false;
            }
            String lastMod = root.path("lastModified").asText("");
            Map<ICommodity, Integer> stocks = parseStocks(root.path("stocks"));
            java.util.List<CarrierTradeOrderEntry> orders = parseOrders(root.path("orders"));
            carrierStatus.restoreJournalPersistedSnapshot(orders, stocks, lastMod);
            ColonisationNotificationService.getInstance().notifyColonisationDataChanged();
            return true;
        } catch (IOException e) {
            System.err.println("Fleet carrier snapshot restore failed: " + e.getMessage());
            return false;
        }
    }

    private void saveSnapshotNow() {
        Path path = snapshotPathForCurrentCommander();
        if (path == null) {
            return;
        }
        CarrierStatus cs = CarrierStatus.getInstance();
        Instant last = cs.getLastModifiedTime();
        if (last == null && cs.getStocksByCommodity().isEmpty() && cs.getMarketByCommodity().isEmpty()) {
            return;
        }
        try {
            Files.createDirectories(path.getParent());
            ObjectNode root = objectMapper.createObjectNode();
            root.put("schema", SCHEMA);
            String fid = CommanderStatus.getInstance().getFID();
            if (fid != null && !fid.isBlank()) {
                root.put("fid", fid);
            }
            if (last != null) {
                root.put("lastModified", last.toString());
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
                    String locPersist = persistLocalisedHint(tr.getCommodity());
                    o.put("commodityLocalised", nullToEmpty(locPersist));
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
            System.err.println("Fleet carrier snapshot save failed: " + e.getMessage());
        }
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

    private java.util.List<CarrierTradeOrderEntry> parseOrders(JsonNode ordersNode) {
        java.util.List<CarrierTradeOrderEntry> out = new java.util.ArrayList<>();
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
            char c = fid.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '-' || c == '_') {
                b.append(c);
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
