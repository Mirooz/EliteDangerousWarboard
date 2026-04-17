package be.mirooz.elitedangerous.backend.spansh;

import be.mirooz.elitedangerous.backend.BackendBundledProperties;
import be.mirooz.elitedangerous.backend.generated.ApiClient;
import be.mirooz.elitedangerous.backend.generated.ApiException;
import be.mirooz.elitedangerous.backend.generated.api.SpanshControllerApi;
import be.mirooz.elitedangerous.backend.generated.model.SpanshRouteRequestDTO;
import be.mirooz.elitedangerous.backend.generated.model.SpanshRouteResponseDTO;
import be.mirooz.elitedangerous.backend.generated.model.SpanshRouteResultsResponse;
import be.mirooz.elitedangerous.backend.generated.model.SpanshSearchRequestDTO;
import be.mirooz.elitedangerous.backend.generated.model.SpanshSearchResponseDTO;
import be.mirooz.elitedangerous.backend.spansh.serialization.SpanshDateTimeDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * Client OpenAPI pour les endpoints Spansh du backend.
 */
public class SpanshFacade {
    private static SpanshFacade instance;
    private final SpanshControllerApi spanshApi;

    private SpanshFacade() {
        ApiClient apiClient = new ApiClient();
        apiClient.updateBaseUri(BackendBundledProperties.get("backend.base-url", "http://localhost:8080"));
        apiClient.setReadTimeout(Duration.ofSeconds(30));

        ObjectMapper mapper = ApiClient.createDefaultObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(OffsetDateTime.class, new SpanshDateTimeDeserializer());
        mapper.registerModule(module);
        apiClient.setObjectMapper(mapper);

        this.spanshApi = new SpanshControllerApi(apiClient);
    }

    public static synchronized SpanshFacade getInstance() {
        if (instance == null) {
            instance = new SpanshFacade();
        }
        return instance;
    }

    private static boolean isExpiredGuidPayload(String body) {
        return body != null
                && body.contains("\"searchReference\":null")
                && body.contains("\"spanshResponse\":null");
    }

    public SpanshSearchResponseDTO searchSpansh(ExplorationMode mode, SpanshSearchRequestDTO searchRequestDTO) throws Exception {
        try {
            if (mode == null) {
                throw new Exception("Mode d'exploration null");
            }
            return switch (mode) {
                case STRATUM_UNDISCOVERED -> spanshApi.apiSpanshSearchesStratumPost(searchRequestDTO);
                default -> throw new Exception("Mode de recherche Spansh non supporté: " + mode);
            };
        } catch (ApiException e) {
            throw new Exception("Erreur Spansh (" + mode + "): HTTP " + e.getCode() + " - " + e.getMessage(), e);
        }
    }

    public SpanshRouteResponseDTO searchSpanshRoute(ExplorationMode mode, SpanshRouteRequestDTO routeRequestDTO) throws Exception {
        try {
            if (mode == null) {
                throw new Exception("Mode d'exploration null");
            }
            return switch (mode) {
                case EXPRESSWAY_TO_EXOMASTERY -> spanshApi.apiSpanshRoutesExobiologyPost(routeRequestDTO);
                case ROAD_TO_RICHES -> spanshApi.apiSpanshRoutesRichesPost(routeRequestDTO);
                default -> throw new Exception("Mode de route Spansh non supporté: " + mode);
            };
        } catch (ApiException e) {
            throw new Exception("Erreur Spansh (" + mode + "): HTTP " + e.getCode() + " - " + e.getMessage(), e);
        }
    }

    public SpanshRouteResultsResponse getSpanshRouteResultsByJob(String job) throws Exception {
        try {
            SpanshRouteResponseDTO routeResponse = spanshApi.apiSpanshRoutesGuidGet(job);
            if (routeResponse != null && routeResponse.getSpanshResponse() != null) {
                return routeResponse.getSpanshResponse();
            }
            throw new Exception("Réponse invalide : spanshResponse est null");
        } catch (ApiException e) {
            throw new Exception("Erreur Spansh (results, job: " + job + "): HTTP " + e.getCode() + " - " + e.getMessage(), e);
        }
    }

    public SpanshRouteResultsResponse getSpanshRouteResultsByGuid(String guid) throws Exception {
        try {
            SpanshRouteResponseDTO routeResponse = spanshApi.apiSpanshRoutesGuidGet(guid);
            if (routeResponse != null && routeResponse.getSpanshResponse() != null) {
                return routeResponse.getSpanshResponse();
            }
            throw new Exception("Réponse invalide : spanshResponse est null");
        } catch (ApiException e) {
            if (e.getCode() == 500 && isExpiredGuidPayload(e.getResponseBody())) {
                throw new SpanshGuidExpiredException(
                        "Le GUID Spansh a expiré ou n'est plus valide. Une nouvelle demande est nécessaire.");
            }
            throw new Exception("Erreur Spansh (route, guid: " + guid + "): HTTP " + e.getCode() + " - " + e.getMessage(), e);
        }
    }

    public SpanshSearchResponseDTO getSpanshSearchByGuid(String guid) throws Exception {
        try {
            return spanshApi.apiSpanshSearchesGuidGet(guid);
        } catch (ApiException e) {
            if (e.getCode() == 500 && isExpiredGuidPayload(e.getResponseBody())) {
                throw new SpanshGuidExpiredException(
                        "Le GUID Spansh a expiré ou n'est plus valide. Une nouvelle demande est nécessaire.");
            }
            throw new Exception("Erreur Spansh (GUID: " + guid + "): HTTP " + e.getCode() + " - " + e.getMessage(), e);
        }
    }
}
