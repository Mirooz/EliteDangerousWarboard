package be.mirooz.elitedangerous.dashboard.model;

import be.mirooz.elitedangerous.dashboard.model.enums.MissionStatus;
import be.mirooz.elitedangerous.dashboard.model.enums.MissionType;
import be.mirooz.elitedangerous.dashboard.model.enums.TargetType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Modèle représentant une mission Elite Dangerous
 */
@Data
public class Mission {
    private String id;
    private String name;
    private String description;
    private MissionType type;
    private MissionStatus status;
    private String faction;
    private String destinationSystem;
    private String originSystem;
    private String originStation;
    private long reward;
    private int influence;
    private int reputation;
    private LocalDateTime expiry;
    private List<String> objectives;
    private String commodity;
    private int commodityCount;
    private String targetFaction;
    private int targetCount;
    private int currentCount;
    private TargetType targetType;
    private LocalDateTime acceptedTime;
    private boolean wing;
    public boolean isShipActivePirateMission(){
        return isShipPirateMission() && MissionStatus.ACTIVE.equals(status);
    }
    public boolean isShipActiveDeserteurMission(){
        return isShipDeserteurMission() && MissionStatus.ACTIVE.equals(status);
    }
    public boolean isShipPirateMission(){
        return (TargetType.PIRATE.equals(targetType));
    }
    public boolean isShipDeserteurMission(){
        return (TargetType.DESERTEUR.equals(targetType));
    }

    public int getTargetCountLeft(){
        return targetCount-currentCount;
    }

    public boolean isMassacreActive(){
        return MissionStatus.ACTIVE.equals(this.status)  && isMassacre();

    }

    public boolean isMassacre(){
        return  (MissionType.MASSACRE.equals(this.type)
                || MissionType.MASSACRE_ONFOOT.equals(this.type)
                || MissionType.CONFLIT.equals(this.type));
    }
    public boolean isActive(){
        return MissionStatus.ACTIVE.equals(this.status);
    }
    public boolean isPending(){
        return isActive() && getTargetCountLeft() ==0 && getTargetCount() !=0;
    }
    public boolean isCompleted(){
        return MissionStatus.COMPLETED.equals(this.status);
    }
    public  boolean isMissionFailed() {
        return this.getStatus() == MissionStatus.FAILED
                || this.getStatus() == MissionStatus.EXPIRED
                || this.getStatus() == MissionStatus.ABANDONED;
    }


}
