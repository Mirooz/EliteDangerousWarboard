package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.service.CarrierTradeService;
import com.fasterxml.jackson.databind.JsonNode;

public class CarrierLocationHandler implements JournalEventHandler {

    private final CarrierTradeService carrierTradeService = CarrierTradeService.getInstance();

    @Override
    public String getEventType() {
        return "CarrierLocation";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            long carrierId = jsonNode.path("CarrierID").asLong(0L);
            String carrierType = jsonNode.path("CarrierType").asText("");
            String starSystem = jsonNode.path("StarSystem").asText("");
            long bodyId = jsonNode.path("BodyID").asLong(-1L);

            carrierTradeService.updateCarrierLocation(carrierId, carrierType, starSystem, bodyId);

            System.out.println("CarrierLocation: CarrierID=" + carrierId
                    + " | " + starSystem
                    + " | BodyID=" + bodyId);
        } catch (Exception e) {
            System.err.println("Erreur lors du traitement de CarrierLocation: " + e.getMessage());
        }
    }
}
