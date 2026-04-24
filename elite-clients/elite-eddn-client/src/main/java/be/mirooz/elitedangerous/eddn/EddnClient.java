package be.mirooz.elitedangerous.eddn;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

/**
 * Client EDDN : file d'attente + worker daemon + POST gzip vers {@code https://eddn.edcd.io:4430/upload/}.
 *
 * <p>Un seul thread de travail consomme la file : les envois sont sérialisés pour limiter les pics.
 * Les appelants ne sont jamais bloqués (enqueue non-bloquant, drop du message si file pleine).</p>
 *
 * <p>Cette classe est agnostique de l'application : elle reçoit {@code softwareName}/{@code softwareVersion}
 * à la construction, et tous les autres paramètres ({@code uploaderId}, {@code gameVersion}, {@code gameBuild},
 * {@code message}) à chaque appel de {@link #publish}. Le stripping des données personnelles est appliqué
 * automatiquement avant envoi.</p>
 */
public final class EddnClient {

    private static final String GATEWAY_URL = "https://eddn.edcd.io:4430/upload/";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final int DEFAULT_QUEUE_CAPACITY = 1024;
    private static final long WORKER_POLL_TIMEOUT_MS = 1_000L;

    private static final AtomicInteger WORKER_COUNTER = new AtomicInteger();

    private final String softwareName;
    private final String softwareVersion;

    private final HttpClient httpClient;
    private final ObjectMapper mapper = EddnEnvelope.mapper();

    private final BlockingQueue<ObjectNode> queue;
    private final Thread worker;
    private volatile boolean running = true;

    public EddnClient(String softwareName, String softwareVersion) {
        this(softwareName, softwareVersion, DEFAULT_QUEUE_CAPACITY);
    }

    public EddnClient(String softwareName, String softwareVersion, int queueCapacity) {
        if (softwareName == null || softwareName.isBlank()) {
            throw new IllegalArgumentException("softwareName must not be blank");
        }
        if (softwareVersion == null || softwareVersion.isBlank()) {
            throw new IllegalArgumentException("softwareVersion must not be blank");
        }
        this.softwareName = softwareName;
        this.softwareVersion = softwareVersion;
        this.queue = new ArrayBlockingQueue<>(queueCapacity);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.worker = new Thread(this::workerLoop, "eddn-uploader-" + WORKER_COUNTER.incrementAndGet());
        this.worker.setDaemon(true);
        this.worker.start();
    }

    /**
     * Empile un message pour publication asynchrone.
     * Non bloquant ; le message est ignoré (log) si la file est saturée.
     *
     * @param schemaRef   constante de {@link EddnSchemas}
     * @param uploaderId  id anonyme stable (cf. {@link EddnUploaderId})
     * @param gameVersion (optionnel) {@code gameversion} tel que reçu de {@code LoadGame}/{@code Fileheader}
     * @param gameBuild   (optionnel) {@code gamebuild} (alias {@code build}) reçu des mêmes events
     * @param message     payload strict du schéma. Un {@link ObjectNode} est attendu pour permettre
     *                    le strip des champs personnels ; tout autre type est ignoré.
     */
    public void publish(String schemaRef,
                        String uploaderId,
                        String gameVersion,
                        String gameBuild,
                        JsonNode message) {
        if (schemaRef == null || uploaderId == null || message == null) {
            return;
        }
        if (!message.isObject()) {
            return;
        }
        ObjectNode body = (ObjectNode) message.deepCopy();
        EddnPersonalDataStripper.stripInPlace(body);

        ObjectNode envelope = EddnEnvelope.build(
                schemaRef,
                uploaderId,
                softwareName,
                softwareVersion,
                gameVersion,
                gameBuild,
                body
        );

        if (!queue.offer(envelope)) {
            System.err.println("EDDN: file pleine, message " + schemaRef + " supprimé.");
        }
    }

    public int queueSize() {
        return queue.size();
    }

    /** Arrête le worker (optionnel : le thread est daemon, la JVM peut sortir sans). */
    public void shutdown() {
        running = false;
        worker.interrupt();
    }

    // ------------------------------------------------------------------
    //  Worker : POST gzip
    // ------------------------------------------------------------------

    private void workerLoop() {
        while (running) {
            try {
                ObjectNode envelope = queue.poll(WORKER_POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                if (envelope == null) {
                    continue;
                }
                post(envelope);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                System.err.println("EDDN worker : " + e.getMessage());
            }
        }
    }

    private void post(ObjectNode envelope) {
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
                return;
            }
            System.err.println("EDDN: rejet " + status + " pour " + schemaRef
                    + (response.body() == null ? "" : " : " + truncate(response.body(), 500)));
        } catch (IOException e) {
            System.err.println("EDDN: erreur réseau POST : " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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
