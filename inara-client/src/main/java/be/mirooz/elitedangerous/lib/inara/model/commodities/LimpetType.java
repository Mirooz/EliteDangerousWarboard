package be.mirooz.elitedangerous.lib.inara.model.commodities;

import java.util.Optional;

/**
 * Enum représentant les limpets dans Elite Dangerous
 */
public enum LimpetType implements ICommodity {
    
    LIMPET("drones");

    private final String cargoJsonName;

    LimpetType(String cargoJsonName) {
        this.cargoJsonName = cargoJsonName;
    }

    @Override
    public String getInaraId() {
        return null;
    }

    @Override
    public String getInaraName() {
        return null;
    }

    @Override
    public String getEdToolName() {
        return null;
    }

    @Override
    public String getCargoJsonName() {
        return cargoJsonName;
    }

    @Override
    public CommodityType getCommodityType() {
        return CommodityType.LIMPET;
    }

    @Override
    public String toString() {
        return "Limpet";
    }

    /**
     * Recherche un limpet par son nom cargo JSON
     */
    public static Optional<LimpetType> fromCargoJsonName(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        
        if (LIMPET.cargoJsonName.equalsIgnoreCase(name)) {
            return Optional.of(LIMPET);
        }
        return Optional.empty();
    }
}
