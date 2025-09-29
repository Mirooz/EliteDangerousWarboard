package be.mirooz.elitedangerous.dashboard.controller;

import be.mirooz.elitedangerous.dashboard.model.Mission;
import be.mirooz.elitedangerous.dashboard.model.MissionStatus;
import be.mirooz.elitedangerous.dashboard.model.MissionType;
import be.mirooz.elitedangerous.dashboard.model.SourceFactionStats;
import be.mirooz.elitedangerous.dashboard.model.TargetFactionStats;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Contrôleur pour le pied de page du dashboard
 */
public class FooterController implements Initializable {

    // Constantes pour garantir l'alignement
    private static final double COL_WIDTH_TARGET = 150;
    private static final double COL_WIDTH_SOURCE = 180;
    private static final double COL_WIDTH_KILLS  = 80;

    @FXML
    private VBox factionStats;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialisation si nécessaire
    }
    
    public void updateFactionStats(List<Mission> massacreMissions) {
        List<TargetFactionStats> stats = computeFactionStats(massacreMissions);

        // Supprimer toutes les lignes après l'en-tête
        if (factionStats.getChildren().size() > 1) {
            factionStats.getChildren().remove(1, factionStats.getChildren().size());
        }

        String lastTargetFaction = null;

        for (TargetFactionStats stat : stats) {
            for (SourceFactionStats src : stat.getSources()) {
                HBox row = new HBox(0);
                row.setAlignment(Pos.CENTER_LEFT);
                row.getStyleClass().add("faction-row");

                Label targetLabel = new Label(
                        lastTargetFaction != null && lastTargetFaction.equals(stat.getTargetFaction())
                                ? "" : stat.getTargetFaction()
                );
                targetLabel.setPrefWidth(COL_WIDTH_TARGET);
                targetLabel.getStyleClass().add("faction-col");

                Label sourceLabel = new Label(src.getSourceFaction());
                sourceLabel.setPrefWidth(COL_WIDTH_SOURCE);
                sourceLabel.getStyleClass().add("faction-col");

                Label killsLabel = new Label(String.valueOf(src.getKills()));
                killsLabel.setPrefWidth(COL_WIDTH_KILLS);
                killsLabel.getStyleClass().addAll("faction-col", "kills");

                row.getChildren().addAll(targetLabel, sourceLabel, killsLabel);
                factionStats.getChildren().add(row);

                lastTargetFaction = stat.getTargetFaction();
            }

            // Ligne total
            HBox totalRow = new HBox(0);
            totalRow.setAlignment(Pos.CENTER_LEFT);
            totalRow.getStyleClass().add("faction-total");

            Label totalLabel = new Label(""); // pas de répétition
            totalLabel.setPrefWidth(COL_WIDTH_TARGET);
            Label textLabel = new Label("➡ Total");
            textLabel.setPrefWidth(COL_WIDTH_SOURCE);
            textLabel.setAlignment(Pos.CENTER);
            textLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #FF6B00;"); // orange Elite
            textLabel.getStyleClass().add("faction-col");

            Label totalKills = new Label(String.valueOf(stat.getTotalKills()));
            totalKills.setPrefWidth(COL_WIDTH_KILLS);
            totalKills.setAlignment(Pos.CENTER_RIGHT);
            totalKills.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #FF6B00;"); // orange Elite
            totalKills.getStyleClass().addAll("faction-col", "kills");

            totalRow.getChildren().addAll(totalLabel, textLabel, totalKills);
            factionStats.getChildren().add(totalRow);

            lastTargetFaction = null;
        }
    }

    private List<TargetFactionStats> computeFactionStats(List<Mission> massacreMissions) {
        Map<String, Map<String, Integer>> groupedKills = new HashMap<>();

        for (Mission mission : massacreMissions) {
            if (mission.getStatus() == MissionStatus.ACTIVE && mission.getTargetFaction() != null) {
                String targetFaction = mission.getTargetFaction();
                String sourceFaction = mission.getFaction();

                groupedKills
                        .computeIfAbsent(targetFaction, k -> new HashMap<>())
                        .merge(sourceFaction, mission.getTargetCount(), Integer::sum);
            }
        }

        List<TargetFactionStats> results = new ArrayList<>();

        for (Map.Entry<String, Map<String, Integer>> entry : groupedKills.entrySet()) {
            String targetFaction = entry.getKey();
            Map<String, Integer> sourceMap = entry.getValue();

            int totalKills = sourceMap.values().stream().max(Integer::compareTo).orElse(0);

            List<SourceFactionStats> sources = sourceMap.entrySet().stream()
                    .map(e -> new SourceFactionStats(e.getKey(), e.getValue()))
                    .toList();

            results.add(new TargetFactionStats(targetFaction, totalKills, sources));
        }

        return results;
    }
}
