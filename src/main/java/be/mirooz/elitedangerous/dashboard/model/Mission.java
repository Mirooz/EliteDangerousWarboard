package be.mirooz.elitedangerous.dashboard.model;

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
    private LocalDateTime acceptedTime;
    private boolean wing;

    public int getTargetCountLeft(){
        return targetCount-currentCount;
    }

}
