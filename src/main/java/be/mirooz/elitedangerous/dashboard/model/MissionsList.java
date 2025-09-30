package be.mirooz.elitedangerous.dashboard.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Singleton pour stocker les missions globales
 */
public class MissionsList {

    // Instance unique
    private static final MissionsList INSTANCE = new MissionsList();

    // Map globale (thread-safe via synchronizedMap si besoin)
    private final Map<String, Mission> globalMissionMap =
            Collections.synchronizedMap(new HashMap<>());

    // Constructeur privé pour empêcher new
    private MissionsList() {}

    // Accès à l’instance unique
    public static MissionsList getInstance() {
        return INSTANCE;
    }

    // Accès à la map
    public Map<String, Mission> getGlobalMissionMap() {
        return globalMissionMap;
    }
}
