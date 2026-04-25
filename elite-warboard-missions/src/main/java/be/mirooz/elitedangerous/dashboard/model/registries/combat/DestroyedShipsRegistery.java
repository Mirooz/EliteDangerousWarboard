package be.mirooz.elitedangerous.dashboard.model.registries.combat;

import be.mirooz.elitedangerous.dashboard.model.ships.DestroyedShip;
import be.mirooz.elitedangerous.dashboard.model.ships.DestroyedShipKind;
import be.mirooz.elitedangerous.dashboard.model.ships.Reward;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Liste des vaisseaux détruits - Singleton pour gérer l'état global
 */
public class DestroyedShipsRegistery {
    private static DestroyedShipsRegistery instance = new DestroyedShipsRegistery();
    @JsonIgnore
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
        if (ship.getKind() == null) {
            ship.setKind(DestroyedShipKind.UNKNOWN);
        }
        destroyedShips.add(0, ship);

        if (ship.getKind() == DestroyedShipKind.BOUNTY) {
            totalBountyEarned += ship.getTotalBountyReward();
            if (ship.getRewards() != null) {
                for (Reward reward : ship.getRewards()) {
                    bountyPerFaction.merge(
                            reward.factionName(),
                            reward.reward(),
                            Integer::sum
                    );
                }
            }
        } else if (ship.getKind() == DestroyedShipKind.CONFLICT) {
            totalConflictBounty += ship.getTotalBountyReward();
            if (ship.getRewards() != null) {
                for (Reward reward : ship.getRewards()) {
                    combatBondPerFaction.merge(
                            reward.factionName(),
                            reward.reward(),
                            Integer::sum
                    );
                }
            }
        }
    }

    public void clearAll() {
        clearBounty();
        clearCombatBond();
        destroyedShips.clear();
    }

    public void clearBounty() {
        totalBountyEarned = 0;
        this.bountyPerFaction = new HashMap<>();
        destroyedShips.removeIf(s -> s.getKind() == DestroyedShipKind.BOUNTY);
    }

    public void clearCombatBond() {
        totalConflictBounty = 0;
        this.combatBondPerFaction = new HashMap<>();
        destroyedShips.removeIf(s -> s.getKind() == DestroyedShipKind.CONFLICT);
    }

    public ObservableList<DestroyedShip> getDestroyedShips() {
        return destroyedShips;
    }

    @JsonProperty("ships")
    public List<DestroyedShip> getPersistedShips() {
        return new ArrayList<>(destroyedShips);
    }

    @JsonProperty("ships")
    public void setPersistedShips(List<DestroyedShip> shipsNewestFirst) {
        this.destroyedShips.clear();
        if (shipsNewestFirst != null) {
            for (DestroyedShip s : shipsNewestFirst) {
                if (s.getKind() == null) {
                    s.setKind(DestroyedShipKind.UNKNOWN);
                }
            }
            this.destroyedShips.addAll(shipsNewestFirst);
        }
    }

    public Map<String, Integer> getBountyPerFaction() {
        return bountyPerFaction;
    }

    public Map<String, Integer> getCombatBondPerFaction() {
        return combatBondPerFaction;
    }

}
