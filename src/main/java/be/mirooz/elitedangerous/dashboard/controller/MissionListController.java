package be.mirooz.elitedangerous.dashboard.controller;

import be.mirooz.elitedangerous.dashboard.model.Mission;
import be.mirooz.elitedangerous.dashboard.model.enums.MissionStatus;
import be.mirooz.elitedangerous.dashboard.model.enums.MissionType;
import be.mirooz.elitedangerous.dashboard.model.MissionsList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Contrôleur pour la liste des missions
 */
public class MissionListController implements Initializable {

    @FXML
    private VBox missionsList;
    
    @FXML
    private Button activeFilterButton;
    
    @FXML
    private Button completedFilterButton;
    
    @FXML
    private Button abandonedFilterButton;
    
    @FXML
    private Button allFilterButton;
    
    private MissionStatus currentFilter = MissionStatus.ACTIVE;
    private MissionsList allMissionsList = MissionsList.getInstance();

    private Consumer<MissionStatus> filterChangeCallback;
    
    // Conteneur pour les popups
    private StackPane popupContainer;
    
    // Méthode pour créer un tooltip avec délai
    private Tooltip createDelayedTooltip(String text) {
        Tooltip tooltip = new Tooltip(text);
        tooltip.setShowDelay(Duration.millis(300)); // 0.3 seconde de délai
        tooltip.setHideDelay(Duration.millis(100));
        return tooltip;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialisation si nécessaire
    }
    
    public void setPopupContainer(StackPane container) {
        this.popupContainer = container;
    }
    
