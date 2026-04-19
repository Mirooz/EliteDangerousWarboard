package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.dashboard.model.colonisation.ColonisationArchitectSystem;
import be.mirooz.elitedangerous.dashboard.model.colonisation.ColonisationConstruction;
import be.mirooz.elitedangerous.dashboard.model.colonisation.ColonisationDockEntry;
import be.mirooz.elitedangerous.dashboard.model.colonisation.ConstructionResourceRemaining;
import be.mirooz.elitedangerous.dashboard.model.registries.colonisation.ColonisationRegistry;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Point d’entrée applicatif pour la colonisation (dock, chantiers) : délègue au {@link ColonisationRegistry}.
 * Les handlers journal doivent passer par ce service, pas par le registre directement.
 */
public class ColonisationService {

    /** Service présent sur les stations / dépôts liés à la colonisation (journal Docked). */
    public static final String COLONISATION_STATION_SERVICE = "colonisationcontribution";

    private static final ColonisationService INSTANCE = new ColonisationService();

    private final ColonisationRegistry registry = ColonisationRegistry.getInstance();

    private ColonisationService() {
    }

    public static ColonisationService getInstance() {
        return INSTANCE;
    }

    public void recordArchitectBeaconDeployed(String starSystem) {
        registry.recordArchitectBeaconDeployed(starSystem);
    }

    public void applyConstructionDepot(long marketId, ColonisationConstruction construction, String starSystem) {
        registry.applyConstructionDepot(marketId, construction, starSystem);
    }

    public List<String> getArchitectStarSystems() {
        return registry.getArchitectStarSystems();
    }

    public List<ColonisationArchitectSystem> getArchitectSystems() {
        return registry.getArchitectSystems();
    }

    public List<ColonisationDockEntry> getDockEntries() {
        return registry.getDockEntries();
    }

    /**
     * Sélectionne le chantier courant par {@code MarketID} (référence partagée avec le registre).
     */
    public void setCurrentConstructionByMarketId(long marketId) {
        registry.setCurrentConstructionByMarketId(marketId);
    }

    public ColonisationConstruction getCurrentConstruction() {
        return registry.getCurrentConstruction();
    }

    /** Ressources du chantier courant avec quantités encore à livrer. */
    public List<ConstructionResourceRemaining> getActualConstruction() {
        return registry.getActualConstruction();
    }

    public void clear() {
        registry.clear();
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
