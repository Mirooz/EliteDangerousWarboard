package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.ardentapi.client.ArdentApiClient;
import be.mirooz.ardentapi.model.CommodityMaxSell;
import be.mirooz.ardentapi.model.CommoditiesStats;
import be.mirooz.ardentapi.model.StationMarket;
import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.Mineral;
import be.mirooz.elitedangerous.dashboard.service.listeners.MineralPriceNotificationService;
import be.mirooz.elitedangerous.lib.edtools.model.MiningHotspot;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ArdentApiService {

    private static volatile ArdentApiService instance;

    private final ArdentApiClient client;
    private ArdentApiService() {
        this.client = new ArdentApiClient();
    }
    // Navigation entre les résultats de recherche
    private List<CommoditiesStats> currentSearchResults = new ArrayList<>();
    private int currentResultIndex = -1;
    // Navigation entre les hotspots
    private List<MiningHotspot> currentHotspots = new ArrayList<>();
    private int currentHotspotIndex = -1;
    private StationMarket currentStationMarket;
    private String currentStationMarketName;
    private String currentStationMarketSystem;

    public static ArdentApiService getInstance() {
        if (instance == null) {
            synchronized (ArdentApiService.class) {
                if (instance == null) {
                    instance = new ArdentApiService();
                }
            }
        }
        return instance;
    }
    private final Cache<String, List<CommoditiesStats>> minerMarketCache = Caffeine.newBuilder()
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();
    private final Cache<String, StationMarket> stationMarketCache = Caffeine.newBuilder()
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .maximumSize(100)
            .build();
    private final MineralPriceNotificationService priceNotificationService = MineralPriceNotificationService.getInstance();
    public List<CommodityMaxSell> fetchCommoditiesMaxSell() throws IOException {
        return client.fetchCommoditiesMaxSell();
    }

    public List<CommoditiesStats> fetchMinerMarket(
            Mineral mineral,
            String sourceSystem,
            int maxDistance,
            int minDemand,
            boolean largePad,
            boolean includeFleetCarrier) {

        String cacheKey = buildCacheKey(mineral.getCargoJsonName(), sourceSystem, maxDistance, minDemand, largePad, includeFleetCarrier);

        // Vérifier le cache
        List<CommoditiesStats> cachedResults = minerMarketCache.getIfPresent(cacheKey);
        if (cachedResults != null) {
            System.out.printf("🎯 Résultats trouvés en cache pour %s: %d stations%n", mineral.getVisibleName(), cachedResults.size());
            return cachedResults;
        }

        try {
            List<CommoditiesStats> commodities = fetchMinerMarketCache(mineral, sourceSystem, maxDistance, minDemand, largePad, includeFleetCarrier);

            if (commodities == null || commodities.isEmpty()) {
                return new ArrayList<>();
            }

            // Filtrer les résultats selon les critères
            List<CommoditiesStats> filteredResults = commodities.stream()
                    .filter(c -> c.getSystemDistance() <= maxDistance)
                    .filter(c -> {
                        int demand = c.getDemand();
                        return c.isFleetCarrier() ? demand >= minDemand : demand >= minDemand * 4;
                    })
                    .sorted(Comparator.comparingDouble(CommoditiesStats::getPrice).reversed()) // Trier par prix décroissant
                    .collect(Collectors.toList());

            // Mettre en cache
            minerMarketCache.put(cacheKey, filteredResults);
            System.out.printf("💾 %d résultats mis en cache pour %s%n", filteredResults.size(), mineral.getVisibleName());

            // Notifier les changements de prix pour le meilleur résultat
            if (!filteredResults.isEmpty()) {
                CommoditiesStats bestResult = filteredResults.get(0);
                long oldPrice = mineral.getPrice();
                mineral.setPrice(bestResult.getPrice());
                if (oldPrice != bestResult.getPrice()) {
                    priceNotificationService.notifyPriceChanged();
                }
            }

            return filteredResults;
        } catch (Exception e) {
            System.err.println("❌ Error while fetching miner market for " + mineral.getInaraName() + ": " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private List<CommoditiesStats> fetchMinerMarketCache(Mineral mineral, String sourceSystem, int maxDistance, int minDemand, boolean largePad, boolean includeFleetCarrier) throws IOException {
        return client.fetchMinerMarket(
                mineral, sourceSystem, maxDistance, minDemand, largePad, includeFleetCarrier);

    }
    private String buildCacheKey(String name, String sourceSystem, int distance, int supplyDemand, boolean largePad, boolean fleetCarrier) {
        return name + "|" + sourceSystem + "|" + distance + "|" + supplyDemand + "|" + largePad + "|" + fleetCarrier;
    }
    public void setCurrentHotspotIndex(int index) {
        if (index >= 0 && index < currentHotspots.size()) {
            this.currentHotspotIndex = index;
        }
    }
    /**
     * Récupère le marché complet d'une station avec mise en cache
     */
    public StationMarket fetchStationMarket(String marketId) throws IOException {
        // Vérifier le cache
        StationMarket cachedMarket = stationMarketCache.getIfPresent(marketId);
        if (cachedMarket != null) {
            System.out.printf("🎯 Marché de station trouvé en cache: %s%n", marketId);
            return cachedMarket;
        }

        // Appel à Inara si pas en cache
        System.out.printf("📡 Appel à Inara pour récupérer le marché: %s%n", marketId);
        StationMarket market = client.fetchStationMarket(marketId);

        // Mettre en cache
        stationMarketCache.put(marketId, market);
        System.out.printf("💾 Marché mis en cache: %s avec %d commodités%n", marketId, market.getCommodities().size());

        return market;
    }
    public void setSearchResults(List<CommoditiesStats> results) {
        this.currentSearchResults = new ArrayList<>(results);
        this.currentResultIndex = -1;

        if (!results.isEmpty()) {
            // Définir le premier résultat comme actuel
            currentResultIndex = 0;
        }
    }

    // Méthodes de navigation entre les hotspots
    public void setHotspots(List<MiningHotspot> hotspots) {
        if (hotspots == null){
            this.currentHotspotIndex =-1;
            this.currentHotspots = new ArrayList<>();
            return;
        }
        this.currentHotspots = new ArrayList<>(hotspots);
        this.currentHotspotIndex = -1;

        if (!hotspots.isEmpty()) {
            // Définir le premier hotspot comme actuel
            currentHotspotIndex = 0;
        }
    }
    public void setCurrentResultIndex(int index) {
        if (index >= 0 && index < currentSearchResults.size()) {
            this.currentResultIndex = index;
        }
    }
    public CommoditiesStats getCurrentResult() {
        if (currentResultIndex >= 0 && currentResultIndex < currentSearchResults.size()) {
            return currentSearchResults.get(currentResultIndex);
        }
        return null;
    }
    public int getCurrentResultIndex() {
        return currentResultIndex;
    }


    public int getTotalResults() {
        return currentSearchResults.size();
    }

    public String getCurrentRingSystem() {
        MiningHotspot hotspot = getCurrentHotspot();
        return hotspot != null ? hotspot.getSystemName() : null;
    }
    public String getCurrentStationName() {
        CommoditiesStats result = getCurrentResult();
        return result != null ? result.getStationName() : null;
    }

    /**
     * Récupère le nom du système de la station actuelle
     */
    public String getCurrentStationSystem() {
        CommoditiesStats result = getCurrentResult();
        return result != null ? result.getSystemName() : null;
    }
    /**
     * Récupère le nom du ring actuel
     */
    public String getCurrentRingName() {
        MiningHotspot hotspot = getCurrentHotspot();
        return hotspot != null ? hotspot.getRingName() : null;
    }
    public int getCurrentHotspotIndex() {
        return currentHotspotIndex;
    }
    public MiningHotspot getCurrentHotspot() {
        if (currentHotspotIndex >= 0 && currentHotspotIndex < currentHotspots.size()) {
            return currentHotspots.get(currentHotspotIndex);
        }
        return null;
    }

    public int getTotalHotspots() {
        return currentHotspots.size();
    }
    public long getMineralPriceInCurrentStation(String mineralName) {
        if (currentStationMarket == null) {
            return 0; // Aucune station définie
        }

        return currentStationMarket.getCommodities().stream()
                .filter(entry -> entry.getCommodityName().equalsIgnoreCase(mineralName))
                .mapToLong(StationMarket.CommodityMarketEntry::getSellPrice)
                .findFirst()
                .orElse(0L); // Minéral non trouvé dans la station
    }
    /**
     * Définit la station actuelle et son marché
     * Appelé après avoir trouvé une station via findMineralStation
     */
    public void setCurrentStationMarket(String stationName, String systemName, StationMarket stationMarket) {
        this.currentStationMarketName = stationName;
        this.currentStationMarketSystem = systemName;
        this.currentStationMarket = stationMarket;
        System.out.printf("📍 Station actuelle définie: %s [%s] avec %d commodités%n",
                stationName, systemName, stationMarket.getCommodities().size());
    }
}
