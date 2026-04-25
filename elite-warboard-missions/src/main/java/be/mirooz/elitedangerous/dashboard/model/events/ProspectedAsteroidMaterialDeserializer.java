package be.mirooz.elitedangerous.dashboard.model.events;

import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.MineralType;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

/**
 * Aligné sur l’ancien {@code MaterialRow#toRuntime} : {@code name} peut être un
 * {@link MineralType} ou une chaîne non référencée.
 */
public final class ProspectedAsteroidMaterialDeserializer extends JsonDeserializer<ProspectedAsteroid.Material> {

    @Override
    public ProspectedAsteroid.Material deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        ProspectedAsteroid.Material m = new ProspectedAsteroid.Material();

        String name = text(node, "name");
        if (name != null) {
            try {
                m.setMineral(MineralType.valueOf(name));
            } catch (IllegalArgumentException e) {
                m.setUnregisteredName(name);
            }
        }
        String unreg = text(node, "unregisteredName");
        if (unreg != null) {
            m.setUnregisteredName(unreg);
        }
        m.setNameLocalised(text(node, "nameLocalised"));
        if (node.has("proportion") && !node.get("proportion").isNull()) {
            m.setProportion(node.get("proportion").asDouble());
        }
        return m;
    }

    private static String text(JsonNode node, String field) {
        if (!node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asText();
    }
}
