package be.mirooz.elitedangerous.dashboard.controller;

import be.mirooz.elitedangerous.dashboard.model.*;
import be.mirooz.elitedangerous.dashboard.model.enums.TargetType;
import be.mirooz.elitedangerous.dashboard.model.targetpanel.CibleStats;
import be.mirooz.elitedangerous.dashboard.model.targetpanel.SourceFactionStats;
import be.mirooz.elitedangerous.dashboard.model.targetpanel.TargetFactionStats;
import be.mirooz.elitedangerous.dashboard.ui.component.FactionStatsComponent;
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
public class FooterController implements Initializable {

    // Constantes pour garantir l'alignement

    @FXML
    private FactionStatsComponent factionStats;

    @FXML
    private Label commanderLabel;

    @FXML
    private Label systemLabel;

    @FXML
    private Label stationLabel;
    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();

    private final MissionsList missionsList = MissionsList.getInstance();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        stationLabel.textProperty().bind(commanderStatus.getCurrentStationName());
        systemLabel.textProperty().bind(commanderStatus.getCurrentStarSystem());
        commanderLabel.textProperty().bind(commanderStatus.getCommanderName());
    }


    public void updateFactionStats() {
        // Mettre à jour les statistiques par faction (toujours basées sur les missions actives)
        List<Mission> massacreMissions = missionsList.getGlobalMissionMap().values().stream().filter(Mission::isMassacreActive).collect(Collectors.toList());
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


}
