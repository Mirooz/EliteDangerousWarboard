package be.mirooz.ardentapi.json;

import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.Mineral;
import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.MineralType;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

public class MineralDeserializer extends JsonDeserializer<Mineral> {

    @Override
    public Mineral deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();

        if (value == null || value.isBlank()) {
            return null;
        }

        // 1️⃣ priorité cargoJsonName (ex: alexandrite)
        return MineralType.fromCargoJsonName(value)
                // 2️⃣ fallback coreMineralName
                .or(() -> MineralType.fromCoreMineralName(value))
                // 3️⃣ fallback inaraName
                .or(() -> MineralType.fromInaraId(value))
                .orElse(null);
    }
}
