package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.CommanderStatus;
import com.fasterxml.jackson.databind.JsonNode;

public class DockedHandler implements JournalEventHandler {

    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();

    @Override
    public String getEventType() {
        return "Docked";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            if (jsonNode.has("StarSystem")) {
                String starSystem = jsonNode.get("StarSystem").asText();
                String stationName = jsonNode.get("StationName").asText();
                commanderStatus.setCurrentStarSystem(starSystem);
                commanderStatus.setCurrentStationName(stationName);
                System.out.println("Système actuel mis à jour: " + starSystem);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de Location: " + e.getMessage());
        }
    }
}
