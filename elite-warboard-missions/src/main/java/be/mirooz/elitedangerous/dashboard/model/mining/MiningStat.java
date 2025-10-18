package be.mirooz.elitedangerous.dashboard.model.mining;

import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.Mineral;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Représente une session de minage avec ses statistiques
 */
public class MiningStat {
    
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String systemName;
    private String bodyName;
    private String ringName;
    private boolean isActive;
    private List<MiningRefinedEvent> refinedMinerals;
    
    public MiningStat() {
        this.refinedMinerals = new ArrayList<>();
        this.isActive = true;
    }
    
    public MiningStat(String systemName, String bodyName, String ringName) {
        this();
        this.startDate = LocalDateTime.now();
        this.systemName = systemName;
        this.bodyName = bodyName;
        this.ringName = ringName;
    }
    
    public MiningStat(String systemName, String bodyName, String ringName, String timestamp) {
        this();
        this.startDate = parseTimestamp(timestamp);
        this.systemName = systemName;
        this.bodyName = bodyName;
        this.ringName = ringName;
    }
    
    /**
     * Parse un timestamp ISO 8601 en LocalDateTime
     */
    private LocalDateTime parseTimestamp(String timestamp) {
        if (timestamp == null) {
            return LocalDateTime.now();
        }
        try {
            // Format ISO 8601: "2025-10-20T00:52:42Z"
            return LocalDateTime.parse(timestamp.replace("Z", ""));
        } catch (Exception e) {
            System.err.println("Erreur lors du parsing du timestamp: " + timestamp + " - " + e.getMessage());
            return LocalDateTime.now();
        }
    }
    
    /**
     * Termine cette session de minage avec timestamp
     */
    public void endSession(String timestamp) {
        this.endDate = parseTimestamp(timestamp);
        this.isActive = false;
    }
    
    /**
     * Termine cette session de minage (utilise l'heure actuelle)
     */
    public void endSession() {
        endSession(null);
    }
    
    /**
     * Retire un minéral raffiné de cette session avec timestamp
     */
    public void removeRefinedMineral(Mineral mineral, int quantity, String timestamp) {
        // Trouver et retirer les événements correspondants
        List<MiningRefinedEvent> toRemove = new ArrayList<>();
        int remainingToRemove = quantity;
        
        // Parcourir les événements du plus récent au plus ancien
        for (int i = refinedMinerals.size() - 1; i >= 0 && remainingToRemove > 0; i--) {
            MiningRefinedEvent event = refinedMinerals.get(i);
            if (event.getMineral().equals(mineral)) {
                if (event.getQuantity() <= remainingToRemove) {
                    // Retirer complètement cet événement
                    toRemove.add(event);
                    remainingToRemove -= event.getQuantity();
                } else {
                    // Réduire la quantité de cet événement
                    event.setQuantity(event.getQuantity() - remainingToRemove);
                    remainingToRemove = 0;
                }
            }
        }
        
        // Supprimer les événements marqués pour suppression
        refinedMinerals.removeAll(toRemove);
        
        System.out.printf("🗑️ Retiré %d unités de %s des statistiques%n", quantity - remainingToRemove, mineral.getVisibleName());
    }
    
    /**
     * Retire un minéral raffiné de cette session (utilise l'heure actuelle)
     */
    public void removeRefinedMineral(Mineral mineral, int quantity) {
        removeRefinedMineral(mineral, quantity, null);
    }
    
    /**
     * Ajoute un minéral raffiné à cette session avec timestamp
     */
    public void addRefinedMineral(Mineral mineral, int quantity, String timestamp) {
        MiningRefinedEvent event = new MiningRefinedEvent(mineral, quantity, parseTimestamp(timestamp));
        this.refinedMinerals.add(event);
    }
    
    /**
     * Ajoute un minéral raffiné à cette session (utilise l'heure actuelle)
     */
    public void addRefinedMineral(Mineral mineral, int quantity) {
        addRefinedMineral(mineral, quantity, null);
    }
    
    /**
     * Calcule la durée totale de la session
     */
    public long getDurationInMinutes() {
        LocalDateTime end = endDate != null ? endDate : LocalDateTime.now();
        return java.time.Duration.between(startDate, end).toMinutes();
    }
    
    /**
     * Récupère le total de minéraux raffinés par type
     */
    public Map<Mineral, Integer> getTotalRefinedMinerals() {
        Map<Mineral, Integer> totals = new HashMap<>();
        for (MiningRefinedEvent event : refinedMinerals) {
            totals.merge(event.getMineral(), event.getQuantity(), Integer::sum);
        }
        return totals;
    }
    
    /**
     * Calcule la valeur totale des minéraux raffinés
     */
    public long getTotalValue() {
        long totalValue = 0;
        Map<Mineral, Integer> totals = getTotalRefinedMinerals();
        for (Map.Entry<Mineral, Integer> entry : totals.entrySet()) {
            totalValue += (long) entry.getKey().getPrice() * entry.getValue();
        }
        return totalValue;
    }
    
    // Getters et Setters
    public LocalDateTime getStartDate() {
        return startDate;
    }
    
    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }
    
    public LocalDateTime getEndDate() {
        return endDate;
    }
    
    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }
    
    public String getSystemName() {
        return systemName;
    }
    
    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }
    
    public String getBodyName() {
        return bodyName;
    }
    
    public void setBodyName(String bodyName) {
        this.bodyName = bodyName;
    }
    
    public String getRingName() {
        return ringName;
    }
    
    public void setRingName(String ringName) {
        this.ringName = ringName;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    public List<MiningRefinedEvent> getRefinedMinerals() {
        return refinedMinerals;
    }
    
    public void setRefinedMinerals(List<MiningRefinedEvent> refinedMinerals) {
        this.refinedMinerals = refinedMinerals;
    }
    
    @Override
    public String toString() {
        return String.format("MiningStat{system='%s', body='%s', ring='%s', start=%s, active=%s, minerals=%d}",
                systemName, bodyName, ringName, startDate, isActive, refinedMinerals.size());
    }
}
