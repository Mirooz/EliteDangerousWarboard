package be.mirooz.elitedangerous.dashboard.controller;

import be.mirooz.elitedangerous.dashboard.comparator.MissionTimestampComparator;
import be.mirooz.elitedangerous.dashboard.controller.ui.context.DashboardContext;
import be.mirooz.elitedangerous.dashboard.controller.ui.manager.UIManager;
import be.mirooz.elitedangerous.dashboard.model.Mission;
import be.mirooz.elitedangerous.dashboard.model.enums.MissionStatus;
import be.mirooz.elitedangerous.dashboard.model.MissionsList;
import be.mirooz.elitedangerous.dashboard.controller.ui.component.GenericListView;
import be.mirooz.elitedangerous.dashboard.controller.ui.component.MissionCardComponent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Contrôleur pour la liste des missions
 */
public class MissionListController implements Initializable, Refreshable {
    @FXML
    private ProgressIndicator loadingIndicator;
    @FXML
    private GenericListView<Mission> missionListView;


    @FXML
    private Button activeFilterButton;

    @FXML
    private Button completedFilterButton;

    @FXML
    private Button failedFilterButton;

    @FXML
    private Button allFilterButton;
    private final MissionsList missionsList = MissionsList.getInstance();

    private final DashboardContext dashboardContext= DashboardContext.getInstance();
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        missionListView.setComponentFactory(MissionCardComponent::new);
        filterActiveMissions();
        dashboardContext.addFilterListener(this::applyFilter);
        UIManager.getInstance().register(this);
    }

    public void postBatch(){
        setLoadingVisible(false);
    }
    public void preBatch(){
        setLoadingVisible(true);
    }
    private void setLoadingVisible(boolean visible) {
        this.loadingIndicator.setVisible(visible);
    }

    private void refreshMissions() {
        applyFilter(DashboardContext.getInstance().getCurrentFilter());
    }
    private void applyFilter(MissionStatus currentFilter) {
        boolean isActive = MissionStatus.ACTIVE.equals(currentFilter);
        List<Mission> filteredMissions = missionsList.getGlobalMissionMap().values().stream()
                .filter(Mission::isMassacre)
                .filter(mission -> currentFilter == null || mission.getStatus() == currentFilter)
                .sorted(new MissionTimestampComparator(isActive,
                        isActive))
                .toList();
        missionListView.getItems().setAll(filteredMissions);
        updateFilterButtons(currentFilter);
    }

    private void updateFilterButtons(MissionStatus currentFilter) {
        // Réinitialiser tous les boutons
        activeFilterButton.getStyleClass().removeAll("active");
        completedFilterButton.getStyleClass().removeAll("active");
        failedFilterButton.getStyleClass().removeAll("active");
        allFilterButton.getStyleClass().removeAll("active");
        if (currentFilter == null) {
            allFilterButton.getStyleClass().add("active");
            return;
        }
        switch (currentFilter) {
            case ACTIVE -> activeFilterButton.getStyleClass().add("active");
            case COMPLETED -> completedFilterButton.getStyleClass().add("active");
            case FAILED -> failedFilterButton.getStyleClass().add("active");
        }
    }

    @FXML
    private void filterActiveMissions() {
        DashboardContext.getInstance().setCurrentFilter(MissionStatus.ACTIVE);
    }

    @FXML
    private void filterCompletedMissions() {
        DashboardContext.getInstance().setCurrentFilter(MissionStatus.COMPLETED);
    }

    @FXML
    private void filterAbandonedMissions() {
        DashboardContext.getInstance().setCurrentFilter(MissionStatus.FAILED);
    }

    @FXML
    private void filterAllMissions() {
        DashboardContext.getInstance().setCurrentFilter(null);
    }

    @Override
    public void refreshUI() {
        refreshMissions();
    }
}
