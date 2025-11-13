package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.exploration.BiologicalSignalProcessor;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Handler pour l'événement FSSBodySignals du journal Elite Dangerous
 * <p>
 * Traite les signaux détectés sur les corps célestes, notamment les signaux biologiques.
 * Exemple d'événement :
 * {
 *   "timestamp" : "2025-11-04T22:10:42Z",
 *   "event" : "FSSBodySignals",
 *   "BodyName" : "Swoilz BI-H b28-0 A 2",
 *   "BodyID" : 13,
 *   "SystemAddress" : 682227345137,
 *   "Signals" : [ {
 *     "Type" : "$SAA_SignalType_Biological;",
 *     "Type_Localised" : "Biologique",
 *     "Count" : 1
 *   } ]
 * }
 */
public class FSSBodySignalsHandler implements JournalEventHandler {

    private final BiologicalSignalProcessor signalProcessor = BiologicalSignalProcessor.getInstance();

    @Override
    public String getEventType() {
        return "FSSBodySignals";
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
                    
                    // Si c'est un signal biologique, l'ajouter au processeur
                    if ("$SAA_SignalType_Biological;".equals(signalType)) {
                        int count = signal.path("Count").asInt(1);
                        signalProcessor.addPendingBiologicalSignal(bodyID, systemAddress, bodyName, count);
                        System.out.printf("🌱 Signal biologique détecté: %s (BodyID: %d, Count: %d)%n", 
                                bodyName, bodyID, count);
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du traitement de l'événement FSSBodySignals: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

