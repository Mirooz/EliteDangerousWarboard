package be.mirooz.elitedangerous.dashboard.model.exploration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Représente les données d'un événement ScanOrganic.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScanOrganicData {
    private String timestamp;
    private String scanType; // "Log", "Sample", "Analyse"
    private String genus; // Codex code (ex: "$Codex_Ent_Osseus_Genus_Name;")
    private String genusLocalised; // Ex: "Osseus"
    private String species; // Codex code (ex: "$Codex_Ent_Osseus_03_Name;")
    private String speciesLocalised; // Ex: "Osseus Spiralis"
    private String variant; // Codex code (ex: "$Codex_Ent_Osseus_03_K_Name;")
    private String variantLocalised; // Ex: "Osseus Spiralis - Indigo"
    private boolean wasLogged;
    private long systemAddress;
    private int body; // BodyID
}

