package be.mirooz.elitedangerous.dashboard.model.registries.mining;

import be.mirooz.elitedangerous.dashboard.model.events.ProspectedAsteroid;

/**
 * Interface pour écouter les changements dans le ProspectedAsteroidRegistry
 */
public interface ProspectedAsteroidListener {
    
    /**
     * Appelé quand un nouveau prospecteur est ajouté au registre
     * @param prospector Le prospecteur qui a été ajouté
     */
    void onProspectorAdded(ProspectedAsteroid prospector);
    
    /**
     * Appelé quand le registre est vidé
     */
    void onRegistryCleared();
}
