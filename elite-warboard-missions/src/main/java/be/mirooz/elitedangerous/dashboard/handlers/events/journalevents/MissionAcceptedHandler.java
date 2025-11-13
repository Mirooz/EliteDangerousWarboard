package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.model.commander.Mission;
import be.mirooz.elitedangerous.dashboard.model.enums.MissionStatus;
import be.mirooz.elitedangerous.dashboard.model.enums.MissionType;
import be.mirooz.elitedangerous.dashboard.model.enums.TargetType;
import be.mirooz.elitedangerous.dashboard.service.listeners.MissionEventNotificationService;
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
            String targetType = jsonNode.has("TargetType") ? jsonNode.get("TargetType").asText() : null;
            String description = jsonNode.has("LocalisedName") ? jsonNode.get("LocalisedName").asText() : null;

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
            mission.setDescription(description);
            mission.setTargetFaction(targetFaction);
            mission.setDestinationSystem(destinationSystem);
            mission.setOriginStation(commanderStatus.getCurrentStationName());
            mission.setOriginSystem(commanderStatus.getCurrentStarSystem());
            mission.setTargetCount(targetCount);
            mission.setTargetType(TargetType.fromCode(targetType));
            mission.setCurrentCount(0);
            mission.setReward(reward);
            mission.setStatus(MissionStatus.ACTIVE);
            mission.setType(MissionType.fromName(missionName));
            mission.setAcceptedTime(parseTimestamp(timestamp));
            mission.setExpiry(expiryTime);
            
            // Détecter si c'est une mission de wing
            boolean isWing = missionName != null && missionName.contains("Wing");
            mission.setWing(isWing);

            System.out.println("Mission accepted : " + mission);
            missionList.getGlobalMissionMap().put(missionId, mission);
            MissionEventNotificationService.getInstance().notifyOnMissionStatusChanged();

        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de MissionAccepted: " + e.getMessage());
        }
    }
}
