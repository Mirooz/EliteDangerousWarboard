package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import com.fasterxml.jackson.databind.JsonNode;

public class DefaultJournalEventHandler implements JournalEventHandler {

    @Override
    public String getEventType() {
        return "DEFAULT"; // ou "DEFAULT" — ce n'est jamais réellement utilisé pour matcher
    }

    @Override
    public void handle(JsonNode jsonNode) {
        String eventType = jsonNode.path("event").asText("UNKNOWN");
      //  System.out.println("[JournalEvent] Aucun handler trouvé pour l'événement : " + eventType);
    }
}
