package be.mirooz.elitedangerous.dashboard.persistence;

import be.mirooz.elitedangerous.dashboard.model.registries.combat.DestroyedShipsRegistery;
import be.mirooz.elitedangerous.dashboard.model.ships.DestroyedBountyShip;
import be.mirooz.elitedangerous.dashboard.model.ships.DestroyedConflictShip;
import be.mirooz.elitedangerous.dashboard.model.ships.DestroyedShip;
import be.mirooz.elitedangerous.dashboard.model.ships.Reward;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link DestroyedShip} est abstraite + {@code @SuperBuilder} sans no-arg ctor : on passe par
 * un snapshot plat avec un discriminateur de type pour éviter d'annoter les POJOs domain.
 */
public class DestroyedShipsStore extends SnapshotJsonStore<DestroyedShipsStore.Payload> {

    public DestroyedShipsStore(Path file) {
        super(
                "destroyed-ships",
                file,
                PolymorphicPersistenceMapper.createSimple(),
                Payload.class,
                Payload::fromRuntime,
                Payload::apply
        );
    }

    static class Payload {
        public List<ShipEntry> ships = new ArrayList<>();
        public Map<String, Integer> bountyPerFaction = new HashMap<>();
        public Map<String, Integer> combatBondPerFaction = new HashMap<>();
        public int totalBountyEarned;
        public int totalConflictBounty;

        @JsonCreator
        public Payload() {}

        static Payload fromRuntime() {
            DestroyedShipsRegistery reg = DestroyedShipsRegistery.getInstance();
            Payload p = new Payload();
            for (DestroyedShip s : reg.getDestroyedShips()) {
                p.ships.add(ShipEntry.fromRuntime(s));
            }
            p.bountyPerFaction.putAll(reg.getBountyPerFaction());
            p.combatBondPerFaction.putAll(reg.getCombatBondPerFaction());
            p.totalBountyEarned = reg.getTotalBountyEarned();
            p.totalConflictBounty = reg.getTotalConflictBounty();
            return p;
        }

        void apply() {
            List<DestroyedShip> restored = new ArrayList<>();
            if (ships != null) {
                for (ShipEntry e : ships) {
                    DestroyedShip ship = e.toRuntime();
                    if (ship != null) {
                        restored.add(ship);
                    }
                }
            }
            DestroyedShipsRegistery.getInstance().applyFullPersistedSnapshot(
                    restored,
                    bountyPerFaction,
                    combatBondPerFaction,
                    totalBountyEarned,
                    totalConflictBounty
            );
        }
    }

    static class ShipEntry {
        public String type; // "BOUNTY" ou "CONFLICT"
        public String shipName;
        public String pilotName;
        public String faction;
        public String bountyFaction;
        public List<Reward> rewards;
        public int totalBountyReward;
        public LocalDateTime destroyedTime;

        @JsonCreator
        public ShipEntry() {}

        static ShipEntry fromRuntime(DestroyedShip s) {
            ShipEntry e = new ShipEntry();
            if (s instanceof DestroyedBountyShip) {
                e.type = "BOUNTY";
            } else if (s instanceof DestroyedConflictShip) {
                e.type = "CONFLICT";
            } else {
                e.type = "UNKNOWN";
            }
            e.shipName = s.getShipName();
            e.pilotName = s.getPilotName();
            e.faction = s.getFaction();
            e.bountyFaction = s.getBountyFaction();
            e.rewards = s.getRewards();
            e.totalBountyReward = s.getTotalBountyReward();
            e.destroyedTime = s.getDestroyedTime();
            return e;
        }

        DestroyedShip toRuntime() {
            if ("BOUNTY".equals(type)) {
                return DestroyedBountyShip.builder()
                        .shipName(shipName).pilotName(pilotName).faction(faction)
                        .bountyFaction(bountyFaction).rewards(rewards)
                        .totalBountyReward(totalBountyReward).destroyedTime(destroyedTime)
                        .build();
            }
            if ("CONFLICT".equals(type)) {
                return DestroyedConflictShip.builder()
                        .shipName(shipName).pilotName(pilotName).faction(faction)
                        .bountyFaction(bountyFaction).rewards(rewards)
                        .totalBountyReward(totalBountyReward).destroyedTime(destroyedTime)
                        .build();
            }
            return null;
        }
    }
}
