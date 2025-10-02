package be.mirooz.elitedangerous.dashboard.ui.component;

import be.mirooz.elitedangerous.dashboard.model.Mission;
import be.mirooz.elitedangerous.dashboard.model.enums.MissionStatus;
import be.mirooz.elitedangerous.dashboard.ui.CopyClipboardManager;
import be.mirooz.elitedangerous.dashboard.ui.PopupManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MissionCardComponent extends VBox {


    public static final String TERMINATED = "Terminée";
    public static final String EXPIRED = "Expirée";
    public static final String FAILED = "Echouée";
    // Conteneur pour les popups
    private final PopupManager popupManager = PopupManager.getInstance();
    private final CopyClipboardManager copyClipboardManager = CopyClipboardManager.getInstance();

    public MissionCardComponent(Mission mission) {
        this.getStyleClass().add("mission-card");

        // Ajouter une classe CSS spéciale si la mission est complète
        if (mission.getCurrentCount() >= mission.getTargetCount() || mission.getStatus().equals(MissionStatus.COMPLETED)) {
            this.getStyleClass().add("mission-card-complete");
        } else if (mission.isMissionFailed()) {
            this.getStyleClass().add("mission-card-failed");
        } else if (mission.isWing()) {
            this.getStyleClass().add("mission-card-wing");
        }
        this.setSpacing(5);
        this.setPadding(new Insets(10));

        // Ligne compacte avec les informations essentielles
        HBox mainRow = new HBox();
        mainRow.setSpacing(15);
        mainRow.setAlignment(Pos.CENTER_LEFT);

        // Faction - largeur fixe pour alignement
        Label sourceFactionLabel = getSourceFaction(mission);

        // Clan cible et système - largeur fixe pour alignement
        Label targetFactionLabel = getTargetFaction(mission);

        Label targetLabel = getTarget(mission);

        // Progression des kills - conteneur avec largeur fixe
        HBox killsSection = getKillsSection(mission);

        // Icône Wing - largeur fixe pour alignement
        Label wingLabel = getWingLabel(mission);

        // Récompense - largeur fixe pour alignement
        Label rewardLabel = getRewardLabel(mission);

        // Temps d'acceptation et temps restant - largeur fixe pour alignement
        VBox timeSection = getTimeRemaining(mission);

        mainRow.getChildren().addAll(sourceFactionLabel, targetFactionLabel, targetLabel, killsSection, wingLabel, rewardLabel, timeSection);

        this.getChildren().add(mainRow);

    }

    private Label getRewardLabel(Mission mission) {
        Label rewardLabel = new Label(String.format("%,d Cr", mission.getReward()));
        rewardLabel.getStyleClass().add("massacre-reward");
        rewardLabel.setPrefWidth(140);
        rewardLabel.setMinWidth(140);
        rewardLabel.setMaxWidth(140);
        return rewardLabel;
    }

    private Label getSourceFaction(Mission mission) {
        Label factionLabel = new Label(mission.getFaction());
        factionLabel.getStyleClass().add("massacre-faction");
        factionLabel.setPrefWidth(180);
        factionLabel.setMinWidth(180);
        factionLabel.setMaxWidth(180);

        // Ajouter tooltip et clic pour copier le système d'origine
        if (mission.getOriginSystem() != null && !mission.getOriginSystem().isEmpty()) {
            factionLabel.setTooltip(new TooltipComponent(mission.getOriginSystem() + " | " + mission.getOriginStation()));
            factionLabel.getStyleClass().add("clickable-system-source");
            factionLabel.setOnMouseClicked(e -> onClickMission(mission.getOriginSystem(), e));
        } else {
            // Debug: afficher un tooltip même si pas de système d'origine
            factionLabel.setTooltip(new TooltipComponent("Système d'origine: Non défini"));
        }
        return factionLabel;
    }

    private Label getTarget(Mission mission) {
        String targetInfo = "";
        if (mission.getTargetType() != null) {
            targetInfo = mission.getTargetType().getDisplayName();
        }
        Label targetLabel = new Label(targetInfo);
        targetLabel.setPrefWidth(100);
        targetLabel.setMinWidth(100);
        targetLabel.setMaxWidth(100);
        if (mission.getTargetType() != null) {
            switch (mission.getTargetType()) {
                case PIRATE -> targetLabel.getStyleClass().add("massacre-pirate");
                case DESERTEUR -> targetLabel.getStyleClass().add("massacre-deserteur");
                // autres cases ici
                default -> targetLabel.getStyleClass().add("massacre-default");
            }
        } else {
            targetLabel.getStyleClass().add("target-unknown");
        }
        return targetLabel;
    }

    private Label getTargetFaction(Mission mission) {
        String targetInfo = "";
        if (mission.getTargetFaction() != null) {
            targetInfo = mission.getTargetFaction();
        }
        if (targetInfo.isEmpty()) {
            targetInfo = "Pirates";
        }
        Label targetLabel = new Label(targetInfo);
        targetLabel.getStyleClass().add("massacre-target");
        targetLabel.setPrefWidth(160);
        targetLabel.setMinWidth(160);
        targetLabel.setMaxWidth(160);

        if (mission.getDestinationSystem() != null && !mission.getDestinationSystem().isEmpty()) {
            // Fallback: utiliser destinationSystem si targetSystem n'est pas défini
            targetLabel.setTooltip(new TooltipComponent(mission.getDestinationSystem()));
            targetLabel.getStyleClass().add("clickable-system-target");
            targetLabel.setOnMouseClicked(e -> onClickMission(mission.getDestinationSystem(), e));
        }
        return targetLabel;
    }

    private VBox getTimeRemaining(Mission mission) {
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
                if (mission.getStatus() == MissionStatus.COMPLETED
                        || (!mission.isMissionFailed() && mission.getTargetCountLeft() == 0)) {
                    timeRemaining = TERMINATED;
                } else if (mission.isMissionFailed()) {
                    timeRemaining = FAILED;
                } else {
                    long hoursRemaining = java.time.Duration.between(LocalDateTime.now(), mission.getExpiry()).toHours();
                    if (hoursRemaining > 0) {
                        timeRemaining = String.format("Restant: %dh", hoursRemaining);
                    } else {
                        timeRemaining = EXPIRED;
                    }
                }
                Label remainingLabel = new Label(timeRemaining);
                if (TERMINATED.equals(timeRemaining)) {
                    remainingLabel.getStyleClass().add("mission-time-completed");
                } else if (FAILED.equals(timeRemaining) || EXPIRED.equals(timeRemaining)) {
                    remainingLabel.getStyleClass().add("mission-time-failed");
                } else {
                    long hoursRemaining = java.time.Duration.between(LocalDateTime.now(), mission.getExpiry()).toHours();
                    remainingLabel.getStyleClass().add(hoursRemaining > 24 ? "mission-time" : "mission-time-urgent");
                }

                timeSection.getChildren().addAll(acceptedLabel, remainingLabel);
            } else {
                timeSection.getChildren().add(acceptedLabel);
            }
        }
        return timeSection;
    }

    private Label getWingLabel(Mission mission) {
        Label wingLabel = new Label();
        wingLabel.setPrefWidth(30);
        wingLabel.setMinWidth(30);
        wingLabel.setMaxWidth(30);
        wingLabel.setAlignment(Pos.CENTER);

        if (mission.isWing()) {
            wingLabel.setText("✈"); // Icône d'avion pour wing
            wingLabel.getStyleClass().add("wing-icon");
            wingLabel.setTooltip(new TooltipComponent("Mission de Wing"));
        } else {
            wingLabel.setText(""); // Vide pour les missions normales
        }
        return wingLabel;
    }

    private HBox getKillsSection(Mission mission) {
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
        return killsSection;
    }

    private void onClickMission(String systemName, javafx.scene.input.MouseEvent event) {
        copyClipboardManager.copyToClipboard(systemName);
        Stage stage = (Stage) getScene().getWindow();
        popupManager.showPopup("Système copié", event.getSceneX(), event.getSceneY(), stage);

    }


}
