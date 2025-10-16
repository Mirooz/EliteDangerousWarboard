package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.commander.CommanderStatus;
import be.mirooz.elitedangerous.commons.lib.models.commodities.ICommodityFactory;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Handler pour l'événement EjectCargo du journal Elite Dangerous
 * 
 * Exemple d'événement :
 * {
 *   "timestamp" : "2025-10-09T23:24:03Z",
 *   "event" : "EjectCargo",
 *   "Type" : "grandidierite",
 *   "Type_Localised" : "Grandidiérite",
 *   "Count" : 16,
 *   "Abandoned" : true
 * }
 */
public class EjectCargoHandler implements JournalEventHandler {

    CommanderStatus commanderStatus = CommanderStatus.getInstance();

    @Override
    public void handle(JsonNode event) {
        try {
            String timestamp = event.get("timestamp").asText();
            String type = event.get("Type").asText();
            String typeLocalised = event.has("Type_Localised") ? event.get("Type_Localised").asText() : type;
            int count = event.get("Count").asInt();
            boolean abandoned = event.has("Abandoned") && event.get("Abandoned").asBoolean();
            
            System.out.printf("📦 EjectCargo: %s (%s) x%d %s at %s%n", 
                typeLocalised, type, count, abandoned ? "(Abandoned)" : "", timestamp);
            
            // Retirer la commodité du cargo du vaisseau
            ICommodityFactory.ofByCargoJson(type)
                    .ifPresent(commodity -> commanderStatus.getShip().removeCommodity(commodity, count));
            
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du traitement de l'événement EjectCargo: " + e.getMessage());
        }
    }

    @Override
    public String getEventType() {
        return "EjectCargo";
    }
}
