package be.mirooz.elitedangerous.dashboard.view.fleetcarrier;

import be.mirooz.elitedangerous.commons.lib.models.commodities.ColonisationCommodityKeys;
import be.mirooz.elitedangerous.commons.lib.models.commodities.ICommodity;
import lombok.Getter;

/**
 * Une ligne du tableau marché / stock Fleet Carrier (même modèle que la grille colonisation).
 */
@Getter
public final class FleetCarrierMarketRow {
    private final ICommodity commodity;
    private final String displayName;
    private final int stock;
    private final int purchaseOrder;
    private final int saleOrder;
    /** Crédits par tonne offerts par le carrier pour un ordre d'achat, 0 si pas d'ordre d'achat. */
    private final long price;
    private final int missing;

    public FleetCarrierMarketRow(ICommodity commodity, String displayName, int stock, int purchaseOrder, int saleOrder, long price) {
        this(commodity, displayName, stock, purchaseOrder, saleOrder, price, 0);
    }

    public FleetCarrierMarketRow(ICommodity commodity, String displayName, int stock, int purchaseOrder, int saleOrder, long price, int missing) {
        this.commodity = commodity;
        String fallback =
                commodity != null && commodity.getCargoJsonName() != null ? commodity.getCargoJsonName() : "";
        this.displayName = displayName != null && !displayName.isBlank() ? displayName : fallback;
        this.stock = stock;
        this.purchaseOrder = purchaseOrder;
        this.saleOrder = saleOrder;
        this.price = price;
        this.missing = missing;
    }

    public FleetCarrierMarketRow withStock(int newStock) {
        return new FleetCarrierMarketRow(commodity, displayName, newStock, purchaseOrder, saleOrder, price, missing);
    }

    public FleetCarrierMarketRow withMissing(int newMissing) {
        return new FleetCarrierMarketRow(commodity, displayName, stock, purchaseOrder, saleOrder, price, newMissing);
    }

    /** Clé de fusion avec les ressources chantier (même logique que l'UI colonisation). */
    public String getCommodityKey() {
        return ColonisationCommodityKeys.mergeKey(commodity);
    }

    public long getCarrierPurchaseBidPerTonCr() {
        return purchaseOrder > 0 ? price : 0L;
    }
}
