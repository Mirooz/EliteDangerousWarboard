package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

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
            missionService.updatFactionKillCount(jsonNode);
            missionService.updateFactionRewards(jsonNode);
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de FactionKillBond: " + e.getMessage());
            e.printStackTrace();

            //467 220 reel
            //114 025 loggé (4 cilbe)
        }
    }
}
