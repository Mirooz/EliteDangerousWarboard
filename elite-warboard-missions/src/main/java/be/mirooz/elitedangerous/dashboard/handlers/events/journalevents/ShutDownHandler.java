package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.service.MiningStatsService;
import com.fasterxml.jackson.databind.JsonNode;

public class ShutDownHandler implements JournalEventHandler {

    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();
    private final MiningStatsService miningStatsService = MiningStatsService.getInstance();

    @Override
    public String getEventType() {
        return "Shutdown";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            String timestamp = jsonNode.get("timestamp").asText();
            
            commanderStatus.setOnline(false);
            System.out.println("Commandant " + commanderStatus.getCommanderName() + " is offline");
            
            // Suspendre la session de minage en cours si elle existe
            if (miningStatsService.isMiningInProgress()) {
                miningStatsService.suspendCurrentMiningSession(timestamp);
                System.out.println("⏸️ Session de minage suspendue (Shutdown)");
            }

        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de Shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
