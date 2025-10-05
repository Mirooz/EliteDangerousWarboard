package be.mirooz.elitedangerous.dashboard.model;

import lombok.Data;

@Data
public class ShipTarget {
    private String timestamp;
    private String event;
    private Boolean targetLocked;
    private String ship;
    private String shipLocalised;
    private Integer scanStage;
    private String pilotName;
    private String pilotNameLocalised;
    private String pilotRank;
    private Double shieldHealth;
    private Double hullHealth;
    private String faction;
    private String legalStatus;
    private Long bounty;
    private boolean crimeCommitted = false;
    public boolean isPirate(){
        return bounty != null && bounty > 0 && !crimeCommitted;
    }
    public boolean isDeserteur(){
        return bounty != null && bounty > 0 && crimeCommitted;
    }
}
