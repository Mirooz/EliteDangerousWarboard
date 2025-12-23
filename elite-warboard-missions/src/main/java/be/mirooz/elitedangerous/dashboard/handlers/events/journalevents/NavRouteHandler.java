package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.service.NavRouteService;
import be.mirooz.elitedangerous.dashboard.view.common.context.DashboardContext;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Handler pour l'événement NavRoute
 * Lit le fichier NavRoute.json immédiatement si le batch n'est pas en cours,
 * sinon le NavRouteService le lira après la fin du batch
 */
public class NavRouteHandler implements JournalEventHandler {

    private final NavRouteService navRouteService = NavRouteService.getInstance();

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
        
        // Sinon, lire immédiatement le fichier NavRoute.json
        navRouteService.loadAndStoreNavRoute();
    }
}

