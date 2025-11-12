package be.mirooz.elitedangerous.biologic;

/**
 * Enum representing all possible body types in Elite Dangerous.
 */
public enum BodyType {
    HIGH_METAL_CONTENT_WORLD("High metal content world"),
    ROCKY_BODY("Rocky body"),
    ROCKY_ICE_WORLD("Rocky Ice world"),
    ICY_BODY("Icy body"),
    WATER_WORLD("Water world"),
    CLASS_I_GAS_GIANT("Class I gas giant"),
    K_YELLOW_ORANGE_STAR("K (Yellow-Orange) Star");

    private final String displayName;

    BodyType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Parses a string to find the matching BodyType.
     * Returns null if no match is found.
     */
    public static BodyType fromString(String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }
        
        String normalized = str.trim();
        
        for (BodyType type : values()) {
            if (type.displayName.equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        
        return null;
    }
}

