package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.backend.edcolonise.EdColoniseBackendApiFacade;
import be.mirooz.elitedangerous.backend.edcolonise.EdColoniseStarSystemSearchQuery;
import be.mirooz.elitedangerous.backend.generated.model.EdColoniseStarSystemSearchResponse;

import java.io.IOException;

/**
 * Recherche de systèmes colonisables via le backend (ED Colonise).
 */
public final class EdColoniseService {

    private static final EdColoniseService INSTANCE = new EdColoniseService();

    private final EdColoniseBackendApiFacade backend = EdColoniseBackendApiFacade.getInstance();

    private EdColoniseService() {
    }

    public static EdColoniseService getInstance() {
        return INSTANCE;
    }

    public EdColoniseStarSystemSearchResponse searchColonisableStarSystems(EdColoniseStarSystemSearchQuery query)
            throws IOException {
        return backend.searchStarSystems(query);
    }
}
