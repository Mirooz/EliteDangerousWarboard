package be.mirooz.elitedangerous.eddn;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * Construit l'enveloppe EDDN attendue par le gateway :
 * <pre>{ "$schemaRef": ..., "header": {...}, "message": {...} }</pre>
 */
public final class EddnEnvelope {

    /**
     * Mapper configuré pour EDDN :
     * <ul>
     *   <li>Sérialise {@link java.util.Date} et les types {@code java.time.*} en ISO-8601 UTC
     *       (non en timestamp numérique) — EDDN exige le format chaîne ISO.</li>
     *   <li>Ignore les valeurs {@code null} dans l'output (évite les champs vides rejetés par les schémas stricts).</li>
     * </ul>
     */
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .setDateFormat(isoUtcDateFormat())
            .setTimeZone(TimeZone.getTimeZone("UTC"));

    private static SimpleDateFormat isoUtcDateFormat() {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f;
    }

    private EddnEnvelope() {}

    /**
     * @param schemaRef       constante de {@link EddnSchemas}
     * @param uploaderId      hash stable du FID (non null/non vide)
     * @param softwareName    nom de l'application qui émet (ex. {@code "Elite Warboard"})
     * @param softwareVersion version de l'application émettrice
     * @param gameVersion     (optionnel) valeur de l'event {@code LoadGame}/{@code Fileheader}
     * @param gameBuild       (optionnel) idem
     * @param message         nœud contenant les champs du schéma (sans le wrapper "message")
     */
    public static ObjectNode build(String schemaRef,
                                   String uploaderId,
                                   String softwareName,
                                   String softwareVersion,
                                   String gameVersion,
                                   String gameBuild,
                                   JsonNode message) {
        ObjectNode envelope = MAPPER.createObjectNode();
        envelope.put("$schemaRef", schemaRef);

        ObjectNode header = MAPPER.createObjectNode();
        header.put("uploaderID", uploaderId);
        header.put("softwareName", softwareName);
        header.put("softwareVersion", softwareVersion);
        if (gameVersion != null && !gameVersion.isBlank()) {
            header.put("gameversion", gameVersion);
        }
        if (gameBuild != null && !gameBuild.isBlank()) {
            header.put("gamebuild", gameBuild);
        }
        envelope.set("header", header);
        envelope.set("message", message);
        return envelope;
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }
}
