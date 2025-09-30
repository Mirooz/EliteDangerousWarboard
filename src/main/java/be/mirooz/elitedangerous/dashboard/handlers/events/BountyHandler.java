package be.mirooz.elitedangerous.dashboard.handlers.events;

import be.mirooz.elitedangerous.dashboard.model.DestroyedShip;
import be.mirooz.elitedangerous.dashboard.model.DestroyedShipsList;
import be.mirooz.elitedangerous.dashboard.model.Reward;
import be.mirooz.elitedangerous.dashboard.service.MissionService;
import be.mirooz.elitedangerous.dashboard.util.DateUtil;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BountyHandler implements JournalEventHandler {
    private final MissionService missionService;
    private final DestroyedShipsList destroyedShipsList;

    public BountyHandler() {
        this.missionService = MissionService.getInstance();
        this.destroyedShipsList = DestroyedShipsList.getInstance();
    }

    @Override
    public String getEventType() {
        return "Bounty";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            System.out.println("Bounty event");
            // Trouver toutes les missions actives de massacre pour cette faction cible
            missionService.updateKillsCount(jsonNode);
            missionService.updateTargetRewards(jsonNode);
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de Bounty: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
