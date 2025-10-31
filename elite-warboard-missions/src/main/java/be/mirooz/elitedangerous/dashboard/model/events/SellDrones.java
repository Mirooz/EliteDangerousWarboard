package be.mirooz.elitedangerous.dashboard.model.events;

import lombok.Data;

/**
 * Modèle représentant l'événement SellDrones du journal Elite Dangerous
 */
@Data
public class SellDrones {
    private String timestamp;
    private String event;
    private String type;
    private int count;
    private long sellPrice;
    private long totalSale;
}
