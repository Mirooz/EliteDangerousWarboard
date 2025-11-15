package be.mirooz.elitedangerous.dashboard.model.exploration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Représente une vente complète de données d'exploration.
 * Accumule toutes les ventes MultiSellExplorationData jusqu'à l'événement Undocked.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExplorationDataOnHold {
    @Builder.Default
    private Map<String,SystemVisited> systemsVisited = new HashMap<>();
    private long value; // Somme de tous les BaseValue
}

