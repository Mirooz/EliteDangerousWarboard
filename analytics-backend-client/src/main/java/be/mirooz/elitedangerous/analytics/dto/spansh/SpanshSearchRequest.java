package be.mirooz.elitedangerous.analytics.dto.spansh;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;

import java.util.List;

/**
 * DTO pour la requête de sauvegarde de recherche vers l'API Spansh.
 * Correspond à la structure JSON attendue par l'endpoint POST /api/bodies/search/save
 */
@ToString
public class SpanshSearchRequest {
    public SpanshSearchFilters filters;
    public List<SpanshSortRequest> sort;
    public int size;
    public int page;
    @JsonProperty("reference_system")
    public String referenceSystem;

    @ToString
    public static class SpanshSearchFilters {
        public SpanshFilter atmosphere;
        public SpanshFilter subtype;
        public SpanshComparisonFilter gravity;
        public SpanshRangeFilter distance;
        public SpanshComparisonFilter distance_to_arrival;
        public SpanshComparisonFilter updated_at;
    }

    @ToString
    public static class SpanshFilter {
        public List<String> value;
    }

    @ToString
    public static class SpanshRangeFilter {
        public String min;
        public String max;
    }

    @ToString
    public static class SpanshComparisonFilter {
        public String comparison;
        public List<Object> value; // Peut contenir String ou Number
    }

    @ToString
    public static class SpanshSortRequest {
        public SpanshSortDirectionRequest distance;
    }

    @ToString
    public static class SpanshSortDirectionRequest {
        public String direction;
    }
}

