package be.mirooz.elitedangerous.dashboard.persistence;

import be.mirooz.elitedangerous.dashboard.model.colonisation.ColonisationArchitectSystem;
import be.mirooz.elitedangerous.dashboard.model.registries.colonisation.ColonisationRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

public class ColonisationRegistryStore implements RegistryStore {

    private final Path file;
    private final ObjectMapper mapper = PolymorphicPersistenceMapper.create();

    public ColonisationRegistryStore(Path file) {
        this.file = file;
    }

    @Override
    public String name() {
        return "colonisation-registry";
    }

    @Override
    public void save() {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            ColonisationRegistry reg = ColonisationRegistry.getInstance();
            Payload p = new Payload();
            p.architectByStarSystem = reg.snapshotArchitectByStarSystem();
            p.beaconDeployedSystems = reg.snapshotBeaconDeployedSystems();
            p.currentConstructionMarketId = reg.getCurrentConstructionMarketId();
            mapper.writeValue(file.toFile(), p);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot save colonisation registry to " + file, e);
        }
    }

    @Override
    public boolean loadIfExists() {
        if (!Files.exists(file)) {
            return false;
        }
        try {
            Payload p = mapper.readValue(file.toFile(), Payload.class);
            ColonisationRegistry.getInstance().applyFullPersistedSnapshot(
                    p.architectByStarSystem,
                    p.beaconDeployedSystems,
                    p.currentConstructionMarketId);
            return true;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load colonisation registry from " + file, e);
        }
    }

    @Override
    public void deleteIfExists() {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot delete colonisation registry file " + file, e);
        }
    }

    public static class Payload {
        public LinkedHashMap<String, ColonisationArchitectSystem> architectByStarSystem;
        public LinkedHashSet<String> beaconDeployedSystems;
        public Long currentConstructionMarketId;
    }
}
