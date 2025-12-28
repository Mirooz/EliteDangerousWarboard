package be.mirooz.elitedangerous.dashboard.model.registries.navigation;

import be.mirooz.elitedangerous.dashboard.model.exploration.ExplorationMode;
import be.mirooz.elitedangerous.dashboard.model.navigation.NavRoute;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry pour stocker les routes de navigation par mode d'exploration.
 * Singleton observable pour la UI.
 */
public class NavRouteRegistry {

    private static final NavRouteRegistry INSTANCE = new NavRouteRegistry();

    // Map générique pour stocker les routes par mode
    private final Map<ExplorationMode, ObjectProperty<NavRoute>> routeMap = new HashMap<>();
    
    // Property combinée pour la compatibilité avec le code existant
    private final ObjectProperty<NavRoute> currentRoute = new SimpleObjectProperty<>(null);

    private NavRouteRegistry() {
        // Initialiser les propriétés pour tous les modes
        for (ExplorationMode mode : ExplorationMode.values()) {
            ObjectProperty<NavRoute> routeProperty = new SimpleObjectProperty<>(null);
            routeProperty.addListener((obs, oldRoute, newRoute) -> updateCurrentRoute());
            routeMap.put(mode, routeProperty);
        }
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
        ObjectProperty<NavRoute> routeProperty = routeMap.get(mode);
        if (routeProperty != null) {
            routeProperty.set(route);
        }
    }

    /**
     * Récupère la route pour un mode spécifique
     */
    public NavRoute getRouteForMode(ExplorationMode mode) {
        ObjectProperty<NavRoute> routeProperty = routeMap.get(mode);
        return routeProperty != null ? routeProperty.get() : null;
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
            ObjectProperty<NavRoute> freeExplorationProperty = routeMap.get(ExplorationMode.FREE_EXPLORATION);
            currentRoute.set(freeExplorationProperty != null ? freeExplorationProperty.get() : null);
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

