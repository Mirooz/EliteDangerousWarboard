package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.lib.edtools.client.EdToolsClient;
import be.mirooz.elitedangerous.lib.edtools.model.MassacreSystem;
import be.mirooz.elitedangerous.lib.edtools.model.MiningHotspot;
import be.mirooz.elitedangerous.lib.inara.model.minerals.CoreMineral;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class EdToolsService {

    private static final EdToolsService INSTANCE = new EdToolsService();
    private final EdToolsClient client;

    private EdToolsService() {
        this.client = new EdToolsClient();
    }

    public static EdToolsService getInstance() {
        return INSTANCE;
    }

    public CompletableFuture<List<MassacreSystem>> findMassacreSystems(String referenceSystem, int maxDistanceLy, int minSourcesPerTarget,boolean largPad) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                return client.sendSystemSearch(referenceSystem, maxDistanceLy, minSourcesPerTarget,largPad).getRows();
            } catch (Exception e) {
                throw new RuntimeException("Erreur lors de l'appel EdTools", e);
            }
        });
    }
    public CompletableFuture<List<MassacreSystem>> findSourcesForTargetSystem(String referenceSystem) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                return client.sendTargetSystemSearch(referenceSystem).getRows();
            } catch (Exception e) {
                throw new RuntimeException("Erreur lors de l'appel EdTools", e);
            }
        });
    }

    public CompletableFuture<List<MiningHotspot>> findMiningHotspots(String referenceSystem, CoreMineral coreMineral) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return client.fetchMiningHotspots(referenceSystem, coreMineral.getEdToolName(), 1,false);
            } catch (Exception e) {
                throw new RuntimeException("Erreur lors de la récupération des hotspots de minage", e);
            }
        });
    }
}