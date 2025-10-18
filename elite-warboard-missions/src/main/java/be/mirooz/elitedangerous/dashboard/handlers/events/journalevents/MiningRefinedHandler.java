package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.service.MiningStatsService;
import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.Mineral;
import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.MineralFactory;
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

    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();
    private final MiningStatsService miningStatsService = MiningStatsService.getInstance();
    
    @Override
    public void handle(JsonNode event) {
        try {
            String timestamp = event.get("timestamp").asText();
            String type = event.get("Type").asText();
            String typeLocalised = event.has("Type_Localised") ? event.get("Type_Localised").asText() : type;
            
            System.out.printf("⛏️ MiningRefined: %s (%s) at %s%n", typeLocalised, type, timestamp);

            Optional<Mineral> mineral = MineralFactory.fromMiningRefinedName(type);
            if (mineral.isPresent()) {
                Mineral m = mineral.get();
                commanderStatus.getShip().addCommodity(m);
                
                // Ajouter aux statistiques de minage si une session est en cours
                if (miningStatsService.isMiningInProgress()) {
                    miningStatsService.addRefinedMineral(m, 1, timestamp);
                    System.out.printf("📊 Ajouté aux statistiques de minage: %s%n", m.getVisibleName());
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du traitement de l'événement MiningRefined: " + e.getMessage());
        }
    }

    @Override
    public String getEventType() {
        return "MiningRefined";
    }
}
