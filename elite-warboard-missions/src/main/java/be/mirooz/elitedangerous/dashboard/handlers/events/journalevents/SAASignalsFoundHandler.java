package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.exploration.BiologicalSignalProcessor;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Handler pour l'événement SAASignalsFound du journal Elite Dangerous
 * <p>
 * Traite les signaux détectés par le scanner de surface (SAA - Surface Area Analysis).
 * Exemple d'événement :
 * {
 *   "timestamp" : "2025-11-04T21:53:20Z",
 *   "event" : "SAASignalsFound",
 *   "BodyName" : "Swoilz UL-S b22-2 7",
 *   "SystemAddress" : 5079468746433,
 *   "BodyID" : 10,
 *   "Signals" : [ {
 *     "Type" : "$SAA_SignalType_Biological;",
 *     "Type_Localised" : "Biologique",
 *     "Count" : 1
 *   } ],
 *   "Genuses" : [ {
 *     "Genus" : "$Codex_Ent_Bacterial_Genus_Name;",
 *     "Genus_Localised" : "Bacterium"
 *   } ]
 * }
 */
public class SAASignalsFoundHandler implements JournalEventHandler {

    private final BiologicalSignalProcessor signalProcessor = BiologicalSignalProcessor.getInstance();

    @Override
    public String getEventType() {
        return "SAASignalsFound";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            String bodyName = jsonNode.path("BodyName").asText();
            int bodyID = jsonNode.path("BodyID").asInt();
            long systemAddress = jsonNode.path("SystemAddress").asLong();

            // Vérifier si l'événement contient des signaux biologiques
            if (jsonNode.has("Signals") && jsonNode.get("Signals").isArray()) {
                jsonNode.get("Signals").forEach(signal -> {
                    String signalType = signal.path("Type").asText();
                    
                    // Si c'est un signal biologique, extraire les genuses et l'ajouter au processeur
                    if ("$SAA_SignalType_Biological;".equals(signalType)) {
                        int count = signal.path("Count").asInt(1);
                        
                        // Extraire les genuses
                        List<String> genuses = new ArrayList<>();
                        if (jsonNode.has("Genuses") && jsonNode.get("Genuses").isArray()) {
                            jsonNode.get("Genuses").forEach(genus -> {
                                String genusLocalised = genus.path("Genus_Localised").asText();
                                if (!genusLocalised.isEmpty()) {
                                    genuses.add(genusLocalised);
                                }
                            });
                        }
                        
                        // Ajouter le signal avec niveau 2 et les genuses
                        signalProcessor.addPendingBiologicalSignal(bodyID, systemAddress, bodyName, count, 2, genuses);
                        System.out.printf("🌱 Signal biologique SAA détecté: %s (BodyID: %d, Count: %d, Genuses: %s)%n", 
                                bodyName, bodyID, count, genuses);
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du traitement de l'événement SAASignalsFound: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

