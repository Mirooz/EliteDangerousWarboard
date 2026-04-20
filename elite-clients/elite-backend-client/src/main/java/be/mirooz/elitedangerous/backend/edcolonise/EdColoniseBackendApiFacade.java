package be.mirooz.elitedangerous.backend.edcolonise;

import be.mirooz.elitedangerous.backend.BackendBundledProperties;
import be.mirooz.elitedangerous.backend.generated.ApiClient;
import be.mirooz.elitedangerous.backend.generated.ApiException;
import be.mirooz.elitedangerous.backend.generated.api.EdColoniseControllerApi;
import be.mirooz.elitedangerous.backend.generated.model.EdColoniseStarSystemSearchResponse;

import java.io.IOException;
import java.util.Arrays;

/**
 * Client OpenAPI du backend pour les endpoints ED Colonise.
 */
public final class EdColoniseBackendApiFacade {

    private static final EdColoniseBackendApiFacade INSTANCE = new EdColoniseBackendApiFacade();

    private final EdColoniseControllerApi edColoniseApi;

    /**
     * Gabarit des max (ordre OpenAPI), aligné StarSystemSearch.
     */
    private static final int[] DEFAULT_MAX_19 = {
            3, 2, 2770, 1, 15, 29, 26, 64, 61, 8, 2, 20, 15, 26, 48, 16, 60, 4, 1
    };

    private static final int MAX_DISTANCE_TO_SOL_CAP = 2770;

    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_RESULTS_PER_PAGE = 10;
    private static final String DEFAULT_SORT_ORDER = "SystemValue";

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
     * @param maxDistanceToSolLy distance max au Sol (AL), plafonnée à {@value #MAX_DISTANCE_TO_SOL_CAP} côté serveur
     * @param minLandables       min corps atterrissables (API {@code minLandables})
     * @param minRings           min anneaux (API {@code minRings})
     */
    public record SearchParams(int maxDistanceToSolLy, int minLandables, int minRings) {
    }

    /**
     * GET {@code /api/edcolonise/star-systems}.
     */
    public EdColoniseStarSystemSearchResponse searchStarSystems(SearchParams params) throws IOException {
        try {
            Integer[] max19 = Arrays.stream(DEFAULT_MAX_19).boxed().toArray(Integer[]::new);
            int dist = Math.min(MAX_DISTANCE_TO_SOL_CAP, Math.max(1, params.maxDistanceToSolLy()));
            max19[2] = dist;

            Integer[] min18 = new Integer[18];
            Arrays.fill(min18, 0);
            min18[7] = Math.max(0, params.minLandables());
            min18[12] = Math.max(0, params.minRings());

            return edColoniseApi.apiEdcoloniseStarSystemsGet(
                    null,
                    null,
                    max19[0], max19[1], max19[2], max19[3], max19[4], max19[5], max19[6], max19[7], max19[8], max19[9],
                    max19[10], max19[11], max19[12], max19[13], max19[14], max19[15], max19[16], max19[17], max19[18],
                    min18[0], min18[1], min18[2], min18[3], min18[4], min18[5], min18[6], min18[7], min18[8], min18[9],
                    min18[10], min18[11], min18[12], min18[13], min18[14], min18[15], min18[16], min18[17],
                    DEFAULT_PAGE_NO,
                    DEFAULT_RESULTS_PER_PAGE,
                    DEFAULT_SORT_ORDER,
                    null);
        } catch (ApiException e) {
            throw new IOException("ED Colonise star-systems search failed: " + e.getMessage(), e);
        }
    }
}
