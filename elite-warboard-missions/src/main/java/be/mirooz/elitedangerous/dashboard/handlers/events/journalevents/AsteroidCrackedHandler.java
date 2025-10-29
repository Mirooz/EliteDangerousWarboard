package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.events.ProspectedAsteroid;
import be.mirooz.elitedangerous.dashboard.service.MiningStatsService;
import be.mirooz.elitedangerous.dashboard.service.listeners.MiningEventNotificationService;
import be.mirooz.elitedangerous.dashboard.service.MiningService;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Handler pour l'événement AsteroidCracked du journal Elite Dangerous
 * 
 * Exemple d'événement :
 * {
 *   "timestamp" : "2025-10-22T21:34:44Z",
 *   "event" : "AsteroidCracked",
 *   "Body" : "Puppis Sector DL-Y d106 1 A Ring"
 * }
 */
public class AsteroidCrackedHandler implements JournalEventHandler {
    
    private final MiningService miningService = MiningService.getInstance();
    private final MiningEventNotificationService notificationService = MiningEventNotificationService.getInstance();
    
    @Override
    public void handle(JsonNode event) {
        try {
            String timestamp = event.get("timestamp").asText();
            String body = event.get("Body").asText();
            
            System.out.printf("💥 AsteroidCracked: %s at %s%n", body, timestamp);
            
            // Récupérer le dernier prospecteur
            ProspectedAsteroid lastProspector = miningService.getLastProspector().orElse(null);
            
            if (lastProspector != null && lastProspector.getCoreMineral() != null) {
                System.out.printf("🔍 Prospecteur avec core détecté: %s%n", lastProspector.getCoreMineral().getVisibleName());

                miningService.setCoreSession(true);
                // Notifier tous les listeners de l'événement AsteroidCracked
                notificationService.notifyAsteroidCracked(lastProspector);
            } else {
                System.out.println("ℹ️ Aucun prospecteur avec core trouvé");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du traitement de l'événement AsteroidCracked: " + e.getMessage());
        }
    }


    @Override
    public String getEventType() {
        return "AsteroidCracked";
    }
}
