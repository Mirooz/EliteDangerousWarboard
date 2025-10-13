package be.mirooz.elitedangerous.lib.inara.model.minerals;

import lombok.ToString;

/**
 * Interface représentant un minéral de core mining dans Elite Dangerous
 */
public abstract class CoreMineral {

    public abstract String getInaraId();
    public abstract String getInaraName();
    public abstract String getEdToolName();
    @Override
    public String toString() {
        return getInaraName() + " (" + getInaraId() + ")";
    }

}