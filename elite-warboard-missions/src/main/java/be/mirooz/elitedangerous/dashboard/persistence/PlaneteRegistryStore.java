package be.mirooz.elitedangerous.dashboard.persistence;

import be.mirooz.elitedangerous.dashboard.model.exploration.ACelesteBody;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.PlaneteRegistry;

import java.nio.file.Path;
import java.util.LinkedHashMap;

public class PlaneteRegistryStore extends SnapshotJsonStore<PlaneteRegistryStore.Payload> {

    public PlaneteRegistryStore(Path file) {
        super(
                "planete-registry",
                file,
                PolymorphicPersistenceMapper.create(),
                Payload.class,
                PlaneteRegistryStore::buildSnapshot,
                PlaneteRegistryStore::restoreSnapshot
        );
    }

    private static Payload buildSnapshot() {
        PlaneteRegistry reg = PlaneteRegistry.getInstance();
        Payload p = new Payload();
        p.planetesMap = new LinkedHashMap<>(reg.snapshotPlanetesMap());
        p.currentStarSystem = reg.getCurrentStarSystem();
        return p;
    }

    private static void restoreSnapshot(Payload p) {
        PlaneteRegistry.getInstance().applyFullPersistedSnapshot(p.planetesMap, p.currentStarSystem);
    }

    public static class Payload {
        public LinkedHashMap<Integer, ACelesteBody> planetesMap;
        public String currentStarSystem;
    }
}
