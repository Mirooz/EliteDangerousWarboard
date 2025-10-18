package be.mirooz.elitedangerous.dashboard.model.mining;

import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.Mineral;

import java.time.LocalDateTime;

/**
 * Représente un événement de raffinage de minéral pendant une session de minage
 */
public class MiningRefinedEvent {
    
    private Mineral mineral;
    private int quantity;
    private LocalDateTime timestamp;
    
    public MiningRefinedEvent() {
    }
    
    public MiningRefinedEvent(Mineral mineral, int quantity, LocalDateTime timestamp) {
        this.mineral = mineral;
        this.quantity = quantity;
        this.timestamp = timestamp;
    }
    
    // Getters et Setters
    public Mineral getMineral() {
        return mineral;
    }
    
    public void setMineral(Mineral mineral) {
        this.mineral = mineral;
    }
    
    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return String.format("MiningRefinedEvent{mineral=%s, quantity=%d, timestamp=%s}",
                mineral != null ? mineral.getVisibleName() : "null", quantity, timestamp);
    }
}
