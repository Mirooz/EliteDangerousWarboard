package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.backend.ardentbackend.ArdentBackendApiFacade;
import be.mirooz.elitedangerous.backend.generated.model.NearbyExportsBestStationResult;
import be.mirooz.elitedangerous.backend.generated.model.NearbyExportsCrosscheckRequest;
import be.mirooz.elitedangerous.backend.generated.model.NearbyExportsCrosscheckResponse;
import be.mirooz.elitedangerous.dashboard.model.colonisation.ColonisationArchitectSystem;
import be.mirooz.elitedangerous.dashboard.model.colonisation.ColonisationConstruction;
import be.mirooz.elitedangerous.dashboard.model.colonisation.ColonisationDockEntry;
import be.mirooz.elitedangerous.dashboard.model.colonisation.ConstructionResource;
import be.mirooz.elitedangerous.dashboard.model.colonisation.ConstructionResourceRemaining;
import be.mirooz.elitedangerous.dashboard.model.registries.colonisation.ColonisationRegistry;
import be.mirooz.elitedangerous.dashboard.model.registries.commander.CommanderStatus;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
/**
 * Point d’entrée applicatif pour la colonisation (dock, chantiers) : délègue au {@link ColonisationRegistry}.
 * Les handlers journal doivent passer par ce service, pas par le registre directement.
 */
public class ColonisationService {

    /** Service présent sur les stations / dépôts liés à la colonisation (journal Docked). */
    public static final String COLONISATION_STATION_SERVICE = "colonisationcontribution";
    private static final Pattern CONSTRUCTION_SITE_PREFIX_PATTERN = Pattern.compile(
            "^(orbital|planetary)\\s+construction\\s+site\\s*:\\s*",
            Pattern.CASE_INSENSITIVE);

    private static final ColonisationService INSTANCE = new ColonisationService();

    private final ColonisationRegistry registry = ColonisationRegistry.getInstance();
    private final ArdentBackendApiFacade ardentBackend = ArdentBackendApiFacade.getInstance();
    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();
    private final PreferencesService preferencesService = PreferencesService.getInstance();
    private final Set<Long> persistedConstructionListMarketIds = new LinkedHashSet<>();
    private String persistedSelectedArchitectSystem = "";
    private boolean persistedUiStateLoadedAfterBatch;

    private ColonisationService() {
    }

    public static ColonisationService getInstance() {
        return INSTANCE;
    }

    public synchronized void loadPersistedUiStateAfterJournalBatch() {
        persistedConstructionListMarketIds.clear();
        persistedConstructionListMarketIds.addAll(preferencesService.loadColonisationConstructionListMarketIds());
        String persisted = preferencesService.loadColonisationConstructionListSelectedArchitectSystem();
        persistedSelectedArchitectSystem = persisted != null ? persisted : "";
        persistedUiStateLoadedAfterBatch = true;
    }

    public synchronized boolean isPersistedUiStateLoadedAfterBatch() {
        return persistedUiStateLoadedAfterBatch;
    }

    public synchronized Set<Long> getPersistedConstructionListMarketIds() {
        return Set.copyOf(persistedConstructionListMarketIds);
    }

    public synchronized String getPersistedSelectedArchitectSystem() {
        return persistedSelectedArchitectSystem;
    }

    public void recordArchitectBeaconDeployed(String starSystem) {
        registry.recordArchitectBeaconDeployed(starSystem);
    }

