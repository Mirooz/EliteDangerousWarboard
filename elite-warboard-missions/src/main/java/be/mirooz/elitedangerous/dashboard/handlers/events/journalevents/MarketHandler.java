package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.service.webservice.eddn.EddnJournalPublisher;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * À l'ouverture du marché (journal {@code Market}) : relais vers EDDN via lecture du fichier
 * {@code Market.json}.
 */
public class MarketHandler implements JournalEventHandler {

    @Override
    public String getEventType() {
        return "Market";
    }

    @Override
    public void handle(JsonNode event) {
        EddnJournalPublisher.getInstance().publish(event);
    }
}
