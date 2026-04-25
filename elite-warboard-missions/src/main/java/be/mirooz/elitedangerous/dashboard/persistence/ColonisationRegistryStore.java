package be.mirooz.elitedangerous.dashboard.persistence;

import be.mirooz.elitedangerous.dashboard.model.colonisation.ColonisationArchitectSystem;
import be.mirooz.elitedangerous.dashboard.model.registries.colonisation.ColonisationRegistry;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

public class ColonisationRegistryStore extends SnapshotJsonStore<ColonisationRegistryStore.Payload> {

    public ColonisationRegistryStore(Path file) {
        super(
                "colonisation-registry",
                file,
                PolymorphicPersistenceMapper.create(),
                Payload.class,
                ColonisationRegistryStore::buildSnapshot,
                ColonisationRegistryStore::restoreSnapshot
        );
    }

    private static Payload buildSnapshot() {
        ColonisationRegistry reg = ColonisationRegistry.getInstance();
        Payload p = new Payload();
        p.architectByStarSystem = reg.snapshotArchitectByStarSystem();
        p.beaconDeployedSystems = reg.snapshotBeaconDeployedSystems();
        p.currentConstructionMarketId = reg.getCurrentConstructionMarketId();
        return p;
    }

    private static void restoreSnapshot(Payload p) {
        ColonisationRegistry.getInstance().applyFullPersistedSnapshot(
                p.architectByStarSystem,
                p.beaconDeployedSystems,
                p.currentConstructionMarketId);
    }

    public static class Payload {
        public LinkedHashMap<String, ColonisationArchitectSystem> architectByStarSystem;
        public LinkedHashSet<String> beaconDeployedSystems;
        public Long currentConstructionMarketId;
    }
}
