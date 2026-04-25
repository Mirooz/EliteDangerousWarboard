package be.mirooz.elitedangerous.dashboard.model.registries.combat;

import be.mirooz.elitedangerous.dashboard.model.ships.DestroyedShip;
import be.mirooz.elitedangerous.dashboard.model.ships.DestroyedShipKind;
import be.mirooz.elitedangerous.dashboard.model.ships.Reward;
import com.fasterxml.jackson.annotation.JsonCreator;
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
    public void applyFullPersistedSnapshot(List<DestroyedShip> shipsNewestFirst,
                                           Map<String, Integer> bountyPerFaction,
                                           Map<String, Integer> combatBondPerFaction,
                                           int totalBountyEarned,
                                           int totalConflictBounty) {
        this.destroyedShips.clear();
        if (shipsNewestFirst != null) {
            for (DestroyedShip s : shipsNewestFirst) {
                if (s.getKind() == null) {
                    s.setKind(DestroyedShipKind.UNKNOWN);
                }
            }
            this.destroyedShips.addAll(shipsNewestFirst);
        }
        this.bountyPerFaction = bountyPerFaction != null ? new HashMap<>(bountyPerFaction) : new HashMap<>();
        this.combatBondPerFaction = combatBondPerFaction != null ? new HashMap<>(combatBondPerFaction) : new HashMap<>();
        this.totalBountyEarned = totalBountyEarned;
        this.totalConflictBounty = totalConflictBounty;
    }

    /**
     * DTO JSON {@code destroyed-ships.json} : la liste d'événements est directement
     * {@link DestroyedShip} (champ {@code type} = {@link DestroyedShipKind}).
     */
    public static final class PersistenceFile {
        public List<DestroyedShip> ships = new ArrayList<>();
        public Map<String, Integer> bountyPerFaction = new HashMap<>();
        public Map<String, Integer> combatBondPerFaction = new HashMap<>();
        public int totalBountyEarned;
        public int totalConflictBounty;

        @JsonCreator
        public PersistenceFile() {}

        public static PersistenceFile fromRuntime(DestroyedShipsRegistery reg) {
            PersistenceFile p = new PersistenceFile();
            p.ships = new ArrayList<>(reg.getDestroyedShips());
            p.bountyPerFaction.putAll(reg.getBountyPerFaction());
            p.combatBondPerFaction.putAll(reg.getCombatBondPerFaction());
            p.totalBountyEarned = reg.getTotalBountyEarned();
            p.totalConflictBounty = reg.getTotalConflictBounty();
            return p;
        }

        public void restore() {
            DestroyedShipsRegistery.getInstance().applyFullPersistedSnapshot(
                    ships, bountyPerFaction, combatBondPerFaction,
                    totalBountyEarned, totalConflictBounty
            );
        }
    }
}
