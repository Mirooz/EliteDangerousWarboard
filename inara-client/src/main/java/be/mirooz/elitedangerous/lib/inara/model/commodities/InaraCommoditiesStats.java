package be.mirooz.elitedangerous.lib.inara.model.commodities;

import be.mirooz.elitedangerous.lib.inara.model.commodities.minerals.CoreMineralType;
import lombok.Data;

@Data
public class InaraCommoditiesStats {
    private CoreMineralType coreMineral;
    private String stationName;
    private String systemName;
    private String landingPadSize;
    private String stationDistance;
    private double systemDistance;
    private int supply;
    private int demand;
    private int price;
    private int priceMin;
    private int priceMax;
    private String lastUpdate;
    private boolean fleetCarrier;
}
