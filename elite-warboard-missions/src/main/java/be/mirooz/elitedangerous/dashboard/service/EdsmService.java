package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.backend.edsm.EdsmBackendApiFacade;
import be.mirooz.elitedangerous.backend.generated.model.EdsmBodiesResponse;
import be.mirooz.elitedangerous.dashboard.model.exploration.SystemVisited;
import be.mirooz.elitedangerous.dashboard.service.mapping.EdsmSystemVisitedMapper;

import java.io.IOException;

/**
 * Service d'accès EDSM via le backend.
 */
public final class EdsmService {

    private static final EdsmService INSTANCE = new EdsmService();

    private final EdsmBackendApiFacade backend = EdsmBackendApiFacade.getInstance();

    private EdsmService() {
    }

    public static EdsmService getInstance() {
        return INSTANCE;
    }

    public EdsmBodiesResponse fetchSystemBodies(String systemName) throws IOException {
        return backend.fetchSystemBodies(systemName);
    }

    public SystemVisited fetchSystemVisited(String systemName) throws IOException {
        EdsmBodiesResponse response = fetchSystemBodies(systemName);
        return EdsmSystemVisitedMapper.toSystemVisited(response, systemName);
    }
}
