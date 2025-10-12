package be.mirooz.elitedangerous.lib.inara.model.minerals;

/**
 * Musgravite - Minéral de core mining
 */
public class Musgravite implements CoreMineral {
    
    @Override
    public String getInaraId() {
        return "10246";
    }
    
    @Override
    public String getInaraName() {
        return MUSGRAVITE;
    }
}
