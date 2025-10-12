package be.mirooz.elitedangerous.lib.inara.model.minerals;

/**
 * Bromellite - Minéral de core mining
 */
public class Bromellite implements CoreMineral {
    
    @Override
    public String getInaraId() {
        return "148";
    }
    
    @Override
    public String getInaraName() {
        return BROMELLITE;
    }
}
