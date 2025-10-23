package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.lib.inara.model.StationMarket;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;

/**
 * Service pour gérer le cache des stations trouvées lors des recherches de minage
 * 
 * Ce service met en cache les marchés complets des stations pendant 5 minutes pour éviter
 * les appels répétés à Inara pour les mêmes stations.
 */
public class StationCacheService {

    private static StationCacheService instance;
    private final Cache<String, StationMarket> stationCache = Caffeine.newBuilder()
            .expireAfterWrite(25, TimeUnit.MINUTES)
            .maximumSize(500)
            .build();
    
    // Station actuellement sélectionnée pour les prix
    private String currentStationName;
    private String currentSystemName;

    private StationCacheService() {
    }

    public static StationCacheService getInstance() {
        if (instance == null) {
            instance = new StationCacheService();
        }
        return instance;
    }

    /**
     * Met en cache le marché complet d'une station
     * 
     * @param stationName Le nom de la station
     * @param systemName Le nom du système
     * @param stationMarket Le marché complet de la station
     */
    public void cacheStationMarket(String stationName, String systemName, StationMarket stationMarket) {
        String cacheKey = buildCacheKey(stationName, systemName);
        stationCache.put(cacheKey, stationMarket);
        System.out.printf("💾 Marché de station mis en cache: %s [%s] avec %d commodités%n", 
            stationName, systemName, stationMarket.getCommodities().size());
    }

    /**
     * Récupère le marché d'une station depuis le cache
     * 
     * @param stationName Le nom de la station
     * @param systemName Le nom du système
     * @return Le marché de la station en cache ou null si non trouvé
     */
    public StationMarket getCachedStationMarket(String stationName, String systemName) {
        String cacheKey = buildCacheKey(stationName, systemName);
        StationMarket stationMarket = stationCache.getIfPresent(cacheKey);
        if (stationMarket != null) {
            System.out.printf("🎯 Marché de station trouvé en cache: %s [%s]%n", stationName, systemName);
        }
        return stationMarket;
    }

    /**
     * Récupère le prix d'un minéral spécifique dans une station depuis le cache
     * 
     * @param stationName Le nom de la station
     * @param systemName Le nom du système
     * @param mineralName Le nom du minéral
     * @return Le prix du minéral dans cette station ou 0 si non trouvé
     */
    public long getMineralPriceInStation(String stationName, String systemName, String mineralName) {
        StationMarket stationMarket = getCachedStationMarket(stationName, systemName);
        if (stationMarket == null) {
            return 0; // Station non trouvée en cache
        }

        return stationMarket.getCommodities().stream()
                .filter(entry -> entry.getCommodityName().equalsIgnoreCase(mineralName))
                .mapToLong(entry -> entry.getSellPrice())
                .findFirst()
                .orElse(0L); // Commodité non trouvée dans la station
    }

    /**
     * Vérifie si une station est en cache
     * 
     * @param stationName Le nom de la station
     * @param systemName Le nom du système
     * @return true si la station est en cache
     */
    public boolean isStationCached(String stationName, String systemName) {
        String cacheKey = buildCacheKey(stationName, systemName);
        return stationCache.getIfPresent(cacheKey) != null;
    }

    /**
     * Vide le cache des stations
     */
    public void clearCache() {
        stationCache.invalidateAll();
        System.out.println("🗑️ Cache des stations vidé");
    }

    /**
     * Retourne la taille actuelle du cache
     */
    public long getCacheSize() {
        return stationCache.estimatedSize();
    }

    /**
     * Définit la station actuellement sélectionnée pour les prix
     */
    public void setCurrentStation(String stationName, String systemName) {
        this.currentStationName = stationName;
        this.currentSystemName = systemName;
        System.out.printf("📍 Station actuelle définie: %s [%s]%n", stationName, systemName);
    }

    /**
     * Récupère le prix d'un minéral dans la station actuellement sélectionnée
     */
    public long getMineralPriceInCurrentStation(String mineralName) {
        if (currentStationName == null || currentSystemName == null) {
            return 0; // Aucune station sélectionnée
        }
        return getMineralPriceInStation(currentStationName, currentSystemName, mineralName);
    }

    /**
     * Récupère toutes les commodités de toutes les stations en cache
     * 
     * @return Liste de toutes les commodités de toutes les stations
     */
    public java.util.List<StationMarket.CommodityMarketEntry> getCommodities() {
        return stationCache.asMap().values().stream()
                .flatMap(stationMarket -> stationMarket.getCommodities().stream())
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Construit une clé de cache unique pour une station
     */
    private String buildCacheKey(String stationName, String systemName) {
        return stationName.toLowerCase().trim() + "|" + systemName.toLowerCase().trim();
    }
}
