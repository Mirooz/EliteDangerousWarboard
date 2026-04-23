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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client OpenAPI pour les endpoints Spansh du backend.
 */
public class SpanshFacade {

    private static final Logger LOG = Logger.getLogger(SpanshFacade.class.getName());

    private static SpanshFacade instance;
    private final SpanshControllerApi spanshApi;

    private SpanshFacade() {
        String backendBaseUrl = BackendBundledProperties.get("backend.base-url", "http://localhost:8080");
        ApiClient apiClient = new ApiClient();
        apiClient.updateBaseUri(backendBaseUrl);
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
        long t0 = System.nanoTime();
        LOG.info("Spansh: apiSpanshSearchesStratumPost (mode=" + mode + ") — démarrage");
        try {
            if (mode == null) {
                throw new Exception("Mode d'exploration null");
            }
            SpanshSearchResponseDTO out = switch (mode) {
                case STRATUM_UNDISCOVERED -> spanshApi.apiSpanshSearchesStratumPost(searchRequestDTO);
                default -> throw new Exception("Mode de recherche Spansh non supporté: " + mode);
            };
            logSpanshDone("apiSpanshSearchesStratumPost", t0);
            return out;
        } catch (ApiException e) {
            logSpanshFailure("apiSpanshSearchesStratumPost", mode != null ? mode.name() : "null", e, t0);
            throw new Exception("Erreur Spansh (" + mode + "): HTTP " + e.getCode() + " - " + e.getMessage(), e);
        }
    }

    public SpanshRouteResponseDTO searchSpanshRoute(ExplorationMode mode, SpanshRouteRequestDTO routeRequestDTO) throws Exception {
        long t0 = System.nanoTime();
        LOG.info("Spansh: route POST (mode=" + mode + ") — démarrage");
        try {
            if (mode == null) {
                throw new Exception("Mode d'exploration null");
            }
            SpanshRouteResponseDTO out = switch (mode) {
                case EXPRESSWAY_TO_EXOMASTERY -> spanshApi.apiSpanshRoutesExobiologyPost(routeRequestDTO);
                case ROAD_TO_RICHES -> spanshApi.apiSpanshRoutesRichesPost(routeRequestDTO);
                default -> throw new Exception("Mode de route Spansh non supporté: " + mode);
            };
            logSpanshDone("apiSpanshRoutes*" + (mode != null ? "(" + mode + ")" : ""), t0);
            return out;
        } catch (ApiException e) {
            logSpanshFailure("searchSpanshRoute", mode != null ? mode.name() : "null", e, t0);
            throw new Exception("Erreur Spansh (" + mode + "): HTTP " + e.getCode() + " - " + e.getMessage(), e);
        }
    }

    public SpanshRouteResultsResponse getSpanshRouteResultsByJob(String job) throws Exception {
        long t0 = System.nanoTime();
        LOG.info("Spansh: apiSpanshRoutesGuidGet(job=" + job + ") — démarrage");
        try {
            SpanshRouteResponseDTO routeResponse = spanshApi.apiSpanshRoutesGuidGet(job);
            if (routeResponse != null && routeResponse.getSpanshResponse() != null) {
                logSpanshDone("apiSpanshRoutesGuidGet", t0);
                return routeResponse.getSpanshResponse();
            }
            throw new Exception("Réponse invalide : spanshResponse est null");
        } catch (ApiException e) {
            logSpanshFailure("apiSpanshRoutesGuidGet", "job=" + job, e, t0);
            throw new Exception("Erreur Spansh (results, job: " + job + "): HTTP " + e.getCode() + " - " + e.getMessage(), e);
        }
    }

