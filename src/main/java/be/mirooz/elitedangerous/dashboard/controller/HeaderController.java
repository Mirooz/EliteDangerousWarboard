package be.mirooz.elitedangerous.dashboard.controller;

import be.mirooz.elitedangerous.dashboard.model.Mission;
import be.mirooz.elitedangerous.dashboard.model.MissionStatus;
import be.mirooz.elitedangerous.dashboard.model.MissionType;
import be.mirooz.elitedangerous.dashboard.model.MissionsList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

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
    private VBox pendingCreditsBox;
    
    @FXML
    private Label pendingCreditsLabel;
    
    @FXML
    private Label pendingCreditsTextLabel;
    
    
    @FXML
    private Label statusLabel;
    
    private MissionStatus currentFilter = MissionStatus.ACTIVE;
    private MissionsList missionsList = MissionsList.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialisation si nécessaire
    }
    
    public void setCurrentFilter(MissionStatus filter) {
        this.currentFilter = filter;
    }
    
    public void updateStats() {
        // Filtrer les missions selon le filtre actuel
        List<Mission> filteredMissions = missionsList.getGlobalMissionMap().values().stream()
                .filter(mission -> mission.getType() == MissionType.MASSACRE)
                .filter(mission -> currentFilter == null || mission.getStatus() == currentFilter)
                .sorted((m1, m2) -> m1.getFaction().compareTo(m2.getFaction()))
                .collect(Collectors.toList());

        if (filteredMissions == null || filteredMissions.isEmpty()) {
            missionCountLabel.setText("0");
            creditsLabel.setText("0");
            thirdStatBox.setVisible(false);
            pendingCreditsBox.setVisible(false);
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
            // Vue active : réorganiser l'ordre des stats
            // 1. Missions actives
            missionCountTextLabel.setText("MISSIONS ACTIVES");
            
            // 2. Factions uniques
            thirdStatValue = (int) filteredMissions.stream().map(Mission::getFaction).distinct().count();
            thirdStatText = "FACTIONS";
            thirdStatBox.setVisible(true);
            
            // 3. Crédits espérés avec crédits déjà possibles en bleu
            totalCredits = filteredMissions.stream().mapToLong(Mission::getReward).sum();
            
            // Calculer les crédits déjà possibles (missions actives complétées)
            long completedCredits = filteredMissions.stream()
                    .filter(mission -> mission.getCurrentCount() >= mission.getTargetCount())
                    .mapToLong(Mission::getReward)
                    .sum();
            
            creditsText = "CRÉDITS ESPÉRÉS";
            creditsLabel.setText(String.format("%,d", totalCredits));
            creditsLabel.setStyle("-fx-text-fill: #FF6B00;"); // Orange pour le total
            creditsTextLabel.setText("CRÉDITS ESPÉRÉS");
            creditsTextLabel.setStyle("-fx-text-fill: #CCCCCC;"); // Couleur par défaut
            
            // Afficher les crédits en attente dans un label séparé
            if (completedCredits > 0) {
                pendingCreditsBox.setVisible(true);
                pendingCreditsLabel.setText(String.format("%,d", completedCredits));
                pendingCreditsLabel.setStyle("-fx-text-fill: #00BFFF;"); // Bleu pour les crédits en attente
                pendingCreditsTextLabel.setText("CRÉDITS EN ATTENTE");
                pendingCreditsTextLabel.setStyle("-fx-text-fill: #00BFFF;"); // Bleu pour le texte
            } else {
                pendingCreditsBox.setVisible(false);
            }
            
        } else if (currentFilter == MissionStatus.COMPLETED) {
            // Vue complétée : crédits gagnés
            totalCredits = filteredMissions.stream().mapToLong(Mission::getReward).sum();
            creditsText = "CRÉDITS GAGNÉS";
            missionCountTextLabel.setText("MISSIONS COMPLÉTÉES");
            
            // Troisième stat : total des kills
            thirdStatValue = filteredMissions.stream().mapToInt(Mission::getTargetCount).sum();
            thirdStatText = "KILLS TOTAL";
            thirdStatBox.setVisible(true);
            pendingCreditsBox.setVisible(false);
            
        } else if (currentFilter == MissionStatus.FAILED) {
            // Vue abandonnée : crédits perdus
            totalCredits = filteredMissions.stream().mapToLong(Mission::getReward).sum();
            creditsText = "CRÉDITS PERDUS";
            missionCountTextLabel.setText("MISSIONS ABANDONNÉES");
            
            // Troisième stat : factions uniques
            thirdStatValue = (int) filteredMissions.stream().map(Mission::getFaction).distinct().count();
            thirdStatText = "FACTIONS";
            thirdStatBox.setVisible(true);
            pendingCreditsBox.setVisible(false);
            
        } else {
            // Vue toutes : afficher les 3 stats séparément
            long activeCredits = missionsList.getGlobalMissionMap().values().stream()
                    .filter(m -> m.getType() == MissionType.MASSACRE && m.getStatus() == MissionStatus.ACTIVE)
                    .mapToLong(Mission::getReward).sum();
            
            long completedCredits = missionsList.getGlobalMissionMap().values().stream()
                    .filter(m -> m.getType() == MissionType.MASSACRE && m.getStatus() == MissionStatus.COMPLETED)
                    .mapToLong(Mission::getReward).sum();
            
            long failedCredits = missionsList.getGlobalMissionMap().values().stream()
                    .filter(m -> m.getType() == MissionType.MASSACRE && m.getStatus() == MissionStatus.FAILED)
                    .mapToLong(Mission::getReward).sum();
            
            // Première colonne : missions actives
            int activeCount = (int) missionsList.getGlobalMissionMap().values().stream()
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
            // Pour la vue ACTIVE, les crédits sont déjà mis à jour avec les couleurs
            if (currentFilter != MissionStatus.ACTIVE) {
                creditsLabel.setText(String.format("%,d", totalCredits));
                creditsTextLabel.setText(creditsText);
            }
            thirdStatLabel.setText(String.valueOf(thirdStatValue));
            thirdStatTextLabel.setText(thirdStatText);
        }
        
        // Masquer les crédits en attente pour les autres vues
        if (currentFilter != MissionStatus.ACTIVE) {
            pendingCreditsBox.setVisible(false);
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
