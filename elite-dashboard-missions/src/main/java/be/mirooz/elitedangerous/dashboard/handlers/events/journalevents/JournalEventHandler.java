package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.MissionsList;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public interface JournalEventHandler {
    final MissionsList missionList = MissionsList.getInstance();
    String getEventType();

    void handle(JsonNode jsonNode);
}
