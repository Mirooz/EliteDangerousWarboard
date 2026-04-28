package be.mirooz.elitedangerous.backend.capi;

import be.mirooz.elitedangerous.backend.BackendBundledProperties;
import be.mirooz.elitedangerous.backend.generated.ApiClient;
import be.mirooz.elitedangerous.backend.generated.ApiException;
import be.mirooz.elitedangerous.backend.generated.api.CapiControllerApi;
import be.mirooz.elitedangerous.backend.generated.model.CapiApiErrorBody;
import be.mirooz.elitedangerous.backend.generated.model.CapiFleetCarrierDto;
import be.mirooz.elitedangerous.backend.generated.model.CapiFleetCarrierProxyRequest;
import be.mirooz.elitedangerous.backend.generated.model.CapiLoginDto;
import be.mirooz.elitedangerous.backend.generated.model.CapiMarketDto;
import be.mirooz.elitedangerous.backend.generated.model.CapiMarketProxyRequest;
import be.mirooz.elitedangerous.backend.generated.model.CapiProfileDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class CapiFacade {

    /** Doit être strictement supérieur au hold serveur (60 s) pour laisser le serveur répondre. */
    private static final Duration WAIT_APPROVAL_REQUEST_TIMEOUT = Duration.ofSeconds(70);

    private static final CapiFacade INSTANCE = new CapiFacade();

    private final CapiControllerApi capiApi;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String backendBaseUrl;
    private final HttpClient longPollingHttpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private CapiFacade() {
        this.backendBaseUrl = BackendBundledProperties.get("backend.base-url", "http://localhost:8080");
        ApiClient apiClient = new ApiClient();
        apiClient.updateBaseUri(backendBaseUrl);
        this.capiApi = new CapiControllerApi(apiClient);
    }

    public static CapiFacade getInstance() {
        return INSTANCE;
    }

    public CapiMarketDto postMarket(CapiMarketProxyRequest request) throws IOException {
        try {
            return capiApi.apiCapiMarketPost(request);
        } catch (ApiException e) {
            handleApiException(e, "market");
            throw new AssertionError("unreachable");
        }
    }

    public CapiLoginDto requestAuthentication(String fid) throws IOException {
        try {
            return capiApi.apiCapiLoginGet(fid);
        } catch (ApiException e) {
            handleApiException(e, "login");
            throw new AssertionError("unreachable");
        }
    }

    /**
     * {@code POST /api/capi/logout?fid=…} — supprime les jetons CAPI côté backend (idempotent : 200 ou 400 acceptés).
     */
    public void logout(String fid) throws IOException, InterruptedException {
        if (fid == null || fid.isBlank()) {
            return;
        }
        URI uri = URI.create(backendBaseUrl + "/api/capi/logout?fid="
                + URLEncoder.encode(fid, StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = longPollingHttpClient.send(
                request, HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        if (status == 200 || status == 400) {
            return;
        }
        throw new IOException("CAPI logout: HTTP " + status + " - " + response.body());
    }

    public CapiProfileDto fetchProfile(String commanderName, String fid, String language) throws IOException {
        try {
            return capiApi.apiCapiProfileGet(commanderName, fid, language);
        } catch (ApiException e) {
            handleApiException(e, "profile");
            throw new AssertionError("unreachable");
        }
    }

    /**
     * Long polling : bloque côté serveur jusqu'à 60 s en attente du callback OAuth Frontier
     * pour ce {@code fid}. À relancer en boucle côté appelant jusqu'à {@code approved=true}
     * ou expiration du budget total (15 min typiquement).
     *
     * <p>Ne passe pas par le {@link CapiControllerApi} généré : l'appel direct via
     * {@link HttpClient} permet un timeout explicite de 70 s adapté au hold serveur.
     */
    public CapiWaitApprovalResponse waitApproval(String fid) throws IOException, InterruptedException {
        if (fid == null || fid.isBlank()) {
            throw new IOException("fid manquant pour waitApproval");
        }
        URI uri = URI.create(backendBaseUrl + "/api/capi/wait-approval?fid="
                + URLEncoder.encode(fid, StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Accept", "application/json")
                .timeout(WAIT_APPROVAL_REQUEST_TIMEOUT)
                .GET()
                .build();
        HttpResponse<String> response = longPollingHttpClient.send(
                request, HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        String body = response.body();
        if (status == 418) {
            throw new CapiServiceDownException();
        }
        if (status == 400) {
            throw new IOException("CAPI wait-approval: requête rejetée (400) - " + body);
        }
        if (status != 200) {
            throw new IOException("CAPI wait-approval: HTTP " + status + " - " + body);
        }
        JsonNode root = mapper.readTree(body != null ? body : "{}");
        boolean approved = root.path("approved").asBoolean(false);
        boolean timeout = root.path("timeout").asBoolean(false);
        String respondedFid = root.path("fid").asText(null);
        return new CapiWaitApprovalResponse(approved, timeout, respondedFid);
    }

    public CapiFleetCarrierDto fetchFleetCarrier(String commanderName, String fid, String language) throws IOException {
        try {
            CapiFleetCarrierProxyRequest request = new CapiFleetCarrierProxyRequest()
                    .commanderName(commanderName)
                    .fid(fid)
                    .language(language);
            return capiApi.apiCapiFleetcarrierPost(request);
        } catch (ApiException e) {
            handleApiException(e, "fleet carrier");
            throw new AssertionError("unreachable");
        }
    }

    private void handleApiException(ApiException e, String operation) throws IOException {
        if (e.getCode() == 401) {
            CapiApiErrorBody error = parseUnauthorizedError(e.getResponseBody());
            throw new UnauthorizedException(error);
        }
        if (e.getCode() == 418) {
            throw new CapiServiceDownException();
        }
        throw new IOException(
                "CAPI backend " + operation + " call failed: HTTP " + e.getCode() + " - " + e.getMessage(),
                e
        );
    }

    private CapiApiErrorBody parseUnauthorizedError(String body) throws IOException {
        if (body == null || body.isBlank()) {
            return null;
        }
        JsonNode root = mapper.readTree(body);
        JsonNode errNode = root.get("error");
        if (errNode == null || errNode.isNull()) {
            return null;
        }
        return mapper.treeToValue(errNode, CapiApiErrorBody.class);
    }
}
