package be.mirooz.elitedangerous.capi.client.relay;

import be.mirooz.elitedangerous.capi.client.CapiBundledProperties;
import be.mirooz.elitedangerous.capi.client.FrontierCapiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;

/**
 * Backend Warboard : {@code GET /market} (proxy CAPI) et {@code POST} relais EDDN / Inara.
 */
public final class CapiClient {

    private static final CapiClient INSTANCE = new CapiClient();

    private final FrontierCapiClient http = new FrontierCapiClient();
    private final ObjectMapper json = new ObjectMapper();

    private CapiClient() {
    }

    public static CapiClient getInstance() {
        return INSTANCE;
    }

    public JsonNode getMarket() throws IOException {
        return http.getJson("/market");
    }

    public void sendEddnCommodity(ObjectNode envelope) throws IOException {
        http.postJson(relayPathEddn(), json.writeValueAsString(envelope));
    }

    public void sendInaraBatch(ObjectNode inaraBatch) throws IOException {
        String responseBody = http.postJson(relayPathInara(), json.writeValueAsString(inaraBatch));
        validateInaraResponse(responseBody);
    }

    public String eddnRelayUrl() {
        return http.absoluteUrl(relayPathEddn());
    }

    public String inaraRelayUrl() {
        return http.absoluteUrl(relayPathInara());
    }

    private static String relayPathEddn() {
        return CapiBundledProperties.get("capi.relay.eddn-path", "/relay/eddn");
    }

    private static String relayPathInara() {
        return CapiBundledProperties.get("capi.relay.inara-path", "/relay/inara");
    }

    private void validateInaraResponse(String responseBody) throws IOException {
        if (responseBody == null || responseBody.isBlank()) {
            return;
        }
        JsonNode root;
        try {
            root = json.readTree(responseBody);
        } catch (Exception e) {
            return;
        }
        if (!root.has("header")) {
            return;
        }
        int headerStatus = root.path("header").path("eventStatus").asInt(0);
        if (headerStatus != 0 && headerStatus != 200 && headerStatus != 202) {
            String statusText = root.path("header").path("eventStatusText").asText("");
            if (headerStatus == 400 && statusText.toLowerCase().contains("no access allowed")) {
                throw new IOException("Inara API header status 400: accès refusé pour l'application '"
                        + InaraEventFactory.resolveInaraAppName()
                        + "'. Whitelist requise sur Inara ou ELITE_INARA_APP_NAME.");
            }
            throw new IOException("Inara API header status " + headerStatus + " : " + statusText);
        }
        JsonNode events = root.path("events");
        if (events.isArray()) {
            for (JsonNode event : events) {
                int status = event.path("eventStatus").asInt(0);
                if (status != 0 && status != 200 && status != 202) {
                    throw new IOException("Inara API event status " + status + " : "
                            + event.path("eventStatusText").asText(""));
                }
            }
        }
    }
}