    private void showSystemCopiedPopup(String message, javafx.scene.input.MouseEvent event) {
        if (popupContainer == null) return;
        
        // Créer le popup
        VBox popup = new VBox();
        popup.getStyleClass().add("system-copied-popup");
        popup.setAlignment(Pos.CENTER);
        popup.setSpacing(3);
        popup.setPadding(new Insets(8, 16, 8, 16));
        
        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("popup-title");
        
        popup.getChildren().add(messageLabel);
        
        // Taille compacte
        popup.setMinSize(120, 40);
        popup.setPrefSize(120, 40);
        popup.setMaxSize(120, 40);
        
        // Positionner le popup avec le coin gauche en bas à droite de la souris
        double mouseX = event.getSceneX();
        double mouseY = event.getSceneY();
        popup.setTranslateX(mouseX + 5); // 5px à droite de la souris
        popup.setTranslateY(mouseY - 40); // 40px au-dessus de la souris (hauteur du popup)
        
        // Ajouter au conteneur
        popupContainer.getChildren().add(popup);
        
        // Animation d'apparition et disparition
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), popup);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        
        PauseTransition pause = new PauseTransition(Duration.millis(1000));
        
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), popup);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        
        SequentialTransition sequence = new SequentialTransition(fadeIn, pause, fadeOut);
        sequence.setOnFinished(e -> popupContainer.getChildren().remove(popup));
        sequence.play();
    }
    
    private void copySystemToClipboard(String systemName, javafx.scene.input.MouseEvent event) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(systemName);
        clipboard.setContent(content);
        
        showSystemCopiedPopup("Système copié", event);
    }
    
    public void setFilterChangeCallback(Consumer<MissionStatus> callback) {
        this.filterChangeCallback = callback;
    }
    

    
    public void setCurrentFilter(MissionStatus filter) {
        this.currentFilter = filter;
        updateFilterButtons();
    }
    
    public void applyFilter() {
        missionsList.getChildren().clear();
        List<Mission> filteredMissions = allMissionsList.getGlobalMissionMap().values().stream()
                .filter(mission -> mission.getType() == MissionType.MASSACRE)
                .filter(mission -> currentFilter == null || mission.getStatus() == currentFilter)
                .sorted((m1, m2) -> {
                    // Trier par date d'acceptation (plus ancienne en premier)
                    if (m1.getAcceptedTime() == null && m2.getAcceptedTime() == null) return 0;
                    if (m1.getAcceptedTime() == null) return 1; // Les missions sans date vont à la fin
                    if (m2.getAcceptedTime() == null) return -1;
                    return m1.getAcceptedTime().compareTo(m2.getAcceptedTime());
                })
                .toList();
        
        for (Mission mission : filteredMissions) {
            VBox missionCard = createMassacreMissionCard(mission);
            missionsList.getChildren().add(missionCard);
        }
        
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
    
    private VBox createMassacreMissionCard(Mission mission) {
        VBox card = new VBox();
        card.getStyleClass().add("mission-card");
        
        // Ajouter une classe CSS spéciale si la mission est active et complète
        if (mission.getStatus() == MissionStatus.ACTIVE && 
            mission.getCurrentCount() >= mission.getTargetCount()) {
            card.getStyleClass().add("mission-card-complete");
        }
        
        card.setSpacing(5);
        card.setPadding(new Insets(10));
        
        // Ligne compacte avec les informations essentielles
        HBox mainRow = new HBox();
        mainRow.setSpacing(15);
        mainRow.setAlignment(Pos.CENTER_LEFT);
        
        // Faction - largeur fixe pour alignement
        Label factionLabel = new Label(mission.getFaction());
        factionLabel.getStyleClass().add("massacre-faction");
        factionLabel.setPrefWidth(180);
        factionLabel.setMinWidth(180);
        factionLabel.setMaxWidth(180);
        
        // Ajouter tooltip et clic pour copier le système d'origine
        if (mission.getOriginSystem() != null && !mission.getOriginSystem().isEmpty()) {
            factionLabel.setTooltip(createDelayedTooltip(mission.getOriginSystem() +" | " + mission.getOriginStation()));
            factionLabel.getStyleClass().add("clickable-system-source");
            factionLabel.setOnMouseClicked(e -> copySystemToClipboard(mission.getOriginSystem(), e));
        } else {
            // Debug: afficher un tooltip même si pas de système d'origine
            factionLabel.setTooltip(createDelayedTooltip("Système d'origine: Non défini"));
        }
        
        // Clan cible et système - largeur fixe pour alignement
        String targetInfo = "";
        if (mission.getTargetFaction() != null) {
            targetInfo = mission.getTargetFaction();
        }
        if (targetInfo.isEmpty()) {
            targetInfo = "Pirates";
        }
        
        Label targetLabel = new Label(targetInfo);
        targetLabel.getStyleClass().add("massacre-target");
        targetLabel.setPrefWidth(200);
        targetLabel.setMinWidth(200);
        targetLabel.setMaxWidth(200);

         if (mission.getDestinationSystem() != null && !mission.getDestinationSystem().isEmpty()) {
             // Fallback: utiliser destinationSystem si targetSystem n'est pas défini
             targetLabel.setTooltip(createDelayedTooltip(mission.getDestinationSystem()));
             targetLabel.getStyleClass().add("clickable-system-target");
             targetLabel.setOnMouseClicked(e -> copySystemToClipboard(mission.getDestinationSystem(), e));
         }
        
        // Progression des kills - conteneur avec largeur fixe
        HBox killsSection = new HBox();
        killsSection.setSpacing(8);
        killsSection.setAlignment(Pos.CENTER_LEFT);
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
        
        // Si la mission est en attente (kills atteints mais pas encore complétée), style bleu
        if (mission.getStatus() == MissionStatus.ACTIVE && 
            mission.getCurrentCount() >= mission.getTargetCount()) {
            killsLabel.getStyleClass().add("massacre-kills-waiting");
        }
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
        
        // Icône Wing - largeur fixe pour alignement
        Label wingLabel = new Label();
        wingLabel.setPrefWidth(30);
        wingLabel.setMinWidth(30);
        wingLabel.setMaxWidth(30);
        wingLabel.setAlignment(Pos.CENTER);
        
        if (mission.isWing()) {
            wingLabel.setText("✈"); // Icône d'avion pour wing
            wingLabel.getStyleClass().add("wing-icon");
            wingLabel.setTooltip(createDelayedTooltip("Mission de Wing"));
        } else {
            wingLabel.setText(""); // Vide pour les missions normales
        }
        
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
        
        mainRow.getChildren().addAll(factionLabel, targetLabel, killsSection, wingLabel, rewardLabel, timeSection);
        
        card.getChildren().add(mainRow);
        
        return card;
    }
    
    @FXML
    private void filterActiveMissions() {
        if (filterChangeCallback != null) {
            filterChangeCallback.accept(MissionStatus.ACTIVE);
        }
    }
    
    @FXML
    private void filterCompletedMissions() {
        if (filterChangeCallback != null) {
            filterChangeCallback.accept(MissionStatus.COMPLETED);
        }
    }
    
    @FXML
    private void filterAbandonedMissions() {
        if (filterChangeCallback != null) {
            filterChangeCallback.accept(MissionStatus.FAILED);
        }
    }
    
    @FXML
    private void filterAllMissions() {
        if (filterChangeCallback != null) {
            filterChangeCallback.accept(null);
        }
    }
}
