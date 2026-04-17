package be.mirooz.elitedangerous.backend.ardentbackend;


import be.mirooz.elitedangerous.backend.BackendBundledProperties;
import be.mirooz.elitedangerous.backend.generated.ApiClient;
import be.mirooz.elitedangerous.backend.generated.ApiException;
import be.mirooz.elitedangerous.backend.generated.api.ArdentApiControllerApi;
import be.mirooz.elitedangerous.backend.generated.model.NearbyExportsCrosscheckRequest;
import be.mirooz.elitedangerous.backend.generated.model.NearbyExportsCrosscheckResponse;

import java.io.IOException;

/**
 * Client OpenAPI du backend analytics pour les endpoints Ardent.
 */
public final class ArdentBackendApiFacade {

    private static final ArdentBackendApiFacade INSTANCE = new ArdentBackendApiFacade();

    private final ArdentApiControllerApi ardentApi;

    private ArdentBackendApiFacade() {
        String baseUrl = BackendBundledProperties.get("backend.base-url", "http://localhost:8080");
        ApiClient apiClient = new ApiClient();
        apiClient.updateBaseUri(baseUrl);
        this.ardentApi = new ArdentApiControllerApi(apiClient);
    }

    public static ArdentBackendApiFacade getInstance() {
        return INSTANCE;
    }

    /**
     * Appelle POST /api/ardent/commodities/nearby/buy (controller suggestBuyStations).
     */
    public NearbyExportsCrosscheckResponse suggestBuyStations(NearbyExportsCrosscheckRequest request) throws IOException {
        try {
            return ardentApi.apiArdentCommoditiesNearbyBuyPost(request);
        } catch (ApiException e) {
            throw new IOException("Ardent backend nearby/buy failed: " + e.getMessage(), e);
        }
    }
}
