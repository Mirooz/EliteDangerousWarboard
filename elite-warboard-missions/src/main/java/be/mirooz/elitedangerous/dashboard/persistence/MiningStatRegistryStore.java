package be.mirooz.elitedangerous.dashboard.persistence;

import be.mirooz.elitedangerous.dashboard.model.mining.MiningStat;
import be.mirooz.elitedangerous.dashboard.model.registries.mining.MiningStatRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MiningStatRegistryStore implements RegistryStore {

    private final Path file;
    private final ObjectMapper mapper = PolymorphicPersistenceMapper.create();

    public MiningStatRegistryStore(Path file) {
        this.file = file;
    }

    @Override
    public String name() {
        return "mining-stat-registry";
    }

    @Override
    public void save() {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            MiningStatRegistry reg = MiningStatRegistry.getInstance();
            Payload p = new Payload();
            p.miningStats = new ArrayList<>(reg.snapshotMiningStats());
            p.currentMiningSession = reg.snapshotCurrentMiningSession();
            mapper.writeValue(file.toFile(), p);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot save mining stat registry to " + file, e);
        }
    }

    @Override
    public boolean loadIfExists() {
        if (!Files.exists(file)) {
            return false;
        }
        try {
            Payload p = mapper.readValue(file.toFile(), Payload.class);
            MiningStatRegistry.getInstance().applyFullPersistedSnapshot(
                    p.miningStats, p.currentMiningSession);
            return true;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load mining stat registry from " + file, e);
        }
    }

    @Override
    public void deleteIfExists() {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot delete mining stat registry file " + file, e);
        }
    }

    public static class Payload {
        public List<MiningStat> miningStats;
        public MiningStat currentMiningSession;
    }
}
