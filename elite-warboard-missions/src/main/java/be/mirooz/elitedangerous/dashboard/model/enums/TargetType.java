package be.mirooz.elitedangerous.dashboard.model.enums;

import be.mirooz.elitedangerous.dashboard.service.LocalizationService;

public enum TargetType {
    PIRATE("target.pirate", "FactionTag_Pirate"),
    DESERTEUR("target.deserter", "FactionTag_Deserter"),
    HUMANOID("target.humanoid","FactionTag_AIHumanoid"),
    UNKNOWN("target.faction", "UNKNOWN");

    private final String localizationKey;
    private final String code;

    TargetType(String localizationKey, String code) {
        this.localizationKey = localizationKey;
        this.code = code;
    }

    public String getDisplayName() {
        return LocalizationService.getInstance().getString(localizationKey);
    }

    public String getCode() {
        return code;
    }

    public static TargetType fromCode(String input) {
        for (TargetType t : values()) {
            if (input == null ||input.isEmpty()) {
                return UNKNOWN;
            }
            if (input.contains(t.getCode())) {
                return t;
            }
        }
        System.out.println("[Error targetType] :" +input);
        return UNKNOWN;
    }
}
