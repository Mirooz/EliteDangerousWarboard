package be.mirooz.elitedangerous.lib.inara.model.minerals;

/**
 * Painite - Minéral de core mining
 */
public class Painite implements CoreMineral {
    
    @Override
    public String getInaraId() {
        return "84";
    }
    
    @Override
    public String getInaraName() {
        return PAINITE;
    }
}
