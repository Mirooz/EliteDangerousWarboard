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
        if (value == null) {
            return null;
        }
        for (ScanTypeBio type : values()) {
            // comparaison case-insensitive sur label et sur name()
            if (type.label.equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown ScanType: " + value);
    }
}
