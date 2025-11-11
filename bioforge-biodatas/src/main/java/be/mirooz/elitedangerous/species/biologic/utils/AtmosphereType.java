package be.mirooz.elitedangerous.species.biologic.utils;

public enum AtmosphereType {
    CARBON_DIOXIDE("CarbonDioxide"),
    AMMONIA("Ammonia"),
    NEON("Neon"),
    METHANE("Methane"),
    METHANE_RICH("MethaneRich"),
    SULPHUR_DIOXIDE("SulphurDioxide"),
    WATER("Water"),
    WATER_RICH("WaterRich"),
    NITROGEN("Nitrogen"),
    HELIUM("Helium"),
    ARGON("Argon"),
    NEON_RICH("NeonRich");

    private final String value;

    AtmosphereType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}

