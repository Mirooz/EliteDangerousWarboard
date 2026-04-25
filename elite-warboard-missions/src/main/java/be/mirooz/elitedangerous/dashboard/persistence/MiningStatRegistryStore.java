package be.mirooz.elitedangerous.dashboard.persistence;

import be.mirooz.elitedangerous.dashboard.model.mining.MiningStat;
import be.mirooz.elitedangerous.dashboard.model.registries.mining.MiningStatRegistry;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MiningStatRegistryStore extends SnapshotJsonStore<MiningStatRegistryStore.Payload> {

    public MiningStatRegistryStore(Path file) {
        super(
                "mining-stat-registry",
                file,
                PolymorphicPersistenceMapper.create(),
                Payload.class,
                MiningStatRegistryStore::buildSnapshot,
                MiningStatRegistryStore::restoreSnapshot
        );
    }

    private static Payload buildSnapshot() {
        MiningStatRegistry reg = MiningStatRegistry.getInstance();
        Payload p = new Payload();
        p.miningStats = new ArrayList<>(reg.snapshotMiningStats());
        p.currentMiningSession = reg.snapshotCurrentMiningSession();
        return p;
    }

    private static void restoreSnapshot(Payload p) {
        MiningStatRegistry.getInstance().applyFullPersistedSnapshot(
                p.miningStats, p.currentMiningSession);
    }

    public static class Payload {
        public List<MiningStat> miningStats;
        public MiningStat currentMiningSession;
    }
}
