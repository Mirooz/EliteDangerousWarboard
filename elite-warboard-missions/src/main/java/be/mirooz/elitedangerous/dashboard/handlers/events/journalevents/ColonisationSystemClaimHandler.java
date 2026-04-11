package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.registries.colonisation.ColonisationRegistry;
import com.fasterxml.jackson.databind.JsonNode;

public class ColonisationSystemClaimHandler implements JournalEventHandler {

    private final ColonisationRegistry colonisationRegistry = ColonisationRegistry.getInstance();

    @Override
    public String getEventType() {
        return "ColonisationSystemClaim";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            String starSystem = jsonNode.path("StarSystem").asText("");
            long systemAddress = jsonNode.path("SystemAddress").asLong();
            colonisationRegistry.recordClaim(starSystem, systemAddress);
            System.out.println("Colonisation: revendication du système " + starSystem + " (SystemAddress=" + systemAddress + ")");
        } catch (Exception e) {
            System.err.println("Erreur lors du traitement de ColonisationSystemClaim: " + e.getMessage());
        }
    }
}
