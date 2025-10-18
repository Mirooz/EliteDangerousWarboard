package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.Mineral;
import be.mirooz.elitedangerous.lib.inara.client.InaraClient;

import be.mirooz.elitedangerous.lib.inara.model.InaraCommoditiesStats;
import be.mirooz.elitedangerous.lib.inara.model.conflictsearch.ConflictSystem;
import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.MineralType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class InaraService {

    private static final InaraService INSTANCE = new InaraService();
    private final InaraClient client;
    private final ExecutorService executorService;

    private InaraService() {
        this.client = new InaraClient();
        this.executorService = Executors.newFixedThreadPool(10);
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
    public CompletableFuture<Optional<InaraCommoditiesStats>> fetchMinerMarket(Mineral mineral, String sourceSystem, int maxDistance, int minDemand, boolean largePad, boolean includeFleetCarrier) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<InaraCommoditiesStats> commodities = client.fetchMinerMarket(
                        mineral, sourceSystem, maxDistance, minDemand, largePad, includeFleetCarrier);

                if (commodities == null || commodities.isEmpty()) {
                    return Optional.empty();
                }
                Optional<InaraCommoditiesStats> bestOpt = commodities.stream()
                        .filter(c -> c.getSystemDistance() <= maxDistance)
                        .filter(c -> c.isFleetCarrier() ? minDemand <= c.getDemand() : minDemand * 4 <= c.getDemand())
                        .max(Comparator.comparingDouble(InaraCommoditiesStats::getPrice));

                bestOpt.ifPresent(best -> mineral.setPrice(best.getPrice()));

                return bestOpt;

            } catch (Exception e) {
                return Optional.empty();
            }
        });
    }
}