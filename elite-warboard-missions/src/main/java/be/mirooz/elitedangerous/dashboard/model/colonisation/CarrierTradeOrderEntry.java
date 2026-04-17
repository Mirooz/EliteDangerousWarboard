package be.mirooz.elitedangerous.dashboard.model.colonisation;

import lombok.Value;

@Value
public class CarrierTradeOrderEntry {
    String timestamp;
    long carrierId;
    String carrierType;
    boolean blackMarket;
    /** Identifiant journal {@code Commodity} (ex. {@code cmmcomposite}). */
    String commodity;
    /** Libellé {@code Commodity_Localised}. */
    String commodityLocalised;
    int purchaseOrder;
    int saleOrder;
    boolean cancelTrade;
    long price;
    int stock;
}
