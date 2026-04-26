package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.service.NavRouteService;
import be.mirooz.elitedangerous.dashboard.service.listeners.NavRouteNotificationService;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Handler pour l'événement FSDTarget du journal Elite Dangerous
 * Récupère le RemainingJumpsInRoute et le stocke dans le registre
 * Exemple d'événement :
 * {
 *   "timestamp" : "2025-12-23T23:37:03Z",
 *   "event" : "FSDTarget",
 *   "Name" : "Synuefai UF-C d14-101",
 *   "SystemAddress" : 3480777361787,
 *   "StarClass" : "F",
 *   "RemainingJumpsInRoute" : 1
 * }
 */
public class FSDTargetHandler implements JournalEventHandler {

    private final NavRouteService navRouteService = NavRouteService.getInstance();
    private final NavRouteNotificationService navRouteNotificationService = NavRouteNotificationService.getInstance();

    @Override
    public String getEventType() {
        return "FSDTarget";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            if (jsonNode.has("RemainingJumpsInRoute")) {
                int remainingJumps = jsonNode.get("RemainingJumpsInRoute").asInt();
                navRouteService.setRemainingJumpsInRoute(remainingJumps);
                System.out.println("🎯 FSDTarget: " + remainingJumps + " saut(s) restant(s)");
                // Notifier le service pour rafraîchir l'affichage de la route
                navRouteNotificationService.notifyRouteRefreshRequired();
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du traitement de l'événement FSDTarget: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

