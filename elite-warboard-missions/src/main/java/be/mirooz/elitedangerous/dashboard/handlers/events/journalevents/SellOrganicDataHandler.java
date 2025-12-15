package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.exploration.OrganicDataSale;
import be.mirooz.elitedangerous.dashboard.model.exploration.SoldBioData;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.OrganicDataSaleRegistry;
import be.mirooz.elitedangerous.dashboard.service.listeners.ExplorationRefreshNotificationService;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Handler pour l'événement SellOrganicData du journal Elite Dangerous
 * <p>
 * Traite les ventes de données organiques collectées.
 * Exemple d'événement :
 * {
 *   "timestamp" : "2025-11-05T22:57:51Z",
 *   "event" : "SellOrganicData",
 *   "MarketID" : 128666762,
 *   "BioData" : [ {
 *     "Genus" : "$Codex_Ent_Stratum_Genus_Name;",
 *     "Genus_Localised" : "Stratum",
 *     "Species" : "$Codex_Ent_Stratum_07_Name;",
 *     "Species_Localised" : "Stratum Tectonicas",
 *     "Variant" : "$Codex_Ent_Stratum_07_K_Name;",
 *     "Variant_Localised" : "Stratum Tectonicas - Vert clair",
 *     "Value" : 19010800,
 *     "Bonus" : 76043200
 *   } ]
 * }
 */
public class SellOrganicDataHandler implements JournalEventHandler {

    private final OrganicDataSaleRegistry registry = OrganicDataSaleRegistry.getInstance();

    @Override
    public String getEventType() {
        return "SellOrganicData";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            String timestamp = jsonNode.path("timestamp").asText();
            long marketID = jsonNode.path("MarketID").asLong();

            // Extraire tous les BioData
            List<SoldBioData> bioDataList = new ArrayList<>();
            long totalValue = 0;
            long totalBonus = 0;

            if (jsonNode.has("BioData") && jsonNode.get("BioData").isArray()) {
                jsonNode.get("BioData").forEach(bioDataNode -> {
                    SoldBioData soldBioData = SoldBioData.builder()
                            .genus(bioDataNode.path("Genus").asText())
                            .genusLocalised(bioDataNode.path("Genus_Localised").asText())
                            .species(bioDataNode.path("Species").asText())
                            .speciesLocalised(bioDataNode.path("Species_Localised").asText())
                            .variant(bioDataNode.path("Variant").asText())
                            .variantLocalised(bioDataNode.path("Variant_Localised").asText())
                            .value(bioDataNode.path("Value").asLong(0))
                            .bonus(bioDataNode.path("Bonus").asLong(0))
                            .build();

                    bioDataList.add(soldBioData);
                });
            }

            // Calculer les totaux
            for (SoldBioData bioData : bioDataList) {
                totalValue += bioData.getValue();
                totalBonus += bioData.getBonus();
            }

            // Créer l'objet de vente
            OrganicDataSale sale = OrganicDataSale.builder()
                    .timestamp(timestamp)
                    .marketID(marketID)
                    .bioData(bioDataList)
                    .totalValue(totalValue)
                    .totalBonus(totalBonus)
                    .build();

            // Ajouter au registry
            registry.addSale(sale);
            
            // Réinitialiser le crédit actuel après la vente
            registry.resetCurrentCredit();

            System.out.printf("💰 Vente de données organiques enregistrée: %d espèces, Total Value: %d, Total Bonus: %d%n",
                    bioDataList.size(), totalValue, totalBonus);

            ExplorationRefreshNotificationService.getInstance().notifyRefreshRequired();
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du traitement de l'événement SellOrganicData: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

