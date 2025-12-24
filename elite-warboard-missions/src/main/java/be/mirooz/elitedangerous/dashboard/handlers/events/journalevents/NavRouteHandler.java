package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.registries.exploration.ExplorationModeRegistry;
import be.mirooz.elitedangerous.dashboard.service.NavRouteService;
import be.mirooz.elitedangerous.dashboard.service.listeners.NavRouteNotificationService;
import be.mirooz.elitedangerous.dashboard.view.common.context.DashboardContext;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Handler pour l'événement NavRoute
 * Lit le fichier NavRoute.json immédiatement si le batch n'est pas en cours,
 * sinon le NavRouteService le lira après la fin du batch.
 * Ne remplace la route que si le mode actuel est "Free Exploration".
 */
public class NavRouteHandler implements JournalEventHandler {

    private final NavRouteService navRouteService = NavRouteService.getInstance();
    private final ExplorationModeRegistry explorationModeRegistry = ExplorationModeRegistry.getInstance();
    private final NavRouteNotificationService navRouteNotificationService = NavRouteNotificationService.getInstance();

    @Override
    public String getEventType() {
        return "NavRoute";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        // Si le batch est en cours, on ignore l'événement
        // Le service le lira automatiquement après la fin du batch
        if (DashboardContext.getInstance().isBatchLoading()) {
            return;
        }
        
        // Ne charger la route que si on est en mode Free Exploration
        // Sinon, on garde la route actuelle (ex: route Spansh pour Stratum Undiscovered)
        if (!explorationModeRegistry.isFreeExploration()) {
            // En mode Stratum, notifier le service pour rafraîchir l'affichage
            navRouteNotificationService.notifyRouteRefreshRequired();
            return;
        }
        
        // Sinon, lire immédiatement le fichier NavRoute.json
        navRouteService.loadAndStoreNavRoute();
        // Notifier le service pour rafraîchir l'affichage après le chargement
        navRouteNotificationService.notifyRouteRefreshRequired();
    }
}

