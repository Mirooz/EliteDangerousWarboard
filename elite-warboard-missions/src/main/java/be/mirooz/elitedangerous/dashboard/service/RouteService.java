package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.lib.inara.model.Commodity;

import java.util.Comparator;
import java.util.List;

public class RouteService {
    InaraService inaraService = InaraService.getInstance();
    private static final RouteService INSTANCE = new RouteService();
    public static RouteService getInstance() {
        return INSTANCE;
    }
    private RouteService(){

    }

    public Commodity findBestMinerCommodity(List<Commodity> commodities) {
        if (commodities == null || commodities.isEmpty()) {
            return null;
        }

        return commodities.stream()
                .filter(c -> c.getPrice() != 0)
                .max(Comparator.comparingDouble(Commodity::getPrice))
                .orElse(null);
    }
}
