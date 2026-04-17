package be.mirooz.elitedangerous.dashboard.model.fleetcarrier;

import be.mirooz.elitedangerous.dashboard.model.colonisation.CarrierTradeOrderEntry;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
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
    private CarrierPosition position = new CarrierPosition("", -1L);

    private final Map<String, CarrierTradeOrderEntry> marketByCommodity = new LinkedHashMap<>();
    private final Map<String, Integer> stocksByCommodity = new LinkedHashMap<>();

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
            return;
        }

        marketByCommodity.put(commodityKey, entry);
    }

    public void applyMarketStockDelta(String commodity, String commodityLocalised, int delta) {
        if (delta == 0) {
            return;
        }

        String commodityKey = commodityPrimaryKey(commodity, commodityLocalised);
        int current = stocksByCommodity.getOrDefault(commodityKey, 0);
        stocksByCommodity.put(commodityKey, Math.max(current + delta, 0));
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

}
