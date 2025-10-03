package be.mirooz.elitedangerous.dashboard.model;

import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Singleton pour stocker les missions globales
 */
@Getter
public class MissionsList {

    // Instance unique
    private static final MissionsList INSTANCE = new MissionsList();
    private final Map<String, Mission> globalMissionMap =
            Collections.synchronizedMap(new HashMap<>());
    private MissionsList() {}

    // Accès à l’instance unique
    public static MissionsList getInstance() {
        return INSTANCE;
    }

}