    public SpanshRouteResultsResponse getSpanshRouteResultsByGuid(String guid) throws Exception {
        long t0 = System.nanoTime();
        LOG.info("Spansh: apiSpanshRoutesGuidGet(guid=" + guid + ") — démarrage");
        try {
            SpanshRouteResponseDTO routeResponse = spanshApi.apiSpanshRoutesGuidGet(guid);
            if (routeResponse != null && routeResponse.getSpanshResponse() != null) {
                logSpanshDone("apiSpanshRoutesGuidGet", t0);
                return routeResponse.getSpanshResponse();
            }
            throw new Exception("Réponse invalide : spanshResponse est null");
        } catch (ApiException e) {
            logSpanshFailure("apiSpanshRoutesGuidGet", "guid=" + guid, e, t0);
            if (e.getCode() == 500 && isExpiredGuidPayload(e.getResponseBody())) {
                throw new SpanshGuidExpiredException(
                        "Le GUID Spansh a expiré ou n'est plus valide. Une nouvelle demande est nécessaire.");
            }
            throw new Exception("Erreur Spansh (route, guid: " + guid + "): HTTP " + e.getCode() + " - " + e.getMessage(), e);
        }
    }

    public SpanshSearchResponseDTO getSpanshSearchByGuid(String guid) throws Exception {
        long t0 = System.nanoTime();
        LOG.info("Spansh: apiSpanshSearchesGuidGet(guid=" + guid + ") — démarrage");
        try {
            SpanshSearchResponseDTO out = spanshApi.apiSpanshSearchesGuidGet(guid);
            logSpanshDone("apiSpanshSearchesGuidGet", t0);
            return out;
        } catch (ApiException e) {
            logSpanshFailure("apiSpanshSearchesGuidGet", "guid=" + guid, e, t0);
            if (e.getCode() == 500 && isExpiredGuidPayload(e.getResponseBody())) {
                throw new SpanshGuidExpiredException(
                        "Le GUID Spansh a expiré ou n'est plus valide. Une nouvelle demande est nécessaire.");
            }
            throw new Exception("Erreur Spansh (GUID: " + guid + "): HTTP " + e.getCode() + " - " + e.getMessage(), e);
        }
    }

    /**
     * Recherche des corps d'un système via {@code GET /api/spansh/bodies/search} (client généré {@link SpanshControllerApi#apiSpanshBodiesSearchGet}).
     *
     * @param systemName nom du système (requis)
     * @param commanderName nom du commandant (query optionnelle côté backend)
     */
    public SpanshSearchResponseDTO searchSpanshBodiesBySystem(String systemName, String commanderName) throws Exception {
        if (systemName == null || systemName.isBlank()) {
            throw new IllegalArgumentException("systemName ne peut pas être vide");
        }
        long t0 = System.nanoTime();
        String sys = systemName.trim();
        String cmd = commanderName != null && !commanderName.isBlank() ? commanderName : "(none)";
        LOG.info("Spansh: apiSpanshBodiesSearchGet(systemName=" + sys + ", commanderName=" + cmd + ") — démarrage");
        try {
            SpanshSearchResponseDTO out = spanshApi.apiSpanshBodiesSearchGet(commanderName, sys);
            logSpanshDone("apiSpanshBodiesSearchGet", t0);
            return out;
        } catch (ApiException e) {
            logSpanshFailure("apiSpanshBodiesSearchGet", "systemName=" + sys, e, t0);
            throw new Exception("Erreur Spansh (bodies/search, système: " + systemName + "): HTTP "
                    + e.getCode() + " - " + e.getMessage(), e);
        }
    }

    /**
     * Surcharge sans commandant (query {@code commanderName} absente).
     */
    public SpanshSearchResponseDTO searchSpanshBodiesBySystem(String systemName) throws Exception {
        return searchSpanshBodiesBySystem(systemName, null);
    }

    private static void logSpanshDone(String operation, long startNanos) {
        long ms = (System.nanoTime() - startNanos) / 1_000_000L;
        LOG.info("Spansh: " + operation + " — terminé en " + ms + " ms");
    }

    private static void logSpanshFailure(String operation, String context, ApiException e, long startNanos) {
        long ms = (System.nanoTime() - startNanos) / 1_000_000L;
        LOG.log(Level.WARNING,
                String.format("Spansh: %s — échec après %d ms (%s) HTTP %d: %s",
                        operation, ms, context, e.getCode(), e.getMessage()),
                e);
    }
}
