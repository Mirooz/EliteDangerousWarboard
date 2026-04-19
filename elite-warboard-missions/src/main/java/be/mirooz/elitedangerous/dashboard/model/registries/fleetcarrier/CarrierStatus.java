package be.mirooz.elitedangerous.dashboard.model.registries.fleetcarrier;

import be.mirooz.elitedangerous.dashboard.model.colonisation.CarrierTradeOrderEntry;
import be.mirooz.elitedangerous.dashboard.model.fleetcarrier.CarrierPosition;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.ToString;

import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * État courant du Fleet Carrier :
 * - transactions actives (ordres achat/vente)
 * - stock courant par commodity
 */
@Getter
@ToString
public class CarrierStatus {

    private static final CarrierStatus INSTANCE = new CarrierStatus();

    public static CarrierStatus getInstance() {
        return INSTANCE;
    }

    private long carrierId;
    private String carrierType = "";
    private String callsign = "";
    private String name = "";
    private int totalCapacity;
    private boolean blackMarket;
    private boolean carrierStatsInitialized;
    private long balance;
    private int fuel;
    private String operationalState = "";
    private CarrierPosition position = new CarrierPosition("", -1L);

    private final Map<String, CarrierTradeOrderEntry> marketByCommodity = new LinkedHashMap<>();
    private final Map<String, Integer> stocksByCommodity = new LinkedHashMap<>();

    /** Dernière activité journal (ordre / achat-vente marché carrier) — pour éviter un sync CAPI redondant. */
    private volatile Instant lastModifiedTime;

    private CarrierStatus() {
    }

    public void updateCarrierStats(long carrierId, String carrierType, String callsign, String name, int totalCapacity) {
        this.carrierId = carrierId;
        this.carrierType = carrierType != null ? carrierType : "";
        this.callsign = callsign != null ? callsign : "";
        this.name = name != null ? name : "";
        this.totalCapacity = Math.max(totalCapacity, 0);
        this.carrierStatsInitialized = true;
    }

    public void updateCarrierLocation(long carrierId, String carrierType, String starSystem, long bodyId) {
        this.carrierId = carrierId;
        if (carrierType != null && !carrierType.isBlank()) {
            this.carrierType = carrierType;
        }
        this.position = new CarrierPosition(starSystem != null ? starSystem : "", bodyId);
        this.carrierStatsInitialized = true;
    }

    public void recordTradeOrder(CarrierTradeOrderEntry entry) {
        if (entry == null) {
            return;
        }

        this.carrierId = entry.getCarrierId();
        this.carrierType = entry.getCarrierType() != null ? entry.getCarrierType() : this.carrierType;
        this.blackMarket = entry.isBlackMarket();

        String commodityKey = commodityPrimaryKey(entry.getCommodity(), entry.getCommodityLocalised());

        if (entry.isCancelTrade()) {
            marketByCommodity.remove(commodityKey);
            stocksByCommodity.remove(commodityKey);
            markLastModifiedFromJournal(entry.getTimestamp());
            return;
        }

        marketByCommodity.put(commodityKey, entry);
        markLastModifiedFromJournal(entry.getTimestamp());
    }

    /**
     * Met à jour l'état à partir de la charge {@code data} de la réponse CAPI {@code /api/capi/fleetcarrier}.
     */
    public void applyCapiFleetCarrierPayload(JsonNode data) {
        if (data == null || data.isNull() || data.isMissingNode()) {
            return;
        }
        JsonNode fleet = data.path("fleetcarrier");
        if (!fleet.isObject() || fleet.isEmpty()) {
            fleet = data.path("fleetCarrier");
        }
        if (!fleet.isObject()) {
            return;
        }

        JsonNode nameNode = fleet.path("name");
        String callsign = textOrEmpty(nameNode, "callsign", "callssign");
        String vanityHex = textOrEmpty(nameNode, "vanityName", "vanityname");
        String filteredHex = textOrEmpty(nameNode, "filteredVanityName", "filteredvanityname");
        String decodedVanity = decodeHexUtf8(vanityHex);
        String decodedFiltered = decodeHexUtf8(filteredHex);
        String displayName = firstNonBlank(decodedVanity, decodedFiltered, vanityHex, filteredHex, callsign);

        JsonNode market = fleet.path("market");
        long marketId = market.path("id").asLong(0L);
        if (marketId > 0L) {
            this.carrierId = marketId;
        }

        this.carrierType = "FleetCarrier";
        this.callsign = callsign;
        this.name = displayName;

        JsonNode cap = fleet.path("capacity");
        if (cap.isObject()) {
            int cargoForSale = cap.path("cargoForSale").asInt(0);
            int cargoNotForSale = cap.path("cargoNotForSale").asInt(0);
            int cargoSpaceReserved = cap.path("cargoSpaceReserved").asInt(0);
            int freeSpace = cap.path("freeSpace").asInt(0);
            this.totalCapacity = Math.max(cargoForSale + cargoNotForSale + cargoSpaceReserved + freeSpace, 0);
        }

        this.balance = fleet.path("balance").asLong(0L);
        this.fuel = fleet.path("fuel").asInt(0);
        this.operationalState = textOrEmpty(fleet, "state");

        JsonNode services = market.path("services");
        if (!services.isObject()) {
            services = market.path("Services");
        }
        if (services.isObject()) {
            String bm = textOrEmpty(services, "blackmarket", "blackMarket");
            this.blackMarket = "ok".equalsIgnoreCase(bm);
        }

        String starSystem = textOrEmpty(fleet, "currentStarSystem", "currentstarsystem");
        long bodyId = this.position != null ? this.position.getBodyId() : -1L;
        if (!starSystem.isBlank()) {
            this.position = new CarrierPosition(starSystem, bodyId);
        }

        JsonNode commodities = market.path("commodities");
        if (!commodities.isArray()) {
            commodities = market.path("Commodities");
        }
        if (commodities.isArray()) {
            for (JsonNode c : commodities) {
                applyOneCapiCommodityStockRow(c);
            }
        }

        JsonNode orders = fleet.path("orders");
        if (!orders.isObject()) {
            orders = fleet.path("Orders");
        }
        if (orders.isObject()) {
            JsonNode orderCommodities = orders.path("commodities");
            if (!orderCommodities.isObject()) {
                orderCommodities = orders.path("Commodities");
            }
            if (orderCommodities.isObject()) {
                JsonNode sales = orderCommodities.path("sales");
                if (!sales.isArray()) {
                    sales = orderCommodities.path("Sales");
                }
                if (sales.isArray()) {
                    for (JsonNode c : sales) {
                        applyOneCapiCommodityStockRow(c);
                    }
                }
            }
        }

        stocksByCommodity.entrySet().removeIf(e -> e.getValue() == null || e.getValue() <= 0);

        this.carrierStatsInitialized = this.carrierId > 0L;
    }

