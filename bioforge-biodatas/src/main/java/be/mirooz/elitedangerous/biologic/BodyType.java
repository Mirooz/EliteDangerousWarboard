package be.mirooz.elitedangerous.biologic;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum BodyType {

    METAL_RICH,
    HIGH_METAL_CONTENT,
    ROCKY,
    ROCKY_ICE,
    ICY,
    WATER_WORLD,
    EARTHLIKE,
    AMMONIA,
    GAS_GIANT,
    GAS_GIANT_I,
    GAS_GIANT_II,
    GAS_GIANT_III,
    GAS_GIANT_IV,
    GAS_GIANT_V,
    HELIUM_RICH_GG,
    HELIUM_GG,
    ICE_GIANT,
    CLUSTER,
    UNKNOWN;

    // --------------------------
    // Alias + K-values
    // --------------------------
    private static final record Info(
            int baseK,
            Integer terraK,
            String... aliases
    ) {}

    private static final List<Info> DATA = List.of(

            new Info(21790, 105678, "High metal content world", "High metal content body"),
            new Info(21790, 105678, "Metal rich body"),

            new Info(300, 93328, "Rocky body"),
            new Info(300, 93328, "Rocky Ice world", "Rocky Ice body", "Rocky ice world", "Rocky ice body"),
            new Info(300, 93328, "Icy body"),

            new Info(64831, 116295, "Water world"),
            new Info(64831, 116295, "Earthlike body"),

            new Info(96932, null, "Ammonia world"),

            new Info(300, 93328, "Gas giant"),
            new Info(1656, null, "Class I gas giant"),
            new Info(9654, 100677, "Class II gas giant"),
            new Info(300, 93328, "Class III gas giant"),
            new Info(300, 93328, "Class IV gas giant"),
            new Info(300, 93328, "Class V gas giant"),

            new Info(300, 93328, "Helium rich gas giant"),
            new Info(300, 93328, "Helium gas giant"),

            new Info(300, 93328, "Ice giant"),
            new Info(0, null, "StellarCluster", "Cluster", "stellar cluster"),
            // fallback
            new Info(300, 93328, "Unknown", "UNKNOWN")
    );

    // --------------------------
    // Lookup table
    // --------------------------
    private static final Map<String, BodyType> LOOKUP = new HashMap<>();
    private static final Map<BodyType, Info> INFO_MAP = new EnumMap<>(BodyType.class);

    static {
        for (Info info : DATA) {
            BodyType type = matchEnum(info.aliases()[0]);
            INFO_MAP.put(type, info);
        }
    }


    private static BodyType matchEnum(String ref) {
        ref = ref.toLowerCase();
        if (ref.contains("metal rich")) return METAL_RICH;
        if (ref.contains("high metal content")) return HIGH_METAL_CONTENT;
        if (ref.contains("rocky ice")) return ROCKY_ICE;
        if (ref.contains("rocky")) return ROCKY;
        if (ref.contains("icy")) return ICY;
        if (ref.contains("water world")) return WATER_WORLD;
        if (ref.contains("earthlike")) return EARTHLIKE;
        if (ref.contains("ammonia")) return AMMONIA;
        if (ref.equals("gas giant")) return GAS_GIANT;
        if (ref.contains("class i gas")) return GAS_GIANT_I;
        if (ref.contains("class ii gas")) return GAS_GIANT_II;
        if (ref.contains("class iii gas")) return GAS_GIANT_III;
        if (ref.contains("class iv gas")) return GAS_GIANT_IV;
        if (ref.contains("class v gas")) return GAS_GIANT_V;
        if (ref.contains("helium rich")) return HELIUM_RICH_GG;
        if (ref.contains("helium gas")) return HELIUM_GG;
        if (ref.contains("ice giant")) return ICE_GIANT;
        if (ref.contains("gas giant")) return GAS_GIANT;
        if (ref.contains("cluster")) return CLUSTER;
        return UNKNOWN;
    }

    // --------------------------
    // API
    // --------------------------
    public static BodyType fromString(String value) {
        if (value == null) return UNKNOWN;
        String ref = value.toLowerCase().trim();

        // cluster = empty
        if (ref.isBlank()) return CLUSTER;

        // apply the matching logic directly
        return matchEnum(ref);
    }

    public int getBaseK() {
        return INFO_MAP.get(this).baseK();
    }

    public Integer getTerraformableK() {
        return INFO_MAP.get(this).terraK();
    }
}
