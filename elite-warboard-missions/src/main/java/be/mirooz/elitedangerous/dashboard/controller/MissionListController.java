package be.mirooz.elitedangerous.dashboard.controller;

import be.mirooz.elitedangerous.dashboard.model.enums.TargetType;
import be.mirooz.elitedangerous.dashboard.model.targetpanel.CibleStats;
import be.mirooz.elitedangerous.dashboard.model.targetpanel.SourceFactionStats;
import be.mirooz.elitedangerous.dashboard.model.targetpanel.TargetFactionStats;
import be.mirooz.elitedangerous.dashboard.service.CombatMissionHistoryService;
import be.mirooz.elitedangerous.dashboard.service.listeners.MissionEventNotificationService;
import be.mirooz.elitedangerous.dashboard.util.comparator.MissionTimestampComparator;
import be.mirooz.elitedangerous.dashboard.controller.ui.context.DashboardContext;
import be.mirooz.elitedangerous.dashboard.controller.ui.manager.UIManager;
import be.mirooz.elitedangerous.dashboard.model.commander.Mission;
import be.mirooz.elitedangerous.dashboard.model.enums.MissionStatus;
import be.mirooz.elitedangerous.dashboard.model.enums.MissionType;
import be.mirooz.elitedangerous.dashboard.model.registries.MissionsRegistry;
import be.mirooz.elitedangerous.dashboard.controller.ui.component.NotSelectableListView;
import be.mirooz.elitedangerous.dashboard.controller.ui.component.combat.MissionCardComponent;
import be.mirooz.elitedangerous.dashboard.controller.ui.component.combat.TargetPanelComponent;
import be.mirooz.elitedangerous.dashboard.controller.ui.component.combat.TargetOverlayComponent;
import be.mirooz.elitedangerous.dashboard.controller.ui.component.combat.CombatMissionHistoryComponent;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Contrôleur pour la liste des missions
 */
public class MissionListController implements Initializable, IRefreshable, IBatchListener {
    @FXML
    private ProgressIndicator loadingIndicator;
    @FXML
    private NotSelectableListView<Mission> missionListView;

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

    @FXML
    private TargetPanelComponent targetPanel;

    @FXML
    private VBox historyContainer;

    private CombatMissionHistoryComponent missionHistoryComponent;

    private final MissionsRegistry missionsRegistry = MissionsRegistry.getInstance();
    private final DashboardContext dashboardContext = DashboardContext.getInstance();
    private final LocalizationService localizationService = LocalizationService.getInstance();
    private final TargetOverlayComponent targetOverlayComponent = new TargetOverlayComponent();
    private final CombatMissionHistoryService historyService = CombatMissionHistoryService.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        missionListView.setComponentFactory(MissionCardComponent::new);

        // Configurer le bouton overlay dans le panneau de cibles
        if (targetPanel != null) {
            targetPanel.setOnOverlayButtonClick(this::showTargetOverlay);
        }

        // Charger le composant d'historique des missions
        initializeHistoryComponent();

        initializeComboBoxes();
        updateLanguage();

        UIManager.getInstance().register(this);

        // Écouter les changements de langue
        localizationService.addLanguageChangeListener(locale -> {
            updateLanguage();
            // Rafraîchir les missions pour recréer les cartes avec les nouvelles traductions
            refreshMissions();
            updateFactionStats(dashboardContext.getCurrentFilter(),dashboardContext.getCurrentTypeFilter());});

