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
    private int reward;
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
    public boolean isActivePirateMission(){
        return isPirateMission() && MissionStatus.ACTIVE.equals(status);
    }
    public boolean isActiveDeserteurMission(){
        return isDeserteurMission() && MissionStatus.ACTIVE.equals(status);
    }
    public boolean isPirateMission(){
        return (TargetType.PIRATE.equals(targetType));
    }
    public boolean isDeserteurMission(){
        return (TargetType.DESERTEUR.equals(targetType));
    }

    public int getTargetCountLeft(){
        return targetCount-currentCount;
    }

    public boolean isMassacreActive(){
        return MissionStatus.ACTIVE.equals(this.status) && MissionType.MASSACRE.equals(this.type);
    }

    public  boolean isMissionFailed() {
        return this.getStatus() == MissionStatus.FAILED
                || this.getStatus() == MissionStatus.EXPIRED
                || this.getStatus() == MissionStatus.ABANDONED;
    }


}
