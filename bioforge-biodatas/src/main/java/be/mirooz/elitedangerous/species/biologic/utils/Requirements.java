package be.mirooz.elitedangerous.species.biologic.utils;

import lombok.Data;

import java.util.List;

@Data
public class Requirements {
        private final AtmosphereDensity atmosphereDensity;
        private final double minGravityG;
        private final double maxGravityG;
        private final List<PlanetType> allowedPlanetTypes;
        private final Double minPressureAtm;
        private final Double maxPressureAtm;
        private final List<AtmosphereCondition> atmosphereConditions; // Alternative atmospheres with their temperature ranges
        private final List<VolcanismType> requiredVolcanism; // Required volcanism types (OR condition)
    }