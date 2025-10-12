package be.mirooz.elitedangerous.lib.inara.model.minerals;

/**
 * Void Opal - Minéral de core mining
 */
public class VoidOpal implements CoreMineral {
    
    @Override
    public String getInaraId() {
        return "10250";
    }
    
    @Override
    public String getInaraName() {
        return VOID_OPAL;
    }
}
