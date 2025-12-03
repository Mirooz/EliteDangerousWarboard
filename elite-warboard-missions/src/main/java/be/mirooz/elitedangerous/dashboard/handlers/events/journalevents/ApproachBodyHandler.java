package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.biologic.BioSpecies;
import be.mirooz.elitedangerous.dashboard.model.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.model.exploration.ACelesteBody;
import be.mirooz.elitedangerous.dashboard.model.exploration.PlaneteDetail;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.PlaneteRegistry;
import be.mirooz.elitedangerous.dashboard.service.DirectionReaderService;
import be.mirooz.elitedangerous.dashboard.service.ExplorationService;
import be.mirooz.elitedangerous.dashboard.service.listeners.ExplorationRefreshNotificationService;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Optional;

import static be.mirooz.elitedangerous.commons.lib.models.commodities.LimpetType.LIMPET;

/**
 * Handler pour l'événement ApproachBody du journal Elite Dangerous
 */
public class ApproachBodyHandler implements JournalEventHandler {

    DirectionReaderService directionReaderService = DirectionReaderService.getInstance();
    ExplorationService explorationService = ExplorationService.getInstance();
    private final PlaneteRegistry planeteRegistry = PlaneteRegistry.getInstance();
    private final ExplorationRefreshNotificationService notificationService = ExplorationRefreshNotificationService.getInstance();

    @Override
    public void handle(JsonNode event) {
        try {
            String timestamp = event.get("timestamp").asText();
            String body = event.get("Body").asText();
            Integer bodyID = event.get("BodyID").asInt();

            System.out.printf("[%s] Approaching body %s, %d \n",
                    timestamp, body, bodyID);
            // Vérifier si la planète a au moins 1 exobio non collecté
            Optional<ACelesteBody> bodyOpt = planeteRegistry.getByBodyID(bodyID);
            if (bodyOpt.isPresent() && bodyOpt.get() instanceof PlaneteDetail planet) {
                // Vérifier s'il y a au moins une espèce non collectée
                boolean hasUncollectedExobio = false;
                if (planet.getNumSpeciesDetected() != null && planet.getNumSpeciesDetected() > planet.getConfirmedSpecies().stream().filter(BioSpecies::isCollected).count()) {
                    hasUncollectedExobio = true;
                }


                // Si la planète a des exobio non collectés, filtrer la liste pour n'afficher que cette planète
                if (hasUncollectedExobio) {
                    notificationService.notifyBodyFilter(bodyID);
                } else {
                    // Sinon, désactiver le filtre
                    notificationService.notifyBodyFilter(null);
                }
            } else {
                // Si ce n'est pas une planète ou si elle n'est pas trouvée, désactiver le filtre
                notificationService.notifyBodyFilter(null);
            }
            
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
