package be.mirooz.elitedangerous.backend.edsm;

import be.mirooz.elitedangerous.backend.BackendBundledProperties;
import be.mirooz.elitedangerous.backend.generated.ApiClient;
import be.mirooz.elitedangerous.backend.generated.ApiException;
import be.mirooz.elitedangerous.backend.generated.api.EdsmControllerApi;
import be.mirooz.elitedangerous.backend.generated.model.EdsmBodiesResponse;

import java.io.IOException;

/**
 * Client OpenAPI du backend pour les endpoints EDSM.
 */
public final class EdsmBackendApiFacade {

    private static final EdsmBackendApiFacade INSTANCE = new EdsmBackendApiFacade();

    private final EdsmControllerApi edsmApi;

    private EdsmBackendApiFacade() {
        String baseUrl = BackendBundledProperties.get("backend.base-url", "http://localhost:8080");
        ApiClient apiClient = new ApiClient();
        apiClient.updateBaseUri(baseUrl);
        this.edsmApi = new EdsmControllerApi(apiClient);
    }

    public static EdsmBackendApiFacade getInstance() {
        return INSTANCE;
    }

    /**
     * Appelle GET /api/edsm/bodies.
     */
    public EdsmBodiesResponse fetchSystemBodies(String systemName) throws IOException {
        try {
            return edsmApi.apiEdsmBodiesGet(systemName);
        } catch (ApiException e) {
            throw new IOException("EDSM bodies call failed: " + e.getMessage(), e);
        }
    }
}
