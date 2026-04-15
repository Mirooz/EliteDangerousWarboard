package be.mirooz.elitedangerous.capi.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * HTTP JSON : {@code capi.base-url} + {@code capi.path-prefix} + endpoint. Config uniquement via {@code capi-client-*.properties}.
 */
public class FrontierCapiClient {

    private static final Duration CONNECT = Duration.ofSeconds(10);
    private static final Duration REQUEST = Duration.ofSeconds(20);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String userAgent;

    public FrontierCapiClient() {
        this.httpClient = HttpClient.newBuilder().connectTimeout(CONNECT).build();
        this.objectMapper = new ObjectMapper();
        this.baseUrl = require("capi.base-url");
        this.userAgent = require("capi.user-agent");
    }

    public JsonNode getJson(String endpoint) throws IOException {
        return getJson(endpoint, null);
    }

    public JsonNode getJson(String endpoint, String accessToken) throws IOException {
        String url = baseUrl + endpoint;
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("User-Agent", userAgent)
                .GET()
                .timeout(REQUEST);
        if (accessToken != null && !accessToken.isBlank()) {
            b.header("Authorization", "Bearer " + accessToken.trim());
        }
        HttpResponse<String> response = send(b.build());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return objectMapper.readTree(response.body());
        }
        throw new IOException("CAPI GET "  + endpoint + " : HTTP " + response.statusCode() + " - " + response.body());
    }

    public String postJson(String endpoint, String jsonBody) throws IOException {
        String url = baseUrl + endpoint;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "application/json")
                .header("User-Agent", userAgent)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .timeout(REQUEST)
                .build();
        HttpResponse<String> response = send(request);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("POST " + endpoint + " : HTTP " + response.statusCode() + " - " + response.body());
        }
        return response.body();
    }

    public String absoluteUrl(String endpoint) {
        return baseUrl + endpoint;
    }

    private HttpResponse<String> send(HttpRequest request) throws IOException {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Requete HTTP interrompue", e);
        }
    }

    private static String require(String key) {
        String v = CapiBundledProperties.get(key);
        if (v == null) {
            throw new IllegalStateException("Propriete obligatoire manquante : " + key + " dans capi-client-"
                    + System.getProperty("app.profile", "dev") + ".properties");
        }
        return v;
    }
}
