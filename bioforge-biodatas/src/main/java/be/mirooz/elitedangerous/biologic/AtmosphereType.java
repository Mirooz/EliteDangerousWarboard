package be.mirooz.elitedangerous.biologic;

/**
 * Enum representing all possible atmosphere types in Elite Dangerous.
 */
public enum AtmosphereType {
    NO_ATMOSPHERE("No atmosphere"),
    THIN_AMMONIA("Thin Ammonia"),
    THIN_ARGON("Thin Argon"),
    THIN_ARGON_RICH("Thin Argon-rich"),
    THIN_CARBON_DIOXIDE("Thin Carbon dioxide"),
    THIN_CARBON_DIOXIDE_RICH("Thin Carbon dioxide-rich"),
    THIN_HELIUM("Thin Helium"),
    THIN_METHANE("Thin Methane"),
    THIN_METHANE_RICH("Thin Methane-rich"),
    THIN_NEON("Thin Neon"),
    THIN_NEON_RICH("Thin Neon-rich"),
    THIN_NITROGEN("Thin Nitrogen"),
    THIN_OXYGEN("Thin Oxygen"),
    THIN_SULPHUR_DIOXIDE("Thin Sulphur dioxide"),
    THIN_WATER("Thin Water"),
    THIN_WATER_RICH("Thin Water-rich"),
    THICK_NITROGEN("Thick Nitrogen"),
    THICK_METHANE_RICH("Thick Methane-rich"),
    THICK_ARGON_RICH("Thick Argon-rich"),
    HOT_THIN_SULPHUR_DIOXIDE("Hot thin Sulphur dioxide"),
    HOT_THICK_WATER("Hot thick Water"),
    HOT_THICK_METHANE_RICH("Hot thick Methane-rich"),
    NEON_RICH("Neon-rich"),
    HELIUM("Helium");

    private final String displayName;

    AtmosphereType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Parses a string to find the matching AtmosphereType.
     * Returns null if no match is found.
     */
    public static AtmosphereType fromString(String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }
        
        String normalized = str.trim();
        
        for (AtmosphereType type : values()) {
            if (type.displayName.equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        
        return null;
    }
}

