package be.mirooz.elitedangerous.capi.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Scanner;

/**
 * App console de test OAuth2 PKCE pour récupérer un access_token Frontier.
 */
public class CapiOAuthPkceTestApp {

    private static final String AUTH_URL = "https://auth.frontierstore.net/auth";
    private static final String TOKEN_URL = "https://auth.frontierstore.net/token";
    private static final String DEFAULT_SCOPE = "auth capi";
    private static final String DEFAULT_AUDIENCE = "frontier";
    private static final String DEFAULT_REDIRECT_URI = "https://localhost/elite-warboard/callback";

    public static void main(String[] args) {
        String clientId = argValue(args, "--client-id=");
        String redirectUri = argValue(args, "--redirect-uri=");
        if (redirectUri == null || redirectUri.isBlank()) {
            redirectUri = DEFAULT_REDIRECT_URI;
        }
        String scope = argValue(args, "--scope=");
        if (scope == null || scope.isBlank()) {
            scope = DEFAULT_SCOPE;
        }
        String audience = argValue(args, "--audience=");
        if (audience == null || audience.isBlank()) {
            audience = DEFAULT_AUDIENCE;
        }

        if (clientId == null || clientId.isBlank()) {
            System.err.println("Client ID manquant. Usage: --client-id=xxxx");
            System.exit(1);
            return;
        }

        try {
            String codeVerifier = generateCodeVerifier();
            String codeChallenge = generateCodeChallenge(codeVerifier);
            String state = randomBase64UrlNoPadding(32);

            String authRequestUrl = AUTH_URL
                    + "?audience=" + encode(audience)
                    + "&scope=" + encode(scope)
                    + "&response_type=code"
                    + "&client_id=" + encode(clientId)
                    + "&code_challenge=" + encode(codeChallenge)
                    + "&code_challenge_method=S256"
                    + "&state=" + encode(state)
                    + "&redirect_uri=" + encode(redirectUri);

            System.out.println("1) Ouvre cette URL dans ton navigateur et autorise l'application :");
            System.out.println(authRequestUrl);
            openBrowserIfPossible(authRequestUrl);

            System.out.println();
            System.out.println("2) Copie/colle soit l'URL de redirection complete, soit juste la valeur du parametre 'code'.");
            System.out.print("> ");

            Scanner scanner = new Scanner(System.in);
            String input = scanner.nextLine();
            String code = extractCode(input);
            String returnedState = extractState(input);
            if (returnedState != null && !returnedState.isBlank() && !state.equals(returnedState)) {
                System.err.println("State invalide. Refuse pour securite.");
                System.exit(2);
                return;
            }
            if (code == null || code.isBlank()) {
                System.err.println("Code introuvable dans la saisie.");
                System.exit(3);
                return;
            }

            JsonNode tokenResponse = exchangeCodeForToken(clientId, redirectUri, code, codeVerifier);
            System.out.println("Token recupere avec succes:");
            System.out.println(tokenResponse.toPrettyString());
        } catch (Exception e) {
            System.err.println("Erreur OAuth PKCE: " + e.getMessage());
            System.exit(10);
        }
    }

    private static JsonNode exchangeCodeForToken(
            String clientId,
            String redirectUri,
            String code,
            String codeVerifier
    ) throws Exception {
        String body = "redirect_uri=" + encode(redirectUri)
                + "&code=" + encode(code)
                + "&grant_type=authorization_code"
                + "&code_verifier=" + encode(codeVerifier)
                + "&client_id=" + encode(clientId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "EDCD-EliteWarboard-1.3.2")
                .timeout(Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Requete interrompue", e);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode() + " - " + response.body());
        }

        return new ObjectMapper().readTree(response.body());
    }

    private static void openBrowserIfPossible(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    desktop.browse(URI.create(url));
                }
            }
        } catch (Exception ignored) {
            // Optionnel: l'utilisateur peut ouvrir l'URL manuellement.
        }
    }

    private static String extractCode(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        if (!input.contains("code=")) {
            return input.trim();
        }
        return extractQueryParam(input, "code");
    }

    private static String extractState(String input) {
        if (input == null || !input.contains("state=")) {
            return null;
        }
        return extractQueryParam(input, "state");
    }

    private static String extractQueryParam(String urlOrQuery, String key) {
        int queryIdx = urlOrQuery.indexOf('?');
        String query = queryIdx >= 0 ? urlOrQuery.substring(queryIdx + 1) : urlOrQuery;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String k = decode(pair.substring(0, idx));
            if (key.equals(k)) {
                return decode(pair.substring(idx + 1));
            }
        }
        return null;
    }

    private static String argValue(String[] args, String prefix) {
        for (String arg : args) {
            if (arg != null && arg.startsWith(prefix)) {
                return arg.substring(prefix.length());
            }
        }
        return null;
    }

    private static String generateCodeVerifier() {
        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        // Important: conserver le '=' final pour Frontier (retour d'experience EDCD).
        return Base64.getUrlEncoder().encodeToString(random);
    }

    private static String generateCodeChallenge(String codeVerifier) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
        // Important: ici il faut supprimer le '=' final.
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

    private static String randomBase64UrlNoPadding(int bytes) {
        byte[] random = new byte[bytes];
        new SecureRandom().nextBytes(random);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String decode(String value) {
        return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
