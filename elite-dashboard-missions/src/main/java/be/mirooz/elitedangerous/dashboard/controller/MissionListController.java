package be.mirooz.elitedangerous.dashboard.controller;

import be.mirooz.elitedangerous.dashboard.comparator.MissionTimestampComparator;
import be.mirooz.elitedangerous.dashboard.controller.ui.context.DashboardContext;
import be.mirooz.elitedangerous.dashboard.controller.ui.manager.UIManager;
import be.mirooz.elitedangerous.dashboard.model.Mission;
import be.mirooz.elitedangerous.dashboard.model.enums.MissionStatus;
import be.mirooz.elitedangerous.dashboard.model.enums.MissionType;
import be.mirooz.elitedangerous.dashboard.model.registries.MissionsRegistry;
import be.mirooz.elitedangerous.dashboard.controller.ui.component.GenericListView;
import be.mirooz.elitedangerous.dashboard.controller.ui.component.MissionCardComponent;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Contrôleur pour la liste des missions
 */
public class MissionListController implements Initializable, IRefreshable, IBatchListener {
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

    @FXML
    private Button massacreTypeFilterButton;

    @FXML
    private Button conflictTypeFilterButton;

    @FXML
    private Button allTypeFilterButton;

    @FXML
    private Label missionsTitleLabel;

    @FXML
    private Label typeFilterLabel;

    @FXML
    private Label statusFilterLabel;


    private final MissionsRegistry missionsRegistry = MissionsRegistry.getInstance();
    private final DashboardContext dashboardContext = DashboardContext.getInstance();
    private final LocalizationService localizationService = LocalizationService.getInstance();
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        missionListView.setComponentFactory(MissionCardComponent::new);
        filterActiveMissions();
        filterAllTypeMissions(); // Initialiser avec tous les types
        dashboardContext.addFilterListener(this::applyFilter);
        UIManager.getInstance().register(this);
        updateLanguage();
        
        // Écouter les changements de langue
        localizationService.addLanguageChangeListener(locale -> updateLanguage());
    }
    @Override
    public void onBatchEnd() {
        setLoadingVisible(false);
    }
    @Override
    public void onBatchStart() {
        setLoadingVisible(true);
    }
    private void setLoadingVisible(boolean visible) {
        this.loadingIndicator.setVisible(visible);
    }

    private void refreshMissions() {
        applyFilter(DashboardContext.getInstance().getCurrentFilter(),DashboardContext.getInstance().getCurrentTypeFilter());
    }
    private void applyFilter(MissionStatus currentFilter,MissionType currentTypeFilter) {
        boolean isActive = MissionStatus.ACTIVE.equals(currentFilter);
        List<Mission> filteredMissions = missionsRegistry.getGlobalMissionMap().values().stream()
                .filter(Mission::isShipMassacre)
                .filter(mission -> currentTypeFilter == null || mission.getType()== currentTypeFilter)
                .filter(mission -> currentFilter == null || mission.getStatus() == currentFilter)
                .sorted(new MissionTimestampComparator(isActive, isActive))
                .toList();
        missionListView.getItems().setAll(filteredMissions);
        updateFilterButtons(currentFilter,currentTypeFilter);
    }

    private void updateFilterButtons(MissionStatus currentFilter,MissionType currentTypeFilter) {
        // Réinitialiser tous les boutons
        activeFilterButton.getStyleClass().removeAll("active");
        completedFilterButton.getStyleClass().removeAll("active");
        failedFilterButton.getStyleClass().removeAll("active");
        allFilterButton.getStyleClass().removeAll("active");
        if (currentFilter == null) {
            allFilterButton.getStyleClass().add("active");
        } else {
            switch (currentFilter) {
                case ACTIVE -> activeFilterButton.getStyleClass().add("active");
                case COMPLETED -> completedFilterButton.getStyleClass().add("active");
                case FAILED -> failedFilterButton.getStyleClass().add("active");
            }
        }

        massacreTypeFilterButton.getStyleClass().removeAll("active");
        conflictTypeFilterButton.getStyleClass().removeAll("active");
        allTypeFilterButton.getStyleClass().removeAll("active");

        if (currentTypeFilter == null) {
            allTypeFilterButton.getStyleClass().add("active");
        } else {
            switch (currentTypeFilter) {
                case MASSACRE -> massacreTypeFilterButton.getStyleClass().add("active");
                case CONFLIT -> conflictTypeFilterButton.getStyleClass().add("active");
            }
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

    @FXML
    private void filterMassacreMissions() {
        DashboardContext.getInstance().setCurrentTypeFilter(MissionType.MASSACRE);
    }

    @FXML
    private void filterConflictMissions() {
        DashboardContext.getInstance().setCurrentTypeFilter(MissionType.CONFLIT);
    }

    @FXML
    private void filterAllTypeMissions() {
        DashboardContext.getInstance().setCurrentTypeFilter(null);
    }


    private void updateLanguage() {
        // Mettre à jour les labels
        missionsTitleLabel.setText(localizationService.getString("missions.title"));
        typeFilterLabel.setText(localizationService.getString("filter.type"));
        statusFilterLabel.setText(localizationService.getString("filter.status"));

        // Mettre à jour les boutons de type
        massacreTypeFilterButton.setText(localizationService.getString("missions.massacre"));
        conflictTypeFilterButton.setText(localizationService.getString("missions.conflict"));
        allTypeFilterButton.setText(localizationService.getString("missions.all"));

        // Mettre à jour les boutons de statut
        activeFilterButton.setText(localizationService.getString("missions.active"));
        completedFilterButton.setText(localizationService.getString("missions.completed"));
        failedFilterButton.setText(localizationService.getString("missions.failed"));
        allFilterButton.setText(localizationService.getString("missions.all_status"));

        
        // Rafraîchir les missions pour recréer les cartes avec les nouvelles traductions
        refreshMissions();
    }

    @Override
    public void refreshUI() {
        refreshMissions();
    }
}
