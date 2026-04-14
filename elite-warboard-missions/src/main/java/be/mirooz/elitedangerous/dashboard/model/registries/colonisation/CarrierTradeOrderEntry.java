package be.mirooz.elitedangerous.dashboard.model.registries.colonisation;

import lombok.Value;

@Value
public class CarrierTradeOrderEntry {
    String timestamp;
    long carrierId;
    String carrierType;
    boolean blackMarket;
    String commodityLocalised;
    int purchaseOrder;
    int saleOrder;
    boolean cancelTrade;
    long price;
}
