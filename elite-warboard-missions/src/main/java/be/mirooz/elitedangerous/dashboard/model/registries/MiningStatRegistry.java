package be.mirooz.elitedangerous.dashboard.model.registries;

import be.mirooz.elitedangerous.dashboard.model.mining.MiningStat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Registre pour gérer les sessions de minage
 */
public class MiningStatRegistry {
    
    private static MiningStatRegistry instance;
    private final List<MiningStat> miningStats;
    private MiningStat currentMiningSession;
    
    private MiningStatRegistry() {
        this.miningStats = new ArrayList<>();
    }
    
    public static MiningStatRegistry getInstance() {
        if (instance == null) {
            instance = new MiningStatRegistry();
        }
        return instance;
    }
    
    /**
     * Démarre une nouvelle session de minage avec timestamp
     */
    public MiningStat startMiningSession(String systemName, String bodyName, String ringName, String timestamp) {
        // Terminer la session précédente si elle existe
        if (currentMiningSession != null && currentMiningSession.isActive()) {
            currentMiningSession.endSession(null);
        }
        
        // Créer une nouvelle session
        currentMiningSession = new MiningStat(systemName, bodyName, ringName, timestamp);
        miningStats.add(currentMiningSession);
        
        return currentMiningSession;
    }
    
    /**
     * Démarre une nouvelle session de minage (utilise l'heure actuelle)
     */
    public MiningStat startMiningSession(String systemName, String bodyName, String ringName) {
        return startMiningSession(systemName, bodyName, ringName, null);
    }
    
    /**
     * Termine la session de minage en cours avec timestamp
     */
    public void endCurrentMiningSession(String timestamp) {
        if (currentMiningSession != null && currentMiningSession.isActive()) {
            currentMiningSession.endSession(timestamp);
            
            // Supprimer la session si aucun minéral n'a été raffiné
            if (currentMiningSession.getRefinedMinerals().isEmpty()) {
                miningStats.remove(currentMiningSession);
                System.out.println("🗑️ Session de minage supprimée (aucun minéral raffiné)");
            }
            
            currentMiningSession = null;
        }
    }
    
    /**
     * Termine la session de minage en cours (utilise l'heure actuelle)
     */
    public void endCurrentMiningSession() {
        endCurrentMiningSession(null);
    }
    
    /**
     * Récupère la session de minage en cours
     */
    public Optional<MiningStat> getCurrentMiningSession() {
        return Optional.ofNullable(currentMiningSession);
    }
    
    /**
     * Vérifie si une session de minage est en cours
     */
    public boolean isMiningInProgress() {
        return currentMiningSession != null && currentMiningSession.isActive();
    }
    
    /**
     * Récupère toutes les sessions de minage
     */
    public List<MiningStat> getAllMiningStats() {
        return new ArrayList<>(miningStats);
    }
    
    /**
     * Récupère les sessions de minage actives
     */
    public List<MiningStat> getActiveMiningStats() {
        return miningStats.stream()
                .filter(MiningStat::isActive)
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Récupère les sessions de minage terminées
     */
    public List<MiningStat> getCompletedMiningStats() {
        return miningStats.stream()
                .filter(stat -> !stat.isActive())
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Suspend la session de minage en cours (pour les événements Fileheader/shutdown)
     */
    public void suspendCurrentMiningSession(String timestamp) {
        if (currentMiningSession != null && currentMiningSession.isActive()) {
            currentMiningSession.suspendSession(timestamp);
            System.out.println("⏸️ Session de minage suspendue: " + currentMiningSession.getSystemName() + " - " + currentMiningSession.getRingName());
        }
    }
    
    /**
     * Reprend la session de minage suspendue (pour les événements Commander)
     */
    public void resumeMiningSession(String timestamp) {
        if (currentMiningSession != null && currentMiningSession.isSuspended()) {
            currentMiningSession.resumeSession(timestamp);
            System.out.println("▶️ Session de minage reprise: " + currentMiningSession.getSystemName() + " - " + currentMiningSession.getRingName());
        }
    }
    
    /**
     * Vérifie si une session de minage est suspendue
     */
    public boolean isMiningSessionSuspended() {
        return currentMiningSession != null && currentMiningSession.isSuspended();
    }
    
    /**
     * Efface toutes les sessions de minage
     */
    public void clearAllStats() {
        miningStats.clear();
        currentMiningSession = null;
    }
    
    /**
     * Efface les sessions terminées
     */
    public void clearCompletedStats() {
        miningStats.removeIf(stat -> !stat.isActive());
    }
}
