package be.mirooz.elitedangerous.capi.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Properties;

/**
 * Client minimal pour Frontier CAPI.
 * Utilise OAuth2 Bearer token et impose un User-Agent compatible EDCD.
 */
public class FrontierCapiClient {

    private static final String DEFAULT_BASE_URL = "https://companion.orerve.net";
    private static final String DEFAULT_USER_AGENT = "EDCD-EliteWarboard-1.3.2";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String userAgent;

    public FrontierCapiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
        this.baseUrl = loadBaseUrl();
        this.userAgent = loadUserAgent();
    }

    /**
     * Récupère le profil commandant via CAPI /profile.
     */
    public JsonNode getProfile(String accessToken) throws IOException {
        return getJson("/profile", accessToken);
    }

    /**
     * Récupère les informations Fleet Carrier via CAPI /fleetcarrier.
     */
    public JsonNode getFleetCarrier(String accessToken) throws IOException {
        return getJson("/fleetcarrier", accessToken);
    }

    /**
     * Appel GET CAPI générique (retour JSON).
     * Exemple endpoint: "/market", "/shipyard", "/profile".
     */
    public JsonNode getJson(String endpoint, String accessToken) throws IOException {
        validateToken(accessToken);
        String resolvedEndpoint = endpoint.startsWith("/") ? endpoint : "/" + endpoint;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + resolvedEndpoint))
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .header("User-Agent", userAgent)
                .GET()
                .timeout(Duration.ofSeconds(20))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return objectMapper.readTree(response.body());
            }
            throw new IOException("Erreur CAPI GET " + resolvedEndpoint + " : HTTP " + response.statusCode()
                    + " - " + response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Appel CAPI interrompu", e);
        }
    }

    private void validateToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("accessToken est obligatoire pour appeler CAPI");
        }
    }

    private String loadBaseUrl() {
        String systemProperty = System.getProperty("elite.capi.base-url");
        if (isNotBlank(systemProperty)) {
            return systemProperty;
        }
        String environment = System.getenv("ELITE_CAPI_BASE_URL");
        if (isNotBlank(environment)) {
            return environment;
        }
        String fileValue = loadFromProperties("capi.base-url");
        return isNotBlank(fileValue) ? fileValue : DEFAULT_BASE_URL;
    }

    private String loadUserAgent() {
        String systemProperty = System.getProperty("elite.capi.user-agent");
        if (isNotBlank(systemProperty)) {
            return systemProperty;
        }
        String environment = System.getenv("ELITE_CAPI_USER_AGENT");
        if (isNotBlank(environment)) {
            return environment;
        }
        String fileValue = loadFromProperties("capi.user-agent");
        return isNotBlank(fileValue) ? fileValue : DEFAULT_USER_AGENT;
    }

    private String loadFromProperties(String key) {
        String profile = System.getProperty("app.profile", "dev");
        String fileName = "/capi-client-" + profile + ".properties";
        try (InputStream inputStream = getClass().getResourceAsStream(fileName)) {
            if (inputStream == null) {
                return null;
            }
            Properties properties = new Properties();
            properties.load(inputStream);
            return properties.getProperty(key);
        } catch (Exception e) {
            System.err.println("Erreur lecture " + fileName + ": " + e.getMessage());
            return null;
        }
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