    private void applyOneCapiCommodityStockRow(JsonNode c) {
        if (c == null || c.isNull() || c.isMissingNode()) {
            return;
        }
        String internal = c.path("name").asText("");
        String loc = c.path("locName").asText("");
        if (internal.isBlank() && loc.isBlank()) {
            return;
        }
        String key = commodityPrimaryKey(internal.toLowerCase(Locale.ROOT), loc);
        int stock = c.path("stock").asInt(0);
        if (stock <= 0) {
            stocksByCommodity.remove(key);
            marketByCommodity.remove(key);
        } else {
            stocksByCommodity.put(key, stock);
        }
    }

    public void applyMarketStockDelta(String commodity, String commodityLocalised, int delta, String eventTimestamp) {
        if (delta == 0) {
            return;
        }

        String commodityKey = commodityPrimaryKey(commodity, commodityLocalised);
        int current = stocksByCommodity.getOrDefault(commodityKey, 0);
        int next = Math.max(current + delta, 0);
        if (next <= 0) {
            stocksByCommodity.remove(commodityKey);
        } else {
            stocksByCommodity.put(commodityKey, next);
        }
        markLastModifiedFromJournal(eventTimestamp);
    }

    /**
     * Vrai si une mise à jour locale (journal) du carrier a eu lieu il y a moins de {@code maxAge}.
     */
    public boolean hasRecentJournalCarrierActivity(Duration maxAge) {
        Instant t = lastModifiedTime;
        if (t == null) {
            return false;
        }
        return !t.isBefore(Instant.now().minus(maxAge));
    }

    private void markLastModifiedFromJournal(String journalIsoTimestamp) {
        lastModifiedTime = parseJournalInstant(journalIsoTimestamp);
    }

    private static Instant parseJournalInstant(String journalIsoTimestamp) {
        if (journalIsoTimestamp == null || journalIsoTimestamp.isBlank()) {
            return Instant.now();
        }
        try {
            return Instant.parse(journalIsoTimestamp);
        } catch (DateTimeException e) {
            return Instant.now();
        }
    }

    public Map<String, CarrierTradeOrderEntry> getMarketByCommodity() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(marketByCommodity));
    }

    public Map<String, Integer> getStocksByCommodity() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(stocksByCommodity));
    }

    public List<String> getPurchaseOrder() {
        return marketByCommodity.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().getPurchaseOrder() != 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public List<String> getSaleOrder() {
        return marketByCommodity.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().getSaleOrder() != 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public List<CarrierTradeOrderEntry> getActiveTransactions() {
        return Collections.unmodifiableList(new ArrayList<>(marketByCommodity.values()));
    }

    /**
     * Clé stable pour maps : identifiant interne {@code Commodity} / {@code Type} si présent, sinon libellé localisé.
     */
    private String commodityPrimaryKey(String commodity, String commodityLocalised) {
        if (commodity != null && !commodity.isBlank()) {
            return commodity;
        }
        return normalizeCommodityKey(commodityLocalised);
    }

    private String normalizeCommodityKey(String commodityLocalised) {
        if (commodityLocalised == null || commodityLocalised.isBlank()) {
            return "__UNKNOWN_COMMODITY__";
        }
        return commodityLocalised;
    }

    private static String textOrEmpty(JsonNode node, String... fieldNames) {
        if (node == null || node.isMissingNode() || node.isNull() || !node.isObject()) {
            return "";
        }
        for (String n : fieldNames) {
            JsonNode vNode = node.get(n);
            if (vNode != null && !vNode.isNull() && !vNode.isMissingNode()) {
                String v = vNode.asText("");
                if (v != null && !v.isBlank()) {
                    return v;
                }
            }
        }
        return "";
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return "";
    }

    private static String decodeHexUtf8(String hex) {
        if (hex == null || hex.isBlank()) {
            return "";
        }
        String h = hex.trim();
        if (h.length() % 2 != 0) {
            return "";
        }
        for (int i = 0; i < h.length(); i++) {
            char c = h.charAt(i);
            if ((c < '0' || c > '9') && (c < 'a' || c > 'f') && (c < 'A' || c > 'F')) {
                return "";
            }
        }
        try {
            byte[] out = new byte[h.length() / 2];
            for (int i = 0; i < out.length; i++) {
                out[i] = (byte) Integer.parseInt(h.substring(2 * i, 2 * i + 2), 16);
            }
            return new String(out, StandardCharsets.UTF_8).trim();
        } catch (NumberFormatException e) {
            return "";
        }
    }

}
