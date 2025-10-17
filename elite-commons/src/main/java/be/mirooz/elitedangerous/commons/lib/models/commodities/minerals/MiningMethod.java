package be.mirooz.elitedangerous.commons.lib.models.commodities.minerals;

import lombok.Getter;

public enum MiningMethod {
    CORE("CORE MINING"),
    LASER("LASER MINING");

    @Getter
    private final String mining;
    MiningMethod(String mining) {
        this.mining = mining;
    }
}