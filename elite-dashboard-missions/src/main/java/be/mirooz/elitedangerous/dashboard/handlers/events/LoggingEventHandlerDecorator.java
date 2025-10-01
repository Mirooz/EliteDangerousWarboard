package be.mirooz.elitedangerous.dashboard.handlers.events;

import be.mirooz.elitedangerous.dashboard.handlers.events.journalevents.JournalEventHandler;
import com.fasterxml.jackson.databind.JsonNode;

public class LoggingEventHandlerDecorator implements JournalEventHandler {
    private final JournalEventHandler delegate;

    public LoggingEventHandlerDecorator(JournalEventHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getEventType() {
        return delegate.getEventType();
    }

    @Override
    public void handle(JsonNode jsonNode) {
        System.out.println("***** Event " + getEventType() + " *****");
        delegate.handle(jsonNode);
    }
}
