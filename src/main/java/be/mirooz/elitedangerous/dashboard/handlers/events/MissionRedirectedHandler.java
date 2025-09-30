package be.mirooz.elitedangerous.dashboard.handlers.events;

import be.mirooz.elitedangerous.dashboard.model.Mission;
import be.mirooz.elitedangerous.dashboard.model.MissionStatus;
import be.mirooz.elitedangerous.dashboard.model.MissionType;
import be.mirooz.elitedangerous.dashboard.model.MissionsList;
import com.fasterxml.jackson.databind.JsonNode;

public class MissionRedirectedHandler implements JournalEventHandler {
    
    private final MissionsList missionList = MissionsList.getInstance();

    @Override
    public String getEventType() {
        return "MissionRedirected";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            String missionId = jsonNode.get("MissionID").asText();
            String missionName = jsonNode.has("Name") ? jsonNode.get("Name").asText() : "";
            
            // Vérifier si c'est une mission de massacre
            if (missionName.contains("Massacre") || missionName.contains("massacre")) {
                Mission mission = missionList.getGlobalMissionMap().get(missionId);
                if (mission != null && mission.getType() == MissionType.MASSACRE) {
                    // Mettre le kill count au target (sans changer le statut)
                    if (mission.getCurrentCount() != mission.getTargetCount()) {
                        mission.setCurrentCount(mission.getTargetCount());


                        System.err.println("Mission de massacre " + missionId + " : kill count mis à " + mission.getTargetCount() + " après redirection");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de MissionRedirected: " + e.getMessage());
            e.printStackTrace();
        }
    }
}