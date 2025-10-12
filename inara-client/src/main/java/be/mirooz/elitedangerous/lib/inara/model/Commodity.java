package be.mirooz.elitedangerous.lib.inara.model;

import lombok.Data;

@Data
public class Commodity {
    private String name;
    private String stationName;
    private String systemName;
    private String landingPadSize;
    private String stationDistance;
    private double systemDistance;
    private String supply;
    private String demand;
    private String price;
    private String priceMin;
    private String priceMax;
    private String lastUpdate;
    private String fleetCarrier;
}
