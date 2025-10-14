package be.mirooz.elitedangerous.lib.inara.model.commodities.minerals;

import be.mirooz.elitedangerous.lib.inara.model.commodities.ICommodity;

import java.util.Optional;

/**
 * Interface représentant un minéral dans Elite Dangerous
 */
public interface Mineral extends ICommodity {
    
    /**
     * Retourne le type de minéral
     * @return Le type de minéral
     */
    MineralType getMineralType();

    String getMiningRefinedName();
    
    /**
     * Enum représentant les types de minéraux disponibles
     */
    enum MineralType {
        CORE_MINERAL,
        SURFACE_MINERAL
    }
}