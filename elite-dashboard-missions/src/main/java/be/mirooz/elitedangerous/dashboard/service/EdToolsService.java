package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.lib.edtools.client.EdToolsPveClient;
import be.mirooz.elitedangerous.lib.edtools.model.MassacreSystem;

import java.util.List;

public class EdToolsService {

    private static final EdToolsService INSTANCE = new EdToolsService();
    private final EdToolsPveClient client;

    private EdToolsService() {
        this.client = new EdToolsPveClient();
    }

    public static EdToolsService getInstance() {
        return INSTANCE;
    }

    public List<MassacreSystem> findMassacreSystems(String referenceSystem, int maxDistanceLy, int minSourcesPerTarget) throws Exception {
        return client.fetch(referenceSystem, maxDistanceLy, minSourcesPerTarget).getRows();
    }
}