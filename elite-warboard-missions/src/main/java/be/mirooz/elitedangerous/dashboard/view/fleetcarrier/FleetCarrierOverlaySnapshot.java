package be.mirooz.elitedangerous.dashboard.view.fleetcarrier;

import javafx.scene.paint.Color;

import java.util.List;
import java.util.Map;

/**
 * Données affichées dans l'overlay Fleet Carrier : mêmes lignes et surlignages que {@code fleetMarketGrid} du contrôleur.
 */
public record FleetCarrierOverlaySnapshot(
        boolean carrierStatsInitialized,
        List<FleetCarrierMarketRow> rows,
        Map<String, Integer> shipStockByMergeKey,
        Map<String, Color> routeHighlightByMergeKey) {
}
