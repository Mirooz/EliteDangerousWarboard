package be.mirooz.elitedangerous.dashboard.model.registries;

import be.mirooz.elitedangerous.dashboard.model.events.ProspectedAsteroid;
import be.mirooz.elitedangerous.dashboard.service.MiningSessionNotificationService;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class ProspectedAsteroidRegistry {

    private static final int MAX_SIZE = 50;
    private final Deque<ProspectedAsteroid> registry = new LinkedList<>();
    private final List<ProspectedAsteroidListener> listeners = new CopyOnWriteArrayList<>();
    private final MiningSessionNotificationService miningSessionNotificationService = MiningSessionNotificationService.getInstance();
    
    private ProspectedAsteroidRegistry() {
        miningSessionNotificationService.addSessionEndListener(this::notifyEndMiningSession);
    }
    
    private static class Holder {
        private static final ProspectedAsteroidRegistry INSTANCE = new ProspectedAsteroidRegistry();
    }

    public static ProspectedAsteroidRegistry getInstance() {
        return Holder.INSTANCE;
    }

    public synchronized void register(ProspectedAsteroid asteroid) {
        if (asteroid == null) return;

        // Optionnel : éviter doublon immédiat
        if (!registry.isEmpty() && registry.peekLast().equals(asteroid)) {
            return;
        }

        registry.addLast(asteroid);

        if (registry.size() > MAX_SIZE) {
            registry.removeFirst();
        }
        
        // Notifier les listeners
        notifyProspectorAdded(asteroid);
    }

    public synchronized Optional<ProspectedAsteroid> getLast() {
        return Optional.ofNullable(registry.peekLast());
    }

    public synchronized Deque<ProspectedAsteroid> getAll() {
        return new LinkedList<>(registry);
    }

    public synchronized void clear() {
        registry.clear();
        // Notifier les listeners
        notifyRegistryCleared();
    }
    
    /**
     * Ajoute un listener pour les changements du registre
     */
    public void addListener(ProspectedAsteroidListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    /**
     * Supprime un listener
     */
    public void removeListener(ProspectedAsteroidListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Notifie tous les listeners qu'un prospecteur a été ajouté
     */
    private void notifyProspectorAdded(ProspectedAsteroid prospector) {
        for (ProspectedAsteroidListener listener : listeners) {
            try {
                listener.onProspectorAdded(prospector);
            } catch (Exception e) {
                System.err.println("❌ Erreur lors de la notification d'ajout de prospecteur: " + e.getMessage());
            }
        }
    }
    private void notifyEndMiningSession() {
        for (ProspectedAsteroidListener listener : listeners) {
            try {
                listener.onMiningSessionEnd();
            } catch (Exception e) {
                System.err.println("❌ Erreur lors de la notification de fin de mining session: " + e.getMessage());
            }
        }
    }

    /**
     * Notifie tous les listeners que le registre a été vidé
     */
    private void notifyRegistryCleared() {
        for (ProspectedAsteroidListener listener : listeners) {
            try {
                listener.onRegistryCleared();
            } catch (Exception e) {
                System.err.println("❌ Erreur lors de la notification de vidage du registre: " + e.getMessage());
            }
        }
    }
}
