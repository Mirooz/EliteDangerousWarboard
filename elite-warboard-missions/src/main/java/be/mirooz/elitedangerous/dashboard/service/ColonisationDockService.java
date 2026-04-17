package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.dashboard.model.colonisation.ColonisationDockEntry;
import be.mirooz.elitedangerous.dashboard.model.registries.colonisation.ColonisationDocksRegistry;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Détecte les amarrages sur les sites de colonisation (journal Docked) et les enregistre.
 * Critère : le tableau {@code StationServices} contient la valeur {@value #COLONISATION_STATION_SERVICE}.
 */
public class ColonisationDockService {

    /** Service présent sur les stations / dépôts liés à la colonisation (journal Docked). */
    public static final String COLONISATION_STATION_SERVICE = "colonisationcontribution";

    private static final ColonisationDockService INSTANCE = new ColonisationDockService();

    private final ColonisationDocksRegistry registry = ColonisationDocksRegistry.getInstance();

    private ColonisationDockService() {
    }

    public static ColonisationDockService getInstance() {
        return INSTANCE;
    }

    /**
     * Si l'événement Docked concerne un site de colonisation, ajoute une entrée au registre seulement si le MarketID n’y est pas encore.
     */
    public void handleDocked(JsonNode jsonNode) {
        if (jsonNode == null || !hasColonisationStationService(jsonNode)) {
            return;
        }
        if (!jsonNode.has("StationName")) {
            return;
        }

        String stationName = jsonNode.path("StationName").asText("");

        ColonisationDockEntry snap = new ColonisationDockEntry();
        snap.setMarketId(jsonNode.path("MarketID").asLong());
        snap.setDockTimestamp(jsonNode.path("timestamp").asText(""));
        snap.setStationNameRaw(stationName);
        snap.setSiteNameLocalised(resolveSiteDisplayName(jsonNode, stationName));
        snap.setStationType(jsonNode.path("StationType").asText(""));
        snap.setStarSystem(jsonNode.path("StarSystem").asText(""));
        snap.setSystemAddress(jsonNode.path("SystemAddress").asLong());
        snap.setStationFactionName(jsonNode.path("StationFaction").path("Name").asText(""));
        snap.setDistFromStarLs(jsonNode.path("DistFromStarLS").asDouble(0));

        if (registry.addDockIfAbsent(snap)) {
            System.out.println("Colonisation: amarrage sur « " + snap.getSiteNameLocalised() + " » ("
                    + snap.getStarSystem() + ", MarketID=" + snap.getMarketId() + ")");
        }
    }

    public static boolean hasColonisationStationService(JsonNode dockedEvent) {
        if (dockedEvent == null) {
            return false;
        }
        JsonNode services = dockedEvent.path("StationServices");
        if (!services.isArray()) {
            return false;
        }
        for (JsonNode s : services) {
            if (COLONISATION_STATION_SERVICE.equals(s.asText())) {
                return true;
            }
        }
        return false;
    }

    private static String resolveSiteDisplayName(JsonNode jsonNode, String stationName) {
        String localised = jsonNode.path("StationName_Localised").asText("").trim();
        if (!localised.isEmpty()) {
            return localised;
        }
        return stripOptionalPanelPrefix(stationName);
    }

    /** Ex. {@code $EXT_PANEL_...; Nom affiché} → partie après {@code ;} si présente. */
    static String stripOptionalPanelPrefix(String stationName) {
        if (stationName == null) {
            return "";
        }
        int semi = stationName.indexOf(';');
        if (semi >= 0 && semi < stationName.length() - 1) {
            return stationName.substring(semi + 1).trim();
        }
        return stationName.trim();
    }
}
