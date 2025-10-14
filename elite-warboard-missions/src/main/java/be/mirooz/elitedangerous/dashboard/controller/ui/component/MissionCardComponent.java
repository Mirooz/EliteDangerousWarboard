package be.mirooz.elitedangerous.dashboard.controller.ui.component;

import be.mirooz.elitedangerous.dashboard.model.Mission;
import be.mirooz.elitedangerous.dashboard.model.enums.MissionStatus;
import be.mirooz.elitedangerous.dashboard.controller.ui.manager.CopyClipboardManager;
import be.mirooz.elitedangerous.dashboard.controller.ui.manager.PopupManager;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import static be.mirooz.elitedangerous.dashboard.util.NumberUtil.getFormattedNumber;

public class MissionCardComponent extends VBox {


    // Conteneur pour les popups
    private final PopupManager popupManager = PopupManager.getInstance();
    private final CopyClipboardManager copyClipboardManager = CopyClipboardManager.getInstance();
    private final LocalizationService localizationService = LocalizationService.getInstance();

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
        this.setPrefHeight(50);
        this.setMinHeight(50);
        this.setMaxHeight(50);
        this.setSpacing(1);
        this.setPadding(new Insets(3, 10, 3, 10));

        // Ligne compacte avec les informations essentielles
        HBox mainRow = new HBox();
        mainRow.setSpacing(15);
        mainRow.setAlignment(Pos.CENTER_LEFT);
        mainRow.setFillHeight(false);

        // Faction - largeur fixe pour alignement
        Label sourceFactionLabel = getSourceFaction(mission);

        // Clan cible et système - largeur fixe pour alignement
        Label targetFactionLabel = getTargetFaction(mission);

        Label targetLabel = getTarget(mission);

        // Progression des kills - conteneur avec largeur fixe
        HBox killsSection = getKillsSection(mission);

        // Icône Wing - largeur fixe pour alignement
        StackPane wingLabel = getWingLabel(mission);

        // Récompense - largeur fixe pour alignement
        Label rewardLabel = getRewardLabel(mission);

        // Temps d'acceptation et temps restant - largeur fixe pour alignement
        VBox timeSection = getTimeRemaining(mission);

        mainRow.getChildren().addAll(sourceFactionLabel, targetFactionLabel, targetLabel, killsSection, wingLabel,rewardLabel,timeSection);

