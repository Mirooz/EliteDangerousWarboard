package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.commander.Mission;
import be.mirooz.elitedangerous.dashboard.model.enums.MissionStatus;
import be.mirooz.elitedangerous.dashboard.service.CombatMissionHistoryService;
import be.mirooz.elitedangerous.dashboard.service.listeners.MissionEventNotificationService;
import com.fasterxml.jackson.databind.JsonNode;

public class MissionCompletedHandler implements JournalEventHandler {
    
    private final CombatMissionHistoryService historyService = CombatMissionHistoryService.getInstance();
    
    @Override
    public String getEventType() {
        return "MissionCompleted";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            String missionId = jsonNode.get("MissionID").asText();
            Mission mission = missionList.getGlobalMissionMap().get(missionId);
            if (mission != null) {
                mission.setStatus(MissionStatus.COMPLETED);
                // Pour les missions complétées, mettre le compteur au maximum
                // Si targetCount est 0, essayer de le récupérer depuis l'événement
                if (mission.getTargetCount() == 0 && jsonNode.has("KillCount")) {
                    mission.setTargetCount(jsonNode.get("KillCount").asInt());
                }
                mission.setCurrentCount(mission.getTargetCount());
                
                // Enregistrer dans l'historique et notifier
                historyService.registerCompletedMission(mission);
                MissionEventNotificationService.getInstance().notifyOnMissionStatusChanged();
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de MissionCompleted: " + e.getMessage());
        }
    }
}
