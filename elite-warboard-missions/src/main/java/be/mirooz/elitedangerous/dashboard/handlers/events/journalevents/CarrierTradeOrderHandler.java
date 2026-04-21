package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.commons.lib.models.commodities.CarrierCommodityResolver;
import be.mirooz.elitedangerous.commons.lib.models.commodities.ICommodity;
import be.mirooz.elitedangerous.dashboard.model.colonisation.CarrierTradeOrderEntry;
import be.mirooz.elitedangerous.dashboard.service.CarrierTradeService;
import be.mirooz.elitedangerous.dashboard.service.listeners.ColonisationNotificationService;
import com.fasterxml.jackson.databind.JsonNode;

public class CarrierTradeOrderHandler implements JournalEventHandler {

    private final CarrierTradeService carrierTradeService = CarrierTradeService.getInstance();

    @Override
    public String getEventType() {
        return "CarrierTradeOrder";
    }

    @Override
    public void handle(JsonNode jsonNode) {
        try {
            String timestamp = jsonNode.path("timestamp").asText("");
            long carrierId = jsonNode.path("CarrierID").asLong();
            String carrierType = jsonNode.path("CarrierType").asText("");
            boolean blackMarket = jsonNode.path("BlackMarket").asBoolean(false);
            String commodity = jsonNode.path("Commodity").asText("");
            String commodityLocalised = jsonNode.path("Commodity_Localised").asText("");
            if (commodityLocalised.isBlank() && !commodity.isBlank()) {
                commodityLocalised = commodity;
            }
            int purchaseOrder = jsonNode.path("PurchaseOrder").asInt(0);
            int saleOrder = jsonNode.path("SaleOrder").asInt(0);
            boolean cancelTrade = jsonNode.path("CancelTrade").asBoolean(false);
            long price = jsonNode.path("Price").asLong();
            int stock = jsonNode.has("Stock")
                    ? jsonNode.path("Stock").asInt(0)
                    : jsonNode.path("TotalStock").asInt(0);

            if (!carrierTradeService.isOwnCarrier(carrierId)) {
                return;
            }

            ICommodity resolved = CarrierCommodityResolver.resolve(commodity, commodityLocalised);
            if (resolved != null && commodityLocalised != null && !commodityLocalised.isBlank()) {
                resolved.setLocalisedName(commodityLocalised);
            }

            carrierTradeService.recordTradeOrder(new CarrierTradeOrderEntry(
                    timestamp, carrierId, carrierType, blackMarket, resolved,
                    purchaseOrder, saleOrder, cancelTrade, price, stock));
            ColonisationNotificationService.getInstance().notifyColonisationDataChanged();

            System.out.println("CarrierTradeOrder: " + commodityLocalised + " (" + commodity + ")"
                    + " (PurchaseOrder=" + purchaseOrder
                    + ", SaleOrder=" + saleOrder
                    + ", CancelTrade=" + cancelTrade + ")"
                    + " @ " + price + " cr, stock=" + stock + " (CarrierID=" + carrierId + ")");
        } catch (Exception e) {
            System.err.println("Erreur lors du traitement de CarrierTradeOrder: " + e.getMessage());
        }
    }
}
