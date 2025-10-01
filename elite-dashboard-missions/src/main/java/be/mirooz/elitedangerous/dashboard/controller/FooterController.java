package be.mirooz.elitedangerous.dashboard.controller;

import be.mirooz.elitedangerous.dashboard.model.*;
import be.mirooz.elitedangerous.dashboard.model.enums.MissionStatus;
import be.mirooz.elitedangerous.dashboard.model.enums.MissionType;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

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
    
    @FXML
    private Button massacreSearchButton;
    

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

    @FXML
    private void openMassacreSearchDialog() {
        try {
            // Charger le FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/massacre-search-dialog.fxml"));
            StackPane dialogContent = loader.load();
            MassacreSearchDialogController controller = loader.getController();

            // Créer la scène
            Scene scene = new Scene(dialogContent, 1000, 700);
            scene.getStylesheets().add(getClass().getResource("/css/elite-theme.css").toExternalForm());

            // Créer la fenêtre
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Recherche de Systèmes Massacre");
            dialogStage.setScene(scene);
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setResizable(false);
            
            // Cacher la barre de titre
            dialogStage.initStyle(javafx.stage.StageStyle.UNDECORATED);
            
            // Centrer la fenêtre sur l'écran actuel (celui de la fenêtre principale)
            Stage primaryStage = (Stage) massacreSearchButton.getScene().getWindow();
            dialogStage.initOwner(primaryStage);
            dialogStage.centerOnScreen();

            // Afficher la fenêtre
            dialogStage.showAndWait();

        } catch (Exception e) {
            System.err.println("Erreur lors de l'ouverture de la fenêtre de recherche: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
