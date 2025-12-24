package be.mirooz.elitedangerous.dashboard.service.listeners;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service pour gérer les notifications de rafraîchissement de la route de navigation
 * Permet aux composants UI de s'abonner aux événements nécessitant un refresh de la route
 */
public class NavRouteNotificationService {
    
    private static NavRouteNotificationService instance;
    private final List<NavRouteRefreshListener> listeners = new CopyOnWriteArrayList<>();
    
    private NavRouteNotificationService() {}
    
    public static NavRouteNotificationService getInstance() {
        if (instance == null) {
            instance = new NavRouteNotificationService();
        }
        return instance;
    }
    
    /**
     * Ajoute un listener pour les événements de rafraîchissement de route
     */
    public void addListener(NavRouteRefreshListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    /**
     * Retire un listener
     */
    public void removeListener(NavRouteRefreshListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Retire tous les listeners
     */
    public void clearListeners() {
        listeners.clear();
    }
    
    /**
     * Notifie tous les listeners qu'un refresh de la route est nécessaire
     */
    public void notifyRouteRefreshRequired() {
        for (NavRouteRefreshListener listener : listeners) {
            try {
                listener.onRouteRefreshRequired();
            } catch (Exception e) {
                System.err.println("❌ Erreur lors de la notification de refresh de route: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Interface pour écouter les événements de rafraîchissement de route
     */
    public interface NavRouteRefreshListener {
        /**
         * Appelé quand un refresh de la route de navigation est nécessaire
         */
        void onRouteRefreshRequired();
    }
}

