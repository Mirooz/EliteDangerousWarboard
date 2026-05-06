package be.mirooz.elitedangerous.dashboard.service.webservice.eddn;

import be.mirooz.elitedangerous.eddn.EddnEnvelope;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Construit le corps {@code message} du schéma EDDN commodity/3 à partir du journal {@code Docked}
 * et du snapshot CAPI {@code /market} (même logique qu’anciennement côté serveur analytics).
 */
public final class CapiMarketEddnMessageBuilder {

    private static final ObjectMapper MAPPER = EddnEnvelope.mapper();

    private CapiMarketEddnMessageBuilder() {}

    /**
     * @return {@code null} si aucune commodité exploitable (pas d’envoi EDDN).
     */
    public static ObjectNode buildCommodityMessage(JsonNode dockedEvent, JsonNode capiMarket) {
        if (capiMarket == null || capiMarket.isNull()) {
            return null;
        }
        List<Map<String, Object>> commodities = mapCommodities(capiMarket);
        if (commodities.isEmpty()) {
            return null;
        }
        ObjectNode message = MAPPER.createObjectNode();
        message.put("timestamp", normalizeTimestamp(text(dockedEvent, "timestamp")));
        message.put("systemName", pickText(text(dockedEvent, "StarSystem"), text(capiMarket, "systemName")));
        message.put("stationName", pickText(text(dockedEvent, "StationName"), text(capiMarket, "name")));
        String stationType = text(dockedEvent, "StationType");
        if (stationType != null && !stationType.isBlank()) {
            message.put("stationType", stationType);
        }
        String docking = text(dockedEvent, "CarrierDockingAccess");
        if (docking != null && !docking.isBlank()) {
            message.put("carrierDockingAccess", docking);
        }
        message.put("marketId", capiMarket.path("id").asLong());
        message.set("commodities", MAPPER.valueToTree(commodities));
        return message;
    }

    private static String text(JsonNode node, String field) {
        if (node == null || node.isNull()) {
            return null;
        }
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) {
            return null;
        }
        return v.asText(null);
    }

    private static List<Map<String, Object>> mapCommodities(JsonNode capiMarket) {
        List<Map<String, Object>> out = new ArrayList<>();
        JsonNode commoditiesNode = capiMarket.path("commodities");
        if (!commoditiesNode.isArray()) {
            return mapFromCarrierOrders(capiMarket.path("orders").path("commodities"));
        }
        for (JsonNode c : commoditiesNode) {
            Map<String, Object> item = new LinkedHashMap<>();
            String name = c.path("name").asText("");
            if (name == null || name.isBlank()) {
                continue;
            }
            item.put("name", name);
            item.put("meanPrice", c.path("meanPrice").asInt(0));
            item.put("buyPrice", c.path("buyPrice").asInt(0));
            item.put("stock", c.path("stock").asInt(0));
            item.put("stockBracket", normalizeBracket(c.path("stockBracket")));
            item.put("sellPrice", c.path("sellPrice").asInt(0));
            item.put("demand", c.path("demand").asInt(0));
            item.put("demandBracket", normalizeBracket(c.path("demandBracket")));
            if (c.path("statusFlags").isArray()) {
                List<String> flags = new ArrayList<>();
                for (JsonNode flag : c.path("statusFlags")) {
                    if (flag.isTextual()) {
                        flags.add(flag.asText());
                    }
                }
                if (!flags.isEmpty()) {
                    item.put("statusFlags", flags);
                }
            }
            out.add(item);
        }
        return out;
    }

    private static List<Map<String, Object>> mapFromCarrierOrders(JsonNode ordersCommoditiesNode) {
        Map<String, Map<String, Object>> byName = new HashMap<>();
        JsonNode sales = ordersCommoditiesNode.path("sales");
        if (sales.isArray()) {
            for (JsonNode sale : sales) {
                String name = sale.path("name").asText("");
                if (name == null || name.isBlank()) {
                    continue;
                }
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("name", name);
                item.put("meanPrice", 0);
                item.put("buyPrice", sale.path("price").asInt(0));
                item.put("stock", sale.path("stock").asInt(0));
                item.put("stockBracket", 0);
                item.put("sellPrice", 0);
                item.put("demand", 0);
                item.put("demandBracket", 0);
                byName.put(name, item);
            }
        }
        JsonNode purchases = ordersCommoditiesNode.path("purchases");
        if (purchases.isArray()) {
            for (JsonNode purchase : purchases) {
                String name = purchase.path("name").asText("");
                if (name == null || name.isBlank()) {
                    continue;
                }
                Map<String, Object> item = byName.getOrDefault(name, new LinkedHashMap<>());
                item.putIfAbsent("name", name);
                item.putIfAbsent("meanPrice", 0);
                item.putIfAbsent("buyPrice", 0);
                item.putIfAbsent("stock", 0);
                item.putIfAbsent("stockBracket", 0);
                item.put("sellPrice", purchase.path("price").asInt(0));
                item.put("demand", purchase.path("outstanding").asInt(purchase.path("total").asInt(0)));
                item.put("demandBracket", 0);
                byName.put(name, item);
            }
        }
        return new ArrayList<>(byName.values());
    }

    private static Object normalizeBracket(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return 0;
        }
        if (node.isInt() || node.isLong()) {
            int v = node.asInt(0);
            if (v < 0 || v > 3) {
                return 0;
            }
            return v;
        }
        String s = node.asText("");
        if (s.isBlank()) {
            return "";
        }
        try {
            int v = Integer.parseInt(s);
            if (v < 0 || v > 3) {
                return 0;
            }
            return v;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String normalizeTimestamp(String raw) {
        try {
            if (raw == null || raw.isBlank()) {
                return Instant.now().toString();
            }
            // EDDN / JSON Schema date-time (RFC 3339) exige HH:mm:ss ; OffsetDateTime#toString()
            // peut omettre les secondes (ex. journal Docked → "…T12:34Z") et le gateway rejette alors le message.
            return OffsetDateTime.parse(raw).withOffsetSameInstant(ZoneOffset.UTC).toInstant().toString();
        } catch (Exception ignored) {
            return Instant.now().toString();
        }
    }

    private static String pickText(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return "";
    }
}
