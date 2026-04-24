package be.mirooz.elitedangerous.dashboard.persistence;

import be.mirooz.elitedangerous.dashboard.model.registries.commander.CommanderStatus;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CommanderStatusStore implements RegistryStore {

    private final Path file;
    private final ObjectMapper mapper = PolymorphicPersistenceMapper.createSimple();

    public CommanderStatusStore(Path file) {
        this.file = file;
    }

    @Override
    public String name() {
        return "commander-status";
    }

    @Override
    public void save() {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            mapper.writeValue(file.toFile(),
                    CommanderStatusSnapshot.fromRuntime(CommanderStatus.getInstance()));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot save commander status to " + file, e);
        }
    }

    @Override
    public boolean loadIfExists() {
        if (!Files.exists(file)) {
            return false;
        }
        try {
            mapper.readValue(file.toFile(), CommanderStatusSnapshot.class).restore();
            return true;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load commander status from " + file, e);
        }
    }

    @Override
    public void deleteIfExists() {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot delete commander status file " + file, e);
        }
    }
}
