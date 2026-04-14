package be.mirooz.elitedangerous.capi.client;

import com.fasterxml.jackson.databind.JsonNode;

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
        boolean fleetCarrierMode = hasArg(args, "--fleetcarrier");
        boolean marketMode = hasArg(args, "--market");
        String endpoint = resolveEndpoint(args, fleetCarrierMode, marketMode);

        if (token == null || token.isBlank()) {
            System.err.println("Token manquant. Passe --token=... ou ELITE_CAPI_ACCESS_TOKEN.");
            System.exit(1);
            return;
        }

        FrontierCapiClient client = new FrontierCapiClient();
        try {
            JsonNode response = fleetCarrierMode
                    ? client.getFleetCarrier(token)
                    : client.getJson(endpoint, token);
            System.out.println("Connexion CAPI OK");
            System.out.println("Endpoint: " + endpoint);
            if (fleetCarrierMode) {
                printFleetCarrierSummary(response);
            } else if (marketMode) {
                printMarketSummary(response);
            } else {
                System.out.println("Reponse (extrait):");
                System.out.println(response.toPrettyString());
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

    private static String resolveEndpoint(String[] args, boolean fleetCarrierMode, boolean marketMode) {
        if (fleetCarrierMode) {
            return "/fleetcarrier";
        }
        if (marketMode) {
            return "/market";
        }
        for (String arg : args) {
            if (arg != null && arg.startsWith("--endpoint=")) {
                return arg.substring("--endpoint=".length());
            }
        }
        return "/profile";
    }

    private static boolean hasArg(String[] args, String key) {
        for (String arg : args) {
            if (key.equals(arg)) {
                return true;
            }
        }
        return false;
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
        System.out.println();
        System.out.println("Extrait cargo:");
        System.out.println(cargo.toPrettyString());
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
