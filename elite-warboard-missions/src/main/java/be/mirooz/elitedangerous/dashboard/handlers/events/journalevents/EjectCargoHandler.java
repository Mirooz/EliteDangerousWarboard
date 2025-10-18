package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.Mineral;
import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.MineralFactory;
import be.mirooz.elitedangerous.dashboard.model.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.service.MiningStatsService;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Optional;

/**
 * Handler pour l'événement EjectCargo du journal Elite Dangerous
 * 
 * Exemple d'événement :
 * {
 *   "timestamp" : "2025-10-20T00:52:42Z",
 *   "event" : "EjectCargo",
 *   "Type" : "alexandrite",
 *   "Count" : 5,
 *   "Abandoned" : true
 * }
 */
public class EjectCargoHandler implements JournalEventHandler {

    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();
    private final MiningStatsService miningStatsService = MiningStatsService.getInstance();
    
    @Override
    public void handle(JsonNode event) {
        try {
            String timestamp = event.get("timestamp").asText();
            String type = event.get("Type").asText();
            int count = event.get("Count").asInt();
            boolean abandoned = event.has("Abandoned") ? event.get("Abandoned").asBoolean() : false;
            
            System.out.printf("📦 EjectCargo: %s x%d (abandoned: %s) at %s%n", type, count, abandoned, timestamp);

            // Vérifier si c'est un minéral
            Optional<Mineral> mineral = MineralFactory.fromMiningRefinedName(type);
            if (mineral.isPresent()) {
                Mineral m = mineral.get();
                
                // Retirer du cargo du commandant
                commanderStatus.getShip().removeCommodity(m, count);
                
                // Si une session de minage est en cours, soustraire des statistiques
                if (miningStatsService.isMiningInProgress()) {
                    miningStatsService.removeRefinedMineral(m, count);
                    System.out.printf("📊 Retiré des statistiques de minage: %s x%d%n", m.getVisibleName(), count);
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du traitement de l'événement EjectCargo: " + e.getMessage());
        }
    }

    @Override
    public String getEventType() {
        return "EjectCargo";
    }
}