package be.mirooz.elitedangerous.analytics.dto.spansh;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpanshSearchResponseDTO {
    /**
     * GUID de référence de la recherche créée
     */
    @JsonProperty("searchReference")
    private String searchReference;
    
    /**
     * Réponse complète de l'API Spansh (bodies, systèmes, référence, etc.)
     */
    @JsonProperty("spanshResponse")
    private SpanshSearchResponse spanshResponse;
}

