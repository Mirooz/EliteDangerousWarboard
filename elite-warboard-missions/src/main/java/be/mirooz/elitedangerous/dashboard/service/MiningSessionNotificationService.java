package be.mirooz.elitedangerous.dashboard.service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service pour notifier les composants UI de la fin de session de minage
 */
public class MiningSessionNotificationService {

    private static MiningSessionNotificationService instance;
    private final List<Runnable> sessionEndListeners = new ArrayList<>();

    private MiningSessionNotificationService() {
    }

    public static MiningSessionNotificationService getInstance() {
        if (instance == null) {
            instance = new MiningSessionNotificationService();
        }
        return instance;
    }

    /**
     * Ajoute un listener pour les événements de fin de session de minage
     */
    public void addSessionEndListener(Runnable listener) {
        if (listener != null && !sessionEndListeners.contains(listener)) {
            sessionEndListeners.add(listener);
        }
    }

    /**
     * Supprime un listener
     */
    public void removeSessionEndListener(Runnable listener) {
        sessionEndListeners.remove(listener);
    }

    /**
     * Notifie tous les listeners de la fin de session de minage
     */
    public void notifySessionEnd() {
        System.out.println("📢 Notification de fin de session de minage envoyée à " + sessionEndListeners.size() + " listeners");
        for (Runnable listener : new ArrayList<>(sessionEndListeners)) {
            try {
                listener.run();
            } catch (Exception e) {
                System.err.println("❌ Erreur lors de la notification de fin de session: " + e.getMessage());
            }
        }
    }
}
