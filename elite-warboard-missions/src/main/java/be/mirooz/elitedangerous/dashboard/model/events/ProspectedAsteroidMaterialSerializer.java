package be.mirooz.elitedangerous.dashboard.model.events;

import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.MineralType;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * Même forme que l’ancien {@code MaterialRow#fromRuntime} pour le JSON sur disque.
 */
public final class ProspectedAsteroidMaterialSerializer extends JsonSerializer<ProspectedAsteroid.Material> {

    @Override
    public void serialize(ProspectedAsteroid.Material value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        gen.writeStartObject();
        if (value.getMineral() != null) {
            MineralType mt = value.getMineral();
            gen.writeStringField("name", mt.name());
        } else if (value.getUnregisteredName() != null) {
            gen.writeStringField("name", value.getUnregisteredName());
        }
        if (value.getUnregisteredName() != null) {
            gen.writeStringField("unregisteredName", value.getUnregisteredName());
        }
        if (value.getNameLocalised() != null) {
            gen.writeStringField("nameLocalised", value.getNameLocalised());
        }
        if (value.getProportion() != null) {
            gen.writeNumberField("proportion", value.getProportion());
        }
        gen.writeEndObject();
    }
}
