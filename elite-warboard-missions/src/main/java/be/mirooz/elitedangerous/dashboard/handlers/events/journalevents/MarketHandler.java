package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.service.webservice.CapiApiService;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * À l'ouverture du marché (journal {@code Market}) : envoi EDDN / Inara via {@link CapiApiService}.
 */
public class MarketHandler implements JournalEventHandler {

    @Override
    public String getEventType() {
        return "Market";
    }

    @Override
    public void handle(JsonNode event) {
    }
}
