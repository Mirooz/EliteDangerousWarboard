package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.CommanderStatus;
import be.mirooz.elitedangerous.lib.inara.model.commodities.ICommodityFactory;
import be.mirooz.elitedangerous.lib.inara.model.commodities.minerals.Mineral;
import be.mirooz.elitedangerous.lib.inara.model.commodities.minerals.MineralFactory;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Optional;

/**
 * Handler pour l'événement MiningRefined du journal Elite Dangerous
 * 
 * Exemple d'événement :
 * {
 *   "timestamp" : "2025-10-09T23:14:25Z",
 *   "event" : "MiningRefined",
 *   "Type" : "$grandidierite_name;",
 *   "Type_Localised" : "Grandidiérite"
 * }
 */
public class MiningRefinedHandler implements JournalEventHandler {

    CommanderStatus commanderStatus = CommanderStatus.getInstance();
    @Override
    public void handle(JsonNode event) {
        try {
            String timestamp = event.get("timestamp").asText();
            String type = event.get("Type").asText();
            String typeLocalised = event.has("Type_Localised") ? event.get("Type_Localised").asText() : type;
            
            System.out.printf("⛏️ MiningRefined: %s (%s) at %s%n", typeLocalised, type, timestamp);

            Optional<Mineral> mineral = MineralFactory.fromMiningRefinedName(type);
            mineral.ifPresent(m -> commanderStatus.getShip().addCommodity(m));
            
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du traitement de l'événement MiningRefined: " + e.getMessage());
        }
    }

    @Override
    public String getEventType() {
        return "MiningRefined";
    }
}
