package be.mirooz.elitedangerous.lib.inara.model.minerals;

/**
 * Alexandrite - Minéral de core mining
 */
public class Alexandrite implements CoreMineral {
    
    @Override
    public String getInaraId() {
        return "10249";
    }
    
    @Override
    public String getInaraName() {
        return ALEXANDRITE;
    }
}
