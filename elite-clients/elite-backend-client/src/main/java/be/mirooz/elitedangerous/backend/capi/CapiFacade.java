package be.mirooz.elitedangerous.backend.capi;

import be.mirooz.elitedangerous.backend.BackendBundledProperties;
import be.mirooz.elitedangerous.backend.generated.ApiClient;
import be.mirooz.elitedangerous.backend.generated.ApiException;
import be.mirooz.elitedangerous.backend.generated.api.CapiControllerApi;
import be.mirooz.elitedangerous.backend.generated.model.CapiApiResponse;
import be.mirooz.elitedangerous.backend.generated.model.CapiFleetCarrierProxyRequest;
import be.mirooz.elitedangerous.backend.generated.model.CapiMarketProxyRequest;
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

    public CapiApiResponse postMarket(CapiMarketProxyRequest request) throws IOException {
        try {
            return capiApi.apiCapiMarketPost(request);
        } catch (ApiException e) {
            handleApiException(e, "market");
            return new CapiApiResponse();
        }
    }

    public CapiApiResponse requestAuthentication(String fid) throws IOException {
        try {
            return capiApi.apiCapiLoginGet(fid);
        } catch (ApiException e) {
            handleApiException(e, "login");
            return new CapiApiResponse();
        }
    }

    public CapiApiResponse fetchProfile(String commanderName, String fid, String language) throws IOException {
        try {
            return capiApi.apiCapiProfileGet(commanderName, fid, language);
        } catch (ApiException e) {
            handleApiException(e, "profile");
            return new CapiApiResponse();
        }
    }

    public CapiApiResponse fetchFleetCarrier(String commanderName, String fid, String language) throws IOException {
        try {
            CapiFleetCarrierProxyRequest request = new CapiFleetCarrierProxyRequest()
                    .commanderName(commanderName)
                    .fid(fid)
                    .language(language);
            return capiApi.apiCapiFleetcarrierPost(request);
        } catch (ApiException e) {
            handleApiException(e, "fleet carrier");
            return new CapiApiResponse();
        }
    }

    private void handleApiException(ApiException e, String operation) throws IOException {
        if (e.getCode() == 401) {
            CapiApiResponse response = parse(e.getResponseBody());
            throw new UnauthorizedException(response);
        }
        throw new IOException(
                "CAPI backend " + operation + " call failed: HTTP " + e.getCode() + " - " + e.getMessage(),
                e
        );
    }

    private CapiApiResponse parse(String body) throws IOException {
        if (body == null || body.isBlank()) {
            return new CapiApiResponse();
        }
        return mapper.readValue(body, CapiApiResponse.class);
    }
}