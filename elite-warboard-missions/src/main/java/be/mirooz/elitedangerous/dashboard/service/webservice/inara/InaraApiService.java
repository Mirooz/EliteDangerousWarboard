package be.mirooz.elitedangerous.dashboard.service.webservice.inara;

import be.mirooz.elitedangerous.dashboard.model.registries.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.service.PreferencesService;
import be.mirooz.elitedangerous.dashboard.service.webservice.eddn.EddnAppInfo;
import be.mirooz.elitedangerous.dashboard.view.common.context.DashboardContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Envois vers l'API Inara (POST {@code https://inara.cz/inapi/v1/}), déclenchés après
 * un accostage + snapshot marché CAPI : évènement {@code addCommanderTravelDock}.
 *
 * <p>Respect d'un intervalle minimum entre requêtes (35 s) aligné sur les usages courants
 * (limite côté Inara ~2 requêtes / minute). Non bloquant pour l'UI (thread démon).</p>
 */
public final class InaraApiService {

    public static final String INARA_URL = "https://inara.cz/inapi/v1/";

    /** Même ordre de grandeur qu'EDMarketConnector pour ne pas saturer l'API. */
    private static final long MIN_INTERVAL_MS = 35_000L;

    private static final InaraApiService INSTANCE = new InaraApiService();

    private static final String APP_NAME = "EliteWarboard";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final PreferencesService preferences = PreferencesService.getInstance();
    private final DashboardContext dashboardContext = DashboardContext.getInstance();
    private final Object intervalLock = new Object();
    private long lastRequestAtMs;

    private InaraApiService() {}

    public static InaraApiService getInstance() {
        return INSTANCE;
    }

    /**
     * Enfile l'envoi d'un accostage Inara (hors fil UI). Sans effet si la préférence ou la clé est absente,
     * ou en rechargement batch journal.
     */
    public void enqueueTravelDockAfterCapiMarket(JsonNode journalDocked, JsonNode capiMarket) {
        if (!preferences.isInaraApiEnabled()) {
            return;
        }
        String key = preferences.getInaraApiKey();
        if (key == null || key.isBlank()) {
            return;
        }
        if (dashboardContext.isBatchLoading()) {
            return;
        }
        if (!shouldSendNow()) {
            return;
        }
        ObjectNode eventData = InaraTravelDockEventBuilder.buildEventData(journalDocked, capiMarket);
        if (eventData == null) {
            return;
        }
        String ts = InaraTravelDockEventBuilder.eventTimestampIso(journalDocked);
        CommanderStatus s = CommanderStatus.getInstance();
        String cmdr = s.getCommanderName();
        String fid = s.getFID();
        if (cmdr == null || cmdr.isBlank() || fid == null || fid.isBlank()) {
            return;
        }
        String tsIso = normalizeEventTimestampIso(ts);
        String fidNorm = normalizeFrontierId(fid);
        String body = buildRequestBody(key, cmdr, fidNorm, tsIso, eventData);
        Thread t = new Thread(() -> postSilently(body), "inara-api");
        t.setDaemon(true);
        t.start();
    }

    private boolean shouldSendNow() {
        long now = System.currentTimeMillis();
        synchronized (intervalLock) {
            if (now - lastRequestAtMs < MIN_INTERVAL_MS) {
                return false;
            }
            lastRequestAtMs = now;
            return true;
        }
    }

    private void postSilently(String body) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(INARA_URL))
                    .timeout(Duration.ofSeconds(35))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() < 200 || res.statusCode() >= 300) {
                System.err.println("Inara API: HTTP " + res.statusCode() + " " + res.body());
                return;
            }
            JsonNode root = MAPPER.readTree(res.body());
            int headerStatus = root.path("header").path("eventStatus").asInt(0);
            int event0Status = root.path("events").isArray() && !root.path("events").isEmpty()
                    ? root.path("events").get(0).path("eventStatus").asInt(0)
                    : 0;
            int status = headerStatus != 0 ? headerStatus : event0Status;
            if (status / 100 == 2) {
                System.out.println(
                        "Inara API : addCommanderTravelDock envoyé avec succès (eventStatus=" + status + ").");
            } else {
                String msg = root.path("header").path("eventStatusText").asText("");
                if (msg.isBlank() && root.path("events").isArray() && !root.path("events").isEmpty()) {
                    msg = root.path("events").get(0).path("eventStatusText").asText("");
                }
                System.err.println("Inara API: " + status + " " + msg);
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            System.err.println("Inara API: " + e.getMessage());
        }
    }

    private String buildRequestBody(
            String apiKey,
            String commanderName,
            String commanderFrontierId,
            String eventTimestamp,
            ObjectNode eventData) {
        ObjectNode header = MAPPER.createObjectNode();
        header.put("appName", APP_NAME);
        header.put("appVersion", EddnAppInfo.version());
        header.put("isBeingDeveloped", true);
        header.put("APIkey", apiKey);
        header.put("commanderName", commanderName);
        header.put("commanderFrontierID", commanderFrontierId);

        ObjectNode ev = MAPPER.createObjectNode();
        ev.put("eventName", "addCommanderTravelDock");
        ev.put("eventTimestamp", eventTimestamp);
        ev.set("eventData", eventData);

        ArrayNode events = JsonNodeFactory.instance.arrayNode();
        events.add(ev);

        ObjectNode root = MAPPER.createObjectNode();
        root.set("header", header);
        root.set("events", events);
        try {
            return MAPPER.writeValueAsString(root);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String normalizeEventTimestampIso(String raw) {
        try {
            if (raw == null || raw.isBlank()) {
                return OffsetDateTime.now(ZoneOffset.UTC).toString();
            }
            return OffsetDateTime.parse(raw).withOffsetSameInstant(ZoneOffset.UTC).toString();
        } catch (Exception ignored) {
            return OffsetDateTime.now(ZoneOffset.UTC).toString();
        }
    }

    private static String normalizeFrontierId(String fid) {
        if (fid == null || fid.isBlank()) {
            return "";
        }
        String t = fid.trim();
        if (t.toUpperCase().startsWith("F")) {
            return t;
        }
        if (t.chars().allMatch(Character::isDigit)) {
            return "F" + t;
        }
        return t;
    }
}
