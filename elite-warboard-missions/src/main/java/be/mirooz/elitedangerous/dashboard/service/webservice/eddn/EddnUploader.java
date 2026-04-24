package be.mirooz.elitedangerous.dashboard.service.webservice.eddn;

import be.mirooz.elitedangerous.dashboard.model.registries.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.service.PreferencesService;
import be.mirooz.elitedangerous.dashboard.view.common.context.DashboardContext;
import be.mirooz.elitedangerous.eddn.EddnClient;
import be.mirooz.elitedangerous.eddn.EddnUploaderId;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Façade warboard pour publier sur EDDN.
 *
 * <p>Singleton léger qui, à chaque appel :</p>
 * <ol>
 *   <li>gate sur {@link DashboardContext#isBatchLoading()} (pas d'envoi pendant le replay journal),</li>
 *   <li>gate sur la préférence {@code eddn.enabled} (toggle utilisateur, défaut on),</li>
 *   <li>récupère FID, gameversion/gamebuild sur {@link CommanderStatus},</li>
 *   <li>délègue à l'{@link EddnClient} du module {@code elite-eddn-client} (queue, strip, enveloppe, POST gzip).</li>
 * </ol>
 */
public final class EddnUploader {

    public static final String PREF_EDDN_ENABLED = "eddn.enabled";

    private static final EddnUploader INSTANCE = new EddnUploader();

    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();
    private final DashboardContext dashboardContext = DashboardContext.getInstance();
    private final PreferencesService preferences = PreferencesService.getInstance();

    private final EddnClient client = new EddnClient(EddnAppInfo.SOFTWARE_NAME, EddnAppInfo.version());

    private EddnUploader() {}

    public static EddnUploader getInstance() {
        return INSTANCE;
    }

    /**
     * Publie un message correspondant à {@code schemaRef} (constante de {@code EddnSchemas}).
     * Non bloquant. Droppé silencieusement si les gates ne sont pas satisfaits.
     */
    public void publishMessage(String schemaRef, ObjectNode message) {
        if (schemaRef == null || message == null) {
            return;
        }
        if (!isPublishingAllowed()) {
            return;
        }
        String uploaderId = EddnUploaderId.fromFid(commanderStatus.getFID());
        if (uploaderId == null) {
            return;
        }
        client.publish(
                schemaRef,
                uploaderId,
                commanderStatus.getGameVersion(),
                commanderStatus.getGameBuild(),
                message
        );
    }

    /**
     * Raccourci quand on dispose déjà d'un {@link JsonNode} quelconque. Les types autres que
     * {@link ObjectNode} sont ignorés (EDDN attend un objet).
     */
    public void publishMessage(String schemaRef, JsonNode message) {
        if (message == null || !message.isObject()) {
            return;
        }
        publishMessage(schemaRef, (ObjectNode) message);
    }

    /**
     * Publie un POJO typé généré à partir des JSON Schema EDDN
     * (cf. {@code be.mirooz.elitedangerous.eddn.generated.*}). Le POJO est sérialisé via Jackson
     * avant strip / envoi. Préférer cette surcharge aux {@link ObjectNode} bruts : les POJOs garantissent
     * {@code additionalProperties: false} à la compilation (impossible d'envoyer un champ non autorisé
     * par le schéma).
     */
    public void publishMessage(String schemaRef, Object messagePojo) {
        if (schemaRef == null || messagePojo == null) {
            return;
        }
        if (messagePojo instanceof JsonNode node) {
            publishMessage(schemaRef, node);
            return;
        }
        if (!isPublishingAllowed()) {
            return;
        }
        String uploaderId = EddnUploaderId.fromFid(commanderStatus.getFID());
        if (uploaderId == null) {
            return;
        }
        client.publish(
                schemaRef,
                uploaderId,
                commanderStatus.getGameVersion(),
                commanderStatus.getGameBuild(),
                messagePojo
        );
    }

    public boolean isEnabled() {
        String v = preferences.getPreference(PREF_EDDN_ENABLED, "true");
        return Boolean.parseBoolean(v);
    }

    public void setEnabled(boolean enabled) {
        preferences.setPreference(PREF_EDDN_ENABLED, Boolean.toString(enabled));
    }

    public int queueSize() {
        return client.queueSize();
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
}
