package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.exploration.PlaneteDetail;
import be.mirooz.elitedangerous.dashboard.model.exploration.ScanOrganicData;
import be.mirooz.elitedangerous.dashboard.model.registries.PlaneteRegistry;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Optional;

/**
 * Handler pour l'événement ScanOrganic du journal Elite Dangerous
 * <p>
 * Traite les scans organiques effectués sur les espèces biologiques.
 * Exemple d'événement :
 * {
 * "timestamp" : "2025-11-04T23:17:01Z",
 * "event" : "ScanOrganic",
 * "ScanType" : "Analyse",
 * "Genus" : "$Codex_Ent_Osseus_Genus_Name;",
 * "Genus_Localised" : "Osseus",
 * "Species" : "$Codex_Ent_Osseus_03_Name;",
 * "Species_Localised" : "Osseus Spiralis",
 * "Variant" : "$Codex_Ent_Osseus_03_K_Name;",
 * "Variant_Localised" : "Osseus Spiralis - Indigo",
 * "WasLogged" : false,
 * "SystemAddress" : 360273548178,
 * "Body" : 20
 * }
 */
public class ScanOrganicHandler implements JournalEventHandler {

    private final PlaneteRegistry planeteRegistry = PlaneteRegistry.getInstance();

    @Override
    public String getEventType() {
        return "ScanOrganic";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            // Créer l'objet ScanOrganicData depuis le JSON
            ScanOrganicData scanOrganicData = ScanOrganicData.builder()
                    .timestamp(jsonNode.path("timestamp").asText())
                    .scanType(jsonNode.path("ScanType").asText())
                    .genus(jsonNode.path("Genus").asText())
                    .genusLocalised(jsonNode.path("Genus_Localised").asText())
                    .species(jsonNode.path("Species").asText())
                    .speciesLocalised(jsonNode.path("Species_Localised").asText())
                    .variant(jsonNode.path("Variant").asText())
                    .variantLocalised(jsonNode.path("Variant_Localised").asText())
                    .wasLogged(jsonNode.path("WasLogged").asBoolean(false))
                    .systemAddress(jsonNode.path("SystemAddress").asLong())
                    .body(jsonNode.path("Body").asInt())
                    .build();
            // Trouver la planète dans le registry via BodyID
            Optional<PlaneteDetail> planeteOpt = planeteRegistry.getByBodyID(scanOrganicData.getBody())
                    .filter(body -> body instanceof PlaneteDetail)
                    .map(body -> (PlaneteDetail) body);

            if (planeteOpt.isPresent()) {
                PlaneteDetail planete = planeteOpt.get();
                planete.addConfirmedSpecies(scanOrganicData);
                System.out.printf("🔬 Scan organique traité: %s (BodyID: %d, ScanType: %s, Species: %s)%n",
                        planete.getBodyName(), scanOrganicData.getBody(), scanOrganicData.getScanType(),
                        scanOrganicData.getVariantLocalised());
            } else {
                System.out.printf("⚠️ Planète non trouvée dans le registry pour BodyID: %d (ScanOrganic)%n",
                        scanOrganicData.getBody());
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du traitement de l'événement ScanOrganic: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

