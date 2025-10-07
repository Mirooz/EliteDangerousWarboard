package be.mirooz.elitedangerous.dashboard.model.enums;

import java.util.Arrays;
import java.util.List;

public enum MissionType {
    ASSASSINATION("Assassinat", List.of("assassination", "assassinat")),
    BOUNTY_HUNTING("Chasse aux primes", List.of("bounty", "prime")),
    COURIER("Transport de courrier", List.of("courier", "courrier")),
    DELIVERY("Livraison", List.of("delivery", "livraison")),
    MINING("Mining", List.of("mining")),
    PASSENGER("Transport de passagers", List.of("passenger", "passager")),
    SALVAGE("Récupération", List.of("salvage", "récupération")),
    SCAN("Scan", List.of("scan")),
    SMUGGLING("Contrebande", List.of("smuggling", "contrebande")),
    TRADING("Commerce", List.of("trading", "commerce")),
    EXPLORATION("Exploration", List.of("exploration")),
    CONFLIT("Conflict",List.of("conflict","civilwar")),

    MASSACRE_ONFOOT("Massacre (foot)", List.of("onfoot_massacre")),
    ALTRUISM("Altruism",List.of("altruism")),
    MASSACRE("Massacre", List.of("massacre", "kill")),
    COMBAT("Combat", List.of()), // fallback
    RESCUE("Sauvetage", List.of("rescue", "sauvetage")),
    DATA("Données", List.of("data", "données"));

    // (⚠ tu peux ajouter tes SETTLEMENT_* si besoin avec leurs mots-clés)

    private final String displayName;
    private final List<String> keywords;

    MissionType(String displayName, List<String> keywords) {
        this.displayName = displayName;
        this.keywords = keywords;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    /**
     * Détermine le type de mission à partir du nom
     */
    public static MissionType fromName(String missionName) {
        if (missionName == null) {
            return COMBAT;
        }
        String lowerName = missionName.toLowerCase();

        return Arrays.stream(values())
                .filter(type -> type.keywords.stream().anyMatch(lowerName::contains))
                .findFirst()
                .orElse(COMBAT);
    }
}
