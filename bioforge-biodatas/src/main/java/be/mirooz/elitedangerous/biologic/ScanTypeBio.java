package be.mirooz.elitedangerous.biologic;

import lombok.Getter;

@Getter
public enum ScanTypeBio {
    LOG("Log"),
    SAMPLE("Sample"),
    ANALYSE("Analyse");

    private final String label;

    ScanTypeBio(String label) {
        this.label = label;
    }

    public static ScanTypeBio fromString(String value) {
        ScanTypeBio parsed = fromStringSafe(value);
        if (parsed == null) {
            throw new IllegalArgumentException("Unknown ScanType: " + value);
        }
        return parsed;
    }

    /**
     * Parse le ScanType du journal ScanOrganic (Log, Sample, Analyse, etc.) sans lever d’exception.
     */
    public static ScanTypeBio fromStringSafe(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.trim();
        // Variantes anglaises / jeux selon version ou locale
        if ("analyze".equalsIgnoreCase(v) || "analysis".equalsIgnoreCase(v)) {
            return ANALYSE;
        }
        for (ScanTypeBio type : values()) {
            if (type.label.equalsIgnoreCase(v) || type.name().equalsIgnoreCase(v)) {
                return type;
            }
        }
        return null;
    }
}
