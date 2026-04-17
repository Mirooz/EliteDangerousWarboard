package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.dashboard.model.colonisation.CarrierTradeOrderEntry;
import be.mirooz.elitedangerous.dashboard.model.fleetcarrier.CarrierStatus;

import java.util.List;
import java.util.Map;

/**
 * Service d'accès à l'état courant du Fleet Carrier.
 * Encapsule les mises à jour de transactions et de stock.
 */
public class CarrierTradeService {

    private static final String FLEET_CARRIER_TYPE = "FleetCarrier";
    private static final CarrierTradeService INSTANCE = new CarrierTradeService();

    private final CarrierStatus carrierStatus = CarrierStatus.getInstance();

    private CarrierTradeService() {
    }

    public static CarrierTradeService getInstance() {
        return INSTANCE;
    }

    public synchronized void updateCarrierStats(long carrierId, String carrierType, String callsign, String name, int totalCapacity) {
        if (!isFleetCarrierType(carrierType)) {
            return;
        }
        carrierStatus.updateCarrierStats(carrierId, carrierType, callsign, name, totalCapacity);
    }

    public synchronized void updateCarrierLocation(long carrierId, String carrierType, String starSystem, long bodyId) {
        if (!isFleetCarrierType(carrierType)) {
            return;
        }
        carrierStatus.updateCarrierLocation(carrierId, carrierType, starSystem, bodyId);
    }

    public synchronized void recordTradeOrder(CarrierTradeOrderEntry entry) {
        carrierStatus.recordTradeOrder(entry);
    }

    public synchronized void applyMarketStockDelta(String commodity, String commodityLocalised, int delta) {
        carrierStatus.applyMarketStockDelta(commodity, commodityLocalised, delta);
    }

    /**
     * Vrai si l'identifiant (CarrierID du journal, ou MarketID du marché fleet) correspond au fleet carrier connu.
     */
    public synchronized boolean isOwnCarrier(long incomingId) {
        return carrierStatus.isCarrierStatsInitialized()
                && incomingId > 0
                && incomingId == carrierStatus.getCarrierId();
    }

    public synchronized CarrierStatus getCarrierStatus() {
        return carrierStatus;
    }

    public synchronized Map<String, CarrierTradeOrderEntry> getMarketByCommodity() {
        return carrierStatus.getMarketByCommodity();
    }

    public synchronized Map<String, Integer> getStocksByCommodity() {
        return carrierStatus.getStocksByCommodity();
    }

    public synchronized List<String> getPurchaseOrder() {
        return carrierStatus.getPurchaseOrder();
    }

    public synchronized List<String> getSaleOrder() {
        return carrierStatus.getSaleOrder();
    }

    private boolean isFleetCarrierType(String carrierType) {
        return FLEET_CARRIER_TYPE.equalsIgnoreCase(carrierType);
    }
}
