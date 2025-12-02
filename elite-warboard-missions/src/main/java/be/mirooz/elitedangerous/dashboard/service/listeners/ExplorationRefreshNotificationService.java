package be.mirooz.elitedangerous.dashboard.service.listeners;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service pour gérer les notifications de rafraîchissement du panneau d'exploration
 * Permet aux composants UI de s'abonner aux événements nécessitant un refresh complet
 */
public class ExplorationRefreshNotificationService {
    
    private static ExplorationRefreshNotificationService instance;
    private final List<ExplorationRefreshListener> listeners = new CopyOnWriteArrayList<>();
    
    private ExplorationRefreshNotificationService() {}
    
    public static ExplorationRefreshNotificationService getInstance() {
        if (instance == null) {
            instance = new ExplorationRefreshNotificationService();
        }
        return instance;
    }
    
    /**
     * Ajoute un listener pour les événements de rafraîchissement d'exploration
     */
    public void addListener(ExplorationRefreshListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    /**
     * Retire un listener
     */
    public void removeListener(ExplorationRefreshListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Retire tous les listeners
     */
    public void clearListeners() {
        listeners.clear();
    }
    
    /**
     * Notifie tous les listeners qu'un refresh complet est nécessaire
     */
    public void notifyRefreshRequired() {
        System.out.println("Notify refresh Explo");
        for (ExplorationRefreshListener listener : listeners) {
            try {
                listener.onRefreshRequired();
            } catch (Exception e) {
                System.err.println("❌ Erreur lors de la notification de refresh d'exploration: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Interface pour écouter les événements de rafraîchissement d'exploration
     */
    public interface ExplorationRefreshListener {
        /**
         * Appelé quand un refresh complet du panneau d'exploration est nécessaire
         */
        void onRefreshRequired();
    }
}

