package be.mirooz.elitedangerous.dashboard.persistence;

import be.mirooz.elitedangerous.backend.spansh.ExplorationMode;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.ExplorationModeRegistry;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Store ultra-simple : un seul enum à persister ({@link ExplorationMode}). On n'a pas créé
 * de classe {@code Snapshot} dédiée — la payload JSON est un objet à une clé.
 */
public class ExplorationModeStore implements RegistryStore {

    private final Path file;
    private final ObjectMapper mapper = PolymorphicPersistenceMapper.createSimple();

    public ExplorationModeStore(Path file) {
        this.file = file;
    }

    @Override
    public String name() {
        return "exploration-mode";
    }

    @Override
    public void save() {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            mapper.writeValue(file.toFile(),
                    new Payload(ExplorationModeRegistry.getInstance().getCurrentMode()));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot save exploration mode to " + file, e);
        }
    }

    @Override
    public boolean loadIfExists() {
        if (!Files.exists(file)) {
            return false;
        }
        try {
            Payload payload = mapper.readValue(file.toFile(), Payload.class);
            if (payload != null && payload.mode != null) {
                ExplorationModeRegistry.getInstance().setCurrentMode(payload.mode);
            }
            return true;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load exploration mode from " + file, e);
        }
    }

    @Override
    public void deleteIfExists() {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot delete exploration mode file " + file, e);
        }
    }

    private static class Payload {
        @JsonProperty
        public ExplorationMode mode;

        public Payload() {}
        public Payload(ExplorationMode mode) { this.mode = mode; }
    }
}
