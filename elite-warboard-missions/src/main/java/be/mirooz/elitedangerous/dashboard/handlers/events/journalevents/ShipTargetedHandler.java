package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.service.AutoPowerplantKeyService;
import be.mirooz.elitedangerous.dashboard.service.ShipTargetService;
import be.mirooz.elitedangerous.dashboard.view.common.context.DashboardContext;
import com.fasterxml.jackson.databind.JsonNode;

public class ShipTargetedHandler implements JournalEventHandler {
    private final ShipTargetService shipTargetService = ShipTargetService.getInstance();
    private final AutoPowerplantKeyService autoPowerplantKeyService = AutoPowerplantKeyService.getInstance();

    @Override
    public String getEventType() {
        return "ShipTargeted";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            if (!DashboardContext.getInstance().isBatchLoading()) {
                autoPowerplantKeyService.onShipTargeted(jsonNode);
            }
            shipTargetService.registerTarget(jsonNode);
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de Bounty: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
