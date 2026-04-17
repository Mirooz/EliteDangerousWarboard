package be.mirooz.elitedangerous.capi.client;

import be.mirooz.elitedangerous.capi.generated.model.CapiApiResponse;
import be.mirooz.elitedangerous.capi.generated.model.CapiMarketProxyRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;

public final class CapiClient {

    private static final CapiClient INSTANCE = new CapiClient();

    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String baseUrl;

    private CapiClient() {
        this.baseUrl = require("capi.base-url");

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public static CapiClient getInstance() {
        return INSTANCE;
    }

    public CapiApiResponse postMarket(CapiMarketProxyRequest request) throws IOException {
        try {
            String json = mapper.writeValueAsString(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/capi/market"))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            int status = response.statusCode();
            String body = response.body();

            CapiApiResponse apiResponse = parse(body);

            if (status == 401) {
                throw new UnauthorizedException(apiResponse);
            }

            if (status < 200 || status >= 300) {
                throw new IOException("HTTP " + status + " - " + body);
            }

            return apiResponse;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request interrupted", e);
        }
    }

    private CapiApiResponse parse(String body) throws IOException {
        if (body == null || body.isBlank()) {
            return new CapiApiResponse();
        }
        return mapper.readValue(body, CapiApiResponse.class);
    }

    private static String require(String key) {
        String value = CapiBundledProperties.get(key);
        if (value == null) {
            throw new IllegalStateException("Missing property: " + key);
        }
        return value.trim();
    }
}