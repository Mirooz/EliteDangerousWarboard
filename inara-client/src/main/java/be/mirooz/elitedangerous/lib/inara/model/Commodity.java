package be.mirooz.elitedangerous.lib.inara.model;

import be.mirooz.elitedangerous.lib.inara.model.minerals.CoreMineral;
import lombok.Data;

@Data
public class Commodity {
    private CoreMineral coreMineral;
    private String stationName;
    private String systemName;
    private String landingPadSize;
    private String stationDistance;
    private double systemDistance;
    private String supply;
    private String demand;
    private int price;
    private int priceMin;
    private int priceMax;
    private String lastUpdate;
    private String fleetCarrier;
}
