package be.mirooz.elitedangerous.lib.inara.model.minerals;

/**
 * Low Temperature Diamonds - Minéral de core mining
 */
public class LowTemperatureDiamonds extends CoreMineral {
    
    @Override
    public String getInaraId() {
        return "144";
    }
    
    @Override
    public String getInaraName() {
        return "Low Temperature Diamonds";
    }
    
    @Override
    public String getEdToolName() {
        return "LowTemperatureDiamond"; // Sans espaces comme dans edtools.cc
    }
}