        this.getChildren().add(mainRow);

    }

    private Label getRewardLabel(Mission mission) {
        Label rewardLabel = new Label(getFormattedNumber(mission.getReward()) + " Cr");
        rewardLabel.getStyleClass().add("massacre-reward");
        rewardLabel.setPrefWidth(140);
        rewardLabel.setMinWidth(70);
        rewardLabel.setMaxWidth(140);
        rewardLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        rewardLabel.setWrapText(false);
        return rewardLabel;
    }

    private Label getSourceFaction(Mission mission) {
        Label factionLabel = new Label(mission.getFaction());
        factionLabel.getStyleClass().add("massacre-faction");
        factionLabel.setPrefWidth(180);
        factionLabel.setMinWidth(70);
        factionLabel.setMaxWidth(180);

        // Ajouter tooltip et clic pour copier le système d'origine
        if (mission.getOriginSystem() != null && !mission.getOriginSystem().isEmpty()) {
            String tooltipText = localizationService.getString("tooltip.origin_system") + ": " + 
                                mission.getOriginSystem() + " | " + mission.getOriginStation();
            factionLabel.setTooltip(new TooltipComponent(tooltipText));
            factionLabel.getStyleClass().add("clickable-system-source");
            factionLabel.setOnMouseClicked(e -> onClickMission(mission.getOriginSystem(), e));
        } else {
            // Debug: afficher un tooltip même si pas de système d'origine
            factionLabel.setTooltip(new TooltipComponent(localizationService.getString("tooltip.origin_system_undefined")));
        }
        return factionLabel;
    }

    private Label getTarget(Mission mission) {
        String targetInfo = "";
        if (mission.getTargetType() != null) {
            targetInfo = mission.getTargetType().getDisplayName();
        }
        Label targetLabel = new Label(targetInfo);
        targetLabel.setPrefWidth(80);
        targetLabel.setMinWidth(30);
        targetLabel.setMaxWidth(80);
        if (mission.getTargetType() != null) {
            switch (mission.getTargetType()) {
                case PIRATE -> targetLabel.getStyleClass().add("massacre-pirate");
                case DESERTEUR -> targetLabel.getStyleClass().add("massacre-deserteur");
                case HUMANOID -> targetLabel.getStyleClass().add("massacre-humain");
                case UNKNOWN -> targetLabel.getStyleClass().add("massacre-conflict");
                // autres cases ici
                default -> targetLabel.getStyleClass().add("massacre-default");
            }
        }
        return targetLabel;
    }

    private Label getTargetFaction(Mission mission) {
        String targetInfo = "";
        if (mission.getTargetFaction() != null) {
            targetInfo = mission.getTargetFaction();
        }
        Label targetLabel = new Label(targetInfo);
        targetLabel.getStyleClass().add("massacre-target");
        targetLabel.setPrefWidth(180);
        targetLabel.setMinWidth(50);
        targetLabel.setMaxWidth(180);
        if (mission.getDestinationSystem() != null && !mission.getDestinationSystem().isEmpty()) {
            // Fallback: utiliser destinationSystem si targetSystem n'est pas défini
            String tooltipText = localizationService.getString("tooltip.destination_system") + ": " + mission.getDestinationSystem();
            targetLabel.setTooltip(new TooltipComponent(tooltipText));
            targetLabel.getStyleClass().add("clickable-system-target");
            targetLabel.setOnMouseClicked(e -> onClickMission(mission.getDestinationSystem(), e));
        } else {
            targetLabel.setTooltip(new TooltipComponent(localizationService.getString("tooltip.destination_system_undefined")));
        }
        return targetLabel;
    }

    private VBox getTimeRemaining(Mission mission) {
        VBox timeSection = new VBox();
        timeSection.setSpacing(2);
        timeSection.setPrefWidth(120);
        timeSection.setMinWidth(70);
        timeSection.setMaxWidth(120);
        timeSection.setPrefHeight(20);
        timeSection.setMinHeight(20);
        timeSection.setMaxHeight(20);
        timeSection.setAlignment(Pos.CENTER_LEFT);

        if (mission.getAcceptedTime() != null) {
            Label acceptedLabel = new Label(localizationService.getString("mission.accepted") + ": " + mission.getAcceptedTime().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")));
            acceptedLabel.getStyleClass().add("mission-time");

            Label remainingLabel = new Label();
            if (mission.getExpiry() != null) {
                if (mission.getStatus() == MissionStatus.COMPLETED
                        || (!mission.isMissionFailed() && mission.getTargetCountLeft() == 0)) {
                    remainingLabel.setText(localizationService.getString("mission.terminated"));
                    remainingLabel.getStyleClass().add("mission-time-completed");
                } else if (mission.isMissionFailed()) {
                    remainingLabel.setText(localizationService.getString("mission.failed"));
                    remainingLabel.getStyleClass().add("mission-time-failed");
                } else {
                    String hoursRemaining = getHoursRemaining(mission);
                    if (hoursRemaining !=null) {
                        remainingLabel.setText(String.format(localizationService.getString("mission.remaining") + ": %s", hoursRemaining));
                        remainingLabel.getStyleClass().add(Long.parseLong(Objects.requireNonNull(getHoursPartRemaining(mission))) > 24 ? "mission-time" : "mission-time-urgent");
                    } else {
                        remainingLabel.setText(localizationService.getString("mission.expired"));
                        remainingLabel.getStyleClass().add("mission-time-expired");
                    }
                }
                timeSection.getChildren().addAll(acceptedLabel, remainingLabel);
            } else {
                timeSection.getChildren().add(acceptedLabel);
            }
        }
        return timeSection;
    }
    private String getHoursPartRemaining(Mission mission) {
        Duration duration = getRemainingDuration(mission);

        if (duration.isNegative()) {
            return null;
        }

        long totalHours = duration.toHours();  // ✅ inclut les jours
        return String.format("%02d", totalHours);
    }
    private ZonedDateTime toUtc(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneOffset.UTC).withZoneSameInstant(ZoneOffset.UTC);
    }

    private Duration getRemainingDuration(Mission mission) {
        ZonedDateTime nowUtc = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime expiryUtc = toUtc(mission.getExpiry());
        return Duration.between(nowUtc, expiryUtc);
    }
    private String getHoursRemaining(Mission mission) {
        Duration duration = getRemainingDuration(mission);

        if (duration.isNegative()) {
            return null;
        }

        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();

        if (days > 0) {
            return String.format("%dd %dh %d min", days, hours, minutes);
        } else if (hours > 0) {
            return String.format("%dh %d min", hours, minutes);
        } else {
            return String.format("%d min", minutes);
        }
    }
    private StackPane getWingLabel(Mission mission) {
        StackPane wingPane = new StackPane();
        wingPane.setPrefSize(30, 20);
        wingPane.setMinSize(30, 20);
        wingPane.setMaxSize(30, 20);
        wingPane.setAlignment(Pos.CENTER);

        if (mission.isWing()) {
            Image image = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/wing.png")));
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(16);
            imageView.setFitHeight(16);
            imageView.setPreserveRatio(true);

            wingPane.getChildren().add(imageView);
            wingPane.getStyleClass().add("wing-icon");

            Tooltip tooltip = new Tooltip(localizationService.getString("tooltip.wing_mission"));
            Tooltip.install(wingPane, tooltip); // ✅ tooltip sur le pane
        }

        return wingPane;
    }

    private HBox getKillsSection(Mission mission) {
        HBox killsSection = new HBox();
        killsSection.setSpacing(8);
        killsSection.setAlignment(Pos.CENTER_LEFT);
        killsSection.setPrefWidth(110);
        killsSection.setMinWidth(90);
        killsSection.setMaxWidth(110);
        killsSection.setFillHeight(false);

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
        progressBar.setMinWidth(40);
        progressBar.setMaxWidth(60);
        killsSection.getChildren().addAll(killsLabel, progressBar);
        return killsSection;
    }

    private void onClickMission(String systemName, javafx.scene.input.MouseEvent event) {
        copyClipboardManager.copyToClipboard(systemName);
        Stage stage = (Stage) getScene().getWindow();
        popupManager.showPopup(localizationService.getString("system.copied"), event.getSceneX(), event.getSceneY(), stage);

    }


}
