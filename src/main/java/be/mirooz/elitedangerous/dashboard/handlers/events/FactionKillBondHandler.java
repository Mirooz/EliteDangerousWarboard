package be.mirooz.elitedangerous.dashboard.handlers.events;

import be.mirooz.elitedangerous.dashboard.service.DashboardService;
import be.mirooz.elitedangerous.dashboard.service.MissionService;
import com.fasterxml.jackson.databind.JsonNode;

public class FactionKillBondHandler implements JournalEventHandler {
    private final MissionService missionService;

    public FactionKillBondHandler() {
        this.missionService = MissionService.getInstance();
    }

    @Override
    public String getEventType() {
        return "FactionKillBond";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            String victimFaction = jsonNode.has("VictimFaction") ? jsonNode.get("VictimFaction").asText() : "";
            int reward = jsonNode.has("Reward") ? jsonNode.get("Reward").asInt() : 0;

            System.out.println("FactionKillBond event - VictimFaction: " + victimFaction + ", Reward: " + reward);

            // Utiliser la même logique que handleBounty
            if (missionService.updateKillsCount(missionList.getGlobalMissionMap().values(), victimFaction))
                return; // Aucune mission éligible
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de FactionKillBond: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
