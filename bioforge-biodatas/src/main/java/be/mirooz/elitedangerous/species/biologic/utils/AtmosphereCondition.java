package be.mirooz.elitedangerous.species.biologic.utils;

import lombok.Data;

@Data
public class AtmosphereCondition {
    private final AtmosphereType atmosphere;
    private final Double minTemperatureK;
    private final Double maxTemperatureK;

    public AtmosphereCondition(AtmosphereType atmosphere, Double minTemperatureK, Double maxTemperatureK) {
        this.atmosphere = atmosphere;
        this.minTemperatureK = minTemperatureK;
        this.maxTemperatureK = maxTemperatureK;
    }
}

