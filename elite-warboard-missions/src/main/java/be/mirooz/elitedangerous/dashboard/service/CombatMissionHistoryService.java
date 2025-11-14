package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.dashboard.model.combat.CombatMissionStats;
import be.mirooz.elitedangerous.dashboard.model.commander.Mission;
import be.mirooz.elitedangerous.dashboard.model.enums.MissionType;
import be.mirooz.elitedangerous.dashboard.model.enums.TargetType;
import be.mirooz.elitedangerous.dashboard.model.registries.combat.MissionsRegistry;
import javafx.application.Platform;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Service pour gérer l'historique des missions de massacre complétées
 * 
 * LOGIQUE DE GROUPAGE :
 * - Chaque mission est groupée par: OriginSystem → DestinationSystem + Category
 * - Si un même système source a plusieurs destinations (ou catégories), 
 *   chaque combinaison crée une entrée séparée dans l'historique
 * - Exemple: "Sol → Lave" (PIRATE) et "Sol → Lave" (CONFLIT) = 2 entrées distinctes
 * - Exemple: "Sol → Lave" (PIRATE) et "Sol → Alpha Centauri" (PIRATE) = 2 entrées distinctes
 */
public class CombatMissionHistoryService {

    private static final CombatMissionHistoryService INSTANCE = new CombatMissionHistoryService();
    
    // Map: "OriginSystem|DestinationSystem|Category" -> CombatMissionStats
    private final Map<String, CombatMissionStats> missionStatsMap = new HashMap<>();
    
    // Listeners pour notifier les changements
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    
    private CombatMissionHistoryService() {
        loadCompletedMissions();
    }
    
    public static CombatMissionHistoryService getInstance() {
        return INSTANCE;
    }
    
    /**
     * Charge les missions complétées depuis le registry
     */
    private void loadCompletedMissions() {
        MissionsRegistry missionsRegistry = MissionsRegistry.getInstance();
        
        missionsRegistry.getGlobalMissionMap().values().stream()
                .filter(mission -> mission.isCompleted() && isCombatMission(mission))
                .forEach(this::addCompletedMissionToStats);
    }
    
    /**
     * Vérifie si c'est une mission de massacre ou de conflit
     */
    private boolean isCombatMission(Mission mission) {
        return (mission.getType() == MissionType.MASSACRE && mission.getTargetType() == TargetType.PIRATE)
                || mission.getType() == MissionType.CONFLIT;
    }
    
    /**
     * Détermine la catégorie de mission
     */
    private MissionType getMissionCategory(Mission mission) {
        return mission.getType();
    }
    
    /**
     * Ajoute une mission complétée aux stats
     */
    private void addCompletedMissionToStats(Mission mission) {
        if (mission.getDestinationSystem() == null) {
            return;
        }
        if (mission.isShipPirateMission() || mission.getType() == MissionType.CONFLIT) {

            String key = buildKey(mission.getOriginSystem(), mission.getDestinationSystem(), mission.getType().getDisplayName());
            CombatMissionStats stats = missionStatsMap.computeIfAbsent(key,
                    k -> new CombatMissionStats(mission.getOriginSystem(), mission.getDestinationSystem(), mission.getType(), mission.getAcceptedTime()));

            int kills = mission.getCurrentCount();
            long reward = mission.getReward();

            stats.addCompletedMission(kills, reward,mission.getAcceptedTime());
        }
    }
    
    /**
     * Enregistre une mission complétée
     */
    public void registerCompletedMission(Mission mission) {
        if (mission == null || !mission.isCompleted()) {
            return;
        }
        
        if (!isCombatMission(mission)) {
            return;
        }
        
        addCompletedMissionToStats(mission);
        notifyHistoryChanged();
    }
    
    /**
     * Construit une clé unique pour origine, destination et catégorie
     */
    private String buildKey(String originSystem, String destinationSystem, String category) {
        return originSystem + "|" + destinationSystem + "|" + category;
    }
    
    /**
     * Récupère toutes les stats triées par dernière mission complétée
     */
    public List<CombatMissionStats> getAllStats() {
        return missionStatsMap.values().stream()
                .sorted(Comparator.comparing(CombatMissionStats::getLastCompleted).reversed())
                .collect(Collectors.toList());
    }
    
    /**
     * Nettoie l'historique
     */
    public void clear() {
        missionStatsMap.clear();
    }
    
    /**
     * Ajoute un listener pour les changements d'historique
     */
    public void addListener(Runnable listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    /**
     * Supprime un listener
     */
    public void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    public void removeListeners() {
        listeners.clear();
    }

    /**
     * Notifie tous les listeners
     */
    public void notifyHistoryChanged() {
        for (Runnable listener : listeners) {
            try {
                Platform.runLater(listener);
            } catch (Exception e) {
                System.err.println("Erreur lors de la notification: " + e.getMessage());
            }
        }
    }
    
    /**
     * Notifie tous les listeners (méthode privée)
     */
    private void notifyListeners() {
        notifyHistoryChanged();
    }
}

