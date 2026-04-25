package be.mirooz.elitedangerous.dashboard.persistence;

import be.mirooz.elitedangerous.dashboard.model.exploration.SystemVisited;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.SystemVisitedRegistry;
import com.fasterxml.jackson.core.type.TypeReference;

import java.nio.file.Path;
import java.util.LinkedHashMap;

public class SystemVisitedRegistryStore extends SnapshotJsonStore<LinkedHashMap<String, SystemVisited>> {

    private static final TypeReference<LinkedHashMap<String, SystemVisited>> SNAPSHOT_TYPE =
            new TypeReference<>() {};

    public SystemVisitedRegistryStore(Path file) {
        super(
                "system-visited-registry",
                file,
                PolymorphicPersistenceMapper.create(),
                SNAPSHOT_TYPE,
                SystemVisitedRegistryStore::buildSnapshot,
                SystemVisitedRegistry.getInstance()::applyFullPersistedSnapshot
        );
    }

    private static LinkedHashMap<String, SystemVisited> buildSnapshot() {
        return new LinkedHashMap<>(SystemVisitedRegistry.getInstance().snapshotSystems());
    }
}
