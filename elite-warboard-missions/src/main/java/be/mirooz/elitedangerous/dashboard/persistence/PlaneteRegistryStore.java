package be.mirooz.elitedangerous.dashboard.persistence;

import be.mirooz.elitedangerous.dashboard.model.exploration.ACelesteBody;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.PlaneteRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;

public class PlaneteRegistryStore implements RegistryStore {

    private final Path file;
    private final ObjectMapper mapper = PolymorphicPersistenceMapper.create();

    public PlaneteRegistryStore(Path file) {
        this.file = file;
    }

    @Override
    public String name() {
        return "planete-registry";
    }

    @Override
    public void save() {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            PlaneteRegistry reg = PlaneteRegistry.getInstance();
            Payload p = new Payload();
            p.planetesMap = new LinkedHashMap<>(reg.snapshotPlanetesMap());
            p.currentStarSystem = reg.getCurrentStarSystem();
            mapper.writeValue(file.toFile(), p);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot save planete registry to " + file, e);
        }
    }

    @Override
    public boolean loadIfExists() {
        if (!Files.exists(file)) {
            return false;
        }
        try {
            Payload p = mapper.readValue(file.toFile(), Payload.class);
            PlaneteRegistry.getInstance().applyFullPersistedSnapshot(p.planetesMap, p.currentStarSystem);
            return true;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load planete registry from " + file, e);
        }
    }

    @Override
    public void deleteIfExists() {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot delete planete registry file " + file, e);
        }
    }

    public static class Payload {
        public LinkedHashMap<Integer, ACelesteBody> planetesMap;
        public String currentStarSystem;
    }
}
