package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.model.registries.PlaneteRegistry;
import be.mirooz.elitedangerous.dashboard.service.MiningStatsService;
import com.fasterxml.jackson.databind.JsonNode;

public class FSDJumpHandler implements JournalEventHandler {

    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();
    private final MiningStatsService miningStatsService = MiningStatsService.getInstance();
    private final PlaneteRegistry planeteRegistry = PlaneteRegistry.getInstance();

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
                
                // Terminer la session de minage en cours si elle existe
                if (miningStatsService.isMiningInProgress()) {
                    String timestamp = jsonNode.has("timestamp") ? jsonNode.get("timestamp").asText() : null;
                    miningStatsService.endCurrentMiningSession(timestamp);
                    System.out.println("⛏️ Session de minage terminée (FSD Jump)");
                }
                planeteRegistry.clear();
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de Location: " + e.getMessage());
        }
    }
}
