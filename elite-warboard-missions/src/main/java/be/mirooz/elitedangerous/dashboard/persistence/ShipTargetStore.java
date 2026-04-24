package be.mirooz.elitedangerous.dashboard.persistence;

import be.mirooz.elitedangerous.dashboard.model.registries.combat.ShipTargetRegistry;
import be.mirooz.elitedangerous.dashboard.model.ships.ShipTarget;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class ShipTargetStore implements RegistryStore {

    private final Path file;
    // Mapper tolérant aux champs inconnus : ShipTarget#isPirate() / isDeserteur() sont des
    // getters calculés Lombok qui apparaissent comme propriétés "pirate"/"deserteur" dans
    // le JSON sérialisé, mais n'ont pas de setter → FAIL_ON_UNKNOWN_PROPERTIES=false.
    private final ObjectMapper mapper = PolymorphicPersistenceMapper.createSimple();

    public ShipTargetStore(Path file) {
        this.file = file;
    }

    @Override
    public String name() {
        return "ship-targets";
    }

    @Override
    public void save() {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            // LinkedHashMap → l'ordre d'insertion est préservé, c'est ce qu'on veut pour
            // l'affichage (plus récent au bout).
            Map<String, ShipTarget> snapshot = new LinkedHashMap<>(
                    ShipTargetRegistry.getInstance().getAll());
            mapper.writeValue(file.toFile(), snapshot);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot save ship targets to " + file, e);
        }
    }

    @Override
    public boolean loadIfExists() {
        if (!Files.exists(file)) {
            return false;
        }
        try {
            Map<String, ShipTarget> snapshot = mapper.readValue(file.toFile(),
                    new TypeReference<LinkedHashMap<String, ShipTarget>>() {});
            ShipTargetRegistry.getInstance().applyFullPersistedSnapshot(snapshot);
            return true;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load ship targets from " + file, e);
        }
    }

    @Override
    public void deleteIfExists() {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot delete ship targets file " + file, e);
        }
    }
}
