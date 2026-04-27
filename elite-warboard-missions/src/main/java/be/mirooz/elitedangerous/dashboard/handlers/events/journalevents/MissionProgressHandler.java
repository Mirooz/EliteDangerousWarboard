package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.commander.Mission;
import be.mirooz.elitedangerous.dashboard.model.enums.MissionStatus;
import be.mirooz.elitedangerous.dashboard.service.listeners.MissionEventNotificationService;
import com.fasterxml.jackson.databind.JsonNode;

public class MissionProgressHandler implements JournalEventHandler {
    @Override
    public String getEventType() {
        return "MissionProgress";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            String missionId = jsonNode.get("MissionID").asText();
            Mission mission = missionList.getGlobalMissionMap().get(missionId);
            if (mission != null && mission.getStatus() == MissionStatus.ACTIVE) {
                boolean touchedProgress = false;
                // Mettre à jour la progression si disponible
                if (jsonNode.has("Progress")) {
                    int progress = jsonNode.get("Progress").asInt();
                    mission.setCurrentCount(Math.min(progress, mission.getTargetCount()));
                    touchedProgress = true;
                }
                // Essayer de récupérer le nombre total requis si pas encore défini
                if (mission.getTargetCount() == 0 && jsonNode.has("KillCount")) {
                    mission.setTargetCount(jsonNode.get("KillCount").asInt());
                }
                // setCurrentCount notifie déjà les kills ; si seul KillCount (cible) a été appliqué, rafraîchir la liste
                if (!touchedProgress && jsonNode.has("KillCount")) {
                    MissionEventNotificationService.getInstance().notifyOnMissionStatusChanged();
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de MissionProgress: " + e.getMessage());
        }
    }
}
