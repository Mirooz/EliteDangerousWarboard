package be.mirooz.elitedangerous.dashboard.persistence;

import be.mirooz.elitedangerous.dashboard.model.commander.Mission;
import be.mirooz.elitedangerous.dashboard.model.registries.combat.MissionsRegistry;
import com.fasterxml.jackson.core.type.TypeReference;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class MissionsStore extends SnapshotJsonStore<LinkedHashMap<String, Mission>> {

    // Mapper tolérant : Mission#isActive() / isCompleted() / isPending() / etc. sont des
    // getters calculés Lombok → propriétés "active" / "completed" / "pending" dans le JSON,
    // sans setter. FAIL_ON_UNKNOWN_PROPERTIES=false évite de casser le load.
    private static final TypeReference<LinkedHashMap<String, Mission>> SNAPSHOT_TYPE =
            new TypeReference<>() {};

    public MissionsStore(Path file) {
        super(
                "missions",
                file,
                PolymorphicPersistenceMapper.createSimple(),
                SNAPSHOT_TYPE,
                MissionsStore::buildSnapshot,
                MissionsRegistry.getInstance()::applyFullPersistedSnapshot
        );
    }

    private static LinkedHashMap<String, Mission> buildSnapshot() {
        return new LinkedHashMap<>(
                MissionsRegistry.getInstance().getGlobalMissionMap());
    }
}
