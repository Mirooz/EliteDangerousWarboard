package be.mirooz.elitedangerous.dashboard.handlers.events;

import be.mirooz.elitedangerous.dashboard.model.CommanderStatus;
import com.fasterxml.jackson.databind.JsonNode;

public class LoadGameHandler implements JournalEventHandler {

    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();

    @Override
    public String getEventType() {
        return "LoadGame";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            commanderStatus.setOnline(true);
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de Location: " + e.getMessage());
        }
    }
}
