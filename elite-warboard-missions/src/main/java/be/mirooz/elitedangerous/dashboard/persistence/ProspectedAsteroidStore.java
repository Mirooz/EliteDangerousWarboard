package be.mirooz.elitedangerous.dashboard.persistence;

import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.Mineral;
import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.MineralType;
import be.mirooz.elitedangerous.dashboard.model.events.ProspectedAsteroid;
import be.mirooz.elitedangerous.dashboard.model.registries.mining.ProspectedAsteroidRegistry;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * {@link ProspectedAsteroid} référence {@link Mineral} (interface). Jackson est incapable
 * de deserialiser une interface polymorphe sans type info — d'où ce snapshot plat
 * qui sérialise l'enum sous son nom et le résout à la restauration.
 */
public class ProspectedAsteroidStore implements RegistryStore {

    private final Path file;
    private final ObjectMapper mapper = PolymorphicPersistenceMapper.createSimple();

    public ProspectedAsteroidStore(Path file) {
        this.file = file;
    }

    @Override
    public String name() {
        return "prospected-asteroids";
    }

    @Override
    public void save() {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Deque<ProspectedAsteroid> all = ProspectedAsteroidRegistry.getInstance().getAll();
            List<AsteroidSnapshot> snapshots = new ArrayList<>(all.size());
            for (ProspectedAsteroid a : all) {
                snapshots.add(AsteroidSnapshot.fromRuntime(a));
            }
            mapper.writeValue(file.toFile(), snapshots);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot save prospected asteroids to " + file, e);
        }
    }

    @Override
    public boolean loadIfExists() {
        if (!Files.exists(file)) {
            return false;
        }
        try {
            AsteroidSnapshot[] snapshots = mapper.readValue(file.toFile(), AsteroidSnapshot[].class);
            List<ProspectedAsteroid> restored = new ArrayList<>();
            for (AsteroidSnapshot s : snapshots) {
                ProspectedAsteroid p = s.toRuntime();
                if (p != null) restored.add(p);
            }
            ProspectedAsteroidRegistry.getInstance().applyFullPersistedSnapshot(restored);
            return true;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load prospected asteroids from " + file, e);
        }
    }

    @Override
    public void deleteIfExists() {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot delete prospected asteroids file " + file, e);
        }
    }

    static class AsteroidSnapshot {
        public String timestamp;
        public String event;
        public String motherlodeMaterial;
        public String coreMineralName; // MineralType enum name
        public String content;
        public String contentLocalised;
        public Double remaining;
        public boolean cracked;
        public List<MaterialSnapshot> materials;

        @JsonCreator
        public AsteroidSnapshot() {}

        static AsteroidSnapshot fromRuntime(ProspectedAsteroid a) {
            AsteroidSnapshot s = new AsteroidSnapshot();
            s.timestamp = a.getTimestamp();
            s.event = a.getEvent();
            s.motherlodeMaterial = a.getMotherlodeMaterial();
            s.coreMineralName = a.getCoreMineral() != null ? a.getCoreMineral().name() : null;
            s.content = a.getContent();
            s.contentLocalised = a.getContentLocalised();
            s.remaining = a.getRemaining();
            s.cracked = a.isCracked();
            s.materials = new ArrayList<>();
            if (a.getMaterials() != null) {
                for (ProspectedAsteroid.Material m : a.getMaterials()) {
                    s.materials.add(MaterialSnapshot.fromRuntime(m));
                }
            }
            return s;
        }

        ProspectedAsteroid toRuntime() {
            ProspectedAsteroid p = new ProspectedAsteroid();
            p.setTimestamp(timestamp);
            p.setEvent(event);
            p.setMotherlodeMaterial(motherlodeMaterial);
            if (coreMineralName != null) {
                try {
                    p.setCoreMineral(MineralType.valueOf(coreMineralName));
                } catch (IllegalArgumentException ignored) {
                    // Nouvel enum / renommé → on ignore silencieusement
                }
            }
            p.setContent(content);
            p.setContentLocalised(contentLocalised);
            p.setRemaining(remaining);
            p.setCracked(cracked);
            List<ProspectedAsteroid.Material> mats = new ArrayList<>();
            if (materials != null) {
                for (MaterialSnapshot ms : materials) {
                    ProspectedAsteroid.Material m = ms.toRuntime();
                    if (m != null) mats.add(m);
                }
            }
            p.setMaterials(mats);
            return p;
        }
    }

    static class MaterialSnapshot {
        @JsonProperty("name")
        public String mineralName;
        public String nameLocalised;
        public Double proportion;

        @JsonCreator
        public MaterialSnapshot() {}

        static MaterialSnapshot fromRuntime(ProspectedAsteroid.Material m) {
            MaterialSnapshot s = new MaterialSnapshot();
            s.mineralName = m.getName() instanceof MineralType mt ? mt.name() : null;
            s.nameLocalised = m.getNameLocalised();
            s.proportion = m.getProportion();
            return s;
        }

        ProspectedAsteroid.Material toRuntime() {
            ProspectedAsteroid.Material m = new ProspectedAsteroid.Material();
            if (mineralName != null) {
                try {
                    m.setName(MineralType.valueOf(mineralName));
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
            m.setNameLocalised(nameLocalised);
            m.setProportion(proportion);
            return m;
        }
    }
}
