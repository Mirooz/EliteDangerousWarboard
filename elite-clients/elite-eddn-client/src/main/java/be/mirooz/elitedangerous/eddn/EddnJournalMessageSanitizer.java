package be.mirooz.elitedangerous.eddn;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Nettoyage du corps {@code message} pour le schéma EDDN {@code journal/1}.
 *
 * <p>À l’init de la classe, on lit une seule fois {@code /eddn-schemas/journal.json} et on retient les noms
 * des propriétés racines de {@code message} dont la définition est {@code "$ref": "#/definitions/disallowed"}.
 * {@link #prepareJournalV1MessageInPlace} ne fait que {@code remove} sur ces clés — pas de boucle de
 * validation, pas d’heuristique sur les messages d’erreur.</p>
 *
 * <p>On retire aussi {@code Factions} si le tableau est vide ou ne contient que des objets vides (qualité
 * d’envoi EDDN, hors expression simple {@code disallowed} dans le schéma).</p>
 *
 * <p>{@link EddnPersonalDataStripper} continue de s’appliquer à <i>tous</i> les messages avant envoi
 * (PII, {@code *_Localised}, …).</p>
 */
public final class EddnJournalMessageSanitizer {

    private static final String SCHEMA_RESOURCE = "/eddn-schemas/journal.json";

    private static final Set<String> JOURNAL_V1_ROOT_DISALLOWED = loadJournalV1RootDisallowedFromBundledSchema();

    private EddnJournalMessageSanitizer() {}

    private static Set<String> loadJournalV1RootDisallowedFromBundledSchema() {
        ObjectMapper mapper = EddnEnvelope.mapper();
        try (InputStream in = EddnJournalMessageSanitizer.class.getResourceAsStream(SCHEMA_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Ressource classpath absente : " + SCHEMA_RESOURCE);
            }
            JsonNode root = mapper.readTree(in);
            JsonNode msgProps = root.path("properties").path("message").path("properties");
            if (!msgProps.isObject()) {
                throw new IllegalStateException(
                        "Schéma journal.json : chemin properties.message.properties absent ou invalide");
            }
            LinkedHashSet<String> keys = new LinkedHashSet<>();
            msgProps.fields().forEachRemaining(e -> {
                JsonNode def = e.getValue();
                if (def != null && def.isObject()
                        && "#/definitions/disallowed".equals(def.path("$ref").asText())) {
                    keys.add(e.getKey());
                }
            });
            if (keys.isEmpty()) {
                throw new IllegalStateException(
                        "Schéma journal.json : aucune propriété racine $ref disallowed trouvée (fichier corrompu ?)");
            }
            return Collections.unmodifiableSet(keys);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /** Exposé pour tests : clés racine interdites dérivées du schéma bundlé. */
    public static Set<String> journalV1RootDisallowedKeys() {
        return JOURNAL_V1_ROOT_DISALLOWED;
    }

    /**
     * Retire les propriétés racine listées comme {@code disallowed} dans le schéma journal/1, puis
     * {@code Factions} inutile si besoin.
     */
    public static void prepareJournalV1MessageInPlace(ObjectNode message) {
        if (message == null) {
            return;
        }
        for (String key : JOURNAL_V1_ROOT_DISALLOWED) {
            message.remove(key);
        }
        removeFactionsIfNoUsefulData(message);
    }

    private static void removeFactionsIfNoUsefulData(ObjectNode msg) {
        JsonNode factions = msg.get("Factions");
        if (factions == null || factions.isNull() || !factions.isArray()) {
            return;
        }
        if (factions.isEmpty()) {
            msg.remove("Factions");
            return;
        }
        for (JsonNode item : factions) {
            if (item != null && item.isObject() && item.size() > 0) {
                return;
            }
        }
        msg.remove("Factions");
    }
}
