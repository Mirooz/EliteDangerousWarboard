package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.service.CarrierTradeService;
import be.mirooz.elitedangerous.dashboard.service.listeners.ColonisationNotificationService;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Handler pour l'événement MarketBuy du journal Elite Dangerous.
 */
public class MarketBuyHandler implements JournalEventHandler {

    private final CarrierTradeService carrierTradeService = CarrierTradeService.getInstance();

    @Override
    public void handle(JsonNode event) {
        try {
            long marketId = event.get("MarketID").asLong();
            String type = event.get("Type").asText();
            String typeLocalised = event.has("Type_Localised") ? event.get("Type_Localised").asText() : type;
            int count = event.get("Count").asInt();

            if (!carrierTradeService.isOwnCarrier(marketId)) {
                return;
            }

            String timestamp = event.path("timestamp").asText("");
            // Achat au marché du carrier => le stock du carrier diminue.
            carrierTradeService.applyMarketStockDelta(type, typeLocalised, -Math.max(count, 0), timestamp);
            ColonisationNotificationService.getInstance().notifyColonisationDataChanged();
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du traitement de l'événement MarketBuy: " + e.getMessage());
        }
    }

    @Override
    public String getEventType() {
        return "MarketBuy";
    }
}
