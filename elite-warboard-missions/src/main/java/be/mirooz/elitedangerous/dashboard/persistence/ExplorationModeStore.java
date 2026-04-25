package be.mirooz.elitedangerous.dashboard.persistence;

import be.mirooz.elitedangerous.backend.spansh.ExplorationMode;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.ExplorationModeRegistry;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.nio.file.Path;

/**
 * Store ultra-simple : un seul enum à persister ({@link ExplorationMode}). On n'a pas créé
 * de classe {@code Snapshot} dédiée — la payload JSON est un objet à une clé.
 */
public class ExplorationModeStore extends SnapshotJsonStore<ExplorationModeStore.Payload> {

    public ExplorationModeStore(Path file) {
        super(
                "exploration-mode",
                file,
                PolymorphicPersistenceMapper.createSimple(),
                Payload.class,
                () -> new Payload(ExplorationModeRegistry.getInstance().getCurrentMode()),
                ExplorationModeStore::restoreSnapshot
        );
    }

    private static void restoreSnapshot(Payload payload) {
        if (payload != null && payload.mode != null) {
            ExplorationModeRegistry.getInstance().setCurrentMode(payload.mode);
        }
    }

    static class Payload {
        @JsonProperty
        public ExplorationMode mode;

        public Payload() {}

        public Payload(ExplorationMode mode) {
            this.mode = mode;
        }
    }
}
