package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.commander.CommanderStatus;
import com.fasterxml.jackson.databind.JsonNode;

import static be.mirooz.elitedangerous.commons.lib.models.commodities.LimpetType.LIMPET;

/**
 * Handler pour l'événement SellDrones du journal Elite Dangerous
 * <p>
 * Exemple d'événement :
 * {
 * "timestamp" : "2025-10-14T22:11:43Z",
 * "event" : "SellDrones",
 * "Type" : "Drones",
 * "Count" : 19,
 * "SellPrice" : 95,
 * "TotalSale" : 1805
 * }
 */
public class SellDronesHandler implements JournalEventHandler {

    CommanderStatus commanderStatus = CommanderStatus.getInstance();

    @Override
    public void handle(JsonNode event) {
        try {
            String timestamp = event.get("timestamp").asText();
            String type = event.get("Type").asText();
            int count = event.get("Count").asInt();
            long sellPrice = event.get("SellPrice").asLong();
            long totalSale = event.get("TotalSale").asLong();

            System.out.printf("💰 SellDrones: %d x %s for %d Cr each (Total: %d Cr) at %s%n",
                    count, type, sellPrice, totalSale, timestamp);

            // Retirer les limpets du cargo
            commanderStatus.getShip().removeCommodity(LIMPET, count);


        } catch (Exception e) {
            System.err.println("❌ Erreur lors du traitement de l'événement SellDrones: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public String getEventType() {
        return "SellDrones";
    }
}
