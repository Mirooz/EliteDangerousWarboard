package be.mirooz.elitedangerous.dashboard.controller;

import be.mirooz.elitedangerous.dashboard.model.*;
import be.mirooz.elitedangerous.dashboard.service.MissionService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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

    // Constantes pour garantir l'alignement
    private static final double COL_WIDTH_TARGET = 150;
    private static final double COL_WIDTH_SOURCE = 180;
    private static final double COL_WIDTH_KILLS  = 80;

    @FXML
    private VBox missionsList;
    
    @FXML
    private Label statusLabel;
    
    @FXML
    private Button refreshButton;
    
    @FXML
    private Label allianceStats;

    @FXML
    private VBox factionStats;
    
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
                .toList();
        
        for (Mission mission : filteredMissions) {
            VBox missionCard = createMassacreMissionCard(mission);
            missionsList.getChildren().add(missionCard);
        }
        
        // Mettre à jour les statistiques par faction (toujours basées sur les missions actives)
        List<Mission> activeMissions = allMissions.stream()
                .filter(mission -> mission.getType() == MissionType.MASSACRE && mission.getStatus() == MissionStatus.ACTIVE)
                .collect(java.util.stream.Collectors.toList());
        updateFactionStats(activeMissions);
        
        // Mettre à jour les statistiques du dashboard
        updateDashboardStats(filteredMissions);
        
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
        mainRow.setSpacing(15);
        mainRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        // Faction - largeur fixe pour alignement
        Label factionLabel = new Label(mission.getFaction());
        factionLabel.getStyleClass().add("massacre-faction");
        factionLabel.setPrefWidth(180);
        factionLabel.setMinWidth(180);
        factionLabel.setMaxWidth(180);
        
        // Clan cible et système - largeur fixe pour alignement
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
        targetLabel.setMinWidth(200);
        targetLabel.setMaxWidth(200);
        
        // Progression des kills - conteneur avec largeur fixe
        HBox killsSection = new HBox();
        killsSection.setSpacing(8);
        killsSection.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        killsSection.setPrefWidth(120);
        killsSection.setMinWidth(120);
        killsSection.setMaxWidth(120);
        
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
        killsLabel.setPrefWidth(50);
        killsLabel.setMinWidth(50);
        killsLabel.setMaxWidth(50);
        
        ProgressBar progressBar = new ProgressBar();
        progressBar.setProgress((double) mission.getCurrentCount() / mission.getTargetCount());
        progressBar.getStyleClass().add("massacre-progress");
        progressBar.setPrefWidth(60);
        progressBar.setMinWidth(60);
        progressBar.setMaxWidth(60);
        
        killsSection.getChildren().addAll(killsLabel, progressBar);
        
        // Récompense - largeur fixe pour alignement
        Label rewardLabel = new Label(String.format("%,d Cr", mission.getReward()));
        rewardLabel.getStyleClass().add("massacre-reward");
        rewardLabel.setPrefWidth(140);
        rewardLabel.setMinWidth(140);
        rewardLabel.setMaxWidth(140);
        
        // Temps d'acceptation et temps restant - largeur fixe pour alignement
        VBox timeSection = new VBox();
        timeSection.setSpacing(2);
        timeSection.setPrefWidth(150);
        timeSection.setMinWidth(150);
        timeSection.setMaxWidth(150);
        
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
        List<TargetFactionStats> stats = computeFactionStats(massacreMissions);

        // Supprimer toutes les lignes après l'en-tête
        if (factionStats.getChildren().size() > 1) {
            factionStats.getChildren().remove(1, factionStats.getChildren().size());
        }

        String lastTargetFaction = null;

        for (TargetFactionStats stat : stats) {
            for (SourceFactionStats src : stat.getSources()) {
                HBox row = new HBox(0);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
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
            totalRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
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
    
    private void updateDashboardStats(List<Mission> missions) {
        if (missions == null || missions.isEmpty()) {
            missionCountLabel.setText("0");
            creditsLabel.setText("0");
            thirdStatBox.setVisible(false);
            return;
        }
        
        // Compter les missions
        int missionCount = missions.size();
        missionCountLabel.setText(String.valueOf(missionCount));
        
        // Calculer les crédits selon le filtre
        long totalCredits = 0;
        String creditsText = "CRÉDITS";
        String thirdStatText = "";
        int thirdStatValue = 0;
        
        if (currentFilter == MissionStatus.ACTIVE) {
            // Vue active : crédits espérés
            totalCredits = missions.stream().mapToLong(Mission::getReward).sum();
            creditsText = "CRÉDITS ESPÉRÉS";
            missionCountTextLabel.setText("MISSIONS ACTIVES");
            
            // Troisième stat : factions uniques
            thirdStatValue = (int) missions.stream().map(Mission::getFaction).distinct().count();
            thirdStatText = "FACTIONS";
            thirdStatBox.setVisible(true);
            
        } else if (currentFilter == MissionStatus.COMPLETED) {
            // Vue complétée : crédits gagnés
            totalCredits = missions.stream().mapToLong(Mission::getReward).sum();
            creditsText = "CRÉDITS GAGNÉS";
            missionCountTextLabel.setText("MISSIONS COMPLÉTÉES");
            
            // Troisième stat : total des kills
            thirdStatValue = missions.stream().mapToInt(Mission::getTargetCount).sum();
            thirdStatText = "KILLS TOTAL";
            thirdStatBox.setVisible(true);
            
        } else if (currentFilter == MissionStatus.FAILED) {
            // Vue abandonnée : crédits perdus
            totalCredits = missions.stream().mapToLong(Mission::getReward).sum();
            creditsText = "CRÉDITS PERDUS";
            missionCountTextLabel.setText("MISSIONS ABANDONNÉES");
            
            // Troisième stat : factions uniques
            thirdStatValue = (int) missions.stream().map(Mission::getFaction).distinct().count();
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
