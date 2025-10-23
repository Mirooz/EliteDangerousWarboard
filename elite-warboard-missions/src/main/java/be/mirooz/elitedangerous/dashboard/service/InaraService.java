package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.Mineral;
import be.mirooz.elitedangerous.lib.inara.client.InaraClient;

import be.mirooz.elitedangerous.lib.inara.model.InaraCommoditiesStats;
import be.mirooz.elitedangerous.lib.inara.model.StationMarket;
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
    private final Cache<String, InaraCommoditiesStats> minerMarketCache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();
    private final PreferencesService preferencesService = PreferencesService.getInstance();
    private final MineralPriceNotificationService priceNotificationService = MineralPriceNotificationService.getInstance();


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

    public Optional<InaraCommoditiesStats> fetchMinerMarket(
            Mineral mineral,
            String sourceSystem,
            int maxDistance,
            int minDemand,
            boolean largePad,
            boolean includeFleetCarrier) {

        String cacheKey = buildCacheKey(mineral.getInaraName(), sourceSystem, maxDistance, minDemand, largePad, includeFleetCarrier);
        InaraCommoditiesStats best = minerMarketCache.get(cacheKey, k -> {
            System.out.println("🆕 Cache miss: " + mineral.getCargoJsonName());
            try {
                List<InaraCommoditiesStats> commodities =
                        fetchMinerMarketCache(mineral, sourceSystem, maxDistance, minDemand, largePad, includeFleetCarrier);

                if (commodities == null || commodities.isEmpty()) {
                    return null;
                }
                return commodities.stream()
                        .filter(c -> c.getSystemDistance() <= maxDistance)
                        .filter(c -> {
                            int demand = c.getDemand();
                            return c.isFleetCarrier() ? demand >= minDemand : demand >= minDemand * 4;
                        })
                        .max(Comparator.comparingDouble(InaraCommoditiesStats::getPrice))
                        .map(bestOpt -> {
                            long oldPrice = mineral.getPrice();
                            mineral.setPrice(bestOpt.getPrice());
                            // Notifier le changement de prix
                            if (oldPrice != bestOpt.getPrice()) {
                                priceNotificationService.notifyPriceChanged(mineral, oldPrice, bestOpt.getPrice());
                            }
                            return bestOpt;
                        })
                        .orElse(null);

            } catch (Exception e) {
                System.err.println("❌ Error while fetching miner market for " + mineral.getInaraName() + ": " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        });
        saveMineralPriceToPreferences(mineral, mineral.getPrice());
        return Optional.ofNullable(best);
    }


    private List<InaraCommoditiesStats> fetchMinerMarketCache(Mineral mineral, String sourceSystem, int maxDistance, int minDemand, boolean largePad, boolean includeFleetCarrier) throws IOException {
        return client.fetchMinerMarket(
                mineral, sourceSystem, maxDistance, minDemand, largePad, includeFleetCarrier);

    }

    /**
     * Récupère le marché complet d'une station
     */
    public StationMarket fetchStationMarket(String stationUrl) throws IOException {
        return client.fetchStationMarket(stationUrl);
    }
}