package be.mirooz.ardentapi.model;

import be.mirooz.elitedangerous.commons.lib.models.commodities.ICommodity;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Représente le marché d'une station Inara avec toutes ses commodités
 */
@Data
public class StationMarket {
    private String stationName;
    private String stationUrl;
    private String systemName;
    private String stationDistance;
    private String landingPadSize;
    private String marketUpdate;
    private String stationUpdate;
    private String locationUpdate;
    private List<CommodityMarketEntry> commodities = new ArrayList<>();
    
    /**
     * Représente une entrée de commodité dans le marché
     */
    @Data
    public static class CommodityMarketEntry {
        private String commodityName;
        private int sellPrice;
        private int demand;
        private int buyPrice;
        private int supply;
        private boolean isBestPrice;
        private boolean isBetterThanAverage;
        private ICommodity commodity;
    }
}
