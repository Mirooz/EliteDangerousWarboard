package be.mirooz.elitedangerous.dashboard.controller;

import be.mirooz.elitedangerous.dashboard.model.Mission;
import be.mirooz.elitedangerous.dashboard.model.enums.MissionStatus;
import be.mirooz.elitedangerous.dashboard.model.enums.MissionType;
import be.mirooz.elitedangerous.dashboard.model.MissionsList;
import be.mirooz.elitedangerous.dashboard.ui.CopyClipboardManager;
import be.mirooz.elitedangerous.dashboard.ui.PopupManager;
import be.mirooz.elitedangerous.dashboard.ui.component.TooltipComponent;
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

    public static final String TERMINATED = "Terminée";
    public static final String EXPIRED = "Expirée";
    public static final String FAILED = "Echouée";
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
    // Conteneur pour les popups

    private final PopupManager popupManager = PopupManager.getInstance();
    private final CopyClipboardManager copyClipboardManager = CopyClipboardManager.getInstance();

    private MissionStatus currentFilter = MissionStatus.ACTIVE;
    private MissionsList allMissionsList = MissionsList.getInstance();

    private Consumer<MissionStatus> filterChangeCallback;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    private void onClickSystem(String systemName, javafx.scene.input.MouseEvent event) {
        copyClipboardManager.copyToClipboard(systemName);
        popupManager.showPopup("Système copié", event.getSceneX(), event.getSceneY());
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
            factionLabel.setTooltip(new TooltipComponent(mission.getOriginSystem() + " | " + mission.getOriginStation()));
            factionLabel.getStyleClass().add("clickable-system-source");
            factionLabel.setOnMouseClicked(e -> onClickSystem(mission.getOriginSystem(), e));
        } else {
            // Debug: afficher un tooltip même si pas de système d'origine
            factionLabel.setTooltip(new TooltipComponent("Système d'origine: Non défini"));
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
            targetLabel.setTooltip(new TooltipComponent(mission.getDestinationSystem()));
            targetLabel.getStyleClass().add("clickable-system-target");
            targetLabel.setOnMouseClicked(e -> onClickSystem(mission.getDestinationSystem(), e));
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
            wingLabel.setTooltip(new TooltipComponent("Mission de Wing"));
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
                if (mission.getStatus() == MissionStatus.COMPLETED
                    || (!missionFailed(mission) &&mission.getTargetCountLeft()==0)) {
                    timeRemaining = TERMINATED;
                } else if (missionFailed(mission)) {
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
                }
                else if (FAILED.equals(timeRemaining) || EXPIRED.equals(timeRemaining)){
                    remainingLabel.getStyleClass().add("mission-time-failed");
                }else {
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

    private  boolean missionFailed(Mission mission) {
        return mission.getStatus() == MissionStatus.FAILED
                || mission.getStatus() == MissionStatus.EXPIRED
                || mission.getStatus() == MissionStatus.ABANDONED;
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
