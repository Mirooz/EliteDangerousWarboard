package be.mirooz.elitedangerous.dashboard.model.registries.exploration;

import be.mirooz.elitedangerous.dashboard.model.exploration.ExplorationDataSale;
import be.mirooz.elitedangerous.dashboard.model.exploration.SystemVisited;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Data;

import java.util.List;

/**
 * Registry pour stocker les ventes de données d'exploration.
 * Singleton observable pour la UI.
 */
@Data
public class ExplorationDataSaleRegistry {

    private static final ExplorationDataSaleRegistry INSTANCE = new ExplorationDataSaleRegistry();

    private final ObservableList<ExplorationDataSale> sales = FXCollections.observableArrayList();
    
    // Vente en cours (accumule les MultiSellExplorationData jusqu'à Undocked)
    private ExplorationDataSale currentSale = null;

    private ExplorationDataSaleRegistry() {
    }

    public static ExplorationDataSaleRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Ajoute ou met à jour la vente en cours avec de nouvelles données.
     */
    public void addToCurrentSale(List<SystemVisited> discoveredSystems,
                                 long baseValue, long bonus, long totalEarnings, String timestamp) {
        if (currentSale == null) {
            // Créer une nouvelle vente en cours
            currentSale = ExplorationDataSale.builder()
                    .timestamp(timestamp)
                    .endTimestamp(timestamp)
                    .systemsVisited(new java.util.ArrayList<>())
                    .baseValue(0)
                    .bonus(0)
                    .totalEarnings(0)
                    .build();
            sales.add(currentSale);
        }
        
        // Ajouter les systèmes découverts
        currentSale.getSystemsVisited().addAll(discoveredSystems);
        
        // Accumuler les valeurs
        currentSale.setBaseValue(currentSale.getBaseValue() + baseValue);
        currentSale.setBonus(currentSale.getBonus() + bonus);
        currentSale.setTotalEarnings(currentSale.getTotalEarnings() + totalEarnings);
        currentSale.setEndTimestamp(timestamp);
    }

    /**
     * Finalise la vente en cours et l'ajoute à la liste des ventes.
     * Appelé lors de l'événement Undocked.
     */
    public void finalizeCurrentSale(String timestamp) {
        if (currentSale != null) {
            currentSale.setEndTimestamp(timestamp);
            currentSale = null;
        }
    }

    /**
     * Récupère toutes les ventes.
     */
    public List<ExplorationDataSale> getAllSales() {
        return sales;
    }

    /**
     * Vide le registry.
     */
    public void clear() {
        sales.clear();
        currentSale = null;
    }

    /**
     * Retourne le nombre de ventes dans le registry.
     */
    public int size() {
        return sales.size();
    }
}

