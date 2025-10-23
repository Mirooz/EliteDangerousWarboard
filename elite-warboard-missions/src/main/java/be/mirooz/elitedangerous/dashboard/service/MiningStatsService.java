package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.Mineral;
import be.mirooz.elitedangerous.dashboard.model.mining.MiningStat;
import be.mirooz.elitedangerous.dashboard.model.registries.MiningStatRegistry;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service pour gérer les statistiques de minage
 */
public class MiningStatsService {
    
    private static MiningStatsService instance;
    private final MiningStatRegistry miningStatRegistry;

    private final MiningSessionNotificationService notificationService = MiningSessionNotificationService.getInstance();
    private MiningStatsService() {
        this.miningStatRegistry = MiningStatRegistry.getInstance();
    }
    
    public static MiningStatsService getInstance() {
        if (instance == null) {
            instance = new MiningStatsService();
        }
        return instance;
    }
    
    /**
     * Démarre une nouvelle session de minage avec timestamp
     */
    public MiningStat startMiningSession(String systemName, String bodyName, String ringName, String timestamp) {
        return miningStatRegistry.startMiningSession(systemName, bodyName, ringName, timestamp);
    }
    
    /**
     * Démarre une nouvelle session de minage (utilise l'heure actuelle)
     */
    public MiningStat startMiningSession(String systemName, String bodyName, String ringName) {
        return miningStatRegistry.startMiningSession(systemName, bodyName, ringName, null);
    }
    
    /**
     * Termine la session de minage en cours avec timestamp
     */
    public void endCurrentMiningSession(String timestamp) {
        miningStatRegistry.endCurrentMiningSession(timestamp);
        // Notifier tous les composants UI de la fin de session
        notificationService.notifySessionEnd();
    }
    
    /**
     * Termine la session de minage en cours (utilise l'heure actuelle)
     */
    public void endCurrentMiningSession() {
        endCurrentMiningSession(null);
    }
    
    /**
     * Suspend la session de minage en cours (pour les événements Fileheader/shutdown)
     */
    public void suspendCurrentMiningSession(String timestamp) {
        miningStatRegistry.suspendCurrentMiningSession(timestamp);
        System.out.println("⛏️ Session de minage suspendue (Fileheader/Shutdown)");
    }
    
    /**
     * Reprend la session de minage suspendue (pour les événements Commander)
     */
    public void resumeMiningSession(String timestamp) {
        miningStatRegistry.resumeMiningSession(timestamp);
        System.out.println("⛏️ Session de minage reprise (Commander)");
    }
    
    /**
     * Vérifie si une session de minage est suspendue
     */
    public boolean isMiningSessionSuspended() {
        return miningStatRegistry.isMiningSessionSuspended();
    }
    
    /**
     * Vérifie si une session de minage est en cours
     */
    public boolean isMiningInProgress() {
        return miningStatRegistry.isMiningInProgress();
    }
    
    /**
     * Récupère la session de minage en cours
     */
    public Optional<MiningStat> getCurrentMiningSession() {
        return miningStatRegistry.getCurrentMiningSession();
    }
    
    /**
     * Retire un minéral raffiné de la session en cours avec timestamp
     */
    public void removeRefinedMineral(Mineral mineral, int quantity, String timestamp) {
        Optional<MiningStat> currentSession = getCurrentMiningSession();
        if (currentSession.isPresent()) {
            currentSession.get().removeRefinedMineral(mineral, quantity, timestamp);
        }
    }
    
    /**
     * Retire un minéral raffiné de la session en cours (utilise l'heure actuelle)
     */
    public void removeRefinedMineral(Mineral mineral, int quantity) {
        removeRefinedMineral(mineral, quantity, null);
    }
    
    /**
     * Ajoute un minéral raffiné à la session en cours avec timestamp
     */
    public void addRefinedMineral(Mineral mineral, int quantity, String timestamp) {
        Optional<MiningStat> currentSession = getCurrentMiningSession();
        if (currentSession.isPresent()) {
            currentSession.get().addRefinedMineral(mineral, quantity, timestamp);
        }
    }
    
    /**
     * Ajoute un minéral raffiné à la session en cours (utilise l'heure actuelle)
     */
    public void addRefinedMineral(Mineral mineral, int quantity) {
        addRefinedMineral(mineral, quantity, null);
    }
    
    /**
     * Récupère toutes les sessions de minage
     */
    public List<MiningStat> getAllMiningStats() {
        return miningStatRegistry.getAllMiningStats();
    }
    
    /**
     * Récupère les sessions de minage terminées
     */
    public List<MiningStat> getCompletedMiningStats() {
        return miningStatRegistry.getCompletedMiningStats();
    }
    
    /**
     * Calcule les statistiques globales de minage
     */
    public MiningGlobalStats getGlobalStats() {
        List<MiningStat> completedStats = getCompletedMiningStats();
        return new MiningGlobalStats(completedStats);
    }
    
    /**
     * Efface toutes les statistiques
     */
    public void clearAllStats() {
        miningStatRegistry.clearAllStats();
    }
    
    /**
     * Efface les statistiques terminées
     */
    public void clearCompletedStats() {
        miningStatRegistry.clearCompletedStats();
    }
    
    /**
     * Classe interne pour les statistiques globales
     */
    public static class MiningGlobalStats {
        private final int totalSessions;
        private final long totalDurationMinutes;
        private final Map<Mineral, Integer> totalMineralsRefined;
        private final long totalValue;
        
        public MiningGlobalStats(List<MiningStat> completedStats) {
            this.totalSessions = completedStats.size();
            this.totalDurationMinutes = completedStats.stream()
                    .mapToLong(MiningStat::getDurationInMinutes)
                    .sum();
            this.totalMineralsRefined = completedStats.stream()
                    .map(MiningStat::getTotalRefinedMinerals)
                    .flatMap(map -> map.entrySet().stream())
                    .collect(java.util.stream.Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            Integer::sum
                    ));
            this.totalValue = completedStats.stream()
                    .mapToLong(MiningStat::getTotalValue)
                    .sum();
        }
        
        public int getTotalSessions() {
            return totalSessions;
        }
        
        public long getTotalDurationMinutes() {
            return totalDurationMinutes;
        }
        
        public Map<Mineral, Integer> getTotalMineralsRefined() {
            return totalMineralsRefined;
        }
        
        public long getTotalValue() {
            return totalValue;
        }
        
        public double getAverageSessionDurationMinutes() {
            return totalSessions > 0 ? (double) totalDurationMinutes / totalSessions : 0.0;
        }
        
        public double getAverageValuePerSession() {
            return totalSessions > 0 ? (double) totalValue / totalSessions : 0.0;
        }
    }
}
