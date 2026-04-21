package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.service.listeners.ColonisationNotificationService;
import com.fasterxml.jackson.databind.JsonNode;

public class ColonisationSystemClaimHandler implements JournalEventHandler {

    @Override
    public String getEventType() {
        return "ColonisationSystemClaim";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            String starSystem = jsonNode.path("StarSystem").asText("");
            long systemAddress = jsonNode.path("SystemAddress").asLong();
            System.out.println("Colonisation: revendication du système " + starSystem + " (SystemAddress=" + systemAddress + ")");
            ColonisationNotificationService.getInstance().notifyColonisationDataChanged();
        } catch (Exception e) {
            System.err.println("Erreur lors du traitement de ColonisationSystemClaim: " + e.getMessage());
        }
    }
}
