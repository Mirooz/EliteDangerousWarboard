package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.siriuscorp.client.SiriuscorpClient;
import be.mirooz.elitedangerous.siriuscorp.model.ConflictSystem;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SiriuscorpService {
    private static volatile SiriuscorpService instance;

    private final SiriuscorpClient client;
    private SiriuscorpService() {
        this.client = new SiriuscorpClient();
    }

    public static SiriuscorpService getInstance() {
        if (instance == null) {
            synchronized (SiriuscorpService.class) {
                if (instance == null) {
                    instance = new SiriuscorpService();
                }
            }
        }
        return instance;
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
