package be.mirooz.elitedangerous.dashboard.model.registries;

import be.mirooz.elitedangerous.dashboard.controller.ui.context.DashboardContext;
import be.mirooz.elitedangerous.dashboard.model.commander.Mission;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import lombok.Data;

/**
 * Singleton pour stocker les missions globales, observable par la UI
 */
@Data
public class MissionsRegistry {

    private static final MissionsRegistry INSTANCE = new MissionsRegistry();

    private final ObservableMap<String, Mission> globalMissionMap =
            FXCollections.observableHashMap();

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
        globalMissionMap.clear();
    }


}
