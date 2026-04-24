package be.mirooz.elitedangerous.eddn;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Construit l'enveloppe EDDN attendue par le gateway :
 * <pre>{ "$schemaRef": ..., "header": {...}, "message": {...} }</pre>
 */
public final class EddnEnvelope {

    private static final ObjectMapper MAPPER = new ObjectMapper();

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
