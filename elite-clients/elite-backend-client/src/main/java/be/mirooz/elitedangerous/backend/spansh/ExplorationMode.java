package be.mirooz.elitedangerous.backend.spansh;

/**
 * Modes d'exploration disponibles et support Spansh associé.
 */
public enum ExplorationMode {
    FREE_EXPLORATION(
            "Free Exploration",
            "Explore freely without specific constraints",
            false
    ),
    EXPRESSWAY_TO_EXOMASTERY(
            "Expressway to Exomastery",
            "Find systems for exobiology exploration",
            true
    ),
    ROAD_TO_RICHES(
            "Road to Riches",
            "Find valuable exploration targets",
            true
    ),
    STRATUM_UNDISCOVERED(
            "Stratum Undiscovered",
            "Find undiscovered Stratum Tectonicas",
            true
    );

    private final String displayName;
    private final String description;
    private final boolean spanshMode;

    ExplorationMode(String displayName, String description, boolean spanshMode) {
        this.displayName = displayName;
        this.description = description;
        this.spanshMode = spanshMode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean requiresSpanshApi() {
        return spanshMode;
    }
}
