package be.mirooz.elitedangerous.dashboard.ui.context;

import be.mirooz.elitedangerous.dashboard.model.enums.MissionStatus;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.function.Consumer;

public class DashboardContext {

    private static final DashboardContext INSTANCE = new DashboardContext();

    // Filtre observable
    private final ObjectProperty<MissionStatus> currentFilter = new SimpleObjectProperty<>();

    private final BooleanProperty batchLoading = new SimpleBooleanProperty(false);

    private DashboardContext() {}

    public static DashboardContext getInstance() {
        return INSTANCE;
    }

    public MissionStatus getCurrentFilter() {
        return currentFilter.get();
    }

    public void setCurrentFilter(MissionStatus filter) {
        currentFilter.set(filter);
    }

    public ObjectProperty<MissionStatus> currentFilterProperty() {
        return currentFilter;
    }

    public boolean isBatchLoading() {
        return batchLoading.get();
    }

    public void setBatchLoading(boolean loading) {
        batchLoading.set(loading);
    }
    public void addFilterListener(Consumer<MissionStatus> action) {
        currentFilter.addListener((obs, oldVal, newVal) -> {
            if (!isBatchLoading()) {
                Platform.runLater(() -> action.accept(newVal));
            }
        });
    }
}
