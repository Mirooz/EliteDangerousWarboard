package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.lib.inara.client.InaraClient;
import be.mirooz.elitedangerous.lib.inara.model.commodities.InaraCommoditiesStats;
import be.mirooz.elitedangerous.lib.inara.model.conflictsearch.ConflictSystem;
import be.mirooz.elitedangerous.lib.inara.model.commodities.minerals.CoreMineralType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
    public CompletableFuture<List<InaraCommoditiesStats>> fetchAllMinerMarkets(String sourceSystem, int distance,
                                                                               int supplyDemand, boolean largePad) {

        // Récupérer tous les minéraux de core mining
        List<CoreMineralType> allMinerals = CoreMineralType.all();

        // Lancer une requête par minéral en parallèle
        List<CompletableFuture<List<InaraCommoditiesStats>>> futures = new ArrayList<>();
        long start = System.currentTimeMillis();
        for (CoreMineralType mineral : allMinerals) {
            CompletableFuture<List<InaraCommoditiesStats>> future = CompletableFuture.supplyAsync(() -> {
                try {
                    System.out.println("🔍 Recherche du marché pour: " + mineral.getInaraName());
                    return client.fetchMinerMarket(mineral, sourceSystem, distance, supplyDemand, largePad).stream()
                            .filter(commodity -> commodity.getSystemDistance() <= distance)
                            .collect(Collectors.toList());
                } catch (IOException e) {
                    System.err.println("❌ Erreur lors de la recherche pour " + mineral.getInaraName() + ": " + e.getMessage());
                    return new ArrayList<>();
                }
            }, executorService);

            futures.add(future);
        }

        // Combiner toutes les futures en une seule
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    List<InaraCommoditiesStats> allCommodities = new ArrayList<>();
                    for (CompletableFuture<List<InaraCommoditiesStats>> future : futures) {
                        try {
                            allCommodities.addAll(future.get());
                        } catch (InterruptedException | ExecutionException e) {
                            System.err.println("⚠️ Erreur dans une future: " + e.getMessage());
                        }
                    }
                    System.out.println("✅ Recherche terminée. Total de " + allCommodities.size() + " commodités trouvées.");
                    long durationCall = System.currentTimeMillis() - start;
                    System.out.println("INARA total calls duration: " + durationCall + " ms");
                    return allCommodities;
                });
    }

}