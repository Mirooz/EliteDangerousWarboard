package be.mirooz.elitedangerous.dashboard.persistence;

import be.mirooz.elitedangerous.dashboard.model.registries.combat.ShipTargetRegistry;
import be.mirooz.elitedangerous.dashboard.model.ships.ShipTarget;
import com.fasterxml.jackson.core.type.TypeReference;

import java.nio.file.Path;
import java.util.LinkedHashMap;

public class ShipTargetStore extends SnapshotJsonStore<LinkedHashMap<String, ShipTarget>> {

    // Mapper tolérant aux champs inconnus : ShipTarget#isPirate() / isDeserteur() sont des
    // getters calculés Lombok qui apparaissent comme propriétés "pirate"/"deserteur" dans
    // le JSON sérialisé, mais n'ont pas de setter → FAIL_ON_UNKNOWN_PROPERTIES=false.
    private static final TypeReference<LinkedHashMap<String, ShipTarget>> SNAPSHOT_TYPE =
            new TypeReference<>() {};

    public ShipTargetStore(Path file) {
        super(
                "ship-targets",
                file,
                PolymorphicPersistenceMapper.createSimple(),
                SNAPSHOT_TYPE,
                ShipTargetStore::buildSnapshot,
                ShipTargetRegistry.getInstance()::applyFullPersistedSnapshot
        );
    }

    private static LinkedHashMap<String, ShipTarget> buildSnapshot() {
        // LinkedHashMap → l'ordre d'insertion est préservé, c'est ce qu'on veut pour
        // l'affichage (plus récent au bout).
        return new LinkedHashMap<>(ShipTargetRegistry.getInstance().getAll());
    }
}
