package be.mirooz.elitedangerous.backend.spansh.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

/**
 * Deserializer pour les dates Spansh.
 * Gère le format "yyyy-MM-dd HH:mm:ssZ" en plus de l'ISO-8601 standard.
 */
public class SpanshDateTimeDeserializer extends JsonDeserializer<OffsetDateTime> {

    private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .appendOffsetId()
            .toFormatter();

    @Override
    public OffsetDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String dateString = p.getText();
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }

        try {
            return OffsetDateTime.parse(dateString);
        } catch (Exception e) {
            String normalizedDate = dateString.replaceFirst(" ", "T");
            try {
                return OffsetDateTime.parse(normalizedDate);
            } catch (Exception e2) {
                return OffsetDateTime.parse(dateString, FORMATTER);
            }
        }
    }
}
