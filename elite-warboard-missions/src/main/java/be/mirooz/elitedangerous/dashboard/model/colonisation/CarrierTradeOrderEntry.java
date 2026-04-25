package be.mirooz.elitedangerous.dashboard.model.colonisation;

import be.mirooz.elitedangerous.commons.lib.models.commodities.ICommodity;
import lombok.Value;

@Value
public class CarrierTradeOrderEntry {
    String timestamp;
    long carrierId;
    String carrierType;
    boolean blackMarket;
    /** Commodité résolue (journal {@code Commodity} / {@code Commodity_Localised}, CAPI, etc.). */
    ICommodity commodity;
    int purchaseOrder;
    int saleOrder;
    boolean cancelTrade;
    long price;
    int stock;

}
