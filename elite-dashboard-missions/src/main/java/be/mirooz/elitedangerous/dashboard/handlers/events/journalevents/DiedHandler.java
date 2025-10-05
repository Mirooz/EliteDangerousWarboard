package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.DestroyedShipsRegistery;
import be.mirooz.elitedangerous.dashboard.service.MissionService;
import com.fasterxml.jackson.databind.JsonNode;

public class DiedHandler implements JournalEventHandler {
    private final DestroyedShipsRegistery destroyedShipsRegistery = DestroyedShipsRegistery.getInstance();;

    @Override
    public String getEventType() {
        return "Died";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            destroyedShipsRegistery.clearBounty();
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de Died: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
