package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.lib.edtools.client.EdToolsPveClient;
import be.mirooz.elitedangerous.lib.edtools.model.MassacreSystem;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class EdToolsService {

    private static final EdToolsService INSTANCE = new EdToolsService();
    private final EdToolsPveClient client;

    private EdToolsService() {
        this.client = new EdToolsPveClient();
    }

    public static EdToolsService getInstance() {
        return INSTANCE;
    }

    public CompletableFuture<List<MassacreSystem>> findMassacreSystems(String referenceSystem, int maxDistanceLy, int minSourcesPerTarget) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                return client.fetch(referenceSystem, maxDistanceLy, minSourcesPerTarget).getRows();
            } catch (Exception e) {
                throw new RuntimeException("Erreur lors de l'appel EdTools", e);
            }
        });
    }
}