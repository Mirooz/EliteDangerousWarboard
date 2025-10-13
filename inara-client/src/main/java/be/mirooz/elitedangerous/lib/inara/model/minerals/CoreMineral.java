package be.mirooz.elitedangerous.lib.inara.model.minerals;

import lombok.ToString;

/**
 * Interface représentant un minéral de core mining dans Elite Dangerous
 */
public abstract class CoreMineral {
    
    // Constantes des noms de minéraux
    String VOID_OPAL = "Void Opal";
    String LOW_TEMPERATURE_DIAMONDS = "Low Temperature Diamonds";
    String ALEXANDRITE = "Alexandrite";
    String MONAZITE = "Monazite";
    String MUSGRAVITE = "Musgravite";
    String BENITOITE = "Benitoite";
    String GRANDIDIERITE = "Grandidierite";
    String RHODPLUMSITE = "Rhodplumsite";
    String SERENDIBITE = "Serendibite";
    String PAINITE = "Painite";
    String BROMELLITE = "Bromellite";


    public abstract String getInaraId();
    public abstract String getInaraName();
    @Override
    public String toString() {
        return getInaraName() + " (" + getInaraId() + ")";
    }

}