package be.mirooz.elitedangerous.backend.edcolonise;

import be.mirooz.elitedangerous.backend.BackendBundledProperties;
import be.mirooz.elitedangerous.backend.generated.ApiClient;
import be.mirooz.elitedangerous.backend.generated.ApiException;
import be.mirooz.elitedangerous.backend.generated.api.EdColoniseControllerApi;
import be.mirooz.elitedangerous.backend.generated.model.EdColoniseStarSystemSearchResponse;

import java.io.IOException;

/**
 * Client OpenAPI du backend pour les endpoints ED Colonise.
 */
public final class EdColoniseBackendApiFacade {

    private static final EdColoniseBackendApiFacade INSTANCE = new EdColoniseBackendApiFacade();

    private final EdColoniseControllerApi edColoniseApi;

    private EdColoniseBackendApiFacade() {
        String baseUrl = BackendBundledProperties.get("backend.base-url", "http://localhost:8080");
        ApiClient apiClient = new ApiClient();
        apiClient.updateBaseUri(baseUrl);
        this.edColoniseApi = new EdColoniseControllerApi(apiClient);
    }

    public static EdColoniseBackendApiFacade getInstance() {
        return INSTANCE;
    }

    /**
     * GET {@code /api/edcolonise/star-systems}.
     */
    public EdColoniseStarSystemSearchResponse searchStarSystems(EdColoniseStarSystemSearchQuery query) throws IOException {
        try {
            int[] max = query.maxValues();
            int[] min = query.minValues();
            return edColoniseApi.apiEdcoloniseStarSystemsGet(
                    query.factionName(),
                    query.hotspotTypes(),
                    max[0], max[1], max[2], max[3], max[4], max[5], max[6], max[7], max[8], max[9],
                    max[10], max[11], max[12], max[13], max[14], max[15], max[16], max[17], max[18],
                    min[0], min[1], min[2], min[3], min[4], min[5], min[6], min[7], min[8], min[9],
                    min[10], min[11], min[12], min[13], min[14], min[15], min[16], min[17],
                    query.pageNo(),
                    query.resultsPerPage(),
                    query.sortOrder(),
                    query.systemName());
        } catch (ApiException e) {
            throw new IOException("ED Colonise star-systems search failed: " + e.getMessage(), e);
        }
    }
}
