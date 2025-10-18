package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.lib.inara.client.InaraClient;

import be.mirooz.elitedangerous.lib.inara.model.InaraCommoditiesStats;
import be.mirooz.elitedangerous.lib.inara.model.conflictsearch.ConflictSystem;
import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.MineralType;

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
}