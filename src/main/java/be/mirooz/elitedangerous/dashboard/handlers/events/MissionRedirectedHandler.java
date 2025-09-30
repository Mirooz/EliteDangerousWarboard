package be.mirooz.elitedangerous.dashboard.handlers.events;

import be.mirooz.elitedangerous.dashboard.model.Mission;
import com.fasterxml.jackson.databind.JsonNode;

public class MissionRedirectedHandler implements JournalEventHandler {
    @Override
    public String getEventType() {
        return "MissionRedirected";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            String missionId = jsonNode.get("MissionID").asText();
            Mission mission = missionList.getGlobalMissionMap().get(missionId);
            if (mission != null) {
                // Mettre à jour la destination si nécessaire
                if (jsonNode.has("NewDestinationStation")) {
                    mission.setDestination(jsonNode.get("NewDestinationStation").asText());
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de MissionRedirected: " + e.getMessage());
        }
    }
}
