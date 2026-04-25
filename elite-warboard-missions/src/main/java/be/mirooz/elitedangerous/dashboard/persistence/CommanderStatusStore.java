package be.mirooz.elitedangerous.dashboard.persistence;

import be.mirooz.elitedangerous.dashboard.model.registries.commander.CommanderStatus;

import java.nio.file.Path;

public class CommanderStatusStore extends SnapshotJsonStore<CommanderStatusSnapshot> {

    public CommanderStatusStore(Path file) {
        super(
                "commander-status",
                file,
                PolymorphicPersistenceMapper.createSimple(),
                CommanderStatusSnapshot.class,
                () -> CommanderStatusSnapshot.fromRuntime(CommanderStatus.getInstance()),
                CommanderStatusSnapshot::restore
        );
    }
}
