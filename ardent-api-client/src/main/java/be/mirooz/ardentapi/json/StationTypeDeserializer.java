package be.mirooz.ardentapi.json;

import be.mirooz.ardentapi.model.StationType;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

public class StationTypeDeserializer extends JsonDeserializer<StationType> {

    @Override
    public StationType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();

        if (value == null || value.isBlank()) {
            return null;
        }

        return switch (value) {
            case "FleetCarrier" -> StationType.FLEET;
            case "Coriolis","Orbis", "Ocellus" -> StationType.CORIOLIS;
            case "Outpost","Starport", "Port" -> StationType.PORT;
            default -> StationType.PORTPLANET; // safe fallback
        };
    }
}
