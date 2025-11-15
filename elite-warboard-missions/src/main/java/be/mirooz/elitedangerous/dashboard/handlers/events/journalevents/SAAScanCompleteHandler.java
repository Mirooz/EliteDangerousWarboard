package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.exploration.ACelesteBody;
import be.mirooz.elitedangerous.dashboard.model.exploration.PlaneteDetail;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.PlaneteRegistry;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Optional;

/**
 * Handler pour l'événement SAAScanComplete du journal Elite Dangerous
 * <p>
 * Traite la complétion d'un scan de surface (DSS - Detailed Surface Scanner).
 * Exemple d'événement :
 * {
 *   "timestamp" : "2025-11-04T21:53:20Z",
 *   "event" : "SAAScanComplete",
 *   "BodyName" : "Swoilz UL-S b22-2 7",
 *   "SystemAddress" : 5079468746433,
 *   "BodyID" : 10,
 *   "ProbesUsed" : 5,
 *   "EfficiencyTarget" : 6
 * }
 */
public class SAAScanCompleteHandler implements JournalEventHandler {

    private final PlaneteRegistry planeteRegistry = PlaneteRegistry.getInstance();

    @Override
    public String getEventType() {
        return "SAAScanComplete";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            String bodyName = jsonNode.path("BodyName").asText();
            int bodyID = jsonNode.path("BodyID").asInt();
            long systemAddress = jsonNode.path("SystemAddress").asLong();
            int probesUsed = jsonNode.path("ProbesUsed").asInt(0);
            int efficiencyTarget = jsonNode.path("EfficiencyTarget").asInt(0);

            // Trouver le corps céleste dans le registre via BodyID
            Optional<ACelesteBody> bodyOpt = planeteRegistry.getByBodyID(bodyID);

            if (bodyOpt.isPresent()) {
                ACelesteBody body = bodyOpt.get();
                // Marquer le corps comme mappé
                body.setMapped(true);
                body.setEfficiencyTargetMap(efficiencyTarget-probesUsed>=0);
                System.out.printf("🗺️ Scan de surface complété: %s (BodyID: %d, ProbesUsed: %d, EfficiencyTarget: %d)%n",
                        bodyName, bodyID, probesUsed, efficiencyTarget);
            } else {
                System.out.printf("⚠️ Corps céleste non trouvé dans le registry pour BodyID: %d (SAAScanComplete)%n",
                        bodyID);
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du traitement de l'événement SAAScanComplete: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

