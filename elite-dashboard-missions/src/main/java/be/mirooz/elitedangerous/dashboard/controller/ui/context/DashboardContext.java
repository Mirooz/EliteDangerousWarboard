package be.mirooz.elitedangerous.dashboard.controller.ui.context;

import be.mirooz.elitedangerous.dashboard.controller.ui.manager.UIManager;
import be.mirooz.elitedangerous.dashboard.model.enums.MissionStatus;
import be.mirooz.elitedangerous.dashboard.model.enums.MissionType;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class DashboardContext {

    private static final DashboardContext INSTANCE = new DashboardContext();

    // Filtre observable
    private final ObjectProperty<MissionStatus> currentFilter = new SimpleObjectProperty<>();
    
    // Filtre de type observable
    private final ObjectProperty<MissionType> currentTypeFilter = new SimpleObjectProperty();

    private final BooleanProperty batchLoading = new SimpleBooleanProperty(true);

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

    public MissionType getCurrentTypeFilter() {
        return currentTypeFilter.get();
    }

    public void setCurrentTypeFilter(MissionType typeFilter) {
        currentTypeFilter.set(typeFilter);
    }
    public boolean isBatchLoading() {
        return batchLoading.get();
    }

    public void setBatchLoading(boolean loading) {
        batchLoading.set(loading);
    }
    public void addFilterListener(BiConsumer<MissionStatus, MissionType> action) {
        ChangeListener<Object> listener = (obs, oldVal, newVal) -> {
            if (!isBatchLoading()) {
                Platform.runLater(() ->
                        action.accept(currentFilter.getValue(), currentTypeFilter.getValue())
                );
            }
        };

        currentFilter.addListener(listener);
        currentTypeFilter.addListener(listener);
    }
    public void refreshUI(){
        if (!isBatchLoading()){
            System.out.println("UI refreshed");
            UIManager.getInstance().refreshAllUI();
        }
    }
}
