package be.mirooz.elitedangerous.dashboard.handlers.dispatcher;

import be.mirooz.elitedangerous.dashboard.handlers.events.LoggingEventHandlerDecorator;
import be.mirooz.elitedangerous.dashboard.handlers.events.journalevents.DefaultJournalEventHandler;
import be.mirooz.elitedangerous.dashboard.handlers.events.journalevents.JournalEventHandler;
import com.fasterxml.jackson.databind.JsonNode;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.*;

public class JournalEventDispatcher {

    // Instance unique (singleton)
    private static final JournalEventDispatcher INSTANCE = new JournalEventDispatcher();

    private final JournalEventHandler defaultHandler = new DefaultJournalEventHandler();
    public static JournalEventDispatcher getInstance() {
        return INSTANCE;
    }
    private final Map<String, JournalEventHandler> handlers = new HashMap<>();

    private JournalEventDispatcher() {
        String packageName = JournalEventHandler.class.getPackageName();
        Reflections reflections = new Reflections(packageName);
        Set<Class<? extends JournalEventHandler>> handlerClasses =
                reflections.getSubTypesOf(JournalEventHandler.class);

        Map<String, JournalEventHandler> tempHandlers = new HashMap<>();
        handlerClasses.stream()
                .filter(clazz -> !Modifier.isAbstract(clazz.getModifiers()))
                .forEach(clazz -> {
                    try {
                        JournalEventHandler handler = clazz.getDeclaredConstructor().newInstance();
                        tempHandlers.put(handler.getEventType(), handler);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to instantiate handler: " + clazz, e);
                    }
                });

        // Étape 2 : décoration
        tempHandlers.forEach((key, handler) ->
                handlers.put(key, new LoggingEventHandlerDecorator(handler))
        );
    }
    public void dispatch(JsonNode jsonNode) {
        String event = jsonNode.get("event").asText();
        JournalEventHandler handler = handlers.getOrDefault(event, defaultHandler);
        if (handler != null) {
            handler.handle(jsonNode);
        }
    }
}
