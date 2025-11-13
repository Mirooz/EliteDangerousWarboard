package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.commander.Mission;
import be.mirooz.elitedangerous.dashboard.model.enums.MissionStatus;
import be.mirooz.elitedangerous.dashboard.service.listeners.MissionEventNotificationService;
import com.fasterxml.jackson.databind.JsonNode;

public class MissionExpiredHandler implements JournalEventHandler {
    @Override
    public String getEventType() {
        return "MissionExpired";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            String missionId = jsonNode.get("MissionID").asText();
            Mission mission = missionList.getGlobalMissionMap().get(missionId);
            if (mission != null) {
                System.out.println("Mission expired : " + missionId + " Type : " + mission.getType().getDisplayName());
                mission.setStatus(MissionStatus.FAILED);
                MissionEventNotificationService.getInstance().notifyOnMissionStatusChanged();
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de MissionFailed: " + e.getMessage());
        }
    }
}
