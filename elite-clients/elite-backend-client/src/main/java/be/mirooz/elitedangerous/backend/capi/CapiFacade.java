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

public final class CapiFacade {

    private static final CapiFacade INSTANCE = new CapiFacade();

    private final CapiControllerApi capiApi;
    private final ObjectMapper mapper = new ObjectMapper();

    private CapiFacade() {
        String baseUrl = BackendBundledProperties.get("backend.base-url", "http://localhost:8080");
        ApiClient apiClient = new ApiClient();
        apiClient.updateBaseUri(baseUrl);
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

    public CapiProfileDto fetchProfile(String commanderName, String fid, String language) throws IOException {
        try {
            return capiApi.apiCapiProfileGet(commanderName, fid, language);
        } catch (ApiException e) {
            handleApiException(e, "profile");
            throw new AssertionError("unreachable");
        }
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
