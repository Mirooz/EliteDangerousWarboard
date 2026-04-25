package be.mirooz.elitedangerous.dashboard.service.listeners;

import be.mirooz.elitedangerous.dashboard.model.events.ProspectedAsteroid;
import be.mirooz.elitedangerous.dashboard.model.registries.mining.ProspectedAsteroidListener;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Abonnements minage (un seul registre) : astéroïde craqué, prospecteur ajouté, registre vidé.
 * Un même objet peut implémenter {@link MiningEventListener} et/ou {@link ProspectedAsteroidListener}
 * — il n’est enregistré qu’une fois ; chaque notification cible les interfaces réellement implémentées.
 */
public class MiningEventNotificationService {

    private static MiningEventNotificationService instance;
    /** Références uniques ; le type concret peut implémenter une ou les deux interfaces. */
    private final CopyOnWriteArrayList<Object> listeners = new CopyOnWriteArrayList<>();

    private MiningEventNotificationService() {}

    public static MiningEventNotificationService getInstance() {
        if (instance == null) {
            instance = new MiningEventNotificationService();
        }
        return instance;
    }

    public void addListener(MiningEventListener listener) {
        addIfAbsent(listener);
    }

    public void addProspectedAsteroidListener(ProspectedAsteroidListener listener) {
        addIfAbsent(listener);
    }

    private void addIfAbsent(Object listener) {
        if (listener == null) {
            return;
        }
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeProspectedAsteroidListener(ProspectedAsteroidListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public void removeListeners() {
        listeners.clear();
    }

    public void notifyAsteroidCracked(ProspectedAsteroid prospector) {
        for (Object o : listeners) {
            if (o instanceof MiningEventListener m) {
                try {
                    m.onAsteroidCracked(prospector);
                } catch (Exception e) {
                    System.err.println("Erreur lors de la notification AsteroidCracked: " + e.getMessage());
                }
            }
        }
    }

    public void notifyProspectorAdded(ProspectedAsteroid prospector) {
        for (Object o : listeners) {
            if (o instanceof ProspectedAsteroidListener p) {
                try {
                    p.onProspectorAdded(prospector);
                } catch (Exception e) {
                    System.err.println("❌ Erreur lors de la notification d'ajout de prospecteur: " + e.getMessage());
                }
            }
        }
    }

    public void notifyRegistryCleared() {
        for (Object o : listeners) {
            if (o instanceof ProspectedAsteroidListener p) {
                try {
                    p.onRegistryCleared();
                } catch (Exception e) {
                    System.err.println("❌ Erreur lors de la notification de vidage du registre: " + e.getMessage());
                }
            }
        }
    }

    public interface MiningEventListener {
        void onAsteroidCracked(ProspectedAsteroid prospector);
    }
}
