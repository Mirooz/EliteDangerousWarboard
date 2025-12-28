package be.mirooz.elitedangerous.analytics.dto.spansh;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * DTO pour la réponse des résultats d'une route Spansh
 * Structure retournée par /api/spansh/{endpoint} (expressway-to-exomastery, road-to-riches)
 * Identique à SpanshRouteResultsResponse du spansh-client
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpanshRouteResultsResponseDTO {
    /**
     * GUID de la route (job)
     */
    @JsonProperty("job")
    public String job;
    
    /**
     * Paramètres de la route
     */
    @JsonProperty("parameters")
    public RouteParameters parameters;
    
    /**
     * Liste des systèmes avec leurs bodies
     */
    @JsonProperty("result")
    public List<SystemResult> result;
    
    /**
     * État de la route (completed, pending, etc.)
     */
    @JsonProperty("state")
    public String state;
    
    /**
     * Statut de la route (ok, error, etc.)
     */
    @JsonProperty("status")
    public String status;
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RouteParameters {
        @JsonProperty("avoid_thargoids")
        public Integer avoid_thargoids;
        @JsonProperty("loop")
        public String loop;
        @JsonProperty("max_distance")
        public String max_distance;
        @JsonProperty("max_results")
        public String max_results;
        @JsonProperty("min_value")
        public String min_value;
        @JsonProperty("radius")
        public String radius;
        @JsonProperty("range")
        public String range;
        @JsonProperty("source")
        public String source;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SystemResult {
        /**
         * Liste des bodies dans ce système
         */
        @JsonProperty("bodies")
        public List<BodyResult> bodies;
        
        /**
         * ID64 du système (peut être une chaîne dans le JSON)
         */
        @JsonProperty("id64")
        public String id64;
        
        /**
         * Nombre de sauts depuis le départ
         */
        @JsonProperty("jumps")
        public int jumps;
        
        /**
         * Nom du système
         */
        @JsonProperty("name")
        public String name;
        
        /**
         * Coordonnées X, Y, Z
         */
        @JsonProperty("x")
        public double x;
        @JsonProperty("y")
        public double y;
        @JsonProperty("z")
        public double z;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BodyResult {
        @JsonProperty("distance_to_arrival")
        public double distance_to_arrival;
        @JsonProperty("estimated_mapping_value")
        public int estimated_mapping_value;
        @JsonProperty("estimated_scan_value")
        public int estimated_scan_value;
        @JsonProperty("id")
        public long id;
        @JsonProperty("id64")
        public long id64;
        @JsonProperty("landmark_value")
        public int landmark_value;
        
        /**
         * Liste des landmarks (exobiologie)
         */
        @JsonProperty("landmarks")
        public List<Landmark> landmarks;
        
        @JsonProperty("name")
        public String name;
        @JsonProperty("subtype")
        public String subtype;
        @JsonProperty("type")
        public String type;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Landmark {
        @JsonProperty("count")
        public int count;
        @JsonProperty("subtype")
        public String subtype;
        @JsonProperty("type")
        public String type;
        @JsonProperty("value")
        public int value;
    }
}

