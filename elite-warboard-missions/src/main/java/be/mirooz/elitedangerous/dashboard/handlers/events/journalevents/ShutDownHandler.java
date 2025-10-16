package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.commander.CommanderStatus;
import com.fasterxml.jackson.databind.JsonNode;

public class ShutDownHandler implements JournalEventHandler {

    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();

    @Override
    public String getEventType() {
        return "Shutdown";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            commanderStatus.setOnline(false);
            System.out.println("Commandant " + commanderStatus.getCommanderName() + " is offline");

        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de Location: " + e.getMessage());
        }
    }
}
