package be.mirooz.elitedangerous.dashboard.model.registries.navigation;

import be.mirooz.elitedangerous.dashboard.model.exploration.ExplorationMode;
import be.mirooz.elitedangerous.dashboard.model.navigation.NavRoute;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * Registry pour stocker les routes de navigation par mode d'exploration.
 * Singleton observable pour la UI.
 */
public class NavRouteRegistry {

    private static final NavRouteRegistry INSTANCE = new NavRouteRegistry();

    // Routes séparées par mode
    private final ObjectProperty<NavRoute> freeExplorationRoute = new SimpleObjectProperty<>(null);
    private final ObjectProperty<NavRoute> stratumRoute = new SimpleObjectProperty<>(null);
    
    // Property combinée pour la compatibilité avec le code existant
    private final ObjectProperty<NavRoute> currentRoute = new SimpleObjectProperty<>(null);

    private NavRouteRegistry() {
        // Écouter les changements des routes individuelles pour mettre à jour currentRoute
        freeExplorationRoute.addListener((obs, oldRoute, newRoute) -> updateCurrentRoute());
        stratumRoute.addListener((obs, oldRoute, newRoute) -> updateCurrentRoute());
    }

    public static NavRouteRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Met à jour la route de navigation actuelle (méthode de compatibilité)
     * Utilise le mode actuel pour déterminer quelle route mettre à jour
     */
    public void setCurrentRoute(NavRoute route) {
        try {
            ExplorationMode currentMode = be.mirooz.elitedangerous.dashboard.model.registries.exploration.ExplorationModeRegistry.getInstance().getCurrentMode();
            setRouteForMode(route, currentMode);
        } catch (Exception e) {
            // Si le registre n'est pas encore initialisé, utiliser Free Exploration par défaut
            setRouteForMode(route, ExplorationMode.FREE_EXPLORATION);
        }
    }

    /**
     * Met à jour la route pour un mode spécifique
     */
    public void setRouteForMode(NavRoute route, ExplorationMode mode) {
        if (mode == ExplorationMode.FREE_EXPLORATION) {
            freeExplorationRoute.set(route);
        } else if (mode == ExplorationMode.STRATUM_UNDISCOVERED) {
            stratumRoute.set(route);
        }
    }

    /**
     * Récupère la route pour un mode spécifique
     */
    public NavRoute getRouteForMode(ExplorationMode mode) {
        if (mode == ExplorationMode.FREE_EXPLORATION) {
            return freeExplorationRoute.get();
        } else if (mode == ExplorationMode.STRATUM_UNDISCOVERED) {
            return stratumRoute.get();
        }
        return null;
    }

    /**
     * Met à jour currentRoute selon le mode actuel
     */
    private void updateCurrentRoute() {
        try {
            ExplorationMode currentMode = be.mirooz.elitedangerous.dashboard.model.registries.exploration.ExplorationModeRegistry.getInstance().getCurrentMode();
            NavRoute route = getRouteForMode(currentMode);
            currentRoute.set(route);
        } catch (Exception e) {
            // Si le registre n'est pas encore initialisé, utiliser Free Exploration par défaut
            currentRoute.set(freeExplorationRoute.get());
        }
    }

    /**
     * Récupère la property de la route (pour les listeners)
     * Retourne la route selon le mode actuel
     */
    public ObjectProperty<NavRoute> getCurrentRouteProperty() {
        // Mettre à jour currentRoute avant de le retourner
        updateCurrentRoute();
        return currentRoute;
    }

    /**
     * Récupère la route de navigation actuelle selon le mode
     */
    public NavRoute getCurrentRoute() {
        try {
            ExplorationMode currentMode = be.mirooz.elitedangerous.dashboard.model.registries.exploration.ExplorationModeRegistry.getInstance().getCurrentMode();
            return getRouteForMode(currentMode);
        } catch (Exception e) {
            // Si le registre n'est pas encore initialisé, retourner Free Exploration par défaut
            return getRouteForMode(ExplorationMode.FREE_EXPLORATION);
        }
    }

    /**
     * Vérifie si une route est active pour le mode actuel
     */
    public boolean hasRoute() {
        NavRoute route = getCurrentRoute();
        return route != null && route.getRoute() != null && !route.getRoute().isEmpty();
    }

    /**
     * Efface la route pour un mode spécifique
     */
    public void clearRouteForMode(ExplorationMode mode) {
        setRouteForMode(null, mode);
    }

    /**
     * Efface la route actuelle (méthode de compatibilité)
     */
    public void clearRoute() {
        try {
            ExplorationMode currentMode = be.mirooz.elitedangerous.dashboard.model.registries.exploration.ExplorationModeRegistry.getInstance().getCurrentMode();
            clearRouteForMode(currentMode);
        } catch (Exception e) {
            // Si le registre n'est pas encore initialisé, utiliser Free Exploration par défaut
            clearRouteForMode(ExplorationMode.FREE_EXPLORATION);
        }
    }
}

