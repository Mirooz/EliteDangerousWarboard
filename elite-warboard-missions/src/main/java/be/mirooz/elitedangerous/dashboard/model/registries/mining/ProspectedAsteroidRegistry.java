package be.mirooz.elitedangerous.dashboard.model.registries.mining;

import be.mirooz.elitedangerous.dashboard.model.events.ProspectedAsteroid;
import be.mirooz.elitedangerous.dashboard.service.listeners.MiningEventNotificationService;
import be.mirooz.elitedangerous.dashboard.service.listeners.MiningSessionNotificationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Registre des astéroïdes prospectés (FIFO bornée). Structures : {@link ArrayList} uniquement.
 * <p>La persistance JSON réutilise {@link #getAll()} (snapshot) et {@link #applyFullPersistedSnapshot(List)}.
 * Les écoutes UI passent par {@link MiningEventNotificationService}.</p>
 */
public class ProspectedAsteroidRegistry {

    private static final int MAX_SIZE = 50;
    private final List<ProspectedAsteroid> items = new ArrayList<>();
    private final MiningSessionNotificationService miningSessionNotificationService = MiningSessionNotificationService.getInstance();
    private final MiningEventNotificationService miningEventNotifications = MiningEventNotificationService.getInstance();

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

        if (!items.isEmpty() && items.get(items.size() - 1).equals(asteroid)) {
            return;
        }

        items.add(asteroid);
        while (items.size() > MAX_SIZE) {
            items.remove(0);
        }

        miningEventNotifications.notifyProspectorAdded(asteroid);
    }

    public synchronized Optional<ProspectedAsteroid> getLast() {
        if (items.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(items.get(items.size() - 1));
    }

    /**
     * Copie de l’ordre d’insertion (plus ancien en tête, plus récent en fin). Utilisé aussi pour la persistance.
     */
    public synchronized List<ProspectedAsteroid> getAll() {
        return new ArrayList<>(items);
    }

    public synchronized void clear() {
        items.clear();
        miningEventNotifications.notifyRegistryCleared();
    }

    /** Restauration silencieuse (aucune notification) — alimente la persistance JSON. */
    public synchronized void applyFullPersistedSnapshot(List<ProspectedAsteroid> snapshot) {
        items.clear();
        if (snapshot != null) {
            for (ProspectedAsteroid a : snapshot) {
                if (a != null) {
                    items.add(a);
                }
            }
            while (items.size() > MAX_SIZE) {
                items.remove(0);
            }
        }
    }

    private void notifyEndMiningSession() {
        this.clear();
    }
}
