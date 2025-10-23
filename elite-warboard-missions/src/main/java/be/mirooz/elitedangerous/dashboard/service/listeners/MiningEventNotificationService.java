package be.mirooz.elitedangerous.dashboard.service.listeners;

import be.mirooz.elitedangerous.dashboard.model.events.ProspectedAsteroid;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service pour gérer les notifications liées aux événements de minage
 * Permet aux composants UI de s'abonner aux événements de minage
 */
public class MiningEventNotificationService {
    
    private static MiningEventNotificationService instance;
    private final List<MiningEventListener> listeners = new CopyOnWriteArrayList<>();
    
    private MiningEventNotificationService() {}
    
    public static MiningEventNotificationService getInstance() {
        if (instance == null) {
            instance = new MiningEventNotificationService();
        }
        return instance;
    }
    
    /**
     * Ajoute un listener pour les événements de minage
     */
    public void addListener(MiningEventListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Retire un listener
     */
    public void removeListener(MiningEventListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Notifie tous les listeners qu'un astéroïde a été craqué
     */
    public void notifyAsteroidCracked(ProspectedAsteroid prospector) {
        for (MiningEventListener listener : listeners) {
            try {
                listener.onAsteroidCracked(prospector);
            } catch (Exception e) {
                System.err.println("Erreur lors de la notification AsteroidCracked: " + e.getMessage());
            }
        }
    }
    
    /**
     * Interface pour écouter les événements de minage
     */
    public interface MiningEventListener {
        /**
         * Appelé quand un astéroïde est craqué
         * @param prospector Le prospecteur associé à l'astéroïde craqué
         */
        void onAsteroidCracked(ProspectedAsteroid prospector);
    }
}
