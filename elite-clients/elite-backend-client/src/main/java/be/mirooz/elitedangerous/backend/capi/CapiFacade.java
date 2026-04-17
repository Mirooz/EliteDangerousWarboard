package be.mirooz.elitedangerous.backend.capi;

import be.mirooz.elitedangerous.backend.BackendBundledProperties;
import be.mirooz.elitedangerous.backend.generated.ApiClient;
import be.mirooz.elitedangerous.backend.generated.ApiException;
import be.mirooz.elitedangerous.backend.generated.api.CapiControllerApi;
import be.mirooz.elitedangerous.backend.generated.model.CapiApiResponse;
import be.mirooz.elitedangerous.backend.generated.model.CapiMarketProxyRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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
            if (e.getCode() == 401) {
                CapiApiResponse response = parse(e.getResponseBody());
                throw new UnauthorizedException(response);
            }
            throw new IOException("CAPI backend market call failed: HTTP " + e.getCode() + " - " + e.getMessage(), e);
        }
    }

    public CapiApiResponse requestAuthentication(String fid) throws IOException {
        try {
            return capiApi.apiCapiLoginGet(fid);
        } catch (ApiException e) {
            throw new IOException("CAPI backend login call failed: HTTP " + e.getCode() + " - " + e.getMessage(), e);
        }
    }

    public CapiApiResponse fetchProfile(String commanderName, String fid, String language) throws IOException {
        try {
            Method profileMethod = capiApi.getClass().getMethod(
                    "apiCapiProfileGet",
                    String.class,
                    String.class,
                    String.class
            );
            Object result = profileMethod.invoke(capiApi, commanderName, fid, language);
            if (result instanceof CapiApiResponse response) {
                return response;
            }
            return new CapiApiResponse();
        } catch (NoSuchMethodException e) {
            throw new IOException("CAPI backend profile call unavailable (apiCapiProfileGet missing)", e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ApiException apiException) {
                throw new IOException(
                        "CAPI backend profile call failed: HTTP " + apiException.getCode() + " - " + apiException.getMessage(),
                        apiException
                );
            }
            throw new IOException("CAPI backend profile call failed", cause != null ? cause : e);
        } catch (IllegalAccessException e) {
            throw new IOException("CAPI backend profile call access error", e);
        }
    }

    private CapiApiResponse parse(String body) throws IOException {
        if (body == null || body.isBlank()) {
            return new CapiApiResponse();
        }
        return mapper.readValue(body, CapiApiResponse.class);
    }
}