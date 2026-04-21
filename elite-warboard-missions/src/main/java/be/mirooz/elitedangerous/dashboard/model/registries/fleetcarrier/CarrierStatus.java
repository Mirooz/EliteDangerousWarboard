package be.mirooz.elitedangerous.dashboard.model.registries.fleetcarrier;

import be.mirooz.elitedangerous.commons.lib.models.commodities.CarrierCommodityResolver;
import be.mirooz.elitedangerous.commons.lib.models.commodities.ColonisationCommodityKeys;
import be.mirooz.elitedangerous.commons.lib.models.commodities.ICommodity;
import be.mirooz.elitedangerous.backend.generated.model.CapiFcCapacity;
import be.mirooz.elitedangerous.backend.generated.model.CapiFcCargoItem;
import be.mirooz.elitedangerous.backend.generated.model.CapiFcCommodityPurchase;
import be.mirooz.elitedangerous.backend.generated.model.CapiFcMarketCommodity;
import be.mirooz.elitedangerous.backend.generated.model.CapiFcName;
import be.mirooz.elitedangerous.backend.generated.model.CapiFcOrderCommodities;
import be.mirooz.elitedangerous.backend.generated.model.CapiFcOrders;
import be.mirooz.elitedangerous.backend.generated.model.CapiFcStationMarket;
import be.mirooz.elitedangerous.backend.generated.model.CapiFleetCarrierPayload;
import be.mirooz.elitedangerous.backend.generated.model.CapiFleetCarrierProxyResponse;
import be.mirooz.elitedangerous.dashboard.model.colonisation.CarrierTradeOrderEntry;
import be.mirooz.elitedangerous.dashboard.model.fleetcarrier.CarrierPosition;
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
import java.util.Objects;
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

    private final Map<ICommodity, CarrierTradeOrderEntry> marketByCommodity = new LinkedHashMap<>();
    private final Map<ICommodity, Integer> stocksByCommodity = new LinkedHashMap<>();

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

        ICommodity commodityKey = canonicalCommodity(entry);
        markLastModifiedFromJournal(entry.getTimestamp());
        if (commodityKey == null) {
            return;
        }

        if (entry.isCancelTrade()) {
            marketByCommodity.remove(commodityKey);
            stocksByCommodity.remove(commodityKey);
            return;
        }

        marketByCommodity.put(commodityKey, entry);
    }

    /**
     * Met à jour l'état à partir de {@link CapiFleetCarrierProxyResponse#getFleetcarrier()}
     * (données CAPI {@code /api/capi/fleetcarrier}).
     */
    public void applyCapiFleetCarrierPayload(CapiFleetCarrierProxyResponse response) {
        if (response == null || response.getFleetcarrier() == null) {
            return;
        }
        CapiFleetCarrierPayload fleet = response.getFleetcarrier();

        CapiFcName nameNode = fleet.getName();
        String callsign = nameNode != null && nameNode.getCallsign() != null ? nameNode.getCallsign() : "";
        String vanityHex = nameNode != null && nameNode.getVanityName() != null ? nameNode.getVanityName() : "";
        String filteredHex = nameNode != null && nameNode.getFilteredVanityName() != null ? nameNode.getFilteredVanityName() : "";
        String decodedVanity = decodeHexUtf8(vanityHex);
        String decodedFiltered = decodeHexUtf8(filteredHex);
        String displayName = firstNonBlank(decodedVanity, decodedFiltered, vanityHex, filteredHex, callsign);

        CapiFcStationMarket market = fleet.getMarket();
        long marketId = market != null && market.getId() != null ? market.getId() : 0L;
        if (marketId > 0L) {
            this.carrierId = marketId;
        }

        this.carrierType = "FleetCarrier";
        this.callsign = callsign;
        this.name = displayName;

        CapiFcCapacity cap = fleet.getCapacity();
        if (cap != null) {
            int cargoForSale = nz(cap.getCargoForSale());
            int cargoNotForSale = nz(cap.getCargoNotForSale());
            int cargoSpaceReserved = nz(cap.getCargoSpaceReserved());
            int freeSpace = nz(cap.getFreeSpace());
            this.totalCapacity = Math.max(cargoForSale + cargoNotForSale + cargoSpaceReserved + freeSpace, 0);
        }

        this.balance = fleet.getBalance() != null ? fleet.getBalance() : 0L;
        this.fuel = fleet.getFuel() != null ? fleet.getFuel() : 0;
        this.operationalState = fleet.getState() != null ? fleet.getState() : "";

        if (market != null && market.getServices() != null) {
            String bm = firstServiceValue(market.getServices(), "blackmarket", "blackMarket");
            this.blackMarket = "ok".equalsIgnoreCase(bm);
        }

        String starSystem = fleet.getCurrentStarSystem() != null ? fleet.getCurrentStarSystem() : "";
        long bodyId = this.position != null ? this.position.getBodyId() : -1L;
        if (!starSystem.isBlank()) {
            this.position = new CarrierPosition(starSystem, bodyId);
        }
        marketByCommodity.clear();
        stocksByCommodity.clear();

        if (fleet.getCargo() != null) {
            for (CapiFcCargoItem row : fleet.getCargo()) {
                applyOneCapiCargoItem(row);
            }
        }

        if (market != null && market.getCommodities() != null) {
            for (CapiFcMarketCommodity c : market.getCommodities()) {
                applyOneCapiMarketCommodity(c);
            }
        }
        applyCapiOrderPurchases(fleet);

        stocksByCommodity.entrySet().removeIf(e -> e.getValue() == null || e.getValue() <= 0);

        this.carrierStatsInitialized = this.carrierId > 0L;
    }

    private static int nz(Integer v) {
        return v != null ? v : 0;
    }

    private static String firstServiceValue(Map<String, String> services, String... keys) {
        if (services == null || keys == null) {
            return "";
        }
        for (String k : keys) {
            String v = services.get(k);
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return "";
    }

    private void applyOneCapiCargoItem(CapiFcCargoItem c) {
        if (c == null) {
            return;
        }
        String internal = Objects.toString(c.getCommodity(), "");
        String loc = Objects.toString(c.getLocName(), "");
        int qty = nz(c.getQty());
        if (internal.isBlank() && loc.isBlank()) {
            return;
        }
        if (qty <= 0) {
            return;
        }
        if (isFleetStockExcludedDrone(internal, loc)) {
            return;
        }
        ICommodity key = CarrierCommodityResolver.resolve(internal, loc);
        stocksByCommodity.merge(key, qty, Integer::sum);
    }

    private void applyOneCapiMarketCommodity(CapiFcMarketCommodity c) {
        if (c == null) {
            return;
        }
        String internal = Objects.toString(c.getName(), "");
        String loc = Objects.toString(c.getLocName(), "");
        if (internal.isBlank() && loc.isBlank()) {
            return;
        }
        ICommodity key = CarrierCommodityResolver.resolve(internal, loc);
        if (isFleetStockExcludedDrone(internal, loc)) {
            marketByCommodity.remove(key);
            return;
        }
        int stock = nz(c.getStock());
        int demand = nz(c.getDemand());
        if (stock <= 0 && demand <= 0) {
            marketByCommodity.remove(key);
            return;
        }
        long pricePerTon = 0L;
        if (demand > 0) {
            pricePerTon = nz(c.getSellPrice());
        } else {
            pricePerTon = nz(c.getBuyPrice());
        }
        CarrierTradeOrderEntry e = new CarrierTradeOrderEntry(
                Instant.now().toString(),
                this.carrierId,
                "FleetCarrier",
                false,
                key,
                demand,
                stock,
                false,
                pricePerTon,
                stock);
        marketByCommodity.put(key, e);
    }

    /**
     * Ordres d’achat détaillés côté CAPI (complète {@code market.commodities} si besoin).
     */
    private void applyCapiOrderPurchases(CapiFleetCarrierPayload fleet) {
        if (fleet == null || fleet.getOrders() == null) {
            return;
        }
        CapiFcOrders orders = fleet.getOrders();
        CapiFcOrderCommodities commodities = orders.getCommodities();
        if (commodities == null) {
            return;
        }
        List<CapiFcCommodityPurchase> purchases = commodities.getPurchases();
        if (purchases == null) {
            return;
        }
        for (CapiFcCommodityPurchase p : purchases) {
            if (p == null) {
                continue;
            }
            String internal = Objects.toString(p.getName(), "");
            if (internal.isBlank()) {
                continue;
            }
            if (isFleetStockExcludedDrone(internal, "")) {
                continue;
            }
            ICommodity key = CarrierCommodityResolver.resolve(internal, "");
            int po = nz(p.getOutstanding());
            if (po <= 0) {
                po = nz(p.getTotal());
            }
            if (po <= 0) {
                continue;
            }
            boolean bm = Boolean.TRUE.equals(p.getBlackmarket());
            long bid = nz(p.getPrice());
            CarrierTradeOrderEntry cur = marketByCommodity.get(key);
            if (cur == null) {
                marketByCommodity.put(key, new CarrierTradeOrderEntry(
                        Instant.now().toString(),
                        this.carrierId,
                        "FleetCarrier",
                        bm,
                        key,
                        po,
                        0,
                        false,
                        bid,
                        0));
            } else if (po > cur.getPurchaseOrder()) {
                marketByCommodity.put(key, new CarrierTradeOrderEntry(
                        cur.getTimestamp(),
                        this.carrierId,
                        "FleetCarrier",
                        cur.isBlackMarket() || bm,
                        cur.getCommodity(),
                        po,
                        cur.getSaleOrder(),
                        false,
                        bid,
                        cur.getStock()));
            }
        }
    }

    public void applyMarketStockDelta(String commodity, String commodityLocalised, int delta, String eventTimestamp) {
        if (delta == 0) {
            return;
        }
        if (isFleetStockExcludedDrone(commodity, commodityLocalised)) {
            return;
        }

        ICommodity commodityKey = CarrierCommodityResolver.resolve(commodity, commodityLocalised);
        int current = stocksByCommodity.getOrDefault(commodityKey, 0);
        int next = Math.max(current + delta, 0);
        if (next <= 0) {
            stocksByCommodity.remove(commodityKey);
        } else {
            stocksByCommodity.put(commodityKey, next);
        }

        ICommodity mapKey = linkedMarketKey(commodityKey);
        if (mapKey != null) {
            CarrierTradeOrderEntry cur = marketByCommodity.get(mapKey);
            if (cur != null && cur.getCommodity() != null && !cur.isCancelTrade()) {
                int newPo = cur.getPurchaseOrder();
                int newSo = cur.getSaleOrder();
                int newListingStock = cur.getStock();
                if (delta > 0) {
                    // MarketSell sur le FC : le commandant livre → l’ordre d’achat du carrier diminue.
                    newPo = Math.max(0, newPo - delta);
                } else {
                    // MarketBuy sur le FC : le commandant achète → stock listé / ordre de vente diminuent.
                    int bought = -delta;
                    newListingStock = Math.max(0, newListingStock - bought);
                    newSo = Math.max(0, newSo - bought);
                }
                if (newPo <= 0 && newSo <= 0 && newListingStock <= 0) {
                    marketByCommodity.remove(mapKey);
                } else {
                    marketByCommodity.put(mapKey, new CarrierTradeOrderEntry(
                            cur.getTimestamp(),
                            cur.getCarrierId(),
                            cur.getCarrierType(),
                            cur.isBlackMarket(),
                            cur.getCommodity(),
                            newPo,
                            newSo,
                            cur.isCancelTrade(),
                            cur.getPrice(),
                            newListingStock));
                }
            }
        }

        markLastModifiedFromJournal(eventTimestamp);
    }

    /** Clé présente dans {@link #marketByCommodity} pour la même commodité (instance ou {@link ColonisationCommodityKeys}). */
    private ICommodity linkedMarketKey(ICommodity resolved) {
        if (resolved == null) {
            return null;
        }
        if (marketByCommodity.containsKey(resolved)) {
            return resolved;
        }
        String mk = ColonisationCommodityKeys.mergeKey(resolved);
        if (mk.isBlank()) {
            return null;
        }
        for (ICommodity k : marketByCommodity.keySet()) {
            if (k != null && ColonisationCommodityKeys.mergeKey(k).equals(mk)) {
                return k;
            }
        }
        return null;
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

    /**
     * Remplace stocks et ordres marché issus du journal par un snapshot disque
     * (lorsque le sync CAPI fleet carrier est sauté).
     */
    public void restoreJournalPersistedSnapshot(
            List<CarrierTradeOrderEntry> orders,
            Map<ICommodity, Integer> stocks,
            String lastModifiedIso) {
        marketByCommodity.clear();
        if (orders != null) {
            for (CarrierTradeOrderEntry e : orders) {
                if (e == null || e.isCancelTrade()) {
                    continue;
                }
                ICommodity key = canonicalCommodity(e);
                if (key == null) {
                    continue;
                }
                marketByCommodity.put(key, e);
            }
        }
        stocksByCommodity.clear();
        if (stocks != null) {
            for (Map.Entry<ICommodity, Integer> e : stocks.entrySet()) {
                if (e.getKey() == null || e.getValue() == null || e.getValue() <= 0) {
                    continue;
                }
                if (isFleetStockExcludedDrone(e.getKey())) {
                    continue;
                }
                stocksByCommodity.put(e.getKey(), e.getValue());
            }
        }
        if (lastModifiedIso != null && !lastModifiedIso.isBlank()) {
            lastModifiedTime = parseJournalInstant(lastModifiedIso);
        }
    }

    /**
     * Remplace tout l’état carrier depuis un snapshot disque (schéma 2 — fermeture d’app).
     */
    public synchronized void applyFullPersistedSnapshot(
            long carrierId,
            String carrierType,
            String callsign,
            String name,
            int totalCapacity,
            boolean blackMarket,
            boolean carrierStatsInitialized,
            long balance,
            int fuel,
            String operationalState,
            String positionStarSystem,
            long positionBodyId,
            Instant lastModifiedTimeOrNull,
            Map<ICommodity, Integer> stocks,
            List<CarrierTradeOrderEntry> orders) {
        marketByCommodity.clear();
        stocksByCommodity.clear();
        this.carrierId = carrierId;
        this.carrierType = carrierType != null ? carrierType : "";
        this.callsign = callsign != null ? callsign : "";
        this.name = name != null ? name : "";
        this.totalCapacity = Math.max(totalCapacity, 0);
        this.blackMarket = blackMarket;
        this.carrierStatsInitialized = carrierStatsInitialized;
        this.balance = balance;
        this.fuel = fuel;
        this.operationalState = operationalState != null ? operationalState : "";
        this.position = new CarrierPosition(positionStarSystem != null ? positionStarSystem : "", positionBodyId);
        this.lastModifiedTime = lastModifiedTimeOrNull;
        if (stocks != null) {
            for (Map.Entry<ICommodity, Integer> e : stocks.entrySet()) {
                if (e.getKey() == null || e.getValue() == null || e.getValue() <= 0) {
                    continue;
                }
                if (isFleetStockExcludedDrone(e.getKey())) {
                    continue;
                }
                stocksByCommodity.put(e.getKey(), e.getValue());
            }
        }
        if (orders != null) {
            for (CarrierTradeOrderEntry entry : orders) {
                if (entry == null || entry.isCancelTrade()) {
                    continue;
                }
                ICommodity key = canonicalCommodity(entry);
                if (key == null) {
                    continue;
                }
                marketByCommodity.put(key, entry);
            }
        }
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

    public Map<ICommodity, CarrierTradeOrderEntry> getMarketByCommodity() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(marketByCommodity));
    }

    public Map<ICommodity, Integer> getStocksByCommodity() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(stocksByCommodity));
    }

    public List<ICommodity> getPurchaseOrder() {
        return marketByCommodity.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().getPurchaseOrder() != 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public List<ICommodity> getSaleOrder() {
        return marketByCommodity.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().getSaleOrder() != 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public List<CarrierTradeOrderEntry> getActiveTransactions() {
        return Collections.unmodifiableList(new ArrayList<>(marketByCommodity.values()));
    }

    /**
     * Limpets / drones cargo : hors stock carrier (CAPI, MarketBuy/MarketSell sur le FC).
     * Couvre l’identifiant standard {@code drones} et les variantes rares se terminant par {@code drones}.
     */
    public static boolean isFleetStockExcludedDrone(String commodity, String commodityLocalised) {
        String c = commodity != null ? commodity.trim().toLowerCase(Locale.ROOT) : "";
        String loc = commodityLocalised != null ? commodityLocalised.trim().toLowerCase(Locale.ROOT) : "";
        if (!c.isBlank() && c.endsWith("drones")) {
            return true;
        }
        return !loc.isBlank() && (loc.contains("limpet") || loc.contains("limpét"));
    }

    public static boolean isFleetStockExcludedDrone(ICommodity c) {
        if (c == null) {
            return false;
        }
        return isFleetStockExcludedDrone(c.getCargoJsonName(), c.getInaraName());
    }

    /**
     * Commodité canonique pour une entrée marché / ordre (alignée sur les clés de {@link #stocksByCommodity}).
     */
    public ICommodity canonicalCommodity(CarrierTradeOrderEntry e) {
        if (e == null || e.getCommodity() == null) {
            return null;
        }
        return e.getCommodity();
    }

    /** Tonnes physiques pour cette commodité (égalité des instances ou clé de fusion {@link ColonisationCommodityKeys}). */
    public int physicalStock(ICommodity c) {
        if (c == null) {
            return 0;
        }
        if (isFleetStockExcludedDrone(c)) {
            return 0;
        }
        Integer v = stocksByCommodity.get(c);
        if (v != null) {
            return v;
        }
        String mk = ColonisationCommodityKeys.mergeKey(c);
        for (Map.Entry<ICommodity, Integer> e : stocksByCommodity.entrySet()) {
            if (e.getKey() != null && ColonisationCommodityKeys.mergeKey(e.getKey()).equals(mk)) {
                return e.getValue() != null ? e.getValue() : 0;
            }
        }
        return 0;
    }

    /** Libellé affichable (journal / CAPI si ordre connu pour la même commodité fusionnée). */
    public String displayLabel(ICommodity c) {
        if (c == null) {
            return "?";
        }
        String mk = ColonisationCommodityKeys.mergeKey(c);
        if (mk.isBlank()) {
            return "?";
        }
        for (Map.Entry<ICommodity, CarrierTradeOrderEntry> e : marketByCommodity.entrySet()) {
            if (e.getKey() != null && ColonisationCommodityKeys.mergeKey(e.getKey()).equals(mk)) {
                CarrierTradeOrderEntry tr = e.getValue();
                if (tr != null && tr.getCommodity() != null) {
                    return firstNonBlank(
                            tr.getCommodity().getTitleName(),
                            tr.getCommodity().getVisibleName(),
                            tr.getCommodity().getCargoJsonName(),
                            titleOrCargo(c));
                }
            }
        }
        return titleOrCargo(c);
    }

    private static String titleOrCargo(ICommodity c) {
        String t = c.getTitleName();
        if (t != null && !t.isBlank()) {
            return t;
        }
        if (c.getVisibleName() != null) {
            return c.getVisibleName();
        }
        return c.getCargoJsonName();
    }

    /** Somme des tonnes en stock sur le carrier (cartographie CAPI / journal), hors drones / limpets. */
    public int sumPhysicalStocksTons() {
        int s = 0;
        for (Map.Entry<ICommodity, Integer> e : stocksByCommodity.entrySet()) {
            if (e.getValue() == null || e.getValue() <= 0) {
                continue;
            }
            if (isFleetStockExcludedDrone(e.getKey())) {
                continue;
            }
            s += e.getValue();
        }
        return s;
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
