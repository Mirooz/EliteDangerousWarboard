package be.mirooz.elitedangerous.dashboard.model;

/**
 * Types de missions disponibles dans Elite Dangerous
 */
public enum MissionType {
    ASSASSINATION("Assassinat"),
    BOUNTY_HUNTING("Chasse aux primes"),
    COURIER("Transport de courrier"),
    DELIVERY("Livraison"),
    MINING("Mining"),
    PASSENGER("Transport de passagers"),
    SALVAGE("Récupération"),
    SCAN("Scan"),
    SMUGGLING("Contrebande"),
    TRADING("Commerce"),
    EXPLORATION("Exploration"),
    COMBAT("Combat"),
    RESCUE("Sauvetage"),
    DATA("Données"),
    MASSACRE("Massacre"),
    PIRATE_MASSACRE("Massacre de pirates"),
    SETTLEMENT_RAID("Raid de colonie"),
    SETTLEMENT_DEFENCE("Défense de colonie"),
    SETTLEMENT_REACTIVATE("Réactivation de colonie"),
    SETTLEMENT_SALVAGE("Récupération de colonie"),
    SETTLEMENT_SCAN("Scan de colonie"),
    SETTLEMENT_ASSAULT("Assaut de colonie"),
    SETTLEMENT_ASSAULT_GOLIATH("Assaut Goliath de colonie"),
    SETTLEMENT_ASSAULT_RAID("Raid d'assaut de colonie"),
    SETTLEMENT_ASSAULT_DEFENCE("Défense d'assaut de colonie"),
    SETTLEMENT_ASSAULT_REACTIVATE("Réactivation d'assaut de colonie"),
    SETTLEMENT_ASSAULT_SALVAGE("Récupération d'assaut de colonie"),
    SETTLEMENT_ASSAULT_SCAN("Scan d'assaut de colonie"),
    SETTLEMENT_ASSAULT_ASSAULT("Assaut d'assaut de colonie"),
    SETTLEMENT_ASSAULT_ASSAULT_GOLIATH("Assaut Goliath d'assaut de colonie"),
    SETTLEMENT_ASSAULT_ASSAULT_RAID("Raid d'assaut d'assaut de colonie"),
    SETTLEMENT_ASSAULT_ASSAULT_DEFENCE("Défense d'assaut d'assaut de colonie"),
    SETTLEMENT_ASSAULT_ASSAULT_REACTIVATE("Réactivation d'assaut d'assaut de colonie"),
    SETTLEMENT_ASSAULT_ASSAULT_SALVAGE("Récupération d'assaut d'assaut de colonie"),
    SETTLEMENT_ASSAULT_ASSAULT_SCAN("Scan d'assaut d'assaut de colonie");

    private final String displayName;

    MissionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
