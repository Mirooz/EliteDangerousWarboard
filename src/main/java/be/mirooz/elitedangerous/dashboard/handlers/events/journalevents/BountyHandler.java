package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.service.MissionService;
import com.fasterxml.jackson.databind.JsonNode;

public class BountyHandler implements JournalEventHandler {
    private final MissionService missionService = MissionService.getInstance();;

    @Override
    public String getEventType() {
        return "Bounty";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            // Trouver toutes les missions actives de massacre pour cette faction cible
            missionService.updateKillsCount(jsonNode);
            missionService.updateTargetRewards(jsonNode);
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de Bounty: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
