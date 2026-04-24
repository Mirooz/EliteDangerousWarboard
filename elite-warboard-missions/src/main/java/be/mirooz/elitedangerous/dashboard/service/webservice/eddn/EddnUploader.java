package be.mirooz.elitedangerous.dashboard.service.webservice.eddn;

import be.mirooz.elitedangerous.dashboard.model.registries.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.service.PreferencesService;
import be.mirooz.elitedangerous.dashboard.view.common.context.DashboardContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Façade haut niveau pour publier sur EDDN.
 *
 * <p>Responsabilités :</p>
 * <ul>
 *   <li>gater les envois ({@link DashboardContext#isBatchLoading()}, toggle préférences,
 *       présence FID/gameversion) ;</li>
 *   <li>construire l'enveloppe ({@link EddnEnvelope}) et la retirer des champs personnels
 *       ({@link EddnPersonalDataStripper}) ;</li>
 *   <li>empiler dans une file bornée consommée par un unique worker daemon pour sérialiser
 *       les uploads et ne pas bloquer les threads appelants (UI / dispatch journal).</li>
 * </ul>
 */
public final class EddnUploader {

    private static final int QUEUE_CAPACITY = 1024;
    private static final long WORKER_POLL_TIMEOUT_MS = 1_000L;

    public static final String PREF_EDDN_ENABLED = "eddn.enabled";

    private static final EddnUploader INSTANCE = new EddnUploader();

    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();
    private final DashboardContext dashboardContext = DashboardContext.getInstance();
    private final PreferencesService preferences = PreferencesService.getInstance();

    private final BlockingQueue<ObjectNode> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
    private final Thread worker;
    private volatile boolean running = true;

    private EddnUploader() {
        this.worker = new Thread(this::workerLoop, "eddn-uploader");
        this.worker.setDaemon(true);
        this.worker.start();
    }

    public static EddnUploader getInstance() {
        return INSTANCE;
    }

    /**
     * Empile un message (corps du {@code message} EDDN, sans l'enveloppe) pour publication.
     * Peut être appelé depuis n'importe quel thread ; non bloquant.
     *
     * @param schemaRef une constante de {@link EddnSchemas}
     * @param message   payload strict du schéma (doit être un {@link ObjectNode} pour permettre le stripping)
     */
    public void publishMessage(String schemaRef, ObjectNode message) {
        if (schemaRef == null || message == null) {
            return;
        }
        if (!isPublishingAllowed()) {
            return;
        }
        String fid = commanderStatus.getFID();
        String uploaderId = EddnUploaderId.fromFid(fid);
        if (uploaderId == null) {
            return;
        }

        EddnPersonalDataStripper.stripInPlace(message);

        ObjectNode envelope = EddnEnvelope.build(
                schemaRef,
                uploaderId,
                commanderStatus.getGameVersion(),
                commanderStatus.getGameBuild(),
                message
        );

        if (!queue.offer(envelope)) {
            System.err.println("EDDN: file pleine, message " + schemaRef + " supprimé.");
        }
    }

    /**
     * Raccourci quand on dispose déjà d'un {@link JsonNode} quelconque (ex. nœud journal brut).
     * Un {@link ObjectNode} sera dupliqué, les autres types seront ignorés (EDDN attend un objet).
     */
    public void publishMessage(String schemaRef, JsonNode message) {
        if (message == null || !message.isObject()) {
            return;
        }
        publishMessage(schemaRef, ((ObjectNode) message.deepCopy()));
    }

    public boolean isEnabled() {
        String v = preferences.getPreference(PREF_EDDN_ENABLED, "true");
        return Boolean.parseBoolean(v);
    }

    public void setEnabled(boolean enabled) {
        preferences.setPreference(PREF_EDDN_ENABLED, Boolean.toString(enabled));
    }

    /**
     * Appelé au shutdown JVM (optionnel, le worker est daemon).
     */
    public void shutdown() {
        running = false;
        worker.interrupt();
    }

    /** Pour inspection / logs. */
    public int queueSize() {
        return queue.size();
    }

    private boolean isPublishingAllowed() {
        if (!isEnabled()) {
            return false;
        }
        if (dashboardContext.isBatchLoading()) {
            return false;
        }
        String fid = commanderStatus.getFID();
        if (fid == null || fid.isBlank()) {
            return false;
        }
        String gv = commanderStatus.getGameVersion();
        String gb = commanderStatus.getGameBuild();
        return gv != null && !gv.isBlank() && gb != null && !gb.isBlank();
    }

    private void workerLoop() {
        while (running) {
            try {
                ObjectNode envelope = queue.poll(WORKER_POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                if (envelope == null) {
                    continue;
                }
                EddnClient.getInstance().post(envelope);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                System.err.println("EDDN worker : " + e.getMessage());
            }
        }
    }
}
