package be.mirooz.elitedangerous.lib.inara.model.minerals;

/**
 * Interface représentant un minéral de core mining dans Elite Dangerous
 */
public interface CoreMineral {
    
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
    
    /**
     * Retourne l'ID unique du minéral
     * @return L'ID du minéral
     */
    String getInaraId();
    
    /**
     * Retourne le nom du minéral
     * @return Le nom du minéral
     */
    String getInaraName();
    
}