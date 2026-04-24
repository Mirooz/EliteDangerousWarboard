package be.mirooz.elitedangerous.dashboard.persistence;

import be.mirooz.elitedangerous.dashboard.model.commander.Mission;
import be.mirooz.elitedangerous.dashboard.model.registries.combat.MissionsRegistry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class MissionsStore implements RegistryStore {

    private final Path file;
    // Mapper tolérant : Mission#isActive() / isCompleted() / isPending() / etc. sont des
    // getters calculés Lombok → propriétés "active" / "completed" / "pending" dans le JSON,
    // sans setter. FAIL_ON_UNKNOWN_PROPERTIES=false évite de casser le load.
    private final ObjectMapper mapper = PolymorphicPersistenceMapper.createSimple();

    public MissionsStore(Path file) {
        this.file = file;
    }

    @Override
    public String name() {
        return "missions";
    }

    @Override
    public void save() {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Map<String, Mission> snapshot = new LinkedHashMap<>(
                    MissionsRegistry.getInstance().getGlobalMissionMap());
            mapper.writeValue(file.toFile(), snapshot);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot save missions to " + file, e);
        }
    }

    @Override
    public boolean loadIfExists() {
        if (!Files.exists(file)) {
            return false;
        }
        try {
            Map<String, Mission> snapshot = mapper.readValue(file.toFile(),
                    new TypeReference<LinkedHashMap<String, Mission>>() {});
            MissionsRegistry.getInstance().applyFullPersistedSnapshot(snapshot);
            return true;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load missions from " + file, e);
        }
    }

    @Override
    public void deleteIfExists() {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot delete missions file " + file, e);
        }
    }
}
