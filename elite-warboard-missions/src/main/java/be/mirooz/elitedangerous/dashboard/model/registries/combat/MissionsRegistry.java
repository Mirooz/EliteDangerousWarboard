package be.mirooz.elitedangerous.dashboard.model.registries.combat;

import be.mirooz.elitedangerous.dashboard.view.common.context.DashboardContext;
import be.mirooz.elitedangerous.dashboard.model.commander.Mission;
import be.mirooz.elitedangerous.dashboard.model.enums.MissionStatus;
import be.mirooz.elitedangerous.dashboard.service.listeners.MissionEventNotificationService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Singleton pour stocker les missions globales, observable par la UI
 */
@Data
public class MissionsRegistry {

    private static final MissionsRegistry INSTANCE = new MissionsRegistry();

    private final ObservableMap<String, Mission> globalMissionMap =
            FXCollections.synchronizedObservableMap(FXCollections.observableHashMap());

    public static MissionsRegistry getInstance() {
        return INSTANCE;
    }
    public void addMissionMapListener(Runnable action) {
        globalMissionMap.addListener((MapChangeListener<String, Mission>) change -> {
            if (!DashboardContext.getInstance().isBatchLoading()) {
                Platform.runLater(action);
            }
        });
    }
    public void clear(){
        synchronized (globalMissionMap) {
            globalMissionMap.clear();
        }
    }

    /** Restaure l'ensemble des missions à partir d'un snapshot persisté. */
    public void applyFullPersistedSnapshot(Map<String, Mission> snapshot) {
        synchronized (globalMissionMap) {
            globalMissionMap.clear();
            if (snapshot != null) {
                globalMissionMap.putAll(snapshot);
            }
        }
    }

    public void setActiveMissionsToFailed(){
        synchronized (globalMissionMap) {
            globalMissionMap.forEach((s, mission) -> {
                if (mission.isActive()) {
                    mission.setStatus(MissionStatus.FAILED);
                }
            });
        }
        MissionEventNotificationService.getInstance().notifyOnMissionStatusChanged();
    }

    /**
     * Snapshot défensif des missions pour itération côté UI (évite les CME
     * si le journal thread modifie la map en parallèle).
     */
    public List<Mission> snapshotMissions() {
        synchronized (globalMissionMap) {
            return new ArrayList<>(globalMissionMap.values());
        }
    }

    /**
     * Snapshot défensif de la map missions pour consommation read-only (UI/overlay).
     */
    public Map<String, Mission> snapshotMissionMap() {
        synchronized (globalMissionMap) {
            return new LinkedHashMap<>(globalMissionMap);
        }
    }

}
