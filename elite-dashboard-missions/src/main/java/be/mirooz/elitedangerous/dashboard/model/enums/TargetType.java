package be.mirooz.elitedangerous.dashboard.model.enums;

public enum TargetType {
    PIRATE("Pirate", "FactionTag_Pirate"),
    DESERTEUR("Deserteur", "FactionTag_Deserter"),
    HUMANOID("Humain","FactionTag_AIHumanoid"),
    UNKNOWN("Faction", "UNKNOWN"); // valeur par défaut

    private final String displayName;
    private final String code;

    TargetType(String displayName, String code) {
        this.displayName = displayName;
        this.code = code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCode() {
        return code;
    }

    public static TargetType fromCode(String input) {
        for (TargetType t : values()) {
            if (input == null ||input.isEmpty())
                return UNKNOWN;
            if (input.contains(t.getCode())) {
                return t;
            }
        }
        return UNKNOWN;
    }
}
