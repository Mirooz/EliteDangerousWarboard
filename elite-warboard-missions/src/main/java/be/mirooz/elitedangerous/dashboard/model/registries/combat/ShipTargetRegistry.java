package be.mirooz.elitedangerous.dashboard.model.registries.combat;

import be.mirooz.elitedangerous.dashboard.model.ships.ShipTarget;

import java.util.HashMap;
import java.util.Map;

public class ShipTargetRegistry {

    private final Map<String, ShipTarget> targetMap = new HashMap<>();

    private static final ShipTargetRegistry INSTANCE = new ShipTargetRegistry();

    private ShipTargetRegistry() {}

    public static ShipTargetRegistry getInstance() {
        return INSTANCE;
    }

    public void put(ShipTarget target) {
        if (target.getPilotNameLocalised() != null) {
            targetMap.put(target.getPilotNameLocalised(), target);
        }
    }

    public ShipTarget get(String pilotNameLocalised) {
        return targetMap.get(pilotNameLocalised);
    }

    public Map<String, ShipTarget> getAll() {
        return targetMap;
    }

    public void clear() {
        targetMap.clear();
    }

    /** Restaure l'ensemble des targets à partir d'un snapshot persisté. */
    public synchronized void applyFullPersistedSnapshot(Map<String, ShipTarget> snapshot) {
        targetMap.clear();
        if (snapshot != null) {
            targetMap.putAll(snapshot);
        }
    }

}
