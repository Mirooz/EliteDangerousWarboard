package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.CommanderStatus;
import be.mirooz.elitedangerous.lib.inara.model.commodities.ICommodityFactory;
import com.fasterxml.jackson.databind.JsonNode;

import static be.mirooz.elitedangerous.lib.inara.model.commodities.ICommodity.CommodityType.MINERAL;

/**
 * Handler pour l'événement MarketSell du journal Elite Dangerous
 * <p>
 * Exemple d'événement :
 * {
 * "timestamp" : "2025-10-10T00:05:47Z",
 * "event" : "MarketSell",
 * "MarketID" : 3229983232,
 * "Type" : "opal",
 * "Type_Localised" : "Opale du vide",
 * "Count" : 77,
 * "SellPrice" : 945937,
 * "TotalSale" : 72837149,
 * "AvgPricePaid" : 0
 * }
 */
public class MarketSellHandler implements JournalEventHandler {

    CommanderStatus commanderStatus = CommanderStatus.getInstance();

    @Override
    public void handle(JsonNode event) {
        try {
            String timestamp = event.get("timestamp").asText();
            long marketId = event.get("MarketID").asLong();
            String type = event.get("Type").asText();
            String typeLocalised = event.has("Type_Localised") ? event.get("Type_Localised").asText() : type;
            int count = event.get("Count").asInt();
            int sellPrice = event.get("SellPrice").asInt();
            long totalSale = event.get("TotalSale").asLong();
            int avgPricePaid = event.has("AvgPricePaid") ? event.get("AvgPricePaid").asInt() : 0;

            System.out.printf("💰 MarketSell: %s (%s) x%d at %d Cr each, Total: %d Cr at %s%n", typeLocalised, type, count, sellPrice, totalSale, timestamp);

            // Retirer la commodité du cargo du vaisseau
            //TODO Bug avec mineral qui disparait, vider tout en attendant https://issues.frontierstore.net/issue-detail/79309
//            ICommodityFactory.ofByCargoJson(type)
//                    .ifPresent(commodity -> commanderStatus.getShip().removeCommodity(commodity, count));
            ICommodityFactory.ofByCargoJson(type).ifPresent(commodity -> {
                if (MINERAL.equals(commodity.getCommodityType())) {
                    commanderStatus.getShip().removeAllCommodity(commodity);
                } else {
                    commanderStatus.getShip().removeCommodity(commodity, count);
                }
            });
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du traitement de l'événement MarketSell: " + e.getMessage());
        }
    }

    @Override
    public String getEventType() {
        return "MarketSell";
    }
}
