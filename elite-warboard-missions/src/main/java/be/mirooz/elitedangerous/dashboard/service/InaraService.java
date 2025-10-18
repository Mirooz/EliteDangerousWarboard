package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.Mineral;
import be.mirooz.elitedangerous.lib.inara.client.InaraClient;

import be.mirooz.elitedangerous.lib.inara.model.InaraCommoditiesStats;
import be.mirooz.elitedangerous.lib.inara.model.conflictsearch.ConflictSystem;
import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.MineralType;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class InaraService {

    private static final InaraService INSTANCE = new InaraService();
    private final InaraClient client;
    private final Cache<String, List<InaraCommoditiesStats>> minerMarketCache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();
    private final PreferencesService preferencesService = PreferencesService.getInstance();


    private InaraService() {
        this.client = new InaraClient();
    }

    public static InaraService getInstance() {
        return INSTANCE;
    }

    /**
     * Sauvegarde le prix d'un minéral dans les préférences utilisateur
     */
    private void saveMineralPriceToPreferences(Mineral mineral, int price) {
        if (mineral instanceof MineralType) {
            String key = "mineral.price." + mineral.getCargoJsonName();
            preferencesService.setPreference(key, String.valueOf(price));
            System.out.printf("💾 Prix sauvegardé pour %s: %d Cr%n", mineral.getVisibleName(), price);
        }
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

    public CompletableFuture<Optional<InaraCommoditiesStats>> fetchMinerMarket(Mineral mineral, String sourceSystem, int maxDistance, int minDemand, boolean largePad, boolean includeFleetCarrier) {
        String cacheKey = buildCacheKey(mineral.getInaraName(), sourceSystem, maxDistance, minDemand, largePad, includeFleetCarrier);
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<InaraCommoditiesStats> commodities = fetchMinerMarketCache(mineral, sourceSystem, maxDistance, minDemand, largePad, includeFleetCarrier, cacheKey);
                if (commodities == null || commodities.isEmpty()) {
                    return Optional.empty();
                }
                Optional<InaraCommoditiesStats> bestOpt = commodities.stream()
                        .filter(c -> c.getSystemDistance() <= maxDistance)
                        .filter(c -> c.isFleetCarrier() ? minDemand <= c.getDemand() : minDemand * 4 <= c.getDemand())
                        .max(Comparator.comparingDouble(InaraCommoditiesStats::getPrice));

                bestOpt.ifPresent(best -> {
                    mineral.setPrice(best.getPrice());
                    // Sauvegarder le prix dans les préférences
                    saveMineralPriceToPreferences(mineral, best.getPrice());
                });

                return bestOpt;

            } catch (Exception e) {
                return Optional.empty();
            }
        });
    }

    private List<InaraCommoditiesStats> fetchMinerMarketCache(Mineral mineral, String sourceSystem, int maxDistance, int minDemand, boolean largePad, boolean includeFleetCarrier, String cacheKey) {
        return minerMarketCache.get(cacheKey, k -> {
            System.out.println("🆕 Cache miss " + mineral.getCargoJsonName());
            try {
                List<InaraCommoditiesStats> results = client.fetchMinerMarket(
                        mineral, sourceSystem, maxDistance, minDemand, largePad, includeFleetCarrier);
                
                // Si aucun résultat trouvé, essayer avec les paramètres par défaut (fleet=true, largePad=false)
                if (results.isEmpty() && mineral instanceof MineralType) {
                    String defaultCacheKey = buildCacheKey(mineral.getInaraName(), "Sol", 100, 1000, false, true);
                    List<InaraCommoditiesStats> defaultResults = minerMarketCache.getIfPresent(defaultCacheKey);
                    if (defaultResults != null && !defaultResults.isEmpty()) {
                        System.out.println("🔄 Utilisation du prix par défaut depuis le cache pour " + mineral.getCargoJsonName());
                        return defaultResults;
                    }
                }
                
                return results;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}