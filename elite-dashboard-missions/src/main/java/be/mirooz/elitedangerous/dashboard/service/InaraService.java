package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.lib.edtools.client.EdToolsPveClient;
import be.mirooz.elitedangerous.lib.edtools.model.MassacreSystem;
import be.mirooz.elitedangerous.lib.inara.client.InaraClient;
import be.mirooz.elitedangerous.lib.inara.model.NearbyStation;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class InaraService {

    private static final InaraService INSTANCE = new InaraService();
    private final InaraClient client;

    private InaraService() {
        this.client = new InaraClient();
    }

    public static InaraService getInstance() {
        return INSTANCE;
    }

    public CompletableFuture<List<NearbyStation>> findConflictZoneSystems(String referenceSystem) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                return client.fetchNearbyStations(referenceSystem);
            } catch (Exception e) {
                throw new RuntimeException("Erreur lors de l'appel Inara", e);
            }
        });
    }
}