package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.CommanderStatus;
import be.mirooz.elitedangerous.lib.inara.model.commodities.LimpetType;
import com.fasterxml.jackson.databind.JsonNode;

import static be.mirooz.elitedangerous.lib.inara.model.commodities.LimpetType.LIMPET;

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
public class BuyDronesHandler implements JournalEventHandler {

    CommanderStatus commanderStatus = CommanderStatus.getInstance();

    @Override
    public void handle(JsonNode event) {
        try {
            String timestamp = event.get("timestamp").asText();
            String type = event.get("Type").asText();
            int count = event.get("Count").asInt();
            long buyPrice = event.get("BuyPrice").asLong();
            long totalCost = event.get("TotalCost").asLong();

            System.out.printf("🛒 BuyDrones: %d x %s for %d Cr each (Total: %d Cr) at %s%n",
                    count, type, buyPrice, totalCost, timestamp);

            // Ajouter les limpets au cargo
            commanderStatus.getShip().addCommodity(LIMPET, count);


        } catch (Exception e) {
            System.err.println("❌ Erreur lors du traitement de l'événement BuyDrones: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public String getEventType() {
        return "BuyDrones";
    }
}
