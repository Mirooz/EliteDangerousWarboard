package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.model.CommanderStatus;
import com.fasterxml.jackson.databind.JsonNode;

public class CommanderHandler implements JournalEventHandler {

    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();

    @Override
    public String getEventType() {
        return "Commander";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            if (jsonNode.has("Name")) {
                String name = jsonNode.get("Name").asText();
                String fid = jsonNode.get("FID").asText();
                commanderStatus.setCommanderName(name);
                commanderStatus.setFID(fid);
                System.out.println("Commandant - " + name  + " - " + fid);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de Location: " + e.getMessage());
        }
    }
}
