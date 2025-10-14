package be.mirooz.elitedangerous.dashboard.model.events;

import lombok.Data;

/**
 * Modèle représentant l'événement BuyDrones du journal Elite Dangerous
 */
@Data
public class BuyDrones {
    private String timestamp;
    private String event;
    private String type;
    private int count;
    private long buyPrice;
    private long totalCost;
}
