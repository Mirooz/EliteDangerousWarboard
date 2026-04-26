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
            logSpanshFailure("apiSpanshSearchesStratumPost", e);
            throw new Exception("Erreur Spansh (" + mode + "): " + briefApiErrorMessage(e));
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
            logSpanshFailure("searchSpanshRoute", e);
            throw new Exception("Erreur Spansh (" + mode + "): " + briefApiErrorMessage(e));
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
            logSpanshFailure("apiSpanshRoutesGuidGet", e);
            throw new Exception("Erreur Spansh (results, job: " + job + "): " + briefApiErrorMessage(e));
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
            logSpanshFailure("apiSpanshRoutesGuidGet", e);
            if (e.getCode() == 500 && isExpiredGuidPayload(e.getResponseBody())) {
                throw new SpanshGuidExpiredException(
                        "Le GUID Spansh a expiré ou n'est plus valide. Une nouvelle demande est nécessaire.");
            }
            throw new Exception("Erreur Spansh (route, guid: " + guid + "): " + briefApiErrorMessage(e));
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
            logSpanshFailure("apiSpanshSearchesGuidGet", e);
            if (e.getCode() == 500 && isExpiredGuidPayload(e.getResponseBody())) {
                throw new SpanshGuidExpiredException(
                        "Le GUID Spansh a expiré ou n'est plus valide. Une nouvelle demande est nécessaire.");
            }
            throw new Exception("Erreur Spansh (GUID: " + guid + "): " + briefApiErrorMessage(e));
        }
    }

    /**
     * Recherche des corps d'un système via {@code GET /api/spansh/bodies/search} (client généré {@link SpanshControllerApi#apiSpanshBodiesSearchGet}).
     *
     * @param systemName    nom du système (requis si {@code systemId64} absent)
     * @param systemId64    identifiant 64 bits du système (journal {@code SystemAddress} ou EdColonise {@code systemID}) — optionnel mais recommandé
     * @param commanderName nom du commandant (query optionnelle côté backend)
     */
    public SpanshSearchResponseDTO searchSpanshBodiesBySystem(String systemName, Long systemId64, String commanderName) throws Exception {
        boolean hasName = systemName != null && !systemName.isBlank();
        boolean hasId = systemId64 != null && systemId64 != 0L;
        if (!hasName && !hasId) {
            throw new IllegalArgumentException("systemName ou systemId64 requis");
        }
        long t0 = System.nanoTime();
        String sys = hasName ? systemName.trim() : null;
        String cmd = commanderName != null && !commanderName.isBlank() ? commanderName : "(none)";
        Long id = hasId ? systemId64 : null;
        LOG.info("Spansh: apiSpanshBodiesSearchGet(systemName=" + sys + ", systemId64=" + id
                + ", commanderName=" + cmd + ") — démarrage");
        try {
            SpanshSearchResponseDTO out = spanshApi.apiSpanshBodiesSearchGet(commanderName, id, sys);
            logSpanshDone("apiSpanshBodiesSearchGet", t0);
            return out;
        } catch (ApiException e) {
            logSpanshFailure("apiSpanshBodiesSearchGet", e);
            throw new Exception("Erreur Spansh (bodies/search, système: " + sys + ", id64: " + id + "): "
                    + briefApiErrorMessage(e));
        }
    }

    /**
     * Surcharge sans commandant (query {@code commanderName} absente).
     */
    public SpanshSearchResponseDTO searchSpanshBodiesBySystem(String systemName, Long systemId64) throws Exception {
        return searchSpanshBodiesBySystem(systemName, systemId64, null);
    }

    /**
     * Surcharge legacy : recherche par nom uniquement (sans identifiant 64 bits).
     */
    public SpanshSearchResponseDTO searchSpanshBodiesBySystem(String systemName) throws Exception {
        return searchSpanshBodiesBySystem(systemName, null, null);
    }

    private static void logSpanshDone(String operation, long startNanos) {
        long ms = (System.nanoTime() - startNanos) / 1_000_000L;
        LOG.info("Spansh: " + operation + " — terminé en " + ms + " ms");
    }

    /**
     * Une seule ligne, sans {@link Throwable} passé au logger → pas de stack JUL.
     * Contenu minimal : opération client, code HTTP, cause racine (ex. {@code ConnectException}).
     */
    private static void logSpanshFailure(String operation, ApiException e) {
        int code = e != null ? e.getCode() : 0;
        // Cause directe (souvent ConnectException) — pas la toute dernière (souvent ClosedChannelException).
        String cause = immediateCauseSimpleName(e);
        // Ne pas utiliser LOG.log(level, msg, e) : le 3ᵉ argument imprime toute la pile.
        //LOG.warning(String.format("SpanshFacade %s HTTP_%d %s", operation, code, cause));
    }

    private static String immediateCauseSimpleName(Throwable t) {
        if (t == null) {
            return "?";
        }
        Throwable c = t.getCause();
        if (c != null) {
            return c.getClass().getSimpleName();
        }
        return t.getClass().getSimpleName();
    }

    /**
     * Libellé court pour les exceptions remontées (évite le texte verbeux du client OpenAPI + corps JSON entier).
     */
    private static String briefApiErrorMessage(ApiException e) {
        if (e == null) {
            return "erreur inconnue";
        }
        int code = e.getCode();
        String fromBody = extractJsonMessageField(e.getResponseBody());
        if (fromBody != null && !fromBody.isBlank()) {
            String m = fromBody.trim().replaceAll("\\s+", " ");
            if (m.length() > 160) {
                m = m.substring(0, 157) + "…";
            }
            return "HTTP " + code + " — " + m;
        }
        return "HTTP " + code;
    }

    private static String extractJsonMessageField(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        int key = body.indexOf("\"message\"");
        if (key < 0) {
            return null;
        }
        int colon = body.indexOf(':', key);
        if (colon < 0) {
            return null;
        }
        int i = colon + 1;
        while (i < body.length() && Character.isWhitespace(body.charAt(i))) {
            i++;
        }
        if (i >= body.length()) {
            return null;
        }
        if (body.charAt(i) == '"') {
            int start = i + 1;
            int end = body.indexOf('"', start);
            if (end > start) {
                return body.substring(start, end);
            }
        }
        return null;
    }
}
