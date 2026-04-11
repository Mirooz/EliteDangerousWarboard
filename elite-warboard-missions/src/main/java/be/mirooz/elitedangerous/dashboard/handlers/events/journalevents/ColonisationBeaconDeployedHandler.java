package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.registries.colonisation.ColonisationRegistry;
import com.fasterxml.jackson.databind.JsonNode;

public class ColonisationBeaconDeployedHandler implements JournalEventHandler {

    private final ColonisationRegistry colonisationRegistry = ColonisationRegistry.getInstance();

    @Override
    public String getEventType() {
        return "ColonisationBeaconDeployed";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            String timestamp = jsonNode.path("timestamp").asText("");
            colonisationRegistry.recordBeaconDeployed(timestamp);
            System.out.println("Colonisation: balise de colonisation déployée" + (timestamp.isEmpty() ? "" : " (" + timestamp + ")"));
        } catch (Exception e) {
            System.err.println("Erreur lors du traitement de ColonisationBeaconDeployed: " + e.getMessage());
        }
    }
}
