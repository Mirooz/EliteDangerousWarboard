package be.mirooz.elitedangerous.dashboard.model.colonisation;

import be.mirooz.elitedangerous.dashboard.model.registries.commander.CommanderStatus;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Système courant pour les événements colonisation : champs journal si présents, sinon {@link CommanderStatus}.
 */
public final class ColonisationJournalContext {

    private ColonisationJournalContext() {
    }

    public static String resolveStarSystem(JsonNode event, CommanderStatus commanderStatus) {
        if (event == null) {
            return "";
        }
        String fromEvent = event.path("StarSystem").asText("");
        if (!fromEvent.isBlank()) {
            return fromEvent;
        }
        String current = commanderStatus.getCurrentStarSystem();
        return current != null && !current.isBlank() ? current : "";
    }

    public static long resolveSystemAddress(JsonNode event) {
        if (event == null) {
            return 0L;
        }
        return event.path("SystemAddress").asLong(0L);
    }
}
