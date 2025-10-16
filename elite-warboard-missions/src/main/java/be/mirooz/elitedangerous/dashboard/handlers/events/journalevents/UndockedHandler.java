package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.commander.CommanderStatus;
import com.fasterxml.jackson.databind.JsonNode;

public class UndockedHandler implements JournalEventHandler {

    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();

    @Override
    public String getEventType() {
        return "Undocked";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            commanderStatus.setCurrentStationName("-");
            System.out.println("Undocked");
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de Location: " + e.getMessage());
        }
    }
}
