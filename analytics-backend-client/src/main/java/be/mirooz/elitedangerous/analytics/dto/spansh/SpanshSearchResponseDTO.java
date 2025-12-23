package be.mirooz.elitedangerous.analytics.dto.spansh;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la réponse de recherche Spansh
 * Contient la réponse complète de l'API Spansh avec les bodies, systèmes, etc.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpanshSearchResponseDTO {
    /**
     * GUID de référence de la recherche créée
     */
    private String searchReference;
    
    /**
     * Réponse complète de l'API Spansh (bodies, systèmes, référence, etc.)
     */
    private SpanshSearchResponse spanshResponse;
}

