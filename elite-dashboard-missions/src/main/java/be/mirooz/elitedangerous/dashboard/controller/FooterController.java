package be.mirooz.elitedangerous.dashboard.controller;

import be.mirooz.elitedangerous.dashboard.controller.ui.context.DashboardContext;
import be.mirooz.elitedangerous.dashboard.controller.ui.manager.UIManager;
import be.mirooz.elitedangerous.dashboard.model.*;
import be.mirooz.elitedangerous.dashboard.model.enums.MissionStatus;
import be.mirooz.elitedangerous.dashboard.model.enums.MissionType;
import be.mirooz.elitedangerous.dashboard.model.enums.TargetType;
import be.mirooz.elitedangerous.dashboard.model.registries.MissionsRegistry;
import be.mirooz.elitedangerous.dashboard.model.targetpanel.CibleStats;
import be.mirooz.elitedangerous.dashboard.model.targetpanel.SourceFactionStats;
import be.mirooz.elitedangerous.dashboard.model.targetpanel.TargetFactionStats;
import be.mirooz.elitedangerous.dashboard.controller.ui.component.CommanderStatusComponent;
import be.mirooz.elitedangerous.dashboard.controller.ui.component.FactionStatsComponent;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Contrôleur pour le pied de page du dashboard
 */
public class FooterController implements Initializable, Refreshable {

    // Constantes pour garantir l'alignement

    @FXML
    private FactionStatsComponent factionStats;

    @FXML
    private Label commanderLabel;

    @FXML
    private Label systemLabel;

    @FXML
    private Label stationLabel;

    @FXML
    private Label commanderHeaderLabel;

    @FXML
    private Label systemHeaderLabel;

    @FXML
    private Label stationHeaderLabel;

    @FXML
    private Label targetHeaderLabel;

    @FXML
    private Label targetFactionHeaderLabel;

    @FXML
    private Label sourceFactionHeaderLabel;

    @FXML
    private Label killsHeaderLabel;

    private final CommanderStatusComponent commanderStatusComponent = CommanderStatusComponent.getInstance();
    private final MissionsRegistry missionsRegistry = MissionsRegistry.getInstance();
    private final DashboardContext dashboardContext = DashboardContext.getInstance();
    private final LocalizationService localizationService = LocalizationService.getInstance();
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        UIManager.getInstance().register(this);
        updateTranslations();
        
        // Écouter les changements de langue
        localizationService.addLanguageChangeListener(locale -> updateTranslations());
    }

    private void updateTranslations() {
        commanderHeaderLabel.setText(localizationService.getString("footer.commander"));
        systemHeaderLabel.setText(localizationService.getString("footer.system"));
        stationHeaderLabel.setText(localizationService.getString("footer.station"));
        
        // En-têtes du tableau des factions
        targetHeaderLabel.setText(localizationService.getString("footer.target"));
        targetFactionHeaderLabel.setText(localizationService.getString("footer.target_faction"));
        sourceFactionHeaderLabel.setText(localizationService.getString("footer.source_faction"));
        killsHeaderLabel.setText(localizationService.getString("footer.kills"));
    }

    public void postBatch(){
        stationLabel.textProperty().bind(commanderStatusComponent.getCurrentStationName());
        systemLabel.textProperty().bind(commanderStatusComponent.getCurrentStarSystem());
        commanderLabel.textProperty().bind(commanderStatusComponent.getCommanderName());
        dashboardContext.addFilterListener(this::updateFactionStats);
        updateFactionStats(dashboardContext.getCurrentFilter(),dashboardContext.getCurrentTypeFilter());
    }

    private void updateFactionStats(MissionStatus currentStatus, MissionType currentType) {
        // Mettre à jour les statistiques par faction (toujours basées sur les missions actives)
        List<Mission> massacreMissions = missionsRegistry.getGlobalMissionMap().values().stream()
                .filter(Mission::isShipMassacreActive)
                .filter(mission -> currentType == null || currentType.equals(mission.getType()))
                .collect(Collectors.toList());
        Map<TargetType, CibleStats> stats = computeFactionStats(massacreMissions);

        // Supprimer toutes les lignes après l'en-tête
        if (factionStats.getChildren().size() > 1) {
            factionStats.getChildren().remove(1, factionStats.getChildren().size());
        }
        factionStats.displayStats(stats);

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


    @Override
    public void refreshUI() {
        updateFactionStats(dashboardContext.getCurrentFilter(),dashboardContext.getCurrentTypeFilter());
    }
}
