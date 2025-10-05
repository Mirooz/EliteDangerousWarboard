package be.mirooz.elitedangerous.dashboard.model;

import be.mirooz.elitedangerous.dashboard.controller.ui.context.DashboardContext;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Liste des vaisseaux détruits - Singleton pour gérer l'état global
 */
public class DestroyedShipsRegistery {
    private static DestroyedShipsRegistery instance;
    private final ObservableList<DestroyedShip> destroyedShips =
            FXCollections.observableArrayList();
    private Map<String,Integer> bountyPerFaction = new HashMap<>();
    @Getter
    private int totalBountyEarned;
    @Getter
    private int shipsSinceLastReset;

    private DestroyedShipsRegistery() {
        this.destroyedShips.clear();
        this.totalBountyEarned = 0;
        this.shipsSinceLastReset = 0;
    }

    public static synchronized DestroyedShipsRegistery getInstance() {
        if (instance == null) {
            instance = new DestroyedShipsRegistery();
        }
        return instance;
    }

    public void addDestroyedShip(DestroyedShip ship) {
        destroyedShips.add(0,ship);
        totalBountyEarned += ship.getTotalBountyReward();
        shipsSinceLastReset++;
        for (Reward reward : ship.getRewards()) {
            bountyPerFaction.merge(
                    reward.factionName(),
                    reward.reward(),
                    Integer::sum
            );
        }
    }

    public void clearDestroyedShips() {
        destroyedShips.clear();
        clearBounty();
        clearRewards();
    }
    public void clearBounty(){
        totalBountyEarned=0;
        shipsSinceLastReset=0;
        // Vider la liste des vaisseaux détruits lors de l'encaissement des bounties
        destroyedShips.clear();
    }
    public void clearRewards(){
        this.bountyPerFaction = new HashMap<>();
    }
    public void addDestroyedShipsListener(Runnable action) {
        destroyedShips.addListener((ListChangeListener<DestroyedShip>) change -> {
            if (!DashboardContext.getInstance().isBatchLoading()) {
                Platform.runLater(action);
            }
        });
    }
    public ObservableList<DestroyedShip>  getDestroyedShips() {
        return destroyedShips;
    }

    public List<DestroyedShip> getDestroyedShipsByFaction(String faction) {
        return destroyedShips.stream()
                .filter(ship -> faction.equals(ship.getFaction()))
                .collect(Collectors.toList());
    }

    public int getDestroyedShipsCount() {
        return destroyedShips.size();
    }

    public int getDestroyedShipsCountByFaction(String faction) {
        return (int) destroyedShips.stream()
                .filter(ship -> faction.equals(ship.getFaction()))
                .count();
    }

    public int getBountyEarnedByFaction(String faction) {
        return destroyedShips.stream()
                .filter(ship -> faction.equals(ship.getFaction()))
                .mapToInt(DestroyedShip::getTotalBountyReward)
                .sum();
    }

    public Map<String, Integer> getBountyPerFaction() {
        return new HashMap<>(bountyPerFaction);
    }
}
