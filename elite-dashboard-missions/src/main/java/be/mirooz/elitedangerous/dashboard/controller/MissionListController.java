package be.mirooz.elitedangerous.dashboard.controller;

import be.mirooz.elitedangerous.dashboard.util.comparator.MissionTimestampComparator;
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
import javafx.scene.control.ComboBox;
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
    private ComboBox<String> typeFilterComboBox;

    @FXML
    private ComboBox<String> statusFilterComboBox;

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

        updateLanguage();
        initializeComboBoxes();
        dashboardContext.addFilterListener(this::applyFilter);
        UIManager.getInstance().register(this);

        // Écouter les changements de langue
        localizationService.addLanguageChangeListener(locale -> updateLanguage());
    }

    private void initializeComboBoxes() {
        // Initialiser les ComboBox avec les valeurs par défaut
        typeFilterComboBox.getSelectionModel().select(0); // "Toutes"
        statusFilterComboBox.getSelectionModel().select(1); // "Toutes"

        // Déclencher les filtres initiaux
        onTypeFilterChanged();
        onStatusFilterChanged();
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
        applyFilter(DashboardContext.getInstance().getCurrentFilter(), DashboardContext.getInstance().getCurrentTypeFilter());
    }

    private void applyFilter(MissionStatus currentFilter, MissionType currentTypeFilter) {
        boolean isActive = MissionStatus.ACTIVE.equals(currentFilter);
        List<Mission> filteredMissions = missionsRegistry.getGlobalMissionMap().values().stream()
                .filter(Mission::isShipMassacre)
                .filter(mission -> currentTypeFilter == null || mission.getType() == currentTypeFilter)
                .filter(mission -> currentFilter == null || mission.getStatus() == currentFilter)
                .sorted(new MissionTimestampComparator(isActive, isActive))
                .toList();
        missionListView.getItems().setAll(filteredMissions);
        updateComboBoxSelections(currentFilter, currentTypeFilter);
    }

    private void updateComboBoxSelections(MissionStatus currentFilter, MissionType currentTypeFilter) {
        // Mettre à jour la sélection du ComboBox de type
        if (currentTypeFilter == null) {
            typeFilterComboBox.getSelectionModel().select(0); // "Toutes"
        } else {
            switch (currentTypeFilter) {
                case MASSACRE -> typeFilterComboBox.getSelectionModel().select(1); // "Massacre"
                case CONFLIT -> typeFilterComboBox.getSelectionModel().select(2); // "Conflit"
                default -> typeFilterComboBox.getSelectionModel().select(0); // "Toutes" par défaut
            }
        }

        // Mettre à jour la sélection du ComboBox de statut
        if (currentFilter == null) {
            statusFilterComboBox.getSelectionModel().select(0); // "Toutes"
        } else {
            switch (currentFilter) {
                case ACTIVE -> statusFilterComboBox.getSelectionModel().select(1); // "Actives"
                case COMPLETED -> statusFilterComboBox.getSelectionModel().select(2); // "Complétées"
                case FAILED -> statusFilterComboBox.getSelectionModel().select(3); // "Échouées"
                default -> statusFilterComboBox.getSelectionModel().select(0); // "Toutes" par défaut
            }
        }
    }

    @FXML
    private void onTypeFilterChanged() {
        String selectedType = typeFilterComboBox.getSelectionModel().getSelectedItem();
        if (selectedType == null) return;

        MissionType typeFilter = null;
        if (selectedType.equals(localizationService.getString("missions.massacre"))) {
            typeFilter = MissionType.MASSACRE;
        } else if (selectedType.equals(localizationService.getString("missions.conflict"))) {
            typeFilter = MissionType.CONFLIT;
        }
        // Si "Toutes" ou autre, typeFilter reste null

        dashboardContext.setCurrentTypeFilter(typeFilter);
    }

    @FXML
    private void onStatusFilterChanged() {
        String selectedStatus = statusFilterComboBox.getSelectionModel().getSelectedItem();
        if (selectedStatus == null) return;

        MissionStatus statusFilter = null;
        if (selectedStatus.equals(localizationService.getString("missions.active"))) {
            statusFilter = MissionStatus.ACTIVE;
        } else if (selectedStatus.equals(localizationService.getString("missions.completed"))) {
            statusFilter = MissionStatus.COMPLETED;
        } else if (selectedStatus.equals(localizationService.getString("missions.failed"))) {
            statusFilter = MissionStatus.FAILED;
        }
        // Si "Toutes" ou autre, statusFilter reste null

        dashboardContext.setCurrentFilter(statusFilter);
    }

    private void updateLanguage() {
        // Mettre à jour les labels
        missionsTitleLabel.setText(localizationService.getString("missions.title"));
        typeFilterLabel.setText(localizationService.getString("filter.type"));
        statusFilterLabel.setText(localizationService.getString("filter.status"));

        // Sauvegarder les sélections actuelles
        int currentTypeSelection = typeFilterComboBox.getSelectionModel().getSelectedIndex();
        int currentStatusSelection = statusFilterComboBox.getSelectionModel().getSelectedIndex();

        // Mettre à jour les options des ComboBox
        updateComboBoxOptions();

        // Restaurer les sélections
        restoreComboBoxSelections(currentTypeSelection, currentStatusSelection);

        // Rafraîchir les missions pour recréer les cartes avec les nouvelles traductions
        refreshMissions();
    }

    private void updateComboBoxOptions() {
        // Mettre à jour les options du ComboBox de type
        typeFilterComboBox.getItems().clear();
        typeFilterComboBox.getItems().addAll(
                localizationService.getString("missions.all"),
                localizationService.getString("missions.massacre"),
                localizationService.getString("missions.conflict")
        );

        // Mettre à jour les options du ComboBox de statut
        statusFilterComboBox.getItems().clear();
        statusFilterComboBox.getItems().addAll(
                localizationService.getString("missions.all_status"),
                localizationService.getString("missions.active"),
                localizationService.getString("missions.completed"),
                localizationService.getString("missions.failed")
        );
    }

    private void restoreComboBoxSelections(int currentTypeSelection, int currentStatusSelection) {
        // Restaurer la sélection du type
        if (typeFilterComboBox.getItems().size() > currentTypeSelection) {
            typeFilterComboBox.getSelectionModel().select(currentTypeSelection);
        } else {
            typeFilterComboBox.getSelectionModel().select(0); // Par défaut "Toutes"
        }
        // Restaurer la sélection du statut
        if (statusFilterComboBox.getItems().size() > currentStatusSelection) {
            statusFilterComboBox.getSelectionModel().select(currentStatusSelection);
        } else {
            statusFilterComboBox.getSelectionModel().select(0); // Par défaut "Toutes"
        }

    }

    @Override
    public void refreshUI() {
        refreshMissions();
    }
}