    public void applyConstructionDepot(long marketId,
                                       ColonisationConstruction construction,
                                       String starSystem,
                                       Long bodyId) {
        registry.applyConstructionDepot(marketId, construction, starSystem, bodyId);
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

    /** Sélectionne le chantier courant par {@code MarketID} dans le registre. */
    public void setCurrentConstructionByMarketId(long marketId) {
        registry.setCurrentConstructionByMarketId(marketId);
    }

    /** Définit le site de construction courant (même effet que {@link #setCurrentConstructionByMarketId}). */
    public void designateBuildingSite(long marketId) {
        setCurrentConstructionByMarketId(marketId);
    }

    public ColonisationConstruction getCurrentConstruction() {
        return registry.getCurrentConstruction();
    }

    /** Ressources du chantier courant avec quantités encore à livrer. */
    public List<ConstructionResourceRemaining> getActualConstruction() {
        return registry.getActualConstruction();
    }

    public ColonisationDockEntry getCurrentConstructionSite() {
        return registry.getCurrentConstructionSite();
    }

    /**
     * Appelle {@link ArdentBackendApiFacade#suggestBuyStations} pour les commodités encore à livrer sur le chantier courant
     * (système = celui du site courant, ou système commandant en secours).
     *
     * @return stations suggérées (liste vide si pas de chantier courant, pas de système, ou rien à acheter)
     */
    public List<NearbyExportsBestStationResult> suggestBuyStationsForCurrentConstruction() throws IOException {
        return suggestBuyStationsForCurrentConstruction(false);
    }

    public List<NearbyExportsBestStationResult> suggestBuyStationsForCurrentConstruction(boolean avoidPlanetaryLanding)
            throws IOException {
        ColonisationDockEntry site = registry.getCurrentConstructionSite();
        ColonisationConstruction construction = registry.getCurrentConstruction();
        return suggestBuyStationsInternal(site, construction, avoidPlanetaryLanding);
    }

    /**
     * Suggestions d’achat pour le chantier du site donné (système et commodités = ce site).
     */
    public List<NearbyExportsBestStationResult> suggestBuyStationsForDock(ColonisationDockEntry site)
            throws IOException {
        return suggestBuyStationsForDock(site, false);
    }

    public List<NearbyExportsBestStationResult> suggestBuyStationsForDock(ColonisationDockEntry site,
            boolean avoidPlanetaryLanding) throws IOException {
        if (site == null) {
            return List.of();
        }
        return suggestBuyStationsInternal(site, site.getConstruction(), avoidPlanetaryLanding);
    }

    /**
     * Indique si la clé renvoyée par l’API pour une ligne de match correspond à cette ressource chantier.
     */
    public boolean resourceMatchesNearbyBuyRequest(ConstructionResource resource, String requestedCommodityName) {
        if (requestedCommodityName == null || requestedCommodityName.isBlank()) {
            return false;
        }
        String k = commodityNameForNearbyBuy(resource);
        return !k.isBlank() && k.equalsIgnoreCase(requestedCommodityName.trim());
    }

    private List<NearbyExportsBestStationResult> suggestBuyStationsInternal(
            ColonisationDockEntry site,
            ColonisationConstruction construction,
            boolean avoidPlanetaryLanding) throws IOException {
        String systemName = resolveConstructionSourceSystem(site);
        if (systemName.isBlank()) {
            return List.of();
        }
        if (construction == null || construction.getResourcesRequired() == null) {
            return List.of();
        }
        List<String> commodityNames = new ArrayList<>();
        for (ConstructionResource r : construction.getResourcesRequired()) {
            if (r.getProvidedAmount() >= r.getRequiredAmount()) {
                continue;
            }
            String name = commodityNameForNearbyBuy(r);
            if (!name.isBlank()) {
                commodityNames.add(name);
            }
        }
        if (commodityNames.isEmpty()) {
            return List.of();
        }
        NearbyExportsCrosscheckRequest request = new NearbyExportsCrosscheckRequest()
                .systemName(systemName)
                .commodityNames(commodityNames)
                .avoidPlanetaryLanding(avoidPlanetaryLanding);
        NearbyExportsCrosscheckResponse response = ardentBackend.suggestBuyStations(request);
        if (response.getBestStations() == null) {
            return List.of();
        }
        return List.copyOf(response.getBestStations());
    }

    private String resolveConstructionSourceSystem(ColonisationDockEntry site) {
        if (site != null && site.getStarSystem() != null && !site.getStarSystem().isBlank()) {
            return site.getStarSystem().trim();
        }
        String current = commanderStatus.getCurrentStarSystem();
        return current != null ? current.trim() : "";
    }

    private static String commodityNameForNearbyBuy(ConstructionResource r) {
        if (r == null || r.getCommodity() == null) {
            return "";
        }
        return r.getCommodity().getCargoJsonName();
    }

    public void clear() {
        registry.clear();
        preferencesService.removeColonisationSuggestedBuyStationsFile();
        synchronized (this) {
            persistedConstructionListMarketIds.clear();
            persistedSelectedArchitectSystem = "";
            persistedUiStateLoadedAfterBatch = false;
        }
    }

    /**
     * Si l'événement Docked concerne un site de colonisation, ajoute une entrée au registre seulement si le MarketID n’y est pas encore.
     *
     * @return {@code true} si une nouvelle entrée dock colonisation a été enregistrée
     */
    public boolean handleDocked(JsonNode jsonNode) {
        if (jsonNode == null || !hasColonisationStationService(jsonNode)) {
            return false;
        }
        if (!jsonNode.has("StationName")) {
            return false;
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
            return true;
        }
        return false;
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
            return stripConstructionSitePrefix(localised);
        }
        return stripConstructionSitePrefix(stripOptionalPanelPrefix(stationName));
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

    static String stripConstructionSitePrefix(String siteName) {
        if (siteName == null) {
            return "";
        }
        String cleaned = CONSTRUCTION_SITE_PREFIX_PATTERN.matcher(siteName.trim()).replaceFirst("");
        return cleaned.trim();
    }
}