        dashboardContext.addFilterListener(this::applyFilter);
        dashboardContext.setCurrentFilter(MissionStatus.ACTIVE);
    }

    private void initializeHistoryComponent() {
        try {
            if (historyContainer != null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/combat/combat-mission-history.fxml"));
                VBox historyPanel = loader.load();
                missionHistoryComponent = loader.getController();
                historyContainer.getChildren().add(historyPanel);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement du composant d'historique: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initializeComboBoxes() {
        // Initialiser les ComboBox avec les valeurs par défaut
        // Déclencher les filtres initiaux
        onTypeFilterChanged();
        onStatusFilterChanged();
    }

    @Override
    public void onBatchEnd() {

        dashboardContext.addFilterListener(this::updateFactionStats);
        updateFactionStats(dashboardContext.getCurrentFilter(),dashboardContext.getCurrentTypeFilter());

        // Rafraîchir l'historique des missions complétées
        if (missionHistoryComponent != null) {
            missionHistoryComponent.refreshHistory();
        }
        refreshMissions();
        setLoadingVisible(false);

        MissionEventNotificationService.getInstance().addListener(this::refreshMissions);
    }

    @Override
    public void onBatchStart() {
        setLoadingVisible(true);
        MissionEventNotificationService.getInstance().clearListeners();
    }

    private void setLoadingVisible(boolean visible) {
        this.loadingIndicator.setVisible(visible);
    }

    private void refreshMissions() {
        Platform.runLater(() -> {
            applyFilter(DashboardContext.getInstance().getCurrentFilter(), DashboardContext.getInstance().getCurrentTypeFilter());
        });
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

        // Les traductions pour le panneau de cibles sont gérées dans le composant lui-même

        int currentTypeSelection=0;
        int currentStatusSelection=0;
        // Sauvegarder les sélections actuelles
        if (typeFilterComboBox != null && typeFilterComboBox.getSelectionModel() != null) {
            currentTypeSelection = typeFilterComboBox.getSelectionModel().getSelectedIndex();
        }

        if (statusFilterComboBox != null && statusFilterComboBox.getSelectionModel() != null) {
            currentStatusSelection = statusFilterComboBox.getSelectionModel().getSelectedIndex();
        }
        // Mettre à jour les options des ComboBox
        updateComboBoxOptions();

        // Restaurer les sélections
        restoreComboBoxSelections(currentTypeSelection, currentStatusSelection);

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

    private void updateFactionStats(MissionStatus currentStatus, MissionType currentType) {
        // Mettre à jour les statistiques par faction (toujours basées sur les missions actives)
        Map<TargetType, CibleStats> stats = getFactionStats();

        // Mettre à jour le panneau de cibles avec les nouvelles statistiques
        if (targetPanel != null) {
            targetPanel.displayStats(stats, missionsRegistry.getGlobalMissionMap());
        }
        
        // Mettre à jour l'overlay s'il est ouvert
        if (targetOverlayComponent.isShowing()) {
            targetOverlayComponent.updateContent(stats, missionsRegistry.getGlobalMissionMap());
        }
    }

    private Map<TargetType, CibleStats> getFactionStats() {
        MissionType currentType =dashboardContext.getCurrentTypeFilter();
        List<Mission> targetMissions = missionsRegistry.getGlobalMissionMap().values().stream()
                .filter(mission -> mission.isShipMassacreActive() || mission.isShipActiveFactionConflictMission())
                .filter(mission -> currentType == null || currentType.equals(mission.getType()))
                .collect(Collectors.toList());
        Map<TargetType, CibleStats> stats = computeFactionStats(targetMissions);
        return stats;
    }

    private Map<TargetType, CibleStats> computeFactionStats(List<Mission> massacreMissions) {
        Map<TargetType, CibleStats> stats = new HashMap<>();

        for (Mission mission : massacreMissions) {
            if (mission.getTargetFaction() == null || mission.getTargetType() == null) continue;

            CibleStats cibleStats = stats.computeIfAbsent(mission.getTargetType(), c -> new CibleStats(mission.getTargetType()));

            TargetFactionStats targetStats = cibleStats.getOrCreateFaction(mission.getTargetFaction());

            targetStats.addSource(new SourceFactionStats(mission.getFaction(), mission.getTargetCountLeft()));
        }
        return stats;
    }

    @FXML
    private void showTargetOverlay() {
        Map<TargetType, CibleStats> stats = getFactionStats();
        Map<String, Mission> missions = missionsRegistry.getGlobalMissionMap();
        
        // Si l'overlay est déjà ouvert, on le ferme
        if (targetOverlayComponent.isShowing()) {
            targetOverlayComponent.closeOverlay();
            updateOverlayButtonText();
        } else {
            // Sinon, on l'ouvre
            targetOverlayComponent.showOverlay(stats, missions);
            updateOverlayButtonText();
        }
    }
    
    private void updateOverlayButtonText() {
        if (targetPanel != null) {
            targetPanel.updateOverlayButtonText(targetOverlayComponent.isShowing());
        }
    }
    
    @Override
    public void refreshUI() {
        updateComboBoxSelections(dashboardContext.getCurrentFilter(),dashboardContext.getCurrentTypeFilter());
        updateFactionStats(dashboardContext.getCurrentFilter(),dashboardContext.getCurrentTypeFilter());
        missionListView.refresh();
    }
}
