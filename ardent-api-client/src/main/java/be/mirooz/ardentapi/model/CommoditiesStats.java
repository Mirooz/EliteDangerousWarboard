package be.mirooz.ardentapi.model;

import be.mirooz.ardentapi.json.MineralDeserializer;
import be.mirooz.ardentapi.json.StationTypeDeserializer;
import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.Mineral;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class CommoditiesStats {

    @JsonProperty("commodityName")
    @JsonDeserialize(using = MineralDeserializer.class)
    private Mineral mineral;

    private String stationName;
    private String systemName;
    private String marketId;

    @JsonProperty("maxLandingPadSize")
    private String landingPadSize;

    @JsonProperty("distanceToArrival")
    private String stationDistance;

    @JsonProperty("distance")
    private double systemDistance;

    @JsonProperty("stock")
    private int supply;

    private int demand;

    @JsonProperty("sellPrice")
    private int price;

    @JsonProperty("updatedAt")
    private String lastUpdate;

    @JsonProperty("stationType")
    @JsonDeserialize(using = StationTypeDeserializer.class)
    private StationType stationType;
    public boolean isFleetCarrier() {
        return stationType == StationType.FLEET;
    }
}
