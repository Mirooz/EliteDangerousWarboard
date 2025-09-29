package be.mirooz.elitedangerous.dashboard.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Modèle représentant une mission Elite Dangerous
 */
public class Mission {
    private String id;
    private String name;
    private String description;
    private MissionType type;
    private MissionStatus status;
    private String faction;
    private String destination;
    private String origin;
    private int reward;
    private int influence;
    private int reputation;
    private LocalDateTime expiry;
    private List<String> objectives;
    private String commodity;
    private int commodityCount;
    private String targetFaction;
    private String targetSystem;
    private int targetCount;
    private int currentCount;
    private LocalDateTime acceptedTime;

    public Mission() {}

    public Mission(String id, String name, String description, MissionType type, 
                   MissionStatus status, String faction, String destination, 
                   String origin, int reward, LocalDateTime expiry) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.status = status;
        this.faction = faction;
        this.destination = destination;
        this.origin = origin;
        this.reward = reward;
        this.expiry = expiry;
    }

    // Getters et Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public MissionType getType() { return type; }
    public void setType(MissionType type) { this.type = type; }

    public MissionStatus getStatus() { return status; }
    public void setStatus(MissionStatus status) { this.status = status; }

    public String getFaction() { return faction; }
    public void setFaction(String faction) { this.faction = faction; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public int getReward() { return reward; }
    public void setReward(int reward) { this.reward = reward; }

    public int getInfluence() { return influence; }
    public void setInfluence(int influence) { this.influence = influence; }

    public int getReputation() { return reputation; }
    public void setReputation(int reputation) { this.reputation = reputation; }

    public LocalDateTime getExpiry() { return expiry; }
    public void setExpiry(LocalDateTime expiry) { this.expiry = expiry; }

    public List<String> getObjectives() { return objectives; }
    public void setObjectives(List<String> objectives) { this.objectives = objectives; }

    public String getCommodity() { return commodity; }
    public void setCommodity(String commodity) { this.commodity = commodity; }

    public int getCommodityCount() { return commodityCount; }
    public void setCommodityCount(int commodityCount) { this.commodityCount = commodityCount; }

    public String getTargetFaction() { return targetFaction; }
    public void setTargetFaction(String targetFaction) { this.targetFaction = targetFaction; }

    public int getTargetCount() { return targetCount; }
    public void setTargetCount(int targetCount) { this.targetCount = targetCount; }

    public int getTargetCountLeft(){
        return targetCount-currentCount;
    }

    public int getCurrentCount() { return currentCount; }
    public void setCurrentCount(int currentCount) { this.currentCount = currentCount; }

    public String getTargetSystem() { return targetSystem; }
    public void setTargetSystem(String targetSystem) { this.targetSystem = targetSystem; }

    public LocalDateTime getAcceptedTime() { return acceptedTime; }
    public void setAcceptedTime(LocalDateTime acceptedTime) { this.acceptedTime = acceptedTime; }

    @Override
    public String toString() {
        return String.format("Mission{id='%s', name='%s', type=%s, status=%s, faction='%s', reward=%d}", 
                           id, name, type, status, faction, reward);
    }
}
