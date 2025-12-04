package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.service.listeners.ExplorationRefreshNotificationService;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Handler pour l'événement Disembark du journal Elite Dangerous
 * <p>
 * Exemple d'événement :
 * {
 * "timestamp":"2025-12-04T17:46:26Z",
 * "event":"Disembark",
 * "SRV":false,
 * "Taxi":false,
 * "Multicrew":false,
 * "ID":38,
 * "StarSystem":"Wregoe AB-F d11-72",
 * "SystemAddress":2484479215971,
 * "Body":"Wregoe AB-F d11-72 B 6 c",
 * "BodyID":29,
 * "OnStation":false,
 * "OnPlanet":true
 * }
 */
public class DisembarkHandler implements JournalEventHandler {

    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();
    private final ExplorationRefreshNotificationService notificationService = ExplorationRefreshNotificationService.getInstance();

    @Override
    public void handle(JsonNode event) {
        try {
                commanderStatus.setOnFoot(true);
                System.out.println("🚶 Commande à pied (Disembark, SRV=false)");
                // Notifier le changement d'état
                notificationService.notifyOnFootStateChanged(true);
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du traitement de l'événement Disembark: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public String getEventType() {
        return "Disembark";
    }
}

