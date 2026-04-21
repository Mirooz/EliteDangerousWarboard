package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.backend.generated.model.CapiFleetCarrierProxyResponse;
import be.mirooz.elitedangerous.commons.lib.models.commodities.ICommodity;
import be.mirooz.elitedangerous.dashboard.model.colonisation.CarrierTradeOrderEntry;
import be.mirooz.elitedangerous.dashboard.model.registries.fleetcarrier.CarrierStatus;
import be.mirooz.elitedangerous.dashboard.service.listeners.ColonisationNotificationService;

import java.time.Duration;
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

    public synchronized void applyMarketStockDelta(
            String commodity, String commodityLocalised, int delta, String eventTimestamp) {
        carrierStatus.applyMarketStockDelta(commodity, commodityLocalised, delta, eventTimestamp);
    }

    public synchronized boolean hasRecentJournalCarrierActivity(Duration maxAge) {
        return carrierStatus.hasRecentJournalCarrierActivity(maxAge);
    }

    public synchronized void applyFleetCarrierCapiSnapshot(CapiFleetCarrierProxyResponse capiData) {
        carrierStatus.applyCapiFleetCarrierPayload(capiData);
        ColonisationNotificationService.getInstance().notifyColonisationDataChanged();
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

    public synchronized Map<ICommodity, CarrierTradeOrderEntry> getMarketByCommodity() {
        return carrierStatus.getMarketByCommodity();
    }

    public synchronized Map<ICommodity, Integer> getStocksByCommodity() {
        return carrierStatus.getStocksByCommodity();
    }

    public synchronized List<ICommodity> getPurchaseOrder() {
        return carrierStatus.getPurchaseOrder();
    }

    public synchronized List<ICommodity> getSaleOrder() {
        return carrierStatus.getSaleOrder();
    }

    private boolean isFleetCarrierType(String carrierType) {
        return FLEET_CARRIER_TYPE.equalsIgnoreCase(carrierType);
    }
}
