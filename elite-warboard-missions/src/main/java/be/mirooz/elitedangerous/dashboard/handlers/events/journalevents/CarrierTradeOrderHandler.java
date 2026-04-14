package be.mirooz.elitedangerous.dashboard.handlers.events.journalevents;

import be.mirooz.elitedangerous.dashboard.model.registries.colonisation.CarrierTradeOrderEntry;
import be.mirooz.elitedangerous.dashboard.model.registries.colonisation.CarrierTradeOrderRegistry;
import com.fasterxml.jackson.databind.JsonNode;

public class CarrierTradeOrderHandler implements JournalEventHandler {

    private final CarrierTradeOrderRegistry carrierTradeOrderRegistry = CarrierTradeOrderRegistry.getInstance();

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
            String commodity = jsonNode.path("Commodity_Localised").asText(jsonNode.path("Commodity").asText(""));
            int purchaseOrder = jsonNode.path("PurchaseOrder").asInt(0);
            int saleOrder = jsonNode.path("SaleOrder").asInt(0);
            boolean cancelTrade = jsonNode.path("CancelTrade").asBoolean(false);
            long price = jsonNode.path("Price").asLong();

            carrierTradeOrderRegistry.record(new CarrierTradeOrderEntry(
                    timestamp, carrierId, carrierType, blackMarket, commodity, purchaseOrder, saleOrder, cancelTrade, price));

            System.out.println("CarrierTradeOrder: " + commodity
                    + " (PurchaseOrder=" + purchaseOrder
                    + ", SaleOrder=" + saleOrder
                    + ", CancelTrade=" + cancelTrade + ")"
                    + " @ " + price + " cr (CarrierID=" + carrierId + ")");
        } catch (Exception e) {
            System.err.println("Erreur lors du traitement de CarrierTradeOrder: " + e.getMessage());
        }
    }
}
