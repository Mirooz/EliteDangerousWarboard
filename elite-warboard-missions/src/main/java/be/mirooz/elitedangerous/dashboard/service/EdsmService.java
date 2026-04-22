package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.backend.edsm.EdsmBackendApiFacade;
import be.mirooz.elitedangerous.backend.generated.model.EdsmBodiesResponse;
import be.mirooz.elitedangerous.dashboard.model.exploration.SystemVisited;
import be.mirooz.elitedangerous.dashboard.service.mapping.EdsmSystemVisitedMapper;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service d'accès EDSM via le backend.
 */
public final class EdsmService {

    private static final EdsmService INSTANCE = new EdsmService();

    private final EdsmBackendApiFacade backend = EdsmBackendApiFacade.getInstance();

    /**
     * Données EDSM par système : un seul aller-retour API par système et par session d’application
     * (les rafraîchissements d’UI ne rechargent pas depuis EDSM).
     */
    private final ConcurrentHashMap<String, SystemVisited> systemVisitedByName = new ConcurrentHashMap<>();

    private EdsmService() {
    }

    public static EdsmService getInstance() {
        return INSTANCE;
    }

    public EdsmBodiesResponse fetchSystemBodies(String systemName) throws IOException {
        return backend.fetchSystemBodies(systemName);
    }

    public SystemVisited fetchSystemVisited(String systemName) throws IOException {
        if (systemName == null || systemName.isBlank()) {
            throw new IOException("System name is blank");
        }
        String key = systemName.trim();
        SystemVisited cached = systemVisitedByName.get(key);
        if (cached != null) {
            return cached;
        }
        EdsmBodiesResponse response = fetchSystemBodies(key);
        SystemVisited fresh = EdsmSystemVisitedMapper.toSystemVisited(response, key);
        SystemVisited previous = systemVisitedByName.putIfAbsent(key, fresh);
        return previous != null ? previous : fresh;
    }
}
