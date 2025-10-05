package be.mirooz.elitedangerous.dashboard.model.registries;

import be.mirooz.elitedangerous.dashboard.model.DestroyedBountyShip;
import be.mirooz.elitedangerous.dashboard.model.DestroyedConflictShip;
import be.mirooz.elitedangerous.dashboard.model.DestroyedShip;
import be.mirooz.elitedangerous.dashboard.model.Reward;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Liste des vaisseaux détruits - Singleton pour gérer l'état global
 */
public class DestroyedShipsRegistery {
    private static DestroyedShipsRegistery instance = new DestroyedShipsRegistery();
    private final ObservableList<DestroyedShip> destroyedShips =
            FXCollections.observableArrayList();
    private Map<String,Integer> bountyPerFaction = new HashMap<>();
    private Map<String,Integer> combatBondPerFaction = new HashMap<>();
    @Getter
    private int totalBountyEarned;
    @Getter
    private int totalConflictBounty;

    private DestroyedShipsRegistery() {
        this.destroyedShips.clear();
        this.totalBountyEarned = 0;
        this.totalConflictBounty = 0;
    }

    public static DestroyedShipsRegistery getInstance() {
        if (instance == null) {
            instance = new DestroyedShipsRegistery();
        }
        return instance;
    }

    public void addDestroyedShip(DestroyedShip ship) {
        destroyedShips.add(0, ship);

        if (ship instanceof DestroyedBountyShip bountyShip) {
            totalBountyEarned += bountyShip.getTotalBountyReward();
            for (Reward reward : bountyShip.getRewards()) {
                bountyPerFaction.merge(
                        reward.factionName(),
                        reward.reward(),
                        Integer::sum
                );
            }

        } else if (ship instanceof DestroyedConflictShip conflictShip) {
            totalConflictBounty += conflictShip.getTotalBountyReward();
            for (Reward reward : conflictShip.getRewards()) {
                combatBondPerFaction.merge(
                        reward.factionName(),
                        reward.reward(),
                        Integer::sum
                );
            }

        } else {
            // Autres types éventuels si besoin
        }
    }


    public void clearBounty(){
        totalBountyEarned=0;
        this.bountyPerFaction = new HashMap<>();
        destroyedShips.removeIf(destroyedShip -> destroyedShip instanceof DestroyedBountyShip);
    }
    public void clearCombatBond(){
        totalConflictBounty =0;
        this.combatBondPerFaction = new HashMap<>();
        destroyedShips.removeIf(destroyedShip -> destroyedShip instanceof DestroyedConflictShip);

    }

    public ObservableList<DestroyedShip>  getDestroyedShips() {
        return destroyedShips;
    }

    public Map<String, Integer> getBountyPerFaction() {
        return bountyPerFaction;
    }

    public Map<String, Integer> getCombatBondPerFaction() {
        return combatBondPerFaction;
    }
}
