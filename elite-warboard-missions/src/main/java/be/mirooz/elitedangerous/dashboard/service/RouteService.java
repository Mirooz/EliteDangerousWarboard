package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.lib.edtools.model.MiningHotspot;
import be.mirooz.elitedangerous.lib.inara.model.commodities.InaraCommoditiesStats;

import java.util.Comparator;
import java.util.List;

public class RouteService {
    private static final RouteService INSTANCE = new RouteService();
    public static RouteService getInstance() {
        return INSTANCE;
    }
    private RouteService(){

    }

    public InaraCommoditiesStats findBestMinerCommodity(List<InaraCommoditiesStats> commodities, int cargoSpace) {
        if (commodities == null || commodities.isEmpty()) {
            return null;
        }

        return commodities.stream()
                .filter(c -> c.getPrice() != 0)
                .filter(c -> {
                    if (c.isFleetCarrier()){
                        return c.getDemand() >= cargoSpace;
                    }
                    else{
                        return (c.getDemand() >= cargoSpace * 4);
                    }
                })
                .max(Comparator.comparingDouble(InaraCommoditiesStats::getPrice))
                .orElse(null);
    }
    public MiningHotspot findBestHotspot(List<MiningHotspot> miningHotspots) {
        return miningHotspots.stream().min(Comparator.comparingDouble(MiningHotspot::getDistanceFromReference))
                .orElse(null);
    }
}
