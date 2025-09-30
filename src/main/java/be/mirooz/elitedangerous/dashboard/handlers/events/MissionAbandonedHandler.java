package be.mirooz.elitedangerous.dashboard.handlers.events;

import be.mirooz.elitedangerous.dashboard.model.Mission;
import be.mirooz.elitedangerous.dashboard.model.MissionStatus;
import com.fasterxml.jackson.databind.JsonNode;

public class MissionAbandonedHandler implements JournalEventHandler {
    @Override
    public String getEventType() {
        return "MissionAbandoned";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            String missionId = jsonNode.get("MissionID").asText();
            Mission mission = missionList.getGlobalMissionMap().get(missionId);
            if (mission != null) {
                mission.setStatus(MissionStatus.FAILED);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de MissionAbandoned: " + e.getMessage());
        }
    }
}
