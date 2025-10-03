package be.mirooz.elitedangerous.dashboard.model.enums;

public enum TargetType {
    PIRATE("Pirate", "$MissionUtil_FactionTag_Pirate;"),
    DESERTEUR("Deserteur", "$MissionUtil_FactionTag_Deserter;"),
    HUMANOID("Humain","$MissionUtil_FactionTag_AIHumanoid;"),
    UNKNOWN("Unknown", ""); // valeur par défaut

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
            if (t.getCode().equals(input)) {
                return t;
            }
        }
        return UNKNOWN;
    }
}
