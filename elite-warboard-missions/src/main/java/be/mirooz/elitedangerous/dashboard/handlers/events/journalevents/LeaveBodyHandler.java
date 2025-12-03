package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.service.DirectionReaderService;
import be.mirooz.elitedangerous.dashboard.service.ExplorationService;
import be.mirooz.elitedangerous.dashboard.service.listeners.ExplorationRefreshNotificationService;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Handler pour l'événement BuyDrones du journal Elite Dangerous
 * <p>
 * Exemple d'événement :
 * {
 * "timestamp" : "2025-10-14T22:11:31Z",
 * "event" : "BuyDrones",
 * "Type" : "Drones",
 * "Count" : 11,
 * "BuyPrice" : 119,
 * "TotalCost" : 1309
 * }
 */
public class LeaveBodyHandler implements JournalEventHandler {

    DirectionReaderService directionReaderService = DirectionReaderService.getInstance();
    private final ExplorationRefreshNotificationService notificationService = ExplorationRefreshNotificationService.getInstance();

    @Override
    public void handle(JsonNode event) {
        try {
            String timestamp = event.get("timestamp").asText();
            String body = event.get("Body").asText();
            Integer bodyID = event.get("BodyID").asInt();

            System.out.printf("[%s] Leaving body %s, %d \n",
                    timestamp, body, bodyID);
            directionReaderService.stopWatchingStatusFile();
            
            // Désactiver le filtre quand on quitte un corps
            notificationService.notifyBodyFilter(null);

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du traitement de l'événement LeaveBody: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public String getEventType() {
        return "LeaveBody";
    }
}
