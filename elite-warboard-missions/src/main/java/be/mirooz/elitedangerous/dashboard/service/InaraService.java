package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.Mineral;
import be.mirooz.elitedangerous.dashboard.service.listeners.MineralPriceNotificationService;
import be.mirooz.elitedangerous.lib.inara.client.InaraClient;

import be.mirooz.elitedangerous.lib.inara.model.CommodityMaxSell;
import be.mirooz.elitedangerous.lib.inara.model.InaraCommoditiesStats;
import be.mirooz.elitedangerous.lib.inara.model.StationMarket;
import be.mirooz.elitedangerous.lib.inara.model.conflictsearch.ConflictSystem;
import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.MineralType;
import be.mirooz.elitedangerous.lib.edtools.model.MiningHotspot;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class InaraService {

    private static final InaraService INSTANCE = new InaraService();
    private final InaraClient client;
    private final Cache<String, List<InaraCommoditiesStats>> minerMarketCache = Caffeine.newBuilder()
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();
    private final Cache<String, StationMarket> stationMarketCache = Caffeine.newBuilder()
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .maximumSize(100)
            .build();
    private final MineralPriceNotificationService priceNotificationService = MineralPriceNotificationService.getInstance();
    
    // Navigation entre les résultats de recherche
    private List<InaraCommoditiesStats> currentSearchResults = new ArrayList<>();
    private int currentResultIndex = -1;
    
    // Navigation entre les hotspots
    private List<MiningHotspot> currentHotspots = new ArrayList<>();
    private int currentHotspotIndex = -1;
    
    // Station actuelle et son marché
    private StationMarket currentStationMarket;
    private String currentStationMarketName;
    private String currentStationMarketSystem;


    private InaraService() {
        this.client = new InaraClient();
    }

    public static InaraService getInstance() {
        return INSTANCE;
    }



    public CompletableFuture<List<ConflictSystem>> findConflictZoneSystems(String referenceSystem) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                return client.fetchConflictSystems(referenceSystem);
            } catch (Exception e) {
                throw new RuntimeException("Erreur lors de l'appel Inara", e);
            }
        });
    }

    private String buildCacheKey(String name, String sourceSystem, int distance, int supplyDemand, boolean largePad, boolean fleetCarrier) {
        return name + "|" + sourceSystem + "|" + distance + "|" + supplyDemand + "|" + largePad + "|" + fleetCarrier;
    }

    public List<CommodityMaxSell> fetchCommoditiesMaxSell() throws IOException{
        return client.fetchCommoditiesMaxSell();
    }
    public List<InaraCommoditiesStats> fetchMinerMarket(
            Mineral mineral,
            String sourceSystem,
            int maxDistance,
            int minDemand,
            boolean largePad,
            boolean includeFleetCarrier) {

        String cacheKey = buildCacheKey(mineral.getInaraName(), sourceSystem, maxDistance, minDemand, largePad, includeFleetCarrier);
        
        // Vérifier le cache
        List<InaraCommoditiesStats> cachedResults = minerMarketCache.getIfPresent(cacheKey);
        if (cachedResults != null) {
            System.out.printf("🎯 Résultats trouvés en cache pour %s: %d stations%n", mineral.getVisibleName(), cachedResults.size());
            return cachedResults;
        }

        try {
            List<InaraCommoditiesStats> commodities = fetchMinerMarketCache(mineral, sourceSystem, maxDistance, minDemand, largePad, includeFleetCarrier);
            
            if (commodities == null || commodities.isEmpty()) {
                return new ArrayList<>();
            }
            
            // Filtrer les résultats selon les critères
            List<InaraCommoditiesStats> filteredResults = commodities.stream()
                    .filter(c -> c.getSystemDistance() <= maxDistance)
                    .filter(c -> {
                        int demand = c.getDemand();
                        return c.isFleetCarrier() ? demand >= minDemand : demand >= minDemand * 4;
                    })
                    .sorted(Comparator.comparingDouble(InaraCommoditiesStats::getPrice).reversed()) // Trier par prix décroissant
                    .collect(Collectors.toList());
            
            // Mettre en cache
            minerMarketCache.put(cacheKey, filteredResults);
            System.out.printf("💾 %d résultats mis en cache pour %s%n", filteredResults.size(), mineral.getVisibleName());
            
            // Notifier les changements de prix pour le meilleur résultat
            if (!filteredResults.isEmpty()) {
                InaraCommoditiesStats bestResult = filteredResults.get(0);
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


    private List<InaraCommoditiesStats> fetchMinerMarketCache(Mineral mineral, String sourceSystem, int maxDistance, int minDemand, boolean largePad, boolean includeFleetCarrier) throws IOException {
        return client.fetchMinerMarket(
                mineral, sourceSystem, maxDistance, minDemand, largePad, includeFleetCarrier);

    }

    /**
     * Récupère le marché complet d'une station avec mise en cache
     */
    public StationMarket fetchStationMarket(String stationUrl) throws IOException {
        // Vérifier le cache
        StationMarket cachedMarket = stationMarketCache.getIfPresent(stationUrl);
        if (cachedMarket != null) {
            System.out.printf("🎯 Marché de station trouvé en cache: %s%n", stationUrl);
            return cachedMarket;
        }
        
        // Appel à Inara si pas en cache
        System.out.printf("📡 Appel à Inara pour récupérer le marché: %s%n", stationUrl);
        StationMarket market = client.fetchStationMarket(stationUrl);

        // Mettre en cache
        stationMarketCache.put(stationUrl, market);
        System.out.printf("💾 Marché mis en cache: %s avec %d commodités%n", stationUrl, market.getCommodities().size());
        
        return market;
    }

    // Méthodes de navigation entre les résultats
    public void setSearchResults(List<InaraCommoditiesStats> results) {
        this.currentSearchResults = new ArrayList<>(results);
        this.currentResultIndex = -1;
        
        if (!results.isEmpty()) {
            // Définir le premier résultat comme actuel
            currentResultIndex = 0;
        }
    }

    public void navigateToPreviousResult() {
        if (currentResultIndex > 0) {
            currentResultIndex--;
        }
    }

    public void navigateToNextResult() {
        if (currentResultIndex < currentSearchResults.size() - 1) {
            currentResultIndex++;
        }
    }

    public boolean hasPreviousResult() {
        return currentResultIndex > 0;
    }

    public boolean hasNextResult() {
        return currentResultIndex < currentSearchResults.size() - 1;
    }

    public InaraCommoditiesStats getCurrentResult() {
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

    public void setCurrentResultIndex(int index) {
        if (index >= 0 && index < currentSearchResults.size()) {
            this.currentResultIndex = index;
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

    public MiningHotspot getCurrentHotspot() {
        if (currentHotspotIndex >= 0 && currentHotspotIndex < currentHotspots.size()) {
            return currentHotspots.get(currentHotspotIndex);
        }
        return null;
    }

    public int getCurrentHotspotIndex() {
        return currentHotspotIndex;
    }

    public int getTotalHotspots() {
        return currentHotspots.size();
    }
    
    /**
     * Récupère le nom du système actuel du ring
     */
    public String getCurrentRingSystem() {
        MiningHotspot hotspot = getCurrentHotspot();
        return hotspot != null ? hotspot.getSystemName() : null;
    }
    
    /**
     * Récupère le nom du ring actuel
     */
    public String getCurrentRingName() {
        MiningHotspot hotspot = getCurrentHotspot();
        return hotspot != null ? hotspot.getRingName() : null;
    }
    
    /**
     * Récupère le nom de la station actuelle
     */
    public String getCurrentStationName() {
        InaraCommoditiesStats result = getCurrentResult();
        return result != null ? result.getStationName() : null;
    }
    
    /**
     * Récupère le nom du système de la station actuelle
     */
    public String getCurrentStationSystem() {
        InaraCommoditiesStats result = getCurrentResult();
        return result != null ? result.getSystemName() : null;
    }

    public void setCurrentHotspotIndex(int index) {
        if (index >= 0 && index < currentHotspots.size()) {
            this.currentHotspotIndex = index;
        }
    }
    
    // ─────────────────────────────────────────────
    // Station actuelle et son marché
    // ─────────────────────────────────────────────
    
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
    
    /**
     * Récupère le marché de la station actuelle
     */
    public StationMarket getCurrentStationMarket() {
        return currentStationMarket;
    }
    
    /**
     * Vérifie si une station actuelle est définie
     */
    public boolean hasCurrentStationMarket() {
        return currentStationMarket != null;
    }
    
    /**
     * Récupère le nom de la station actuelle
     */
    public String getCurrentStationMarketName() {
        return currentStationMarketName;
    }
    
    /**
     * Récupère le nom du système de la station actuelle
     */
    public String getCurrentStationMarketSystem() {
        return currentStationMarketSystem;
    }
    
    /**
     * Récupère le prix d'un minéral dans la station actuelle
     * @param mineralName Le nom du minéral (nom Inara, ex: "Alexandrite")
     * @return Le prix de vente du minéral dans la station actuelle, ou 0 si non trouvé
     */
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
}