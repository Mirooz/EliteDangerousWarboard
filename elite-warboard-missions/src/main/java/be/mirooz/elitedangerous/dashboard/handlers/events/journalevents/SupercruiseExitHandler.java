package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.service.MiningStatsService;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Handler pour l'événement SupercruiseExit du journal Elite Dangerous
 * 
 * Exemple d'événement :
 * {
 *   "timestamp" : "2025-10-09T23:14:25Z",
 *   "event" : "SupercruiseExit",
 *   "StarSystem" : "Sol",
 *   "Body" : "Earth",
 *   "BodyType" : "Planet",
 *   "BodyID" : 1
 * }
 */
public class SupercruiseExitHandler implements JournalEventHandler {
    
    @Override
    public void handle(JsonNode event) {
        try {
            String timestamp = event.get("timestamp").asText();
            String starSystem = event.get("StarSystem").asText();
            String body = event.get("Body").asText();
            String bodyType = event.get("BodyType").asText();
            
            System.out.printf("🚀 SupercruiseExit: %s - %s (%s) at %s%n", starSystem, body, bodyType, timestamp);

            // Si on sort du supercruise vers un anneau planétaire, démarrer une session de minage
            if ("PlanetaryRing".equals(bodyType)) {
                String ringName = body; // Le nom du corps est généralement le nom de l'anneau
                MiningStatsService.getInstance().startMiningSession(starSystem, body, ringName, timestamp);
                System.out.printf("⛏️ Session de minage démarrée: %s - %s%n", starSystem, ringName);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du traitement de l'événement SupercruiseExit: " + e.getMessage());
        }
    }

    @Override
    public String getEventType() {
        return "SupercruiseExit";
    }
}
