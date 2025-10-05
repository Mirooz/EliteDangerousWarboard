package be.mirooz.elitedangerous.dashboard.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class ShipTargetRegistry {

    private static final int MAX_SIZE = 500;

    private final Map<String, ShipTarget> targetMap = new LinkedHashMap<>(MAX_SIZE, 0.75f, false) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, ShipTarget> eldest) {
            return size() > MAX_SIZE;
        }
    };

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
}
