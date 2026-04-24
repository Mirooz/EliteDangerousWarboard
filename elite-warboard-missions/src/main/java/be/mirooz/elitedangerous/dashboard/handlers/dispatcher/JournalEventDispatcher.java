package be.mirooz.elitedangerous.dashboard.handlers.dispatcher;

import be.mirooz.elitedangerous.dashboard.handlers.events.EddnPublishingEventHandlerDecorator;
import be.mirooz.elitedangerous.dashboard.handlers.events.LoggingEventHandlerDecorator;
import be.mirooz.elitedangerous.dashboard.handlers.events.journalevents.DefaultJournalEventHandler;
import be.mirooz.elitedangerous.dashboard.handlers.events.journalevents.JournalEventHandler;
import be.mirooz.elitedangerous.dashboard.service.journal.watcher.JournalFileTracker;
import be.mirooz.elitedangerous.dashboard.service.persistence.PersistenceService;
import com.fasterxml.jackson.databind.JsonNode;
import org.reflections.Reflections;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.*;

public class JournalEventDispatcher {

    // Instance unique (singleton)
    private static final JournalEventDispatcher INSTANCE = new JournalEventDispatcher();

    /**
     * Handler utilisé pour les events qu'aucun handler métier ne couvre.
     * Décoré par {@link EddnPublishingEventHandlerDecorator} : ça permet aux events purement
     * "relais EDDN" (ApproachSettlement, CodexEntry, DockingDenied/Granted, Location, etc.)
     * d'être publiés sans avoir à créer un handler dédié.
     */
    private final JournalEventHandler defaultHandler =
            new EddnPublishingEventHandlerDecorator(new DefaultJournalEventHandler());
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

        // Étape 2 : décoration. Ordre d'exécution (extérieur -> intérieur) :
        //   Logging -> EddnPublishing -> handler métier.
        // => log de l'event, puis handler, puis publication EDDN, puis refreshUI (fin du log).
        tempHandlers.forEach((key, handler) ->
                handlers.put(key, new LoggingEventHandlerDecorator(
                        new EddnPublishingEventHandlerDecorator(handler)))
        );
    }
    public void dispatch(JsonNode jsonNode) {
        String event = jsonNode.get("event").asText();
        JournalEventHandler handler = handlers.getOrDefault(event, defaultHandler);
        if (handler != null) {
            handler.handle(jsonNode);
        }
        updateResumeCursor(jsonNode);
    }

    /**
     * Met à jour le curseur de reprise en mémoire après chaque dispatch.
     *
     * <p>On ne persiste rien ici : la politique choisie est "save only on shutdown" (cf.
     * {@link PersistenceService#saveAllNow()} appelé depuis le close handler de
     * {@code EliteDashboardApp} et depuis le shutdown hook JVM). Ça évite de spammer le disque
     * à chaque event et les race conditions entre save debouncé et {@code loadAll()} sur des
     * snapshots volumineux.</p>
     */
    private void updateResumeCursor(JsonNode jsonNode) {
        JsonNode tsNode = jsonNode.get("timestamp");
        if (tsNode == null || tsNode.isNull()) {
            return;
        }
        String timestamp = tsNode.asText();
        if (timestamp == null || timestamp.isBlank()) {
            return;
        }
        String fileName = null;
        File currentFile = JournalFileTracker.getInstance().getCurrentFile();
        if (currentFile != null) {
            fileName = currentFile.getName();
        }
        PersistenceService.getInstance().updateCursor(timestamp, fileName);
    }
}
