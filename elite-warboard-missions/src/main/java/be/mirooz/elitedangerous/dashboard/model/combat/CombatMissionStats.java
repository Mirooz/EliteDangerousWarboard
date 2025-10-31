package be.mirooz.elitedangerous.dashboard.model.combat;

import be.mirooz.elitedangerous.dashboard.model.enums.MissionType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Statistiques pour une mission de massacre complétée
 */
@Data
public class CombatMissionStats {
    private String originSystem;
    private String destinationSystem;
    private MissionType missionCategory; // "PIRATE" ou "CONFLIT"
    private int completedMissions;
    private int totalKills;
    private long totalReward;
    private LocalDateTime lastCompleted;
    
    public CombatMissionStats(String originSystem, String destinationSystem, MissionType missionCategory, LocalDateTime lastCompleted) {
        this.originSystem = originSystem;
        this.destinationSystem = destinationSystem;
        this.missionCategory = missionCategory;
        this.completedMissions = 0;
        this.totalKills = 0;
        this.totalReward = 0L;
        this.lastCompleted = lastCompleted;
    }
    
    public void addCompletedMission(int kills, long reward, LocalDateTime lastCompleted) {
        this.completedMissions++;
        this.totalKills += kills;
        this.totalReward += reward;
        this.lastCompleted = lastCompleted;
    }
}

