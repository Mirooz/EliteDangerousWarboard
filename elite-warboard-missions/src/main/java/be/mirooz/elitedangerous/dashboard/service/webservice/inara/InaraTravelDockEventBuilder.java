package be.mirooz.elitedangerous.dashboard.service.webservice.inara;

import be.mirooz.elitedangerous.dashboard.model.registries.commander.CommanderShip;
import be.mirooz.elitedangerous.dashboard.model.registries.commander.CommanderStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Optional;

/**
 * Construit {@code eventData} pour l'évènement Inara {@code addCommanderTravelDock}
 * à partir du journal {@code Docked} et du snapshot CAPI {@code /market} (prix, identifiant de marché).
 */
public final class InaraTravelDockEventBuilder {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private InaraTravelDockEventBuilder() {}

    public static ObjectNode buildEventData(JsonNode dockedEvent, JsonNode capiMarket) {
        if (dockedEvent == null || dockedEvent.isNull()) {
            return null;
        }
        String system = text(dockedEvent, "StarSystem");
        String station = text(dockedEvent, "StationName");
        if (system == null || system.isBlank() || station == null || station.isBlank()) {
            return null;
        }
        ObjectNode data = MAPPER.createObjectNode();
        data.put("starsystemName", system.trim());
        data.put("stationName", station.trim());
        long marketId = 0L;
        if (capiMarket != null && !capiMarket.isNull() && capiMarket.has("id") && !capiMarket.get("id").isNull()) {
            marketId = capiMarket.get("id").asLong(0L);
        }
        if (marketId == 0L) {
            marketId = dockedEvent.path("MarketID").asLong(0L);
        }
        if (marketId != 0L) {
            data.put("marketID", marketId);
        }
        Optional.ofNullable(coordsFromCapi(capiMarket))
                .or(() -> Optional.ofNullable(coordsFromCommander()))
                .ifPresent(n -> data.set("starsystemCoords", n));
        if (!dockedEvent.path("Taxi").asBoolean(false)) {
            String shipType = firstNonBlank(
                    text(dockedEvent, "ShipType"),
                    text(dockedEvent, "Ship"),
                    CommanderShip.getInstance().getShip()
            );
            int shipGid = dockedEvent.path("ShipID").asInt(0);
            if (shipGid == 0) {
                Integer sg = CommanderShip.getInstance().getShipGameId();
                if (sg != null) {
                    shipGid = sg;
                }
            }
            if (shipType != null && !shipType.isBlank()) {
                data.put("shipType", shipType.trim());
            }
            if (shipGid > 0) {
                data.put("shipGameID", shipGid);
            }
        } else {
            if (dockedEvent.path("Dropship").asBoolean(false)) {
                data.put("isTaxiDropship", true);
            } else {
                data.put("isTaxiShuttle", true);
            }
        }
        return data;
    }

    private static JsonNode coordsFromCapi(JsonNode capiMarket) {
        if (capiMarket == null || capiMarket.isNull() || !capiMarket.has("StarPos") || !capiMarket.get("StarPos").isArray()) {
            return null;
        }
        return capiMarket.get("StarPos");
    }

    private static JsonNode coordsFromCommander() {
        double[] p = CommanderStatus.getInstance().getCurrentStarPos();
        if (p == null || p.length < 3) {
            return null;
        }
        var arr = MAPPER.createArrayNode();
        for (int i = 0; i < 3; i++) {
            arr.add(p[i]);
        }
        return arr;
    }

    public static String eventTimestampIso(JsonNode dockedEvent) {
        if (dockedEvent == null || !dockedEvent.has("timestamp")) {
            return null;
        }
        return dockedEvent.get("timestamp").asText(null);
    }

    private static String text(JsonNode n, String field) {
        if (n == null || n.isNull()) {
            return null;
        }
        JsonNode v = n.get(field);
        if (v == null || v.isNull()) {
            return null;
        }
        return v.asText(null);
    }

    private static String firstNonBlank(String a, String b, String c) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        if (c != null && !c.isBlank()) {
            return c;
        }
        return null;
    }
}
