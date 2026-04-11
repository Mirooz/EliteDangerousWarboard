package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.dashboard.model.registries.colonisation.ColonisationDockEntry;
import be.mirooz.elitedangerous.dashboard.model.registries.colonisation.ColonisationDocksRegistry;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Détecte les amarrages sur les sites de colonisation (journal Docked) et les enregistre.
 */
public class ColonisationDockService {

    public static final String COLONISATION_SHIP_STATION_MARKER = "$EXT_PANEL_ColonisationShip";

    private static final ColonisationDockService INSTANCE = new ColonisationDockService();

    private final ColonisationDocksRegistry registry = ColonisationDocksRegistry.getInstance();

    private ColonisationDockService() {
    }

    public static ColonisationDockService getInstance() {
        return INSTANCE;
    }

    /**
     * Si l'événement Docked concerne un site de colonisation, l'ajoute au registre.
     */
    public void handleDocked(JsonNode jsonNode) {
        if (jsonNode == null || !jsonNode.has("StationName")) {
            return;
        }
        String stationName = jsonNode.get("StationName").asText("");
        if (!stationName.contains(COLONISATION_SHIP_STATION_MARKER)) {
            return;
        }

        String timestamp = jsonNode.path("timestamp").asText("");
        String stationType = jsonNode.path("StationType").asText("");
        String starSystem = jsonNode.path("StarSystem").asText("");
        long systemAddress = jsonNode.path("SystemAddress").asLong();
        long marketId = jsonNode.path("MarketID").asLong();
        String faction = jsonNode.path("StationFaction").path("Name").asText("");
        double distLs = jsonNode.path("DistFromStarLS").asDouble(0);

        ColonisationDockEntry entry = new ColonisationDockEntry(
                timestamp,
                stationName,
                parseSiteDisplayName(stationName),
                stationType,
                starSystem,
                systemAddress,
                marketId,
                faction,
                distLs
        );
        registry.record(entry);
        System.out.println("Colonisation: amarrage sur « " + entry.getSiteNameLocalised() + " » (" + starSystem + ", MarketID=" + marketId + ")");
    }

    static String parseSiteDisplayName(String stationName) {
        int semi = stationName.indexOf(';');
        if (semi >= 0 && semi < stationName.length() - 1) {
            return stationName.substring(semi + 1).trim();
        }
        return stationName.trim();
    }
}
