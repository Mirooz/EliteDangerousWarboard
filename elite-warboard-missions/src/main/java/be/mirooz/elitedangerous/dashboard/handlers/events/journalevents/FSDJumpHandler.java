package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.ExplorationDataSaleRegistry;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.PlaneteRegistry;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.SystemVisitedRegistry;
import be.mirooz.elitedangerous.dashboard.service.DirectionReaderService;
import be.mirooz.elitedangerous.dashboard.service.ExplorationService;
import be.mirooz.elitedangerous.dashboard.service.MiningStatsService;
import com.fasterxml.jackson.databind.JsonNode;

public class FSDJumpHandler implements JournalEventHandler {

    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();
    private final MiningStatsService miningStatsService = MiningStatsService.getInstance();
    private final PlaneteRegistry planeteRegistry = PlaneteRegistry.getInstance();

    private final DirectionReaderService directionReaderService = DirectionReaderService.getInstance();
    @Override
    public String getEventType() {
        return "FSDJump";
    }

    @Override
    public void handle(JsonNode jsonNode) {

        try {
            if (jsonNode.has("StarSystem")) {
                String starSystem = jsonNode.get("StarSystem").asText();
                commanderStatus.setCurrentStarSystem(starSystem);
                System.out.println("FSD Jump detected to - " + starSystem);
                String timestamp = jsonNode.has("timestamp") ? jsonNode.get("timestamp").asText() : null;
                // Terminer la session de minage en cours si elle existe
                if (miningStatsService.isMiningInProgress()) {
                    miningStatsService.endCurrentMiningSession(timestamp);
                    System.out.println("⛏️ Session de minage terminée (FSD Jump)");
                }
                //Ajoute l'ancien dans les visited
                if (planeteRegistry.getCurrentStarSystem()!= null) {
                    ExplorationService.getInstance().addOrUpdateSystem(planeteRegistry.getCurrentStarSystem(), planeteRegistry.getAllPlanetes(), timestamp);
                    ExplorationDataSaleRegistry.getInstance().addToOnHold(SystemVisitedRegistry.getInstance().getSystem(planeteRegistry.getCurrentStarSystem()));
                }
                planeteRegistry.clear();
                planeteRegistry.setCurrentStarSystem(starSystem);
                if (SystemVisitedRegistry.getInstance().getSystems().containsKey(starSystem)){
                    planeteRegistry.setAllPlanetes(SystemVisitedRegistry.getInstance().getSystems().get(starSystem).getCelesteBodies());
                }
                directionReaderService.stopWatchingStatusFile();
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de Location: " + e.getMessage());
        }
    }
}
