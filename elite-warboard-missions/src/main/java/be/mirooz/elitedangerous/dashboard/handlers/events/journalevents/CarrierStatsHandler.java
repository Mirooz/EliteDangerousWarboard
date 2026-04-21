package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.service.CarrierTradeService;
import be.mirooz.elitedangerous.dashboard.service.listeners.ColonisationNotificationService;
import com.fasterxml.jackson.databind.JsonNode;

public class CarrierStatsHandler implements JournalEventHandler {

    private static final String FLEET_CARRIER_TYPE = "FleetCarrier";

    private final CarrierTradeService carrierTradeService = CarrierTradeService.getInstance();

    @Override
    public String getEventType() {
        return "CarrierStats";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            long carrierId = jsonNode.path("CarrierID").asLong(0L);
            String carrierType = jsonNode.path("CarrierType").asText("");
            String callsign = jsonNode.path("Callsign").asText("");
            String name = jsonNode.path("Name").asText("");
            int totalCapacity = jsonNode.path("SpaceUsage").path("TotalCapacity").asInt(0);

            carrierTradeService.updateCarrierStats(carrierId, carrierType, callsign, name, totalCapacity);
            if (FLEET_CARRIER_TYPE.equalsIgnoreCase(carrierType)) {
                ColonisationNotificationService.getInstance().notifyColonisationDataChanged();
            }

            System.out.println("CarrierStats: " + callsign
                    + " | " + name
                    + " | CarrierID=" + carrierId
                    + " | TotalCapacity=" + totalCapacity);
        } catch (Exception e) {
            System.err.println("Erreur lors du traitement de CarrierStats: " + e.getMessage());
        }
    }
}
