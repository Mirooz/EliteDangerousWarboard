package be.mirooz.elitedangerous.dashboard.model.mining;

import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.Mineral;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Représente une session de minage avec ses statistiques
 */
@Data
public class MiningStat {
    
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String systemName;
    private String bodyName;
    private String ringName;
    private boolean isActive;
    private boolean isSuspended;
    private List<MiningRefinedEvent> refinedMinerals;
    private List<SuspensionPeriod> suspensionPeriods;
    private SuspensionPeriod currentSuspension;
    private boolean isCoreSession = false;
    
    public MiningStat() {
        this.refinedMinerals = new ArrayList<>();
        this.suspensionPeriods = new ArrayList<>();
        this.isActive = true;
        this.isSuspended = false;
        this.currentSuspension = null;
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
        this.isSuspended = false;
    }
    
    /**
     * Termine cette session de minage (utilise l'heure actuelle)
     */
    public void endSession() {
        endSession(null);
    }
    
    /**
     * Suspend cette session de minage avec timestamp
     */
    public void suspendSession(String timestamp) {
        // Créer une nouvelle période de suspension
        this.currentSuspension = SuspensionPeriod.createSuspension(timestamp);
        this.isSuspended = true;
        this.isActive = false; // Une session suspendue n'est plus active
        System.out.println("⏸️ Session suspendue: " + this.currentSuspension.getSuspendDate());
    }
    
    /**
     * Suspend cette session de minage (utilise l'heure actuelle)
     */
    public void suspendSession() {
        suspendSession(null);
    }
    
    /**
     * Reprend cette session de minage avec timestamp
     */
    public void resumeSession(String timestamp) {
        if (this.currentSuspension != null) {
            // Terminer la période de suspension actuelle
            this.currentSuspension.endSuspension(timestamp);
            this.suspensionPeriods.add(this.currentSuspension);
            System.out.println("▶️ Session reprise: " + this.currentSuspension.getResumeDate() + 
                             " (durée suspension: " + this.currentSuspension.getDurationInMinutes() + " min)");
            this.currentSuspension = null;
        }
        this.isSuspended = false;
        this.isActive = true; // Une session reprise redevient active
    }
    
    /**
     * Reprend cette session de minage (utilise l'heure actuelle)
     */
    public void resumeSession() {
        resumeSession(null);
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
     * Calcule la durée totale de la session (en excluant toutes les périodes de suspension)
     */
    public long getDurationInMinutes() {
        LocalDateTime end = endDate != null ? endDate : LocalDateTime.now();
        
        // Si la session est suspendue, utiliser la date de suspension comme fin
        if (isSuspended && currentSuspension != null && currentSuspension.getSuspendDate() != null) {
            end = currentSuspension.getSuspendDate();
        }
        
        long totalDuration = java.time.Duration.between(startDate, end).toMinutes();
        
        // Soustraire toutes les périodes de suspension terminées
        for (SuspensionPeriod period : suspensionPeriods) {
            if (period.isCompleted()) {
                totalDuration -= period.getDurationInMinutes();
            }
        }
        
        return Math.max(0, totalDuration); // Ne pas retourner de durée négative
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
    
    /**
     * Récupère toutes les périodes de suspension
     */
    public List<SuspensionPeriod> getSuspensionPeriods() {
        return new ArrayList<>(suspensionPeriods);
    }
    
    /**
     * Récupère la période de suspension actuelle (si suspendue)
     */
    public SuspensionPeriod getCurrentSuspension() {
        return currentSuspension;
    }
    
    /**
     * Calcule la durée totale de toutes les suspensions
     */
    public long getTotalSuspensionDurationInMinutes() {
        long totalSuspensionDuration = 0;
        
        // Ajouter toutes les périodes de suspension terminées
        for (SuspensionPeriod period : suspensionPeriods) {
            if (period.isCompleted()) {
                totalSuspensionDuration += period.getDurationInMinutes();
            }
        }
        
        // Ajouter la période de suspension actuelle si elle existe
        if (currentSuspension != null && currentSuspension.isActive()) {
            totalSuspensionDuration += java.time.Duration.between(
                currentSuspension.getSuspendDate(), 
                LocalDateTime.now()
            ).toMinutes();
        }
        
        return totalSuspensionDuration;
    }
    
    /**
     * Récupère le nombre de suspensions
     */
    public int getSuspensionCount() {
        int count = suspensionPeriods.size();
        if (currentSuspension != null && currentSuspension.isActive()) {
            count++; // Compter la suspension actuelle
        }
        return count;
    }
}
