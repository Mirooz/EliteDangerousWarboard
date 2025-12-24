package be.mirooz.elitedangerous.dashboard.model.exploration;

import lombok.Getter;

/**
 * Enum représentant les différents modes d'exploration disponibles
 */
@Getter
public enum ExplorationMode {
    FREE_EXPLORATION(
        "Free Exploration",
        "Explore freely without specific constraints"
    ),
    STRATUM_UNDISCOVERED(
        "Stratum Undiscovered",
        "Find undiscovered systems with potential stratum bodies"
    );

    private final String displayName;
    private final String description;

    ExplorationMode(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
