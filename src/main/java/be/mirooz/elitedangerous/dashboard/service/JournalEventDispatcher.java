package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.dashboard.handlers.events.*;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JournalEventDispatcher {

    // Instance unique (singleton)
    private static final JournalEventDispatcher INSTANCE = new JournalEventDispatcher();

    public static JournalEventDispatcher getInstance() {
        return INSTANCE;
    }
    private final Map<String, JournalEventHandler> handlers = new HashMap<>();

    // Constructeur privé → empêche new de l'extérieur
    private JournalEventDispatcher() {
        List<JournalEventHandler> handlerList = List.of(
                new MissionAcceptedHandler(),
                new MissionCompletedHandler(),
                new MissionAbandonedHandler(),
                new MissionRedirectedHandler(),
                new BountyHandler(),
                new FactionKillBondHandler(),
                new MissionProgressHandler()
        );

        handlerList.forEach(h -> handlers.put(h.getEventType(), h));
    }

    public void dispatch(JsonNode jsonNode) {
        String event = jsonNode.get("event").asText();
        JournalEventHandler handler = handlers.get(event);
        if (handler != null) {
            handler.handle(jsonNode);
        }
    }
}
