package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.registries.combat.MissionsRegistry;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Handler pour l'événement Missions du journal Elite Dangerous
 * 
 * Cet événement contient la liste des missions actives, échouées et complétées.
 * Si le tableau Active est vide, cela signifie qu'il n'y a plus de missions actives.
 * 
 * Exemple d'événement :
 * {
 *   "timestamp" : "2025-10-22T20:29:37Z",
 *   "event" : "Missions",
 *   "Active" : [ ],
 *   "Failed" : [ ],
 *   "Complete" : [ ]
 * }
 */
public class MissionsHandler implements JournalEventHandler {
    
    private final MissionsRegistry missionsRegistry = MissionsRegistry.getInstance();
    
    @Override
    public String getEventType() {
        return "Missions";
    }
    
    @Override
    public void handle(JsonNode event) {
        try {
            // Vérifier si le tableau Active est présent et vide
            if (event.has("Active") && event.get("Active").isArray()) {
                JsonNode activeArray = event.get("Active");
                
                if (activeArray.size() == 0) {
                    // Le tableau Active est vide, vider le globalMissionMap
                    missionsRegistry.setActiveMissionsToFailed();
                    System.out.println("📋 Missions: Tableau Active vide - globalMissionMap vidé");
                } else {
                    System.out.printf("📋 Missions: %d missions actives détectées%n", activeArray.size());
                }
            }
            
            // Optionnel: traiter les missions Failed et Complete si nécessaire
            if (event.has("Failed") && event.get("Failed").isArray()) {
                JsonNode failedArray = event.get("Failed");
                if (failedArray.size() > 0) {
                    System.out.printf("📋 Missions: %d missions échouées%n", failedArray.size());
                }
            }
            
            if (event.has("Complete") && event.get("Complete").isArray()) {
                JsonNode completeArray = event.get("Complete");
                if (completeArray.size() > 0) {
                    System.out.printf("📋 Missions: %d missions complétées%n", completeArray.size());
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du traitement de l'événement Missions: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
