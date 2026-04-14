package be.mirooz.elitedangerous.capi.client;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Petite app console pour tester la connexion CAPI.
 *
 * Usage:
 * - Variable d'environnement: ELITE_CAPI_ACCESS_TOKEN
 * - ou argument CLI: --token=xxx
 * - endpoint optionnel: --endpoint=/profile (par defaut)
 */
public class CapiConnectionTestApp {

    public static void main(String[] args) {
        String token = resolveToken(args);
        boolean fleetCarrierMode = hasAnyArg(args, "--fleetcarrier", "--fleetmarket", "-fleetmarket");
        boolean marketMode = hasAnyArg(args, "--market", "-market");
        boolean journalMode = hasAnyArg(args, "--journal", "-journal");
        String endpoint = resolveEndpoint(args, fleetCarrierMode, marketMode, journalMode);

        if (token == null || token.isBlank()) {
            System.err.println("Token manquant. Passe --token=... ou ELITE_CAPI_ACCESS_TOKEN.");
            System.exit(1);
            return;
        }

        FrontierCapiClient client = new FrontierCapiClient();
        try {
            if (fleetCarrierMode && marketMode) {
                JsonNode fleetCarrierResponse = client.getFleetCarrier(token);
                JsonNode marketResponse = client.getJson("/market", token);
                System.out.println("Connexion CAPI OK");
                System.out.println("Endpoints: /fleetcarrier + /market");
                printFleetCarrierSummary(fleetCarrierResponse);
                System.out.println();
                printMarketSummary(marketResponse);
            } else {
                JsonNode response = fleetCarrierMode
                        ? client.getFleetCarrier(token)
                        : client.getJson(endpoint, token);
                System.out.println("Connexion CAPI OK");
                System.out.println("Endpoint: " + endpoint);
                if (fleetCarrierMode) {
                    printFleetCarrierSummary(response);
                } else if (marketMode) {
                    printMarketSummary(response);
                } else if (journalMode) {
                    printJournalSummary(response);
                } else {
                    System.out.println("Reponse (extrait):");
                    System.out.println(response.toPrettyString());
                }
            }
        } catch (Exception e) {
            System.err.println("Echec connexion CAPI: " + e.getMessage());
            System.exit(2);
        }
    }

    private static String resolveToken(String[] args) {
        for (String arg : args) {
            if (arg != null && arg.startsWith("--token=")) {
                return arg.substring("--token=".length());
            }
        }
        return System.getenv("ELITE_CAPI_ACCESS_TOKEN");
    }

    private static String resolveEndpoint(String[] args, boolean fleetCarrierMode, boolean marketMode, boolean journalMode) {
        if (fleetCarrierMode) {
            return "/fleetcarrier";
        }
        if (marketMode) {
            return "/market";
        }
        if (journalMode) {
            return resolveJournalEndpoint(args);
        }
        for (String arg : args) {
            if (arg != null && arg.startsWith("--endpoint=")) {
                return arg.substring("--endpoint=".length());
            }
        }
        return "/profile";
    }

    private static String resolveJournalEndpoint(String[] args) {
        String year = readOptionValue(args, "--year=");
        String month = readOptionValue(args, "--month=");
        String day = readOptionValue(args, "--day=");

        if (year == null || year.isBlank()) {
            return "/journal";
        }

        StringBuilder endpoint = new StringBuilder("/journal").append("/").append(year);
        if (month == null || month.isBlank()) {
            return endpoint.toString();
        }
        endpoint.append("/").append(month);
        if (day == null || day.isBlank()) {
            return endpoint.toString();
        }
        endpoint.append("/").append(day);
        return endpoint.toString();
    }

