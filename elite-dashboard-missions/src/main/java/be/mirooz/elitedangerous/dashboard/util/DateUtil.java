package be.mirooz.elitedangerous.dashboard.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtil {
    public static LocalDateTime parseTimestamp(String timestamp) {
        try {
            // Format: 2025-09-19T12:12:54Z
            return LocalDateTime.parse(timestamp.replace("Z", ""),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}
