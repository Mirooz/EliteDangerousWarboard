package be.mirooz.elitedangerous.dashboard.model.exploration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Représente une vente complète de données d'exploration.
 * Accumule toutes les ventes MultiSellExplorationData jusqu'à l'événement Undocked.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExplorationDataSale {
    private String timestamp; // Timestamp de la première vente
    private String endTimestamp; // Timestamp de la dernière vente (Undocked)
    
    @Builder.Default
    private List<DiscoveredSystem> discoveredSystems = new ArrayList<>();
    
    private long baseValue; // Somme de tous les BaseValue
    private long bonus; // Somme de tous les Bonus
    private long totalEarnings; // Somme de tous les TotalEarnings
}

