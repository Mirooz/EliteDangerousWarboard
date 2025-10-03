package be.mirooz.elitedangerous.dashboard.model;

import be.mirooz.elitedangerous.dashboard.ui.context.DashboardContext;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * Singleton pour stocker les missions globales, observable par la UI
 */
@Data
public class MissionsList {

    private static final MissionsList INSTANCE = new MissionsList();

    private final ObservableMap<String, Mission> globalMissionMap =
            FXCollections.observableHashMap();

    public static MissionsList getInstance() {
        return INSTANCE;
    }
    public void addMissionMapListener(Runnable action) {
        globalMissionMap.addListener((MapChangeListener<String, Mission>) change -> {
            if (!DashboardContext.getInstance().isBatchLoading()) {
                Platform.runLater(action);
            }
        });
    }

}
