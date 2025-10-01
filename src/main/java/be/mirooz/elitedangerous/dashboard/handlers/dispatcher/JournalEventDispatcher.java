package be.mirooz.elitedangerous.dashboard.handlers.dispatcher;

import be.mirooz.elitedangerous.dashboard.handlers.events.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.reflections.Reflections;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JournalEventDispatcher {

    // Instance unique (singleton)
    private static final JournalEventDispatcher INSTANCE = new JournalEventDispatcher();

    public static JournalEventDispatcher getInstance() {
        return INSTANCE;
    }
    private final Map<String, JournalEventHandler> handlers = new HashMap<>();

    // Constructeur privé → empêche new de l'extérieur
    private JournalEventDispatcher() {
        String packageName = JournalEventHandler.class.getPackageName();
        Reflections reflections = new Reflections(packageName);
        Set<Class<? extends JournalEventHandler>> handlerClasses =
                reflections.getSubTypesOf(JournalEventHandler.class);

        handlerClasses.forEach(clazz -> {
            try {
                JournalEventHandler handler = clazz.getDeclaredConstructor().newInstance();
                handlers.put(handler.getEventType(), handler);
            } catch (Exception e) {
                throw new RuntimeException("Failed to instantiate handler: " + clazz, e);
            }
        });
    }
    public void dispatch(JsonNode jsonNode) {
        String event = jsonNode.get("event").asText();
        JournalEventHandler handler = handlers.get(event);
        if (handler != null) {
            handler.handle(jsonNode);
        }
    }
}
