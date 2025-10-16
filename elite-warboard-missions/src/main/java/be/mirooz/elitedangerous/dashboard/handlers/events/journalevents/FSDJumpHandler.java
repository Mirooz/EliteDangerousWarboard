package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.commander.CommanderStatus;
import com.fasterxml.jackson.databind.JsonNode;

public class FSDJumpHandler implements JournalEventHandler {

    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();

    @Override
    public String getEventType() {
        return "FSDJump";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            if (jsonNode.has("StarSystem")) {
                String starSystem = jsonNode.get("StarSystem").asText();
                commanderStatus.setCurrentStarSystem(starSystem);
                System.out.println("FSD Jump detected to - " + starSystem);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de Location: " + e.getMessage());
        }
    }
}
