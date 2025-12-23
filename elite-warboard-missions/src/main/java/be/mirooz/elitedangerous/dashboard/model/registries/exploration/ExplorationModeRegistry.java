package be.mirooz.elitedangerous.dashboard.model.registries.exploration;

import be.mirooz.elitedangerous.dashboard.model.exploration.ExplorationMode;

/**
 * Registry pour stocker le mode d'exploration actuel.
 * Singleton pour permettre aux handlers d'accéder au mode.
 */
public class ExplorationModeRegistry {

    private static final ExplorationModeRegistry INSTANCE = new ExplorationModeRegistry();

    private ExplorationMode currentMode = ExplorationMode.FREE_EXPLORATION;

    private ExplorationModeRegistry() {
    }

    public static ExplorationModeRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Met à jour le mode d'exploration actuel
     */
    public void setCurrentMode(ExplorationMode mode) {
        if (mode != null) {
            this.currentMode = mode;
        }
    }

    /**
     * Récupère le mode d'exploration actuel
     */
    public ExplorationMode getCurrentMode() {
        return currentMode;
    }

    /**
     * Vérifie si le mode actuel est Free Exploration
     */
    public boolean isFreeExploration() {
        return currentMode == ExplorationMode.FREE_EXPLORATION;
    }
}
