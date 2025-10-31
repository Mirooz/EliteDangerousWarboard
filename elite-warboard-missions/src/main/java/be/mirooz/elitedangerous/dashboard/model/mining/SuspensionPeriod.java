package be.mirooz.elitedangerous.dashboard.model.mining;

import java.time.LocalDateTime;

/**
 * Représente une période de suspension dans une session de minage
 */
public class SuspensionPeriod {
    
    private LocalDateTime suspendDate;
    private LocalDateTime resumeDate;
    
    public SuspensionPeriod() {
    }
    
    public SuspensionPeriod(LocalDateTime suspendDate, LocalDateTime resumeDate) {
        this.suspendDate = suspendDate;
        this.resumeDate = resumeDate;
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
     * Crée une période de suspension avec timestamp
     */
    public static SuspensionPeriod createSuspension(String suspendTimestamp) {
        SuspensionPeriod period = new SuspensionPeriod();
        period.suspendDate = period.parseTimestamp(suspendTimestamp);
        return period;
    }
    
    /**
     * Termine cette période de suspension avec timestamp
     */
    public void endSuspension(String resumeTimestamp) {
        this.resumeDate = parseTimestamp(resumeTimestamp);
    }
    
    /**
     * Termine cette période de suspension (utilise l'heure actuelle)
     */
    public void endSuspension() {
        endSuspension(null);
    }
    
    /**
     * Vérifie si cette période de suspension est terminée
     */
    public boolean isCompleted() {
        return suspendDate != null && resumeDate != null;
    }
    
    /**
     * Vérifie si cette période de suspension est en cours (suspendue mais pas encore reprise)
     */
    public boolean isActive() {
        return suspendDate != null && resumeDate == null;
    }
    
    /**
     * Calcule la durée de cette période de suspension en minutes
     */
    public long getDurationInMinutes() {
        if (!isCompleted()) {
            return 0;
        }
        return java.time.Duration.between(suspendDate, resumeDate).toMinutes();
    }
    
    // Getters et Setters
    public LocalDateTime getSuspendDate() {
        return suspendDate;
    }
    
    public void setSuspendDate(LocalDateTime suspendDate) {
        this.suspendDate = suspendDate;
    }
    
    public LocalDateTime getResumeDate() {
        return resumeDate;
    }
    
    public void setResumeDate(LocalDateTime resumeDate) {
        this.resumeDate = resumeDate;
    }
    
    @Override
    public String toString() {
        return String.format("SuspensionPeriod{suspend=%s, resume=%s, duration=%d min}", 
                suspendDate, resumeDate, getDurationInMinutes());
    }
}
