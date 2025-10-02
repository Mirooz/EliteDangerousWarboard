package be.mirooz.elitedangerous.dashboard.controller;

import be.mirooz.elitedangerous.dashboard.comparator.MissionTimestampComparator;
import be.mirooz.elitedangerous.dashboard.model.Mission;
import be.mirooz.elitedangerous.dashboard.model.enums.MissionStatus;
import be.mirooz.elitedangerous.dashboard.model.enums.MissionType;
import be.mirooz.elitedangerous.dashboard.model.MissionsList;
import be.mirooz.elitedangerous.dashboard.ui.component.GenericListView;
import be.mirooz.elitedangerous.dashboard.ui.component.MissionCardComponent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Contrôleur pour la liste des missions
 */
public class MissionListController implements Initializable {
    @FXML
    private GenericListView<Mission> missionsList;


    @FXML
    private Button activeFilterButton;

    @FXML
    private Button completedFilterButton;

    @FXML
    private Button failedFilterButton;

    @FXML
    private Button allFilterButton;

    private MissionStatus currentFilter = MissionStatus.ACTIVE;
    private MissionsList allMissionsList = MissionsList.getInstance();

    private Consumer<MissionStatus> filterChangeCallback;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        missionsList.setComponentFactory(MissionCardComponent::new);
    }


    public void setFilterChangeCallback(Consumer<MissionStatus> callback) {
        this.filterChangeCallback = callback;
    }


    public void setCurrentFilter(MissionStatus filter) {
        this.currentFilter = filter;
        updateFilterButtons();
    }

    public void applyFilter() {
        missionsList.getItems().clear();
        boolean isActive = MissionStatus.ACTIVE.equals(currentFilter);
        List<Mission> filteredMissions = allMissionsList.getGlobalMissionMap().values().stream()
                .filter(mission -> mission.getType() == MissionType.MASSACRE)
                .filter(mission -> currentFilter == null || mission.getStatus() == currentFilter)
                .sorted(new MissionTimestampComparator(isActive,
                        isActive))
                .toList();

        missionsList.getItems().setAll(filteredMissions);

        updateFilterButtons();
    }

    private void updateFilterButtons() {
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
        if (filterChangeCallback != null) {
            filterChangeCallback.accept(MissionStatus.ACTIVE);
        }
    }

    @FXML
    private void filterCompletedMissions() {
        if (filterChangeCallback != null) {
            filterChangeCallback.accept(MissionStatus.COMPLETED);
        }
    }

    @FXML
    private void filterAbandonedMissions() {
        if (filterChangeCallback != null) {
            filterChangeCallback.accept(MissionStatus.FAILED);
        }
    }

    @FXML
    private void filterAllMissions() {
        if (filterChangeCallback != null) {
            filterChangeCallback.accept(null);
        }
    }
}
