package be.mirooz.elitedangerous.dashboard.model.registries.exploration;

import be.mirooz.elitedangerous.dashboard.model.registries.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.model.exploration.ExplorationDataOnHold;
import be.mirooz.elitedangerous.dashboard.model.exploration.ExplorationDataSale;
import be.mirooz.elitedangerous.dashboard.model.exploration.SystemVisited;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("sales")
    private List<ExplorationDataSale> sales = FXCollections.observableArrayList();
    
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

    /**
     * Après une vente au marchand : le journal ne liste que les systèmes dont les données ont été
     * vendues, pas forcément le système d'amarrage (hub, bulle, etc.). On garde le système actuel
     * dans le groupe {@code currentSale} pour qu'il reste visible dans l'historique d'exploration.
     */
    public void appendCommanderStarSystemToCurrentSaleIfMissing(String journalTimestamp) {
        if (currentSale == null || currentSale.getSystemsVisited() == null) {
            return;
        }
        String name = CommanderStatus.getInstance().getCurrentStarSystem();
        if (name == null || name.isBlank()) {
            return;
        }
        boolean alreadyListed = currentSale.getSystemsVisited().stream()
                .anyMatch(s -> s != null && name.equals(s.getSystemName()));
        if (alreadyListed) {
            return;
        }
        SystemVisited fromRegistry = SystemVisitedRegistry.getInstance().getSystem(name);
        if (fromRegistry != null) {
            currentSale.getSystemsVisited().add(fromRegistry);
            return;
        }
        String ts = journalTimestamp != null && !journalTimestamp.isBlank() ? journalTimestamp : "";
        SystemVisited placeholder = SystemVisited.builder()
                .systemName(name)
                .firstVisitedTime(ts)
                .lastVisitedTime(ts)
                .sold(false)
                .build();
        currentSale.getSystemsVisited().add(placeholder);
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

