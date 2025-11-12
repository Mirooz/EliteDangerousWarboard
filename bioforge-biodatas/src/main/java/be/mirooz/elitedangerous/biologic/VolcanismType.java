package be.mirooz.elitedangerous.biologic;

/**
 * Enum representing all possible volcanism types in Elite Dangerous.
 * Includes both "No volcanism" and all active volcanism types.
 */
public enum VolcanismType {
    NO_VOLCANISM("No volcanism"),
    
    // Carbon-based volcanism
    CARBON_DIOXIDE_GEYSERS("Carbon Dioxide Geysers"),
    MINOR_CARBON_DIOXIDE_GEYSERS("Minor Carbon Dioxide Geysers"),
    
    // Methane-based volcanism
    MINOR_METHANE_MAGMA("Minor Methane Magma"),
    
    // Nitrogen-based volcanism
    MINOR_NITROGEN_MAGMA("Minor Nitrogen Magma"),
    
    // Ammonia-based volcanism
    MINOR_AMMONIA_MAGMA("Minor Ammonia Magma"),
    
    // Water-based volcanism
    WATER_GEYSERS("Water Geysers"),
    WATER_MAGMA("Water Magma"),
    MINOR_WATER_GEYSERS("Minor Water Geysers"),
    MINOR_WATER_MAGMA("Minor Water Magma"),
    MAJOR_WATER_GEYSERS("Major Water Geysers"),
    MAJOR_WATER_MAGMA("Major Water Magma"),
    
    // Metallic volcanism
    METALLIC_MAGMA("Metallic Magma"),
    MINOR_METALLIC_MAGMA("Minor Metallic Magma"),
    MAJOR_METALLIC_MAGMA("Major Metallic Magma"),
    
    // Rocky volcanism
    ROCKY_MAGMA("Rocky Magma"),
    MINOR_ROCKY_MAGMA("Minor Rocky Magma"),
    MAJOR_ROCKY_MAGMA("Major Rocky Magma"),
    
    // Silicate vapour volcanism
    SILICATE_VAPOUR_GEYSERS("Silicate Vapour Geysers"),
    MINOR_SILICATE_VAPOUR_GEYSERS("Minor Silicate Vapour Geysers"),
    MAJOR_SILICATE_VAPOUR_GEYSERS("Major Silicate Vapour Geysers"),
    
    // Generic types (for compatibility)
    NITROGEN_BASED("Nitrogen Based"),
    AMMONIA_BASED("Ammonia Based"),
    CARBON_BASED("Carbon Based"),
    METHANE_BASED("Methane Based"),
    ANY("Any");

    private final String displayName;

    VolcanismType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Parses a string to find the matching VolcanismType.
     * Handles both "Body Type - Volcanism Type" format and direct volcanism type.
     * Returns null if no match is found.
     */
    public static VolcanismType fromString(String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }
        
        String normalized = str.trim();
        
        // Extract volcanism type from "Body Type - Volcanism Type" format
        if (normalized.contains(" - ")) {
            normalized = normalized.substring(normalized.lastIndexOf(" - ") + 3);
        }
        
        for (VolcanismType type : values()) {
            if (type.displayName.equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        
        return null;
    }
}
