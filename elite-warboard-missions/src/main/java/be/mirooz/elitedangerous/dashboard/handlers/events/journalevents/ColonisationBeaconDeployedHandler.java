package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.colonisation.ColonisationJournalContext;
import be.mirooz.elitedangerous.dashboard.model.registries.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.service.ColonisationService;
import be.mirooz.elitedangerous.dashboard.service.listeners.ColonisationNotificationService;
import com.fasterxml.jackson.databind.JsonNode;

public class ColonisationBeaconDeployedHandler implements JournalEventHandler {

    private final ColonisationService colonisationService = ColonisationService.getInstance();
    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();

    @Override
    public String getEventType() {
        return "ColonisationBeaconDeployed";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            String timestamp = jsonNode.path("timestamp").asText("");
            String starSystem = ColonisationJournalContext.resolveStarSystem(jsonNode, commanderStatus);
            colonisationService.recordArchitectBeaconDeployed(starSystem);
            ColonisationNotificationService.getInstance().notifyColonisationDataChanged();
            System.out.println("Colonisation: balise de colonisation déployée"
                    + (starSystem.isEmpty() ? "" : " dans « " + starSystem + " »")
                    + (timestamp.isEmpty() ? "" : " (" + timestamp + ")"));
        } catch (Exception e) {
            System.err.println("Erreur lors du traitement de ColonisationBeaconDeployed: " + e.getMessage());
        }
    }
}
