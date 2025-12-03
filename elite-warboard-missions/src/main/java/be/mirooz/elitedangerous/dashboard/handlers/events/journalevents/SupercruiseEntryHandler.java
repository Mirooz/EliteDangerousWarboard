package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.service.MiningStatsService;
import be.mirooz.elitedangerous.dashboard.service.listeners.ExplorationRefreshNotificationService;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Handler pour l'événement SupercruiseEntry du journal Elite Dangerous
 * 
 * Exemple d'événement :
 * {
 *   "timestamp" : "2025-10-09T23:14:25Z",
 *   "event" : "SupercruiseEntry",
 *   "StarSystem" : "Sol",
 *   "Body" : "Earth"
 * }
 */
public class SupercruiseEntryHandler implements JournalEventHandler {

    private final MiningStatsService miningStatsService = MiningStatsService.getInstance();
    private final ExplorationRefreshNotificationService notificationService = ExplorationRefreshNotificationService.getInstance();

    @Override
    public void handle(JsonNode event) {
        try {
            String timestamp = event.get("timestamp").asText();
            String starSystem = event.get("StarSystem").asText();
            String body = event.has("Body") ? event.get("Body").asText() : "Unknown";
            
            System.out.printf("🚀 SupercruiseEntry: %s - %s at %s%n", starSystem, body, timestamp);

            // Terminer la session de minage en cours si elle existe
            if (miningStatsService.isMiningInProgress()) {
                miningStatsService.endCurrentMiningSession(timestamp);
                System.out.println("⛏️ Session de minage terminée (Supercruise Entry)");
            }

            notificationService.notifyBodyFilter(null);
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du traitement de l'événement SupercruiseEntry: " + e.getMessage());
        }
    }

    @Override
    public String getEventType() {
        return "SupercruiseEntry";
    }
}
