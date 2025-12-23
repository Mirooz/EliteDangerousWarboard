package be.mirooz.elitedangerous.analytics.dto.spansh;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.ToString;

import java.time.OffsetDateTime;
import java.util.List;

public class SpanshSearchResponse {
    public int count;
    public int from;
    public Reference reference;
    @ToString.Exclude
    public List<BodyResult> results; // Exclu pour éviter les lignes trop longues
    public String search_reference; // C'est pour la réponse du POST search/save
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SpanshSearchResponse{\n");
        sb.append(String.format("  count=%d,\n", count));
        sb.append(String.format("  from=%d,\n", from));
        sb.append(String.format("  reference=%s,\n", reference));
        
        if (results != null && !results.isEmpty()) {
            sb.append(String.format("  results=[%d items]:\n", results.size()));
            // Afficher tous les résultats
            for (int i = 0; i < results.size(); i++) {
                sb.append("    ").append(i + 1).append(". ").append(results.get(i)).append("\n");
            }
        } else {
            sb.append("  results=null\n");
        }
        
        sb.append(String.format("  search_reference='%s',\n", search_reference));
        sb.append(String.format("  search=%s\n", search));
        sb.append("}");
        return sb.toString();
    }

    @ToString
    public static class Reference {
        public long id64;
        public String name;
        public double x;
        public double y;
        public double z;
    }

    public static class BodyResult {
        public double arg_of_periapsis;
        public String atmosphere;
        @ToString.Exclude
        public List<AtmosphereComposition> atmosphere_composition; // Exclu pour éviter les lignes trop longues
        public double axis_tilt;
        public int body_id;
        public double distance;
        public double distance_to_arrival;
        public double earth_masses;
        public int estimated_mapping_value;
        public int estimated_scan_value;
        public double gravity;
        public String id;
        public long id64;
        public boolean is_landable;
        public Boolean is_main_star;
        public boolean is_rotational_period_tidally_locked;
        public String name;
        public double orbital_eccentricity;
        public double orbital_inclination;
        public double orbital_period;
        public double orbital_synchronicity;
        @ToString.Exclude
        public List<Parent> parents; // Exclu pour éviter les lignes trop longues
        public double radius;
        public double rotational_period;
        public double semi_major_axis;
        @ToString.Exclude
        public List<SolidComposition> solid_composition; // Exclu pour éviter les lignes trop longues
        public String subtype;
        public double surface_pressure;
        public double surface_temperature;
        public long system_id64;
        public String system_name;
        public String system_region;
        public double system_x;
        public double system_y;
        public double system_z;
        public String terraforming_state;
        public String type;
        @JsonDeserialize(using = SpanshOffsetDateTimeDeserializer.class)
        public OffsetDateTime updated_at;
        public String volcanism_type;
        
        @Override
        public String toString() {
            return String.format(
                "BodyResult{\n" +
                "  name='%s',\n" +
                "  id64=%d,\n" +
                "  distance=%.2f,\n" +
                "  distance_to_arrival=%.2f,\n" +
                "  gravity=%.6f,\n" +
                "  is_landable=%s,\n" +
                "  atmosphere='%s',\n" +
                "  subtype='%s',\n" +
                "  type='%s',\n" +
                "  system_name='%s',\n" +
                "  updated_at=%s\n" +
                "}",
                name, id64, distance, distance_to_arrival, gravity, is_landable,
                atmosphere, subtype, type, system_name, updated_at
            );
        }
    }

    @ToString
    public static class AtmosphereComposition {
        public String name;
        public double share;
    }

    @ToString
    public static class Parent {
        public long id64;
        public String subtype;
        public String type;
    }

    @ToString
    public static class SolidComposition {
        public String name;
        public double share;
    }

    // --- Pour la réponse du POST /api/bodies/search/save ---
    public SpanshSearchSaveResponse search;

    public static class SpanshSearchSaveResponse {
        public SpanshSearchFilters filters;
        public int page;
        public String reference_system;
        public Reference reference_system_details;
        public int size;
        @ToString.Exclude
        public List<SpanshSort> sort; // Exclu pour éviter les lignes trop longues
        
        @Override
        public String toString() {
            return String.format(
                "SpanshSearchSaveResponse{\n" +
                "  page=%d,\n" +
                "  reference_system='%s',\n" +
                "  size=%d,\n" +
                "  filters=%s,\n" +
                "  reference_system_details=%s\n" +
                "}",
                page, reference_system, size, filters, reference_system_details
            );
        }
    }

    @ToString
    public static class SpanshSearchFilters {
        public SpanshFilter atmosphere;
        public SpanshRangeFilter distance;
        public SpanshComparisonFilter distance_to_arrival;
        public SpanshComparisonFilter gravity;
        public SpanshFilter subtype;
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
        public List<Object> value; // Peut contenir String ou Double
    }

    @ToString
    public static class SpanshSort {
        public SpanshSortDirection distance;
    }

    @ToString
    public static class SpanshSortDirection {
        public String direction;
    }
}