    private static boolean hasArg(String[] args, String key) {
        for (String arg : args) {
            if (key.equals(arg)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAnyArg(String[] args, String... keys) {
        for (String key : keys) {
            if (hasArg(args, key)) {
                return true;
            }
        }
        return false;
    }

    private static String readOptionValue(String[] args, String keyPrefix) {
        for (String arg : args) {
            if (arg != null && arg.startsWith(keyPrefix)) {
                return arg.substring(keyPrefix.length());
            }
        }
        return null;
    }

    private static void printFleetCarrierSummary(JsonNode fleetCarrier) {
        System.out.println("FleetCarrier:");
        System.out.println("- nom: " + fleetCarrier.path("name").path("callsign").asText("N/A"));
        System.out.println("- systeme: " + fleetCarrier.path("currentStarSystem").asText("N/A"));
        System.out.println("- balance: " + fleetCarrier.path("balance").asText("N/A"));
        System.out.println("- fuel: " + fleetCarrier.path("fuel").asText("N/A"));

        JsonNode cargo = fleetCarrier.path("cargo");
        JsonNode orders = fleetCarrier.path("orders").path("commodities");
        JsonNode sales = orders.path("sales");
        JsonNode purchases = orders.path("purchases");

        System.out.println("- stock cargo (entrees): " + (cargo.isArray() ? cargo.size() : 0));
        System.out.println("- market sales (entrees): " + (sales.isArray() ? sales.size() : 0));
        System.out.println("- market purchases (entrees): " + (purchases.isArray() ? purchases.size() : 0));
        printFleetCarrierCargoGrouped(cargo);
        printFleetCarrierOrdersDetail("market sales", sales);
        printFleetCarrierOrdersDetail("market purchases", purchases);
        System.out.println();
        System.out.println("Extrait market orders:");
        System.out.println(orders.toPrettyString());
    }

    private static void printMarketSummary(JsonNode market) {
        System.out.println("Market:");
        System.out.println("- station id: " + market.path("id").asText("N/A"));
        System.out.println("- station nom: " + market.path("name").asText("N/A"));
        System.out.println("- type: " + market.path("outpostType").asText("N/A"));

        JsonNode economies = market.path("economies");
        JsonNode commodities = market.path("commodities");
        JsonNode imported = market.path("imported");
        JsonNode exported = market.path("exported");
        JsonNode prohibited = market.path("prohibited");

        System.out.println("- economies: " + fieldCount(economies));
        System.out.println("- commodities: " + (commodities.isArray() ? commodities.size() : 0));
        System.out.println("- imported: " + fieldCount(imported));
        System.out.println("- exported: " + fieldCount(exported));
        System.out.println("- prohibited: " + fieldCount(prohibited));

        if (commodities.isArray() && commodities.size() > 0) {
            System.out.println();
            System.out.println("Top 10 commodities (extrait):");
            int limit = Math.min(10, commodities.size());
            for (int i = 0; i < limit; i++) {
                JsonNode c = commodities.get(i);
                System.out.println("  - " + c.path("name").asText("N/A")
                        + " | buy=" + c.path("buyPrice").asText("0")
                        + " sell=" + c.path("sellPrice").asText("0")
                        + " stock=" + c.path("stock").asText("0")
                        + " demand=" + c.path("demand").asText("0"));
            }
        }
    }

    private static void printFleetCarrierOrdersDetail(String label, JsonNode entries) {
        System.out.println();
        System.out.println("Detail " + label + ":");
        if (!entries.isArray() || entries.isEmpty()) {
            System.out.println("  - aucune entree");
            return;
        }

        for (JsonNode entry : entries) {
            String name = firstNonBlank(
                    entry.path("name").asText(""),
                    entry.path("symbol").asText(""),
                    entry.path("commodity").asText(""),
                    "N/A");
            String price = firstNonBlank(
                    entry.path("price").asText(""),
                    entry.path("salePrice").asText(""),
                    entry.path("buyPrice").asText(""),
                    "0");
            String stock = firstNonBlank(
                    entry.path("stock").asText(""),
                    entry.path("units").asText(""),
                    "0");
            String demand = firstNonBlank(entry.path("demand").asText(""), "0");

            System.out.println("  - " + name
                    + " | price=" + price
                    + " stock=" + stock
                    + " demand=" + demand);
        }
    }

    private static void printJournalSummary(JsonNode journal) {
        System.out.println("Journal:");
        if (journal == null || journal.isMissingNode() || journal.isNull()) {
            System.out.println("- reponse vide");
            return;
        }

        JsonNode entries = journal.path("entries");
        if (!entries.isArray()) {
            entries = journal;
        }

        if (!entries.isArray()) {
            System.out.println("- format inattendu, dump brut:");
            System.out.println(journal.toPrettyString());
            return;
        }

        System.out.println("- nombre d'entrees: " + entries.size());
        int limit = Math.min(15, entries.size());
        if (limit == 0) {
            System.out.println("- aucune entree");
            return;
        }

        System.out.println("Dernieres entrees:");
        for (int i = entries.size() - 1; i >= entries.size() - limit; i--) {
            JsonNode entry = entries.get(i);
            String event = firstNonBlank(entry.path("event").asText(""), entry.path("type").asText(""), "N/A");
            String timestamp = firstNonBlank(entry.path("timestamp").asText(""), entry.path("date").asText(""), "N/A");
            System.out.println("  - " + timestamp + " | " + event);
        }
    }

    private static void printFleetCarrierCargoGrouped(JsonNode cargoEntries) {
        System.out.println();
        System.out.println("Detail cargo groupe par commodity:");
        if (!cargoEntries.isArray() || cargoEntries.isEmpty()) {
            System.out.println("  - aucune entree");
            return;
        }

        Map<String, CargoAggregate> aggregates = new LinkedHashMap<>();
        for (JsonNode entry : cargoEntries) {
            String commodityName = firstNonBlank(
                    entry.path("commodityName").asText(""),
                    entry.path("name").asText(""),
                    entry.path("symbol").asText(""),
                    entry.path("commodity").asText(""),
                    "N/A");
            long quantity = firstLong(entry, "qty", "stock", "units", "count");
            long value = firstLong(entry, "value", "totalValue", "price");

            CargoAggregate aggregate = aggregates.computeIfAbsent(commodityName, key -> new CargoAggregate());
            aggregate.quantity += quantity;
            aggregate.value += value;
        }

        for (Map.Entry<String, CargoAggregate> aggregateEntry : aggregates.entrySet()) {
            CargoAggregate aggregate = aggregateEntry.getValue();
            System.out.println("  - " + aggregateEntry.getKey()
                    + " | qty=" + aggregate.quantity
                    + " value=" + aggregate.value);
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static long firstLong(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.path(fieldName);
            if (value.isNumber()) {
                return value.asLong();
            }
            if (value.isTextual()) {
                String text = value.asText("");
                if (!text.isBlank()) {
                    try {
                        return Long.parseLong(text);
                    } catch (NumberFormatException ignored) {
                        // Keep trying fallback field names.
                    }
                }
            }
        }
        return 0L;
    }

    private static final class CargoAggregate {
        private long quantity;
        private long value;
    }

    private static int fieldCount(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return 0;
        }
        if (node.isObject()) {
            return node.size();
        }
        if (node.isArray()) {
            return node.size();
        }
        return 0;
    }
}
