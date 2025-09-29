package be.mirooz.elitedangerous.dashboard.controller;

import be.mirooz.elitedangerous.dashboard.model.Mission;
import be.mirooz.elitedangerous.dashboard.model.MissionStatus;
import be.mirooz.elitedangerous.dashboard.model.MissionType;
import be.mirooz.elitedangerous.dashboard.service.MissionService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Contrôleur principal du dashboard Elite Dangerous
 */
public class DashboardController implements Initializable {

    @FXML
    private VBox missionsList;
    
    @FXML
    private Label statusLabel;
    
    @FXML
    private Button refreshButton;
    
    @FXML
    private Label allianceStats;
    
    @FXML
    private Label empireStats;
    
    @FXML
    private Label federationStats;
    
    @FXML
    private Button activeFilterButton;
    
    @FXML
    private Button completedFilterButton;
    
    @FXML
    private Button abandonedFilterButton;
    
    @FXML
    private Button allFilterButton;
    
    private MissionService missionService;
    private List<Mission> allMissions = new ArrayList<>();
    private MissionStatus currentFilter = MissionStatus.ACTIVE;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        missionService = new MissionService();
        loadMissions();
    }
    
    @FXML
    private void refreshMissions(){
        statusLabel.setText("ACTUALISATION...");
        loadMissions();
        statusLabel.setText("SYSTÈME EN LIGNE");
    }
    
    private void loadMissions() {
        missionsList.getChildren().clear();
        allMissions = missionService.getActiveMissions();
        applyCurrentFilter();
    }
    
    private void applyCurrentFilter() {
        missionsList.getChildren().clear();
        
        // Filtrer les missions selon le filtre actuel
        List<Mission> filteredMissions = allMissions.stream()
                .filter(mission -> mission.getType() == MissionType.MASSACRE)
                .filter(mission -> currentFilter == null || mission.getStatus() == currentFilter)
                .sorted((m1, m2) -> m1.getFaction().compareTo(m2.getFaction())) // Trier par faction
                .collect(java.util.stream.Collectors.toList());
        
        for (Mission mission : filteredMissions) {
            VBox missionCard = createMassacreMissionCard(mission);
            missionsList.getChildren().add(missionCard);
        }
        
        // Mettre à jour les statistiques par faction (toujours basées sur les missions actives)
        List<Mission> activeMissions = allMissions.stream()
                .filter(mission -> mission.getType() == MissionType.MASSACRE && mission.getStatus() == MissionStatus.ACTIVE)
                .collect(java.util.stream.Collectors.toList());
        updateFactionStats(activeMissions);
        
        // Mettre à jour l'apparence des boutons de filtre
        updateFilterButtons();
    }
    
    private void updateFilterButtons() {
        // Réinitialiser tous les boutons
        activeFilterButton.getStyleClass().removeAll("active");
        completedFilterButton.getStyleClass().removeAll("active");
        abandonedFilterButton.getStyleClass().removeAll("active");
        allFilterButton.getStyleClass().removeAll("active");
        
        // Activer le bouton correspondant au filtre actuel
        if (currentFilter == MissionStatus.ACTIVE) {
            activeFilterButton.getStyleClass().add("active");
        } else if (currentFilter == MissionStatus.COMPLETED) {
            completedFilterButton.getStyleClass().add("active");
        } else if (currentFilter == MissionStatus.FAILED) {
            abandonedFilterButton.getStyleClass().add("active");
        } else if (currentFilter == null) {
            allFilterButton.getStyleClass().add("active");
        }
    }
    
    private VBox createMissionCard(Mission mission) {
        VBox card = new VBox();
        card.getStyleClass().add("mission-card");
        card.setSpacing(8);
        card.setPadding(new Insets(15));
        
        // En-tête de la mission
        HBox header = new HBox();
        header.setSpacing(15);
        
        VBox titleSection = new VBox();
        titleSection.setSpacing(2);
        
        Label title = new Label(mission.getName());
        title.getStyleClass().add("mission-title");
        
        Label type = new Label(mission.getType().getDisplayName());
        type.getStyleClass().add("mission-type");
        
        titleSection.getChildren().addAll(title, type);
        
        // Statut et récompense
        VBox statusSection = new VBox();
        statusSection.setSpacing(5);
        
        Label status = new Label(mission.getStatus().getDisplayName());
        status.getStyleClass().add("mission-status");
        status.getStyleClass().add(mission.getStatus().name().toLowerCase());
        
        Label reward = new Label(String.format("%,d Cr", mission.getReward()));
        reward.getStyleClass().add("mission-reward");
        
        statusSection.getChildren().addAll(status, reward);
        
        header.getChildren().addAll(titleSection, statusSection);
        
        // Détails de la mission
        VBox details = new VBox();
        details.setSpacing(3);
        
        if (mission.getFaction() != null) {
            Label faction = new Label("Faction: " + mission.getFaction());
            faction.getStyleClass().add("mission-faction");
            details.getChildren().add(faction);
        }
        
        if (mission.getDestination() != null) {
            Label destination = new Label("Destination: " + mission.getDestination());
            destination.getStyleClass().add("mission-details");
            details.getChildren().add(destination);
        }
        
        if (mission.getOrigin() != null) {
            Label origin = new Label("Origine: " + mission.getOrigin());
            origin.getStyleClass().add("mission-details");
            details.getChildren().add(origin);
        }
        
        if (mission.getExpiry() != null) {
            Label expiry = new Label("Expire: " + mission.getExpiry().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            expiry.getStyleClass().add("mission-details");
            details.getChildren().add(expiry);
        }
        
        // Barre de progression si applicable
        if (mission.getTargetCount() > 0) {
            HBox progressSection = new HBox();
            progressSection.setSpacing(10);
            
            Label progressLabel = new Label(String.format("Progression: %d/%d", 
                mission.getCurrentCount(), mission.getTargetCount()));
            progressLabel.getStyleClass().add("mission-details");
            
            ProgressBar progressBar = new ProgressBar();
            progressBar.setProgress((double) mission.getCurrentCount() / mission.getTargetCount());
            progressBar.getStyleClass().add("progress-bar");
            progressBar.setPrefWidth(200);
            
            progressSection.getChildren().addAll(progressLabel, progressBar);
            details.getChildren().add(progressSection);
        }
        
        card.getChildren().addAll(header, details);
        
        return card;
    }
    
    private VBox createMassacreMissionCard(Mission mission) {
        VBox card = new VBox();
        card.getStyleClass().add("mission-card");
        card.setSpacing(5);
        card.setPadding(new Insets(10));
        
        // Ligne compacte avec les informations essentielles
        HBox mainRow = new HBox();
        mainRow.setSpacing(20);
        mainRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        // Faction
        Label factionLabel = new Label(mission.getFaction());
        factionLabel.getStyleClass().add("massacre-faction");
        factionLabel.setPrefWidth(200);
        
        // Clan cible et système
        String targetInfo = "";
        if (mission.getTargetFaction() != null) {
            targetInfo = mission.getTargetFaction();
        }
        if (mission.getTargetSystem() != null) {
            targetInfo += (targetInfo.isEmpty() ? "" : " - ") + mission.getTargetSystem();
        }
        if (targetInfo.isEmpty()) {
            targetInfo = "Pirates";
        }
        
        Label targetLabel = new Label(targetInfo);
        targetLabel.getStyleClass().add("massacre-target");
        targetLabel.setPrefWidth(200);
        
        // Progression des kills
        HBox killsSection = new HBox();
        killsSection.setSpacing(10);
        killsSection.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        // Afficher x/y pour les missions actives, y/y pour les missions complétées
        String killsText;
        if (mission.getStatus() == MissionStatus.COMPLETED) {
            // Pour les missions complétées, afficher y/y
            int targetCount = mission.getTargetCount();
            killsText = String.format("%d/%d", targetCount, targetCount);
        } else {
            // Pour les missions actives, afficher x/y
            int currentCount = mission.getCurrentCount();
            int targetCount = mission.getTargetCount();
            killsText = String.format("%d/%d", currentCount, targetCount);
        }
        
        Label killsLabel = new Label(killsText);
        killsLabel.getStyleClass().add("massacre-kills");
        
        ProgressBar progressBar = new ProgressBar();
        progressBar.setProgress((double) mission.getCurrentCount() / mission.getTargetCount());
        progressBar.getStyleClass().add("massacre-progress");
        progressBar.setPrefWidth(100);
        
        killsSection.getChildren().addAll(killsLabel, progressBar);
        
        // Récompense
        Label rewardLabel = new Label(String.format("%,d Cr", mission.getReward()));
        rewardLabel.getStyleClass().add("massacre-reward");
        rewardLabel.setPrefWidth(120);
        
        // Temps d'acceptation et temps restant
        VBox timeSection = new VBox();
        timeSection.setSpacing(2);
        timeSection.setPrefWidth(150);
        
        if (mission.getAcceptedTime() != null) {
            Label acceptedLabel = new Label("Accepté: " + mission.getAcceptedTime().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")));
            acceptedLabel.getStyleClass().add("mission-time");
            
            if (mission.getExpiry() != null) {
                String timeRemaining;
                if (mission.getStatus() == MissionStatus.COMPLETED) {
                    timeRemaining = "Terminée";
                } else {
                    long hoursRemaining = java.time.Duration.between(LocalDateTime.now(), mission.getExpiry()).toHours();
                    if (hoursRemaining > 0) {
                        timeRemaining = String.format("Restant: %dh", hoursRemaining);
                    } else {
                        timeRemaining = "Expirée";
                    }
                }
                Label remainingLabel = new Label(timeRemaining);
                if (mission.getStatus() == MissionStatus.COMPLETED) {
                    remainingLabel.getStyleClass().add("mission-time-completed");
                } else {
                    long hoursRemaining = java.time.Duration.between(LocalDateTime.now(), mission.getExpiry()).toHours();
                    remainingLabel.getStyleClass().add(hoursRemaining > 24 ? "mission-time" : "mission-time-urgent");
                }
                
                timeSection.getChildren().addAll(acceptedLabel, remainingLabel);
            } else {
                timeSection.getChildren().add(acceptedLabel);
            }
        }
        
        mainRow.getChildren().addAll(factionLabel, targetLabel, killsSection, rewardLabel, timeSection);
        
        card.getChildren().add(mainRow);
        
        return card;
    }

    private void updateFactionStats(List<Mission> massacreMissions) {
        // targetFaction -> (sourceFaction -> kills cumulés)
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

        // Construire le résumé
        StringBuilder summaryText = new StringBuilder();

        for (Map.Entry<String, Map<String, Integer>> entry : groupedKills.entrySet()) {
            String targetFaction = entry.getKey();
            Map<String, Integer> sourceMap = entry.getValue();

            // Max des kills parmi les sources regroupées
            int totalKills = sourceMap.values().stream().max(Integer::compareTo).orElse(0);

            if (summaryText.length() > 0) {
                summaryText.append("\n");
            }

            summaryText.append(String.format("🎯 %s: %d kills total\n", targetFaction, totalKills));

            // Détail par source faction
            for (Map.Entry<String, Integer> sourceEntry : sourceMap.entrySet()) {
                summaryText.append(String.format("• %s: %d kills\n", sourceEntry.getKey(), sourceEntry.getValue()));
            }
        }

        // Mise à jour du label
        if (allianceStats != null) {
            allianceStats.setText(summaryText.length() > 0 ? summaryText.toString() : "Aucune mission active");
        }
        if (empireStats != null) empireStats.setText("");
        if (federationStats != null) federationStats.setText("");
    }
    @FXML
    private void filterActiveMissions() {
        currentFilter = MissionStatus.ACTIVE;
        applyCurrentFilter();
    }
    
    @FXML
    private void filterCompletedMissions() {
        currentFilter = MissionStatus.COMPLETED;
        applyCurrentFilter();
    }
    
    @FXML
    private void filterAbandonedMissions() {
        currentFilter = MissionStatus.FAILED;
        applyCurrentFilter();
    }
    
    @FXML
    private void filterAllMissions() {
        currentFilter = null;
        applyCurrentFilter();
    }
}
