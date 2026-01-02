package be.mirooz.elitedangerous.analytics.dto.spansh;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la requête de route Spansh (utilisé pour expressway-to-exomastery et road-to-riches)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpanshRouteRequestDTO {
    /**
     * Portée maximale de saut (correspond à radius et range dans l'URL)
     */
    private Double maxJumpRange;
    
    /**
     * Nom du système de départ
     */
    private String systemName;
    
    /**
     * Nom du système de destination (optionnel)
     */
    private String destinationSystem;
    
    /**
     * Nombre maximum de systèmes dans la route (par défaut : 10)
     */
    private Integer maxSystems;
    
    /**
     * Nom du commandant
     */
    private String commandername;
}

