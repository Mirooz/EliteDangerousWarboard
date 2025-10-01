package be.mirooz.elitedangerous.dashboard.handlers.events;

import be.mirooz.elitedangerous.dashboard.model.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.model.Mission;
import be.mirooz.elitedangerous.dashboard.model.MissionStatus;
import be.mirooz.elitedangerous.dashboard.model.MissionType;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;

import static be.mirooz.elitedangerous.dashboard.util.DateUtil.parseTimestamp;


public class MissionAcceptedHandler implements JournalEventHandler {

    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();
    @Override
    public String getEventType() {
        return "MissionAccepted";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            String missionId = jsonNode.get("MissionID").asText();
            String missionName = jsonNode.get("Name").asText();
            String faction = jsonNode.get("Faction").asText();
            String targetFaction = jsonNode.has("TargetFaction") ? jsonNode.get("TargetFaction").asText() : null;
            String destinationSystem = jsonNode.has("DestinationSystem") ? jsonNode.get("DestinationSystem").asText() : null;

            // Essayer différents champs pour le nombre de kills requis
            int targetCount = 0;
            if (jsonNode.has("TargetCount")) {
                targetCount = jsonNode.get("TargetCount").asInt();
            } else if (jsonNode.has("KillCount")) {
                targetCount = jsonNode.get("KillCount").asInt();
            } else if (jsonNode.has("Count")) {
                targetCount = jsonNode.get("Count").asInt();
            } else if (jsonNode.has("Amount")) {
                targetCount = jsonNode.get("Amount").asInt();
            }
            int reward = jsonNode.has("Reward") ? jsonNode.get("Reward").asInt() : 0;
            String timestamp = jsonNode.get("timestamp").asText();

            // Récupérer la date d'expiration depuis le journal
            LocalDateTime expiryTime = null;
            if (jsonNode.has("Expiry")) {
                expiryTime = parseTimestamp(jsonNode.get("Expiry").asText());
            } else if (jsonNode.has("Deadline")) {
                expiryTime = parseTimestamp(jsonNode.get("Deadline").asText());
            } else {
                // Fallback: 7 jours par défaut
                expiryTime = parseTimestamp(timestamp).plusDays(7);
            }

            Mission mission = new Mission();
            mission.setId(missionId);
            mission.setName(missionName);
            mission.setFaction(faction);
            mission.setTargetFaction(targetFaction);
            mission.setDestinationSystem(destinationSystem);
            mission.setOriginStation(commanderStatus.getCurrentStationNameString());
            mission.setOriginSystem(commanderStatus.getCurrentStarSystemString());
            mission.setTargetCount(targetCount);
            mission.setCurrentCount(0);
            mission.setReward(reward);
            mission.setStatus(MissionStatus.ACTIVE);
            mission.setType(MissionType.fromName(missionName));
            mission.setAcceptedTime(parseTimestamp(timestamp));
            mission.setExpiry(expiryTime);
            
            // Détecter si c'est une mission de wing
            boolean isWing = missionName != null && missionName.contains("Wing");
            mission.setWing(isWing);
            
            missionList.getGlobalMissionMap().put(missionId, mission);

        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de MissionAccepted: " + e.getMessage());
        }
    }
}
