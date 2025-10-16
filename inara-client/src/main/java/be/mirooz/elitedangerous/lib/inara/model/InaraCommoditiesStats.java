package be.mirooz.elitedangerous.lib.inara.model;

import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.CoreMineralType;
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
