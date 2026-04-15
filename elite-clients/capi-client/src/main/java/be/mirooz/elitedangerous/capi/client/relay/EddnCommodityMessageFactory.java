package be.mirooz.elitedangerous.capi.client.relay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 * Message EDDN commodity/3 à partir du journal {@code Market} et de la réponse CAPI {@code /market}.
 */
public final class EddnCommodityMessageFactory {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String SCHEMA_REF = "https://eddn.edcd.io/schemas/commodity/3";
    private static final String SOFTWARE_NAME = "EliteWarboard";
    private static final String SOFTWARE_VERSION = "1.3.2-SNAPSHOT";

    private EddnCommodityMessageFactory() {
    }

    public static ObjectNode build(JsonNode journalMarketEvent, JsonNode capiMarket, MarketRelayCommander commander) {
        String systemName = firstNonBlank(
                text(journalMarketEvent, "StarSystem"),
                text(capiMarket, "starSystem"),
                text(capiMarket, "StarSystem"),
                nz(commander.currentStarSystem()),
                "");
        String stationName = firstNonBlank(
                text(journalMarketEvent, "StationName"),
                text(capiMarket, "name"),
                nz(commander.currentStationName()),
                "");

        long marketId = firstLong(journalMarketEvent, "MarketID");
        if (marketId == 0L) {
            marketId = firstLong(capiMarket, "id");
        }

        String timestamp = firstNonBlank(
                text(journalMarketEvent, "timestamp"),
                text(capiMarket, "timestamp"),
                "");

        ObjectNode root = MAPPER.createObjectNode();
        root.put("$schemaRef", SCHEMA_REF);

        ObjectNode header = root.putObject("header");
        header.put("uploaderID", firstNonBlank(nz(commander.fid()), nz(commander.commanderName()), "unknown"));
        header.put("softwareName", SOFTWARE_NAME);
        header.put("softwareVersion", SOFTWARE_VERSION);
        header.put("gameversion", firstNonBlank(nz(commander.gameVersion()), "CAPI-market"));
        header.put("gamebuild", firstNonBlank(nz(commander.gameBuild()), "CAPI-market"));

        ObjectNode message = root.putObject("message");
        message.put("systemName", systemName);
        message.put("stationName", stationName);
        message.put("marketId", marketId);
        message.put("timestamp", timestamp);

        if (commander.horizons() != null) {
            message.put("horizons", commander.horizons());
        }
        if (commander.odyssey() != null) {
            message.put("odyssey", commander.odyssey());
        }

        String stationType = firstNonBlank(
                text(journalMarketEvent, "StationType"),
                text(capiMarket, "outpostType"),
                text(capiMarket, "stationType"));
        if (!stationType.isEmpty()) {
            message.put("stationType", stationType);
        }
        String carrierDocking = text(journalMarketEvent, "CarrierDockingAccess");
        if (!carrierDocking.isEmpty()) {
            message.put("carrierDockingAccess", carrierDocking);
        }

        ArrayNode commodities = MAPPER.createArrayNode();
        JsonNode capiCommodities = capiMarket.path("commodities");
        if (capiCommodities.isArray()) {
            for (JsonNode c : capiCommodities) {
                if (shouldSkipCommodity(c)) {
                    continue;
                }
                ObjectNode row = MAPPER.createObjectNode();
                row.put("name", firstNonBlank(text(c, "name"), text(c, "symbol"), "unknown"));
                row.put("meanPrice", intOrZero(c, "meanPrice"));
                row.put("buyPrice", intOrZero(c, "buyPrice"));
                row.put("sellPrice", intOrZero(c, "sellPrice"));
                row.put("stock", intOrZero(c, "stock"));
                row.set("stockBracket", bracketNode(c, "stockBracket"));
                row.put("demand", intOrZero(c, "demand"));
                row.set("demandBracket", bracketNode(c, "demandBracket"));
                if (c.path("statusFlags").isArray() && !c.path("statusFlags").isEmpty()) {
                    row.set("statusFlags", c.path("statusFlags").deepCopy());
                }
                commodities.add(row);
            }
        }
        message.set("commodities", commodities);

        JsonNode economies = capiMarket.path("economies");
        if (economies.isArray() && !economies.isEmpty()) {
            ArrayNode mapped = mapEconomies(economies);
            if (!mapped.isEmpty()) {
                message.set("economies", mapped);
            }
        }
        JsonNode prohibited = capiMarket.path("prohibited");
        if (prohibited.isArray() && !prohibited.isEmpty()) {
            message.set("prohibited", prohibited.deepCopy());
        }

        return root;
    }

    private static String nz(String s) {
        return s != null ? s : "";
    }

    private static ArrayNode mapEconomies(JsonNode economies) {
        ArrayNode out = MAPPER.createArrayNode();
        for (JsonNode e : economies) {
            if (!e.isObject()) {
                continue;
            }
            ObjectNode row = MAPPER.createObjectNode();
            String name = firstNonBlank(text(e, "name"), text(e, "economy"), text(e, "type"));
            if (name.isEmpty()) {
                continue;
            }
            row.put("name", name);
            if (e.has("proportion")) {
                row.put("proportion", e.get("proportion").asDouble());
            } else {
                row.put("proportion", 0.0);
            }
            out.add(row);
        }
        return out;
    }

    private static boolean shouldSkipCommodity(JsonNode c) {
        String legality = text(c, "legality");
        if (!legality.isEmpty() && "illegal".equalsIgnoreCase(legality.trim())) {
            return true;
        }
        JsonNode flags = c.path("statusFlags");
        if (flags.isArray()) {
            for (JsonNode f : flags) {
                if (f.isTextual() && "Illegal".equalsIgnoreCase(f.asText().trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static JsonNode bracketNode(JsonNode commodity, String field) {
        JsonNode v = commodity.get(field);
        if (v == null || v.isNull() || v.isMissingNode()) {
            return MAPPER.getNodeFactory().numberNode(0);
        }
        if (v.isIntegralNumber()) {
            return MAPPER.getNodeFactory().numberNode(v.intValue());
        }
        if (v.isTextual()) {
            String s = v.asText().trim();
            if (s.isEmpty()) {
                return TextNode.valueOf("");
            }
            try {
                return MAPPER.getNodeFactory().numberNode(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return MAPPER.getNodeFactory().numberNode(0);
            }
        }
        return MAPPER.getNodeFactory().numberNode(0);
    }

    private static int intOrZero(JsonNode node, String field) {
        JsonNode v = node.get(field);
        if (v == null || v.isNull() || v.isMissingNode()) {
            return 0;
        }
        if (v.isIntegralNumber()) {
            return v.intValue();
        }
        if (v.isNumber()) {
            return (int) v.asDouble();
        }
        if (v.isTextual()) {
            try {
                return Integer.parseInt(v.asText().trim());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    private static long firstLong(JsonNode node, String field) {
        JsonNode v = node.get(field);
        if (v == null || v.isNull() || v.isMissingNode()) {
            return 0L;
        }
        if (v.isIntegralNumber()) {
            return v.longValue();
        }
        if (v.isTextual()) {
            try {
                return Long.parseLong(v.asText().trim());
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
        return 0L;
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        if (v == null || v.isNull() || v.isMissingNode()) {
            return "";
        }
        if (v.isTextual()) {
            return v.asText("");
        }
        if (v.isIntegralNumber()) {
            return v.asText();
        }
        return "";
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
