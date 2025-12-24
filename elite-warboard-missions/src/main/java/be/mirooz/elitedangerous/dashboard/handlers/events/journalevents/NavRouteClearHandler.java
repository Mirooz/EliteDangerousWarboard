package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.registries.exploration.ExplorationModeRegistry;
import be.mirooz.elitedangerous.dashboard.service.listeners.NavRouteNotificationService;
import be.mirooz.elitedangerous.dashboard.view.common.context.DashboardContext;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Handler pour l'événement NavRouteClear
 * Rafraîchit l'affichage de la route en mode Stratum Undiscovered
 * pour mettre à jour le cadre orange autour du dernier système de NavRoute.json
 */
public class NavRouteClearHandler implements JournalEventHandler {

    private final ExplorationModeRegistry explorationModeRegistry = ExplorationModeRegistry.getInstance();
    private final NavRouteNotificationService navRouteNotificationService = NavRouteNotificationService.getInstance();

    @Override
    public String getEventType() {
        return "NavRouteClear";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        // Si le batch est en cours, on ignore l'événement
        if (DashboardContext.getInstance().isBatchLoading()) {
            return;
        }
        
        // En mode Stratum, notifier le service pour rafraîchir l'affichage
        if (!explorationModeRegistry.isFreeExploration()) {
            navRouteNotificationService.notifyRouteRefreshRequired();
        }
    }
}

