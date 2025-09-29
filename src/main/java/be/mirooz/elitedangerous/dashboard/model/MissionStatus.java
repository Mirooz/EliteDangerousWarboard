package be.mirooz.elitedangerous.dashboard.model;

/**
 * Statuts possibles d'une mission
 */
public enum MissionStatus {
    ACTIVE("Active"),
    COMPLETED("Terminée"),
    FAILED("Échouée"),
    ABANDONED("Abandonnée"),
    EXPIRED("Expirée");

    private final String displayName;

    MissionStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
