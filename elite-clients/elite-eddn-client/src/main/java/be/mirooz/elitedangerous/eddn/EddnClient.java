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
 * Client EDDN : file d'attente + worker daemon + POST gzip vers la gateway EDDN configurée
 * dans {@code elite-eddn-client-<app.profile>.properties} ({@link EddnBundledProperties#KEY_UPLOAD_URL}).
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

    /**
     * Fallback si le properties du module est introuvable (profile mal paramétré, ressource shaded
     * out, …) : on pointe sur LIVE pour que l'upload reste fonctionnel.
     */
    private static final String DEFAULT_GATEWAY_URL = "https://eddn.edcd.io:4430/upload/";

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final int DEFAULT_QUEUE_CAPACITY = 1024;
    private static final long WORKER_POLL_TIMEOUT_MS = 1_000L;

    private static final AtomicInteger WORKER_COUNTER = new AtomicInteger();

    private final String softwareName;
    private final String softwareVersion;
    private final String gatewayUrl;
    /** Label concis de l'endpoint cible ({@code live} / {@code beta} / {@code dev}) pour les logs. */
    private final String endpointLabel;

    private final HttpClient httpClient;
    private final ObjectMapper mapper = EddnEnvelope.mapper();

    private final BlockingQueue<ObjectNode> queue;
    private final Thread worker;
    private volatile boolean running = true;

    /** Cible la gateway résolue via {@link EddnBundledProperties} selon {@code app.profile}. */
    public EddnClient(String softwareName, String softwareVersion) {
        this(softwareName, softwareVersion, DEFAULT_QUEUE_CAPACITY,
                EddnBundledProperties.get(EddnBundledProperties.KEY_UPLOAD_URL, DEFAULT_GATEWAY_URL));
    }

    public EddnClient(String softwareName, String softwareVersion, int queueCapacity, String gatewayUrl) {
        if (softwareName == null || softwareName.isBlank()) {
            throw new IllegalArgumentException("softwareName must not be blank");
        }
        if (softwareVersion == null || softwareVersion.isBlank()) {
            throw new IllegalArgumentException("softwareVersion must not be blank");
        }
        if (gatewayUrl == null || gatewayUrl.isBlank()) {
            throw new IllegalArgumentException("gatewayUrl must not be blank");
        }
        this.softwareName = softwareName;
        this.softwareVersion = softwareVersion;
        this.gatewayUrl = gatewayUrl;
        this.endpointLabel = resolveEndpointLabel(gatewayUrl);
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
        publishBody(schemaRef, uploaderId, gameVersion, gameBuild, body);
    }

    /**
     * Surcharge typée : accepte un POJO généré (voir {@code be.mirooz.elitedangerous.eddn.generated})
     * qui sera sérialisé via Jackson. Garantit {@code additionalProperties: false} à la source
     * puisque les classes générées ne contiennent que les champs autorisés par le schéma.
     *
     * @param messagePojo POJO représentant le corps {@code message} (PAS l'enveloppe complète).
     */
    public void publish(String schemaRef,
                        String uploaderId,
                        String gameVersion,
                        String gameBuild,
                        Object messagePojo) {
        if (schemaRef == null || uploaderId == null || messagePojo == null) {
            return;
        }
        if (messagePojo instanceof JsonNode node) {
            publish(schemaRef, uploaderId, gameVersion, gameBuild, node);
            return;
        }
        JsonNode node = mapper.valueToTree(messagePojo);
        if (node == null || !node.isObject()) {
            return;
        }
        publishBody(schemaRef, uploaderId, gameVersion, gameBuild, (ObjectNode) node);
    }

    private void publishBody(String schemaRef,
                             String uploaderId,
                             String gameVersion,
                             String gameBuild,
                             ObjectNode body) {
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
                    .uri(URI.create(gatewayUrl))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Content-Encoding", "gzip")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(gzipped))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            String schemaRef = envelope.path("$schemaRef").asText("?");
            if (status == 200) {
                System.out.println("EDDN: envoyé " + schemaRef + " (" + endpointLabel + ", " + gzipped.length + " o gzip)");
                return;
            }
            System.err.println("EDDN: rejet " + status + " pour " + schemaRef + " (" + endpointLabel + ")"
                    + (response.body() == null ? "" : " : " + truncate(response.body(), 500)));
        } catch (IOException e) {
            System.err.println("EDDN: erreur réseau POST (" + endpointLabel + ") : " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Dérive un label concis ({@code live} / {@code beta} / {@code dev}) à partir de l'URL gateway.
     * Utilisé uniquement pour enrichir les logs d'envoi — pas de logique métier.
     */
    private static String resolveEndpointLabel(String url) {
        try {
            String host = URI.create(url).getHost();
            if (host == null) {
                return url;
            }
            if (host.startsWith("beta.")) {
                return "beta";
            }
            if (host.startsWith("dev.")) {
                return "dev";
            }
            if (host.equals("eddn.edcd.io")) {
                return "live";
            }
            return host;
        } catch (IllegalArgumentException e) {
            return url;
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
