package be.mirooz.elitedangerous.analytics.dto.spansh;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO wrapper pour la réponse d'une route Spansh
 * Contient searchReference et spanshResponse (qui contient les résultats)
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpanshRouteResponseDTO {
    /**
     * GUID de référence de la recherche créée
     */
    @JsonProperty("searchReference")
    private String searchReference;
    
    /**
     * Réponse complète de la route Spansh (job, parameters, result, state, status)
     */
    @JsonProperty("spanshResponse")
    private SpanshRouteResultsResponseDTO spanshResponse;
}
