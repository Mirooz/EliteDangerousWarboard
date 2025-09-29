package be.mirooz.elitedangerous.dashboard.controller;

import be.mirooz.elitedangerous.dashboard.model.Mission;
import be.mirooz.elitedangerous.dashboard.model.MissionStatus;
import be.mirooz.elitedangerous.dashboard.model.MissionType;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Contrôleur pour l'en-tête du dashboard
 */
public class HeaderController implements Initializable {

    @FXML
    private Label missionCountLabel;
    
    @FXML
    private Label missionCountTextLabel;
    
    @FXML
    private Label creditsLabel;
    
    @FXML
    private Label creditsTextLabel;
    
    @FXML
    private VBox thirdStatBox;
    
    @FXML
    private Label thirdStatLabel;
    
    @FXML
    private Label thirdStatTextLabel;
    
    @FXML
    private Button refreshButton;
    
    @FXML
    private Label statusLabel;
    
    private MissionStatus currentFilter = MissionStatus.ACTIVE;
    private List<Mission> allMissions;
    private Runnable refreshCallback;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialisation si nécessaire
    }
    
    public void setRefreshCallback(Runnable callback) {
        this.refreshCallback = callback;
    }
    
    public void setAllMissions(List<Mission> missions) {
        this.allMissions = missions;
    }
    
    public void setCurrentFilter(MissionStatus filter) {
        this.currentFilter = filter;
    }
    
    @FXML
    private void refreshMissions() {
        if (refreshCallback != null) {
            refreshCallback.run();
        }
    }
    
    public void updateStats(List<Mission> filteredMissions) {
        if (filteredMissions == null || filteredMissions.isEmpty()) {
            missionCountLabel.setText("0");
            creditsLabel.setText("0");
            thirdStatBox.setVisible(false);
            return;
        }
        
        // Compter les missions
        int missionCount = filteredMissions.size();
        missionCountLabel.setText(String.valueOf(missionCount));
        
        // Calculer les crédits selon le filtre
        long totalCredits = 0;
        String creditsText = "CRÉDITS";
        String thirdStatText = "";
        int thirdStatValue = 0;
        
        if (currentFilter == MissionStatus.ACTIVE) {
            // Vue active : crédits espérés
            totalCredits = filteredMissions.stream().mapToLong(Mission::getReward).sum();
            creditsText = "CRÉDITS ESPÉRÉS";
            missionCountTextLabel.setText("MISSIONS ACTIVES");
            
            // Troisième stat : factions uniques
            thirdStatValue = (int) filteredMissions.stream().map(Mission::getFaction).distinct().count();
            thirdStatText = "FACTIONS";
            thirdStatBox.setVisible(true);
            
        } else if (currentFilter == MissionStatus.COMPLETED) {
            // Vue complétée : crédits gagnés
            totalCredits = filteredMissions.stream().mapToLong(Mission::getReward).sum();
            creditsText = "CRÉDITS GAGNÉS";
            missionCountTextLabel.setText("MISSIONS COMPLÉTÉES");
            
            // Troisième stat : total des kills
            thirdStatValue = filteredMissions.stream().mapToInt(Mission::getTargetCount).sum();
            thirdStatText = "KILLS TOTAL";
            thirdStatBox.setVisible(true);
            
        } else if (currentFilter == MissionStatus.FAILED) {
            // Vue abandonnée : crédits perdus
            totalCredits = filteredMissions.stream().mapToLong(Mission::getReward).sum();
            creditsText = "CRÉDITS PERDUS";
            missionCountTextLabel.setText("MISSIONS ABANDONNÉES");
            
            // Troisième stat : factions uniques
            thirdStatValue = (int) filteredMissions.stream().map(Mission::getFaction).distinct().count();
            thirdStatText = "FACTIONS";
            thirdStatBox.setVisible(true);
            
        } else {
            // Vue toutes : afficher les 3 stats séparément
            long activeCredits = allMissions.stream()
                    .filter(m -> m.getType() == MissionType.MASSACRE && m.getStatus() == MissionStatus.ACTIVE)
                    .mapToLong(Mission::getReward).sum();
            
            long completedCredits = allMissions.stream()
                    .filter(m -> m.getType() == MissionType.MASSACRE && m.getStatus() == MissionStatus.COMPLETED)
                    .mapToLong(Mission::getReward).sum();
            
            long failedCredits = allMissions.stream()
                    .filter(m -> m.getType() == MissionType.MASSACRE && m.getStatus() == MissionStatus.FAILED)
                    .mapToLong(Mission::getReward).sum();
            
            // Première colonne : missions actives
            int activeCount = (int) allMissions.stream()
                    .filter(m -> m.getType() == MissionType.MASSACRE && m.getStatus() == MissionStatus.ACTIVE)
                    .count();
            missionCountLabel.setText(String.valueOf(activeCount));
            missionCountTextLabel.setText("MISSIONS ACTIVES");
            
            // Deuxième colonne : crédits gagnés
            creditsLabel.setText(String.format("%,d", completedCredits));
            creditsTextLabel.setText("CRÉDITS GAGNÉS");
            
            // Troisième colonne : crédits perdus
            thirdStatLabel.setText(String.format("%,d", failedCredits));
            thirdStatTextLabel.setText("CRÉDITS PERDUS");
            thirdStatBox.setVisible(true);
        }
        
        // Mettre à jour les labels seulement si ce n'est pas la vue "toutes"
        if (currentFilter != null) {
            creditsLabel.setText(String.format("%,d", totalCredits));
            creditsTextLabel.setText(creditsText);
            thirdStatLabel.setText(String.valueOf(thirdStatValue));
            thirdStatTextLabel.setText(thirdStatText);
        }
        
        // Appliquer les couleurs selon le filtre
        updateStatColors();
    }
    
    private void updateStatColors() {
        // Réinitialiser les couleurs
        missionCountLabel.getStyleClass().removeAll("stat-active", "stat-completed", "stat-failed");
        creditsLabel.getStyleClass().removeAll("stat-active", "stat-completed", "stat-failed");
        thirdStatLabel.getStyleClass().removeAll("stat-active", "stat-completed", "stat-failed");
        
        if (currentFilter == MissionStatus.ACTIVE) {
            missionCountLabel.getStyleClass().add("stat-active");
            creditsLabel.getStyleClass().add("stat-active");
            thirdStatLabel.getStyleClass().add("stat-active");
        } else if (currentFilter == MissionStatus.COMPLETED) {
            missionCountLabel.getStyleClass().add("stat-completed");
            creditsLabel.getStyleClass().add("stat-completed");
            thirdStatLabel.getStyleClass().add("stat-completed");
        } else if (currentFilter == MissionStatus.FAILED) {
            missionCountLabel.getStyleClass().add("stat-failed");
            creditsLabel.getStyleClass().add("stat-failed");
            thirdStatLabel.getStyleClass().add("stat-failed");
        } else {
            // Vue "toutes" : couleurs spécifiques pour chaque colonne
            missionCountLabel.getStyleClass().add("stat-active");    // Missions actives en orange
            creditsLabel.getStyleClass().add("stat-completed");      // Crédits gagnés en bleu
            thirdStatLabel.getStyleClass().add("stat-failed");       // Crédits perdus en rouge
        }
    }
    
    public void setStatusText(String text) {
        if (statusLabel != null) {
            statusLabel.setText(text);
        }
    }
}
