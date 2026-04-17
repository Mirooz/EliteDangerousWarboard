package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.registries.commander.CommanderStatus;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Met à jour la version du client (utile pour l'en-tête EDDN).
 */
public class FileheaderHandler implements JournalEventHandler {

    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();

    @Override
    public String getEventType() {
        return "Fileheader";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            if (jsonNode.has("gameversion")) {
                commanderStatus.setGameVersion(jsonNode.get("gameversion").asText());
            }
            if (jsonNode.has("build")) {
                commanderStatus.setGameBuild(jsonNode.get("build").asText());
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing de Fileheader: " + e.getMessage());
        }
    }
}
