package be.mirooz.elitedangerous.dashboard.service.webservice.eddn;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.zip.GZIPOutputStream;

/**
 * Client HTTP bas niveau pour EDDN : POST gzip vers le gateway officiel.
 *
 * <p>Protocole : corps JSON UTF-8 gzippé, {@code Content-Encoding: gzip},
 * {@code Content-Type: application/json}. Réponse {@code 200} si accepté,
 * {@code 400} si schéma invalide (on log l'erreur sans retry, elle ne partira jamais sinon).
 */
public final class EddnClient {

    private static final String GATEWAY_URL = "https://eddn.edcd.io:4430/upload/";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private static final EddnClient INSTANCE = new EddnClient();

    private final HttpClient httpClient;
    private final ObjectMapper mapper = EddnEnvelope.mapper();

    private EddnClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    public static EddnClient getInstance() {
        return INSTANCE;
    }

    /**
     * Envoie l'enveloppe {@code envelope} de façon synchrone et bloquante.
     *
     * @return {@code true} si HTTP 200, {@code false} sinon (l'appelant décide d'un éventuel retry).
     */
    public boolean post(ObjectNode envelope) {
        if (envelope == null) {
            return false;
        }
        try {
            byte[] json = mapper.writeValueAsBytes(envelope);
            byte[] gzipped = gzip(json);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GATEWAY_URL))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Content-Encoding", "gzip")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(gzipped))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            String schemaRef = envelope.path("$schemaRef").asText("?");
            if (status == 200) {
                System.out.println("EDDN: envoyé " + schemaRef + " (" + gzipped.length + " o gzip)");
                return true;
            }
            System.err.println("EDDN: rejet " + status + " pour " + schemaRef
                    + (response.body() == null ? "" : " : " + truncate(response.body(), 500)));
            return false;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            System.err.println("EDDN: erreur réseau POST : " + e.getMessage());
            return false;
        }
    }

    private static byte[] gzip(byte[] data) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(data.length / 4);
        try (GZIPOutputStream gz = new GZIPOutputStream(out)) {
            gz.write(data);
        }
        return out.toByteArray();
    }

    private static String truncate(String s, int max) {
        if (s == null || s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + "…";
    }
}
