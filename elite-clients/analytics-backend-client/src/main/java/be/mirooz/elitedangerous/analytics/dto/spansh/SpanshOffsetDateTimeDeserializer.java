package be.mirooz.elitedangerous.analytics.dto.spansh;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

/**
 * Désérialiseur personnalisé pour OffsetDateTime qui gère le format de date de l'API Spansh.
 * L'API Spansh retourne des dates au format "2021-10-19 15:08:58Z" (avec un espace)
 * au lieu du format ISO-8601 standard "2021-10-19T15:08:58Z" (avec un T).
 */
public class SpanshOffsetDateTimeDeserializer extends JsonDeserializer<OffsetDateTime> {

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
        
        // Remplacer l'espace par 'T' si nécessaire pour le format ISO-8601
        // Sinon, utiliser le format personnalisé
        try {
            // Essayer d'abord le format standard ISO-8601
            return OffsetDateTime.parse(dateString);
        } catch (Exception e) {
            // Si ça échoue, essayer avec le format Spansh (espace au lieu de T)
            String normalizedDate = dateString.replaceFirst(" ", "T");
            try {
                return OffsetDateTime.parse(normalizedDate);
            } catch (Exception e2) {
                // Si ça échoue encore, utiliser le formatter personnalisé
                return OffsetDateTime.parse(dateString, FORMATTER);
            }
        }
    }
}

