package be.mirooz.elitedangerous.dashboard.model.registries;

import be.mirooz.elitedangerous.dashboard.model.events.ProspectedAsteroid;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;

public class ProspectedAsteroidRegistry {

    private static final int MAX_SIZE = 5;
    private final Deque<ProspectedAsteroid> registry = new LinkedList<>();
    private ProspectedAsteroidRegistry() {}
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
    }

    public synchronized Optional<ProspectedAsteroid> getLast() {
        return Optional.ofNullable(registry.peekLast());
    }

    public synchronized Deque<ProspectedAsteroid> getAll() {
        return new LinkedList<>(registry);
    }

    public synchronized void clear() {
        registry.clear();
    }
}
