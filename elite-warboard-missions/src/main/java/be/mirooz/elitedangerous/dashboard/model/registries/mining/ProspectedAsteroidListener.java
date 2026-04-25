package be.mirooz.elitedangerous.dashboard.model.registries.mining;

import be.mirooz.elitedangerous.dashboard.model.events.ProspectedAsteroid;

/**
 * Écoute des changements sur les astéroïdes prospectés. Même liste d’abonnés que
 * {@link be.mirooz.elitedangerous.dashboard.service.listeners.MiningEventNotificationService.MiningEventListener}
 * : {@link be.mirooz.elitedangerous.dashboard.service.listeners.MiningEventNotificationService#addProspectedAsteroidListener}
 * ou {@code addListener} si l’objet implémente aussi l’autre interface.
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
