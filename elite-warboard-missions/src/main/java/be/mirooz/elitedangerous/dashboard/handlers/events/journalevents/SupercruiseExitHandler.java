package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.service.MiningStatsService;
import be.mirooz.elitedangerous.dashboard.model.registries.commander.CommanderStatus;
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
    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();
    
    @Override
    public void handle(JsonNode event) {
        try {
            String timestamp = event.path("timestamp").asText();
            String starSystem = event.path("StarSystem").asText("");
            String body = event.path("Body").asText("");
            String bodyType = event.path("BodyType").asText("");
            Long bodyId = event.has("BodyID") ? event.path("BodyID").asLong() : null;
            
            System.out.printf("🚀 SupercruiseExit: %s - %s (%s) at %s%n", starSystem, body, bodyType, timestamp);
            if (!starSystem.isBlank()) {
                commanderStatus.setCurrentStarSystem(starSystem);
            }
            commanderStatus.setCurrentBody(body.isBlank() ? null : body, bodyId);

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
