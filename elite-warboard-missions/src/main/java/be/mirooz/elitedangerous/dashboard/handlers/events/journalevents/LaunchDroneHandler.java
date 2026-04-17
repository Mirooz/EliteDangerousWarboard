package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.registries.commander.CommanderStatus;
import com.fasterxml.jackson.databind.JsonNode;

import static be.mirooz.elitedangerous.commons.lib.models.commodities.LimpetType.LIMPET;

/**
 * Handler pour l'événement LaunchDrone du journal Elite Dangerous
 * 
 * Exemple d'événement :
 * {
 *   "timestamp" : "2025-10-09T23:05:45Z",
 *   "event" : "LaunchDrone",
 *   "Type" : "Prospector"
 * }
 */
public class LaunchDroneHandler implements JournalEventHandler {

    CommanderStatus commanderStatus = CommanderStatus.getInstance();

    @Override
    public void handle(JsonNode event) {
        try {
            String timestamp = event.get("timestamp").asText();
            String type = event.get("Type").asText();
            System.out.printf("🚁 LaunchDrone: %s at %s%n", type, timestamp);
            
            // Utiliser la factory pour identifier le type de limpet
            commanderStatus.getShip().removeCommodity(LIMPET);
            
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du traitement de l'événement LaunchDrone: " + e.getMessage());
        }
    }

    @Override
    public String getEventType() {
        return "LaunchDrone";
    }
}
