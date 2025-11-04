package be.mirooz.elitedangerous.dashboard.service.listeners;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service pour gérer les notifications liées aux événements de minage
 * Permet aux composants UI de s'abonner aux événements de minage
 */
public class MissionEventNotificationService {

    private static MissionEventNotificationService instance;
    private final List<MissionEventListener> listeners = new CopyOnWriteArrayList<>();

    private MissionEventNotificationService() {}
    
    public static MissionEventNotificationService getInstance() {
        if (instance == null) {
            instance = new MissionEventNotificationService();
        }
        return instance;
    }

    public void addListener(MissionEventListener listener) {
        listeners.add(listener);
    }

    public void clearListeners() {
        listeners.clear();
    }

    public void notifyOnMissionStatusChanged() {
        for (MissionEventListener listener : listeners) {
            try {
                listener.onStatusChanged();
            } catch (Exception e) {
                System.err.println("Erreur lors de la notification AsteroidCracked: " + e.getMessage());
            }
        }
    }
    
    /**
     * Interface pour écouter les événements de minage
     */
    public interface MissionEventListener {
        void onStatusChanged();
    }
}
