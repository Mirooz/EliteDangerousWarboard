package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.service.webservice.eddn.EddnJournalPublisher;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Base pour les handlers qui n'ont qu'un rôle de relais vers EDDN (pas de logique métier locale).
 * Chaque sous-classe déclare uniquement son {@link #getEventType()}.
 */
public abstract class AbstractEddnOnlyJournalEventHandler implements JournalEventHandler {

    @Override
    public void handle(JsonNode jsonNode) {
        EddnJournalPublisher.getInstance().publish(jsonNode);
    }
}
