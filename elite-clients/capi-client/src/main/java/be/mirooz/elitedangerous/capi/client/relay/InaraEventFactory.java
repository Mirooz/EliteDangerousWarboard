package be.mirooz.elitedangerous.capi.client.relay;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Batch Inara API (header + events).
 */
public final class InaraEventFactory {

    private static final JsonNodeFactory JSON = JsonNodeFactory.instance;

    private InaraEventFactory() {
    }

    public static ObjectNode buildDockFromMarketEvent(
            String timestamp,
            String starSystem,
            String stationName,
            long marketId,
            MarketRelayCommander commander) {

        ObjectNode root = JSON.objectNode();
        ObjectNode header = root.putObject("header");
        header.put("appName", resolveInaraAppName());
        header.put("appVersion", resolveInaraAppVersion());
        header.put("isBeingDeveloped", !isProd());
        header.put("APIkey", resolveInaraApiKey());

        if (commander.commanderName() != null && !commander.commanderName().isBlank()) {
            header.put("commanderName", commander.commanderName());
        }
        if (commander.fid() != null && !commander.fid().isBlank()) {
            header.put("commanderFrontierID", commander.fid());
        }

        ArrayNode events = root.putArray("events");
        ObjectNode event = events.addObject();
        event.put("eventName", "addCommanderTravelDock");
        event.put("eventTimestamp", timestamp);

        ObjectNode eventData = event.putObject("eventData");
        eventData.put("starsystemName", starSystem);
        eventData.put("stationName", stationName);
        if (marketId > 0L) {
            eventData.put("marketID", marketId);
        }

        return root;
    }

    public static String resolveInaraApiKey() {
        String env = System.getenv("ELITE_INARA_API_KEY");
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        String prop = System.getProperty("elite.inara.api-key");
        if (prop != null && !prop.isBlank()) {
            return prop.trim();
        }
        return "";
    }

    public static String resolveInaraAppName() {
        String env = System.getenv("ELITE_INARA_APP_NAME");
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        String prop = System.getProperty("elite.inara.app-name");
        if (prop != null && !prop.isBlank()) {
            return prop.trim();
        }
        return "EliteWarboard";
    }

    public static String resolveInaraAppVersion() {
        String env = System.getenv("ELITE_INARA_APP_VERSION");
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        String prop = System.getProperty("elite.inara.app-version");
        if (prop != null && !prop.isBlank()) {
            return prop.trim();
        }
        return "1.3.2";
    }

    private static boolean isProd() {
        String profile = System.getProperty("app.profile", "dev");
        return "prod".equalsIgnoreCase(profile);
    }
}
