package be.mirooz.elitedangerous.dashboard.persistence;

import be.mirooz.elitedangerous.dashboard.model.registries.fleetcarrier.CarrierStatus;

import java.nio.file.Path;

public class CarrierStatusStore extends SnapshotJsonStore<CarrierStatusSnapshot> {

    public CarrierStatusStore(Path file) {
        super(
                "carrier-status",
                file,
                PolymorphicPersistenceMapper.createSimple(),
                CarrierStatusSnapshot.class,
                () -> CarrierStatusSnapshot.fromRuntime(CarrierStatus.getInstance()),
                CarrierStatusSnapshot::restore
        );
    }
}