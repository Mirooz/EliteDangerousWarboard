package be.mirooz.elitedangerous.capi.client.relay;

import be.mirooz.elitedangerous.capi.client.CapiBundledProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Backend Warboard : {@code GET /market} (proxy CAPI) et {@code POST} relais EDDN / Inara.
 */
public final class CapiClient {

    private static final Duration CONNECT = Duration.ofSeconds(10);
    private static final Duration REQUEST = Duration.ofSeconds(20);
    private static final CapiClient INSTANCE = new CapiClient();

    private final HttpClient httpClient;
    private final String baseUrl;
    private final ObjectMapper json = new ObjectMapper();

    private CapiClient() {
        this.httpClient = HttpClient.newBuilder().connectTimeout(CONNECT).build();
        this.baseUrl = require("capi.base-url");
    }

    public static CapiClient getInstance() {
        return INSTANCE;
    }

    public JsonNode getMarket() throws IOException {
        return getJson("/market", null);
    }

    public JsonNode sendMarket(ObjectNode marketPayload) throws IOException {
        HttpResult result = postJsonAbsoluteWithStatus(
                relayMarketUrl(),
                json.writeValueAsString(marketPayload));
        String responseBody = result.body();
        if (result.statusCode() != 401 && (result.statusCode() < 200 || result.statusCode() >= 300)) {
            throw new IOException("POST " + relayMarketUrl() + " : HTTP " + result.statusCode() + " - " + responseBody);
        }
        JsonNode response = responseBody == null || responseBody.isBlank()
                ? json.createObjectNode()
                : json.readTree(responseBody);
        handleMarketResponse(response);
        return response;
    }
    public String relayMarketUrl() {
        return CapiBundledProperties.get("capi.relay.market-url", "http://localhost:8080/api/capi/market");
    }

    private void handleMarketResponse(JsonNode response) {
        if ("authenticate_frontier".equalsIgnoreCase(response.path("action").asText())) {
            String loginUrl = response.path("loginUrl").asText("");
            String error = response.path("error").asText("");
            if (!error.isBlank()) {
                System.err.println("CAPI market: " + error);
            }
            openBrowser(loginUrl);
            return;
        }
        if ("ok".equalsIgnoreCase(response.path("status").asText())) {
            System.out.println("EDDN: marche envoye via CAPI (" + relayMarketUrl() + "), marketId="
                    + response.path("marketId").asText(""));
            return;
        }
        System.err.println("CAPI market: reponse inattendue " + response);
    }

    private static void openBrowser(String loginUrl) {
        if (loginUrl == null || loginUrl.isBlank()) {
            System.err.println("CAPI market: loginUrl manquante pour l'authentification Frontier.");
            return;
        }
        try {
            Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
            if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(URI.create(loginUrl));
                System.out.println("CAPI market: ouverture du navigateur pour authentification Frontier.");
            } else {
                System.err.println("CAPI market: ouverture navigateur non supportee. URL: " + loginUrl);
            }
        } catch (Exception e) {
            System.err.println("CAPI market: impossible d'ouvrir le navigateur. URL: " + loginUrl + " (" + e.getMessage() + ")");
        }
    }

    private JsonNode getJson(String endpoint, String accessToken) throws IOException {
        String url = absoluteUrl(endpoint);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .timeout(REQUEST);
        if (accessToken != null && !accessToken.isBlank()) {
            builder.header("Authorization", "Bearer " + accessToken.trim());
        }
        HttpResponse<String> response = send(builder.build());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return json.readTree(response.body());
        }
        throw new IOException("CAPI GET " + endpoint + " : HTTP " + response.statusCode() + " - " + response.body());
    }

    private HttpResult postJsonAbsoluteWithStatus(String absoluteUrl, String jsonBody) throws IOException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(absoluteUrl))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .timeout(REQUEST)
                .build();
        HttpResponse<String> response = send(request);
        return new HttpResult(response.statusCode(), response.body());
    }

    private String absoluteUrl(String endpoint) {
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
        String value = CapiBundledProperties.get(key);
        if (value == null) {
            throw new IllegalStateException("Propriete obligatoire manquante : " + key + " dans capi-client-"
                    + System.getProperty("app.profile", "dev") + ".properties");
        }
        return value;
    }

    private record HttpResult(int statusCode, String body) {
    }
}
