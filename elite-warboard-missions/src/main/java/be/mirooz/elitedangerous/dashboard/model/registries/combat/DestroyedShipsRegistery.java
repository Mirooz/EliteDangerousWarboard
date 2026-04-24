package be.mirooz.elitedangerous.dashboard.model.registries.combat;

import be.mirooz.elitedangerous.dashboard.model.ships.DestroyedBountyShip;
import be.mirooz.elitedangerous.dashboard.model.ships.DestroyedConflictShip;
import be.mirooz.elitedangerous.dashboard.model.ships.DestroyedShip;
import be.mirooz.elitedangerous.dashboard.model.ships.Reward;
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

    public void clearAll() {
        // Réinitialise les récompenses de bounty
        clearBounty();

        // Réinitialise les récompenses de combat
        clearCombatBond();

        // Vide la liste principale de tous les vaisseaux restants (au cas où il y aurait d'autres types)
        destroyedShips.clear();
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

    /**
     * Restauration en bloc depuis un snapshot persisté. Pas de recomputation : les agrégats
     * ({@link #totalBountyEarned}, {@link #totalConflictBounty}, maps par faction) sont
     * copiés tels quels pour coller à l'état affiché au moment du save.
     */
    public void applyFullPersistedSnapshot(java.util.List<DestroyedShip> shipsNewestFirst,
                                           Map<String, Integer> bountyPerFaction,
                                           Map<String, Integer> combatBondPerFaction,
                                           int totalBountyEarned,
                                           int totalConflictBounty) {
        this.destroyedShips.clear();
        if (shipsNewestFirst != null) {
            this.destroyedShips.addAll(shipsNewestFirst);
        }
        this.bountyPerFaction = bountyPerFaction != null ? new HashMap<>(bountyPerFaction) : new HashMap<>();
        this.combatBondPerFaction = combatBondPerFaction != null ? new HashMap<>(combatBondPerFaction) : new HashMap<>();
        this.totalBountyEarned = totalBountyEarned;
        this.totalConflictBounty = totalConflictBounty;
    }
}
