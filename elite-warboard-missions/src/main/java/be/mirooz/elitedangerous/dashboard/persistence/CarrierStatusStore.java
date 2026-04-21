package be.mirooz.elitedangerous.dashboard.persistence;

import be.mirooz.elitedangerous.dashboard.model.registries.fleetcarrier.CarrierStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CarrierStatusStore {

    private final Path file;
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);

    public CarrierStatusStore(Path file) {
        this.file = file;
    }

    public void save() {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            mapper.writeValue(file.toFile(), CarrierStatusSnapshot.fromRuntime(CarrierStatus.getInstance()));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot save carrier status to " + file, e);
        }
    }

    public boolean loadIfExists() {
        if (!Files.exists(file)) {
            return false;
        }
        try {
            mapper.readValue(file.toFile(), CarrierStatusSnapshot.class).restore();
            return true;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load carrier status from " + file, e);
        }
    }

    public void deleteIfExists() {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot delete carrier status file " + file, e);
        }
    }
}