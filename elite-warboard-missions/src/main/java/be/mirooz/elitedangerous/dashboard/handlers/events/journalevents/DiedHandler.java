package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.model.registries.DestroyedShipsRegistery;
import com.fasterxml.jackson.databind.JsonNode;

public class DiedHandler implements JournalEventHandler {
    private final DestroyedShipsRegistery destroyedShipsRegistery = DestroyedShipsRegistery.getInstance();
    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();

    @Override
    public String getEventType() {
        return "Died";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            destroyedShipsRegistery.clearBounty();
            commanderStatus.getShip().resetCargo();

        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de Died: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
