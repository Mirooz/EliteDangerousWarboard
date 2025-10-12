package be.mirooz.elitedangerous.lib.inara.model.minerals;

/**
 * Benitoite - Minéral de core mining
 */
public class Benitoite implements CoreMineral {
    
    @Override
    public String getInaraId() {
        return "10247";
    }
    
    @Override
    public String getInaraName() {
        return BENITOITE;
    }
}
