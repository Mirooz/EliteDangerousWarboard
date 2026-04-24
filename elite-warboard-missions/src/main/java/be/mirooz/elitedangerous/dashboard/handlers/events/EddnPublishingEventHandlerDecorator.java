package be.mirooz.elitedangerous.dashboard.handlers.events;

import be.mirooz.elitedangerous.dashboard.handlers.events.journalevents.JournalEventHandler;
import be.mirooz.elitedangerous.dashboard.service.webservice.eddn.EddnJournalPublisher;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Décorateur qui, après traitement métier de {@code delegate}, relaie l'événement au
 * {@link EddnJournalPublisher} pour publication EDDN.
 *
 * <p>Appliqué uniformément sur tous les handlers (et le handler par défaut) dans le
 * {@code JournalEventDispatcher}. Les événements non couverts par un schéma EDDN tombent
 * naturellement dans le {@code default: break;} de {@code EddnJournalPublisher.route()} ;
 * c'est donc safe d'appeler {@code publish()} pour chaque événement.</p>
 *
 * <p>La gate effective d'envoi (batch loading, préférence utilisateur, FID manquant) reste
 * gérée par {@link be.mirooz.elitedangerous.dashboard.service.webservice.eddn.EddnUploader#isPublishingAllowed}.</p>
 */
public class EddnPublishingEventHandlerDecorator implements JournalEventHandler {

    private final JournalEventHandler delegate;

    public EddnPublishingEventHandlerDecorator(JournalEventHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getEventType() {
        return delegate.getEventType();
    }

    @Override
    public void handle(JsonNode jsonNode) {
        delegate.handle(jsonNode);
        EddnJournalPublisher.getInstance().publish(jsonNode);
    }
}
