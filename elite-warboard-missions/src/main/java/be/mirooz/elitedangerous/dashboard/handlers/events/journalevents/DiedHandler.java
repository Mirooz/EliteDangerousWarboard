package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.model.registries.DestroyedShipsRegistery;
import be.mirooz.elitedangerous.dashboard.service.MiningStatsService;
import com.fasterxml.jackson.databind.JsonNode;

public class DiedHandler implements JournalEventHandler {
    private final DestroyedShipsRegistery destroyedShipsRegistery = DestroyedShipsRegistery.getInstance();
    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();

    private final MiningStatsService miningStatsService = MiningStatsService.getInstance();
    @Override
    public String getEventType() {
        return "Died";
    }

    @Override
    public void handle(JsonNode event) {
        try {
            destroyedShipsRegistery.clearBounty();
            commanderStatus.getShip().resetCargo();
            // Terminer la session de minage en cours si elle existe
            if (miningStatsService.isMiningInProgress()) {
                String timestamp = event.get("timestamp").asText();
                miningStatsService.endCurrentMiningSession(timestamp);
                System.out.println("⛏️ Session de minage terminée (Mort)");
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de Died: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
