package be.mirooz.elitedangerous.dashboard.controller;

import be.mirooz.elitedangerous.dashboard.model.*;
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
import java.util.stream.Collectors;

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
    
    @FXML
    private Label commanderLabel;
    
    @FXML
    private Label systemLabel;
    
    @FXML
    private Label stationLabel;
    
    @FXML
    private Label shipLabel;

    private final MissionsList missionsList = MissionsList.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialisation si nécessaire
    }
    
    /**
     * Met à jour le nom du commandant dans le footer
     */
    public void updateCommanderName(String commanderName) {
        if (commanderLabel != null) {
            if (commanderName != null && !commanderName.isEmpty()) {
                commanderLabel.setText("Commander: " + commanderName);
            } else {
                commanderLabel.setText("Commander: [NON IDENTIFIÉ]");
            }
        }
    }
    
    /**
     * Met à jour les informations du vaisseau dans le footer
     */
    public void updateShipInfo(String system, String station, String ship) {
        if (systemLabel != null) {
            if (system != null && !system.isEmpty()) {
                systemLabel.setText("Système: " + system);
            } else {
                systemLabel.setText("Système: [NON IDENTIFIÉ]");
            }
        }
        
        if (stationLabel != null) {
            if (station != null && !station.isEmpty()) {
                stationLabel.setText("Station: " + station);
            } else {
                stationLabel.setText("Station: [NON IDENTIFIÉE]");
            }
        }
        
        if (shipLabel != null) {
            if (ship != null && !ship.isEmpty()) {
                shipLabel.setText("Vaisseau: " + ship);
            } else {
                shipLabel.setText("Vaisseau: [NON IDENTIFIÉ]");
            }
        }
    }
    
    public void updateFactionStats() {
        // Mettre à jour les statistiques par faction (toujours basées sur les missions actives)
        List<Mission> massacreMissions = missionsList.getGlobalMissionMap().values().stream()
                .filter(mission -> mission.getType() == MissionType.MASSACRE && mission.getStatus() == MissionStatus.ACTIVE)
                .collect(Collectors.toList());
        List<TargetFactionStats> stats = computeFactionStats(massacreMissions);

        // Supprimer toutes les lignes après l'en-tête
        if (factionStats.getChildren().size() > 1) {
            factionStats.getChildren().remove(1, factionStats.getChildren().size());
        }

        String lastTargetFaction = null;

        for (TargetFactionStats stat : stats) {
            // Trouver le plus haut score pour cette faction cible
            int maxKills = stat.getSources().stream()
                    .mapToInt(SourceFactionStats::getKills)
                    .max()
                    .orElse(0);
            
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

                // Affichage conditionnel des kills
                Label killsLabel;
                if (src.getKills() == maxKills) {
                    // Plus haut score : en gras
                    killsLabel = new Label(String.valueOf(src.getKills()));
                    killsLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #FF6B00;");
                } else {
                    // Autres scores : avec écart en vert
                    int difference = maxKills - src.getKills();
                    killsLabel = new Label(src.getKills() + " (-" + difference + ")");
                    killsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #00FF00;"); // vert pour l'écart
                }
                
                killsLabel.setPrefWidth(COL_WIDTH_KILLS);
                killsLabel.setAlignment(Pos.CENTER_RIGHT);
                killsLabel.getStyleClass().addAll("faction-col", "kills");

                row.getChildren().addAll(targetLabel, sourceLabel, killsLabel);
                factionStats.getChildren().add(row);

                lastTargetFaction = stat.getTargetFaction();
            }

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
                        .merge(sourceFaction, mission.getTargetCountLeft(), Integer::sum);
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
