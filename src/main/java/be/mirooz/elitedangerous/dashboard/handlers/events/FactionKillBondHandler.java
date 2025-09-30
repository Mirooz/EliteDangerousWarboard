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
            System.out.println("FactionKillBond event");
            missionService.updateKillsCount(jsonNode);
            missionService.updateTargetRewards(jsonNode);
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de FactionKillBond: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
