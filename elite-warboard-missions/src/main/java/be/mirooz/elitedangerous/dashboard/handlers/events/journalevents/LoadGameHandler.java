package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.registries.commander.CommanderStatus;
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
            if (jsonNode.has("gameversion")) {
                commanderStatus.setGameVersion(jsonNode.get("gameversion").asText());
            }
            if (jsonNode.has("build")) {
                commanderStatus.setGameBuild(jsonNode.get("build").asText());
            }
            if (jsonNode.has("Horizons")) {
                commanderStatus.setHorizons(jsonNode.get("Horizons").asBoolean());
            }
            if (jsonNode.has("Odyssey")) {
                commanderStatus.setOdyssey(jsonNode.get("Odyssey").asBoolean());
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de Location: " + e.getMessage());
        }
    }
}
