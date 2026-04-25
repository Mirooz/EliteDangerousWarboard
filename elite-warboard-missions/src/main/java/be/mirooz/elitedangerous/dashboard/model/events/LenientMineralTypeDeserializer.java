package be.mirooz.elitedangerous.dashboard.model.events;

import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.MineralType;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 * Valeur d'enum inconnue (patch ED, renommage) → {@code null} au lieu d'échouer le load.
 */
public final class LenientMineralTypeDeserializer extends JsonDeserializer<MineralType> {

    @Override
    public MineralType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String s = p.getValueAsString();
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return MineralType.valueOf(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
