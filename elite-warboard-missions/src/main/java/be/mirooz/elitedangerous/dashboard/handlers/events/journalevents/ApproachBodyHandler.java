package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.service.DirectionReaderService;
import be.mirooz.elitedangerous.dashboard.service.ExplorationService;
import com.fasterxml.jackson.databind.JsonNode;

import static be.mirooz.elitedangerous.commons.lib.models.commodities.LimpetType.LIMPET;

/**
 * Handler pour l'événement BuyDrones du journal Elite Dangerous
 * <p>
 * Exemple d'événement :
 * {
 * "timestamp" : "2025-10-14T22:11:31Z",
 * "event" : "BuyDrones",
 * "Type" : "Drones",
 * "Count" : 11,
 * "BuyPrice" : 119,
 * "TotalCost" : 1309
 * }
 */
public class ApproachBodyHandler implements JournalEventHandler {

    DirectionReaderService directionReaderService = DirectionReaderService.getInstance();
    ExplorationService explorationService = ExplorationService.getInstance();
    @Override
    public void handle(JsonNode event) {
        try {
            String timestamp = event.get("timestamp").asText();
            String body = event.get("Body").asText();
            Integer bodyID = event.get("BodyID").asInt();

            System.out.printf("[%s] Approaching body %s, %d \n",
                    timestamp,body,bodyID);
           /* if (explorationService.isBiologicalAnalysisInProgress() && explorationService.isCurrentBiologicalAnalysisOnCurrentPlanet(body)) {
                directionReaderService.startWatchingStatusFile(explorationService.getCurrentAnalysisPlanet().getRadius());
            }*/
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du traitement de l'événement ApproachBody: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public String getEventType() {
        return "ApproachBody";
    }
}
