package be.mirooz.elitedangerous.commons.lib.models.commodities.minerals;

import be.mirooz.elitedangerous.commons.lib.models.commodities.ICommodity;

/**
 * Représente un minerai inconnu, utilisé comme fallback quand le nom ne correspond à aucun connu.
 */
public record UnknownMineral(String rawName) implements Mineral {

    @Override
    public MiningMethod getMiningMethod() {
        return MiningMethod.LASER;
    }

    @Override
    public String getMiningRefinedName() {
        return rawName;
    }

    @Override
    public int getPrice() {
        return 0;
    }

    @Override
    public void setPrice(int price) {

    }

    @Override
    public String getInaraId() {
        return null;
    }

    @Override
    public String getInaraName() {
        return rawName;
    }

    @Override
    public String getEdToolName() {
        return rawName;
    }

    @Override
    public String getCargoJsonName() {
        return rawName;
    }

    @Override
    public ICommodity.CommodityType getCommodityType() {
        return CommodityType.MINERAL;
    }

    @Override
    public String toString() {
        return "[UnknownMineral: " + rawName + "]";
    }
}
