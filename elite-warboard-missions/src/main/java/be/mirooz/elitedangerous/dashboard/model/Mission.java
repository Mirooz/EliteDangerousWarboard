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
    public boolean isShipActivePirateMassacreMission(){
        return isShipPirateMission() && MissionStatus.ACTIVE.equals(status);
    }
    public boolean isOnFootActiveMassacreMission(){
        return isOnFootMassacre() && MissionStatus.ACTIVE.equals(status) && TargetType.HUMANOID.equals(this.targetType);
    }
    public boolean isShipActiveDeserteurMassacreMission(){
        return isShipDeserteurMission() && MissionStatus.ACTIVE.equals(status);
    }
    public boolean isShipActiveFactionConflictMission(){
        return MissionType.CONFLIT.equals(this.type) && MissionStatus.ACTIVE.equals(status);
    }
    public boolean isShipPirateMission(){
        return TargetType.PIRATE.equals(targetType) && MissionType.MASSACRE.equals(this.type);
    }
    public boolean isShipDeserteurMission(){
        return TargetType.DESERTEUR.equals(targetType) && MissionType.MASSACRE.equals(this.type);
    }

    public int getTargetCountLeft(){
        return targetCount-currentCount;
    }

    public boolean isShipMassacreActive(){
        return MissionStatus.ACTIVE.equals(this.status)  && (isShipMassacre()) ;

    }


    public boolean isShipMassacre(){
        return  ((MissionType.MASSACRE.equals(this.type) && TargetType.PIRATE.equals(this.targetType))
                || MissionType.CONFLIT.equals(this.type));
    }
    public boolean isOnFootMassacre(){
        return (MissionType.MASSACRE_ONFOOT.equals(this.type));
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
