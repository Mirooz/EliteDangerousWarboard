package be.mirooz.elitedangerous.dashboard.persistence;

import be.mirooz.elitedangerous.dashboard.model.exploration.SystemVisited;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.SystemVisitedRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;

public class SystemVisitedRegistryStore implements RegistryStore {

    private final Path file;
    private final ObjectMapper mapper = PolymorphicPersistenceMapper.create();

    public SystemVisitedRegistryStore(Path file) {
        this.file = file;
    }

    @Override
    public String name() {
        return "system-visited-registry";
    }

    @Override
    public void save() {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            LinkedHashMap<String, SystemVisited> snapshot =
                    new LinkedHashMap<>(SystemVisitedRegistry.getInstance().snapshotSystems());
            mapper.writeValue(file.toFile(), snapshot);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot save system visited registry to " + file, e);
        }
    }

    @Override
    public boolean loadIfExists() {
        if (!Files.exists(file)) {
            return false;
        }
        try {
            @SuppressWarnings("unchecked")
            LinkedHashMap<String, SystemVisited> snapshot =
                    mapper.readValue(file.toFile(), LinkedHashMap.class);
            SystemVisitedRegistry.getInstance().applyFullPersistedSnapshot(snapshot);
            return true;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load system visited registry from " + file, e);
        }
    }

    @Override
    public void deleteIfExists() {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot delete system visited registry file " + file, e);
        }
    }
}
