package be.mirooz.elitedangerous.dashboard.model.exploration;

import lombok.Getter;

/**
 * Enum représentant les différents modes d'exploration disponibles
 */
@Getter
public enum ExplorationMode {
    FREE_EXPLORATION(
        "Free Exploration",
        "Explore freely without specific constraints",
        null
    ),
    EXPRESSWAY_TO_EXOMASTERY(
        "Expressway to Exomastery",
        "Find systems for exobiology exploration",
        "expressway-to-exomastery"
    ),
    ROAD_TO_RICHES(
        "Road to Riches",
        "Find valuable exploration targets",
        "road-to-riches"
    ),
    STRATUM_UNDISCOVERED(
        "Stratum Undiscovered",
        "Find undiscovered Stratum Tectonicas",
        "stratum-undiscovered"
    );

    private final String displayName;
    private final String description;
    private final String spanshEndpoint; // Endpoint Spansh pour ce mode (null si pas d'endpoint)

    ExplorationMode(String displayName, String description, String spanshEndpoint) {
        this.displayName = displayName;
        this.description = description;
        this.spanshEndpoint = spanshEndpoint;
    }

    /**
     * Vérifie si ce mode nécessite un appel API Spansh
     */
    public boolean requiresSpanshApi() {
        return spanshEndpoint != null && !spanshEndpoint.isEmpty();
    }
}
