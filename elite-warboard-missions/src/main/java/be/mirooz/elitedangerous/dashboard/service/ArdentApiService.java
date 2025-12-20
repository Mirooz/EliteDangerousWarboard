package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.client.ArdentApiClient;
import be.mirooz.elitedangerous.model.CommodityMaxSell;

import java.io.IOException;
import java.util.List;

public class ArdentApiService {

    private static volatile ArdentApiService instance;

    private final ArdentApiClient client;
    private ArdentApiService() {
        this.client = new ArdentApiClient();
    }

    public static ArdentApiService getInstance() {
        if (instance == null) {
            synchronized (ArdentApiService.class) {
                if (instance == null) {
                    instance = new ArdentApiService();
                }
            }
        }
        return instance;
    }

    public List<CommodityMaxSell> fetchCommoditiesMaxSell() throws IOException {
        return client.fetchCommoditiesMaxSell();
    }
}
