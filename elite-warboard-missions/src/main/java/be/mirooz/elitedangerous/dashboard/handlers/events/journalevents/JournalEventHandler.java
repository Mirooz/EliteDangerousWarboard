package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.registries.MissionsRegistry;
import com.fasterxml.jackson.databind.JsonNode;

public interface JournalEventHandler {
    final MissionsRegistry missionList = MissionsRegistry.getInstance();
    String getEventType();

    void handle(JsonNode jsonNode);
}
