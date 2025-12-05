package be.mirooz.elitedangerous.dashboard.model.registries.exploration;

import be.mirooz.elitedangerous.dashboard.model.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.model.exploration.ACelesteBody;
import be.mirooz.elitedangerous.dashboard.model.exploration.ExplorationDataOnHold;
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

    private ExplorationDataOnHold explorationDataOnHold = null;

    private ExplorationDataSaleRegistry() {
    }

    public static ExplorationDataSaleRegistry getInstance() {
        return INSTANCE;
    }

    public void addToOnHold(SystemVisited systemVisited) {
        if (explorationDataOnHold == null) {
            explorationDataOnHold = ExplorationDataOnHold.builder()
                    .startTimeStamp(systemVisited.getLastVisitedTime())
                    .build();
        }
        explorationDataOnHold.getSystemsVisitedMap().put(systemVisited.getSystemName(),systemVisited);
    }
    /**
     * Ajoute ou met à jour la vente en cours avec de nouvelles données.
     */
    public void addToCurrentSale(List<SystemVisited> discoveredSystems,
                                 long baseValue, long bonus, long totalEarnings, String timestamp) {
        if (currentSale == null) {
            // Récupérer le endTimestamp du précédent dans le registry (null si pas de précédent)
            String startTimeStamp = null;
            if (!sales.isEmpty()) {
                ExplorationDataSale previousSale = sales.get(sales.size() - 1);
                startTimeStamp = previousSale.getEndTimeStamp();
            }
            
            // Créer une nouvelle vente en cours
            currentSale = ExplorationDataSale.builder()
                    .startTimeStamp(startTimeStamp)
                    .endTimeStamp(timestamp)
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
        currentSale.setEndTimeStamp(timestamp);
    }

    /**
     * Finalise la vente en cours et l'ajoute à la liste des ventes.
     * Appelé lors de l'événement Undocked.
     */
    public void finalizeCurrentSale(String timestamp) {
        if (currentSale != null) {
            currentSale.setEndTimeStamp(timestamp);
            currentSale = null;
            //addToOnHold(SystemVisitedRegistry.getInstance().getSystem(CommanderStatus.getInstance().getCurrentStarSystem()));

        }
          }

    /**
     * Récupère toutes les ventes.
     */
    public List<ExplorationDataSale> getAllSales() {
        return sales;
    }


    public void clearOnHold() {
        explorationDataOnHold = null;
    }

    public void clearAll(){
        clearOnHold();
        getAllSales().clear();
    }
    /**
     * Retourne le nombre de ventes dans le registry.
     */
    public int size() {
        return sales.size();
    }
}

