package be.mirooz.elitedangerous.dashboard.model.registries.navigation;

import be.mirooz.elitedangerous.dashboard.model.navigation.NavRoute;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * Registry pour stocker la route de navigation actuelle.
 * Singleton observable pour la UI.
 */
public class NavRouteRegistry {

    private static final NavRouteRegistry INSTANCE = new NavRouteRegistry();

    private final ObjectProperty<NavRoute> currentRoute = new SimpleObjectProperty<>(null);

    private NavRouteRegistry() {
    }

    public static NavRouteRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Met à jour la route de navigation actuelle
     */
    public void setCurrentRoute(NavRoute route) {
        currentRoute.set(route);
    }


    /**
     * Récupère la property de la route (pour les listeners)
     */
    public ObjectProperty<NavRoute> getCurrentRouteProperty() {
        return currentRoute;
    }

    /**
     * Récupère la route de navigation actuelle
     */
    public NavRoute getCurrentRoute() {
        return currentRoute.get();
    }

    /**
     * Vérifie si une route est active
     */
    public boolean hasRoute() {
        NavRoute route = currentRoute.get();
        return route != null && route.getRoute() != null && !route.getRoute().isEmpty();
    }

    /**
     * Efface la route actuelle
     */
    public void clearRoute() {
        currentRoute.set(null);
    }
}

