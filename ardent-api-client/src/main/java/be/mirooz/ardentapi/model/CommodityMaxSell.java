package be.mirooz.ardentapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Modèle pour stocker le prix max de vente d'une commodité depuis Inara
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommodityMaxSell {
    private String commodityName;
    private int maxSellPrice;

}

