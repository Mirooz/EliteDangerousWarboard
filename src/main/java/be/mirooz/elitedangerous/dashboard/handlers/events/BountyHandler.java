package be.mirooz.elitedangerous.dashboard.handlers.events;

import be.mirooz.elitedangerous.dashboard.service.DashboardService;
import be.mirooz.elitedangerous.dashboard.service.MissionService;
import com.fasterxml.jackson.databind.JsonNode;

public class BountyHandler implements JournalEventHandler {
    private final MissionService missionService;

    public BountyHandler() {
        this.missionService = MissionService.getInstance();

    }

    @Override
    public String getEventType() {
        return "Bounty";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            String victimFaction = jsonNode.has("VictimFaction") ? jsonNode.get("VictimFaction").asText() : "";
            int reward = jsonNode.has("TotalReward") ? jsonNode.get("TotalReward").asInt() : 0;

            System.out.println("Bounty event - VictimFaction: " + victimFaction + ", Reward: " + reward);
            // Trouver toutes les missions actives de massacre pour cette faction cible
            if (missionService.updateKillsCount(missionList.getGlobalMissionMap().values(),victimFaction)) return; // Aucune mission éligible
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de Bounty: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
