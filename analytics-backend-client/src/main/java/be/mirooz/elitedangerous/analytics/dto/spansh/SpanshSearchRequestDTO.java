package be.mirooz.elitedangerous.analytics.dto.spansh;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la requête de recherche Spansh
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpanshSearchRequestDTO {
    /**
     * Nom du système de référence (FSD - Frame Shift Drive)
     * Exemple: "Col 173 Sector GC-L d8-16", "Sol"
     */
    private String referenceSystem;
}

