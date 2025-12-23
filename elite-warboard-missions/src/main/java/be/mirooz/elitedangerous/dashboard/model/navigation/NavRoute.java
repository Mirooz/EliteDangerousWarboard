package be.mirooz.elitedangerous.dashboard.model.navigation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Représente une route de navigation complète
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NavRoute {
    /**
     * Timestamp de l'événement NavRoute
     */
    private String timestamp;
    
    /**
     * Liste des systèmes dans la route
     * Le premier système est le système actuel
     */
    private List<RouteSystem> route = new ArrayList<>();
}

