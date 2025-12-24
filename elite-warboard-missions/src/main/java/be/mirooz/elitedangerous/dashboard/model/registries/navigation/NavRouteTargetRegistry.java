package be.mirooz.elitedangerous.dashboard.model.registries.navigation;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

/**
 * Registry pour stocker les informations de la cible FSD (RemainingJumpsInRoute).
 * Singleton observable pour la UI.
 */
public class NavRouteTargetRegistry {

    private static final NavRouteTargetRegistry INSTANCE = new NavRouteTargetRegistry();

    private final IntegerProperty remainingJumpsInRoute = new SimpleIntegerProperty(-1);

    private NavRouteTargetRegistry() {
    }

    public static NavRouteTargetRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Met à jour le nombre de sauts restants dans la route
     */
    public void setRemainingJumpsInRoute(int remainingJumps) {
        remainingJumpsInRoute.set(remainingJumps);
    }

    /**
     * Récupère la property du nombre de sauts restants (pour les listeners)
     */
    public IntegerProperty getRemainingJumpsInRouteProperty() {
        return remainingJumpsInRoute;
    }

    /**
     * Récupère le nombre de sauts restants dans la route
     */
    public int getRemainingJumpsInRoute() {
        return remainingJumpsInRoute.get();
    }

    /**
     * Vérifie si une cible FSD est active (RemainingJumpsInRoute >= 0)
     */
    public boolean hasTarget() {
        return remainingJumpsInRoute.get() >= 0;
    }

    /**
     * Efface la cible actuelle
     */
    public void clearTarget() {
        remainingJumpsInRoute.set(-1);
    }
}

