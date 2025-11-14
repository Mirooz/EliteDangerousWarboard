package be.mirooz.elitedangerous.dashboard.model.exploration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Représente un élément BioData vendu dans une transaction SellOrganicData.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SoldBioData {
    private String genus; // Codex code (ex: "$Codex_Ent_Stratum_Genus_Name;")
    private String genusLocalised; // Ex: "Stratum"
    private String species; // Codex code (ex: "$Codex_Ent_Stratum_07_Name;")
    private String speciesLocalised; // Ex: "Stratum Tectonicas"
    private String variant; // Codex code (ex: "$Codex_Ent_Stratum_07_K_Name;")
    private String variantLocalised; // Ex: "Stratum Tectonicas - Vert clair"
    private long value; // Valeur de base
    private long bonus; // Bonus (première découverte)
}

