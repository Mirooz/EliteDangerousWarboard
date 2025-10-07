package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.service.MissionService;
import be.mirooz.elitedangerous.dashboard.service.ShipTargetService;
import com.fasterxml.jackson.databind.JsonNode;

public class ShipTargetedHandler implements JournalEventHandler {
    private final ShipTargetService shipTargetService = ShipTargetService.getInstance();;

    @Override
    public String getEventType() {
        return "ShipTargeted";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            shipTargetService.registerTarget(jsonNode);
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de Bounty: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
