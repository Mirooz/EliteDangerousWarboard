package be.mirooz.elitedangerous.capi.client;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

/**
 * App console : un GET {@code /market} via {@link FrontierCapiClient} (config {@code capi-client-*.properties}).
 * <p>
 * Lancement : {@code mvn -pl elite-clients/capi-client exec:java}
 * <br>
 * Profil : {@code mvn -pl elite-clients/capi-client exec:java -Dapp.profile=prod}
 * <p>
 * Option : {@code --token=JWT} (Bearer, optionnel).
 */
public final class CapiHttpTestApp {

    private static final String ENDPOINT = "/market";

    public static void main(String[] args) {
        if (hasHelp(args)) {
            printHelp();
            return;
        }

        String token = argValue(args, "--token=");

        FrontierCapiClient client = new FrontierCapiClient();
        System.out.println("Base (effective) : requêtes sur " + describeBase(client));

        try {
            JsonNode body = token.isBlank()
                    ? client.getJson(ENDPOINT)
                    : client.getJson(ENDPOINT, token);
            System.out.println();
            System.out.println("=== GET " + ENDPOINT + " ===");
            System.out.println(body.toPrettyString());
        } catch (IOException e) {
            System.err.println("Echec GET " + ENDPOINT + " : " + e.getMessage());
            System.exit(2);
        }
    }

    private static String describeBase(FrontierCapiClient client) {
        try {
            return client.absoluteUrl("/");
        } catch (Exception e) {
            return "(voir capi.base-url dans capi-client-*.properties)";
        }
    }

    private static boolean hasHelp(String[] args) {
        for (String a : args) {
            if ("--help".equals(a) || "-h".equals(a)) {
                return true;
            }
        }
        return false;
    }

    private static void printHelp() {
        System.out.println("""
                CapiHttpTestApp — GET /market (JSON) via backend (properties embarquées).

                mvn -pl elite-clients/capi-client exec:java
                mvn -pl elite-clients/capi-client exec:java -Dexec.args="--token=xxx"

                Options :
                  --token=JWT    Authorization Bearer (optionnel, ex. Frontier direct).
                  -h, --help     Aide.
                """);
    }

    private static String argValue(String[] args, String prefix) {
        for (String a : args) {
            if (a != null && a.startsWith(prefix)) {
                return a.substring(prefix.length()).trim();
            }
        }
        return "";
    }

    private CapiHttpTestApp() {
    }
}
