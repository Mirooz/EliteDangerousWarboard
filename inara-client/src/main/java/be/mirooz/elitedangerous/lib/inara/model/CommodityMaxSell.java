package be.mirooz.elitedangerous.lib.inara.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Modèle pour stocker le prix max de vente d'une commodité depuis Inara
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommodityMaxSell {
    private String commodityName;
    private String inaraId;
    private int maxSellPrice;
    
    public CommodityMaxSell(String commodityName, String inaraId) {
        this.commodityName = commodityName;
        this.inaraId = inaraId;
        this.maxSellPrice = 0;
    }
}

