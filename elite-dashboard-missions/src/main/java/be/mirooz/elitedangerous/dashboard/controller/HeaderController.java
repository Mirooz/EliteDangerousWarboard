package be.mirooz.elitedangerous.dashboard.controller;

import be.mirooz.elitedangerous.dashboard.controller.ui.context.DashboardContext;
import be.mirooz.elitedangerous.dashboard.controller.ui.manager.UIManager;
import be.mirooz.elitedangerous.dashboard.model.*;
import be.mirooz.elitedangerous.dashboard.model.enums.MissionStatus;
import be.mirooz.elitedangerous.dashboard.controller.ui.component.CommanderStatusComponent;
import be.mirooz.elitedangerous.dashboard.controller.ui.component.DialogComponent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

import static be.mirooz.elitedangerous.dashboard.util.NumberUtil.getFormattedNumber;

/**
 * Contrôleur pour l'en-tête du dashboard
 */
public class HeaderController implements Initializable, Refreshable {
    @FXML
    public Label missionCountTextLabel;
    @FXML
    public VBox lostCreditsBox;
    @FXML
    public VBox earnCreditsBox;
    @FXML
    public Label lostCreditsLabel;
    @FXML
    public VBox missionStatBox;
    @FXML
    public Label potentialCreditsLabel;
    @FXML
    public VBox potentialCreditsBox;
    @FXML
    private Label missionCountLabel;
    @FXML
    private Label earnCreditsLabel;

    @FXML
    private VBox pendingCreditsBox;

    @FXML
    private Label pendingCreditsLabel;

    @FXML
    private Button massacreSearchButton;

    @FXML
    private Label statusLabel;

    private final MissionsRegistry missionsRegistry = MissionsRegistry.getInstance();

    public static final DashboardContext dashboardContext = DashboardContext.getInstance();
    private final CommanderStatusComponent commanderStatusComponent = CommanderStatusComponent.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        dashboardContext.addFilterListener(this::updateStats);
        UIManager.getInstance().register(this);

    }

    public void refreshUI(){
        updateStats(DashboardContext.getInstance().getCurrentFilter());
    }
    private void updateStats(MissionStatus currentFilter) {
        // Filtrer les missions selon le filtre actuel
        List<Mission> filteredMissions = missionsRegistry.getGlobalMissionMap().values().stream()
                .filter(Mission::isShipMassacre)
                .filter(mission -> currentFilter == null || mission.getStatus() == currentFilter)
                .sorted(Comparator.comparing(Mission::getFaction)).toList();
        hide(missionStatBox);
        hide(earnCreditsBox);
        hide(pendingCreditsBox);
        hide(potentialCreditsBox);
        hide(lostCreditsBox);
        // Calculer les crédits selon le filtre
        if (currentFilter == MissionStatus.ACTIVE) {
            setMissionsLabel("MISSIONS ACTIVES", filteredMissions);
            setPotentialCredits(filteredMissions);
            setPendingCredits(filteredMissions);
        } else if (currentFilter == MissionStatus.COMPLETED) {
            setMissionsLabel("MISSIONS COMPLÉTÉES", filteredMissions);
            setEarnCredits(filteredMissions);
        } else if (currentFilter == MissionStatus.FAILED) {
            setMissionsLabel("MISSIONS ABANDONNÉES", filteredMissions);
            setLostCredits(filteredMissions);

        } else {
            setMissionsLabel("MISSIONS", filteredMissions);
            setEarnCredits(filteredMissions);
            setPotentialCredits(filteredMissions);
            setPendingCredits(filteredMissions);
            setLostCredits(filteredMissions);
        }
    }

    private void setMissionsLabel(String label, List<Mission> filteredMissions) {
        missionCountTextLabel.setText(label);
        int missionCount = filteredMissions.size();
        missionCountLabel.setText(String.valueOf(missionCount));
        show(missionStatBox);;
    }

    private void setPotentialCredits(List<Mission> filteredMissions) {
        long totalCredits = filteredMissions.stream()
                .filter(Mission::isActive)
                .mapToLong(Mission::getReward).sum();
        potentialCreditsLabel.setText(getFormattedNumber(totalCredits));
        show(potentialCreditsBox);
    }
    private void setPendingCredits(List<Mission> filteredMissions) {
        long pending = filteredMissions.stream()
                .filter(Mission::isPending)
                .mapToLong(Mission::getReward).sum();
        if (pending>0) {
            pendingCreditsLabel.setText(getFormattedNumber(pending));
            show(pendingCreditsBox);
        }
    }
    private void setEarnCredits(List<Mission> filteredMissions) {
        long completedCredits = filteredMissions.stream()
                .filter(Mission::isCompleted)
                .mapToLong(Mission::getReward).sum();
        String formatted = getFormattedNumber(completedCredits);
        earnCreditsLabel.setText(formatted);
        show(earnCreditsBox);
    }



    private void setLostCredits(List<Mission> filteredMissions) {
        long completedCredits = filteredMissions.stream()
                .filter(Mission::isMissionFailed)
                .mapToLong(Mission::getReward).sum();
        if (completedCredits >0) {
            lostCreditsLabel.setText(getFormattedNumber(completedCredits));
            show(lostCreditsBox);
        }
    }
    private void hide(VBox box){
        box.setVisible(false);
        box.setManaged(false);
    }
    private void show(VBox box){
        box.setVisible(true);
        box.setManaged(true);
    }

    @FXML
    private void openMassacreSearchDialog() {
        Stage primaryStage = (Stage) massacreSearchButton.getScene().getWindow();

        DialogComponent dialog = new DialogComponent("/fxml/massacre-search-dialog.fxml", "/css/elite-theme.css", "Recherche de Systèmes Massacre", 1000, 700);

        dialog.init(primaryStage);
        dialog.showAndWait();

    }

    public void postBatch() {
        statusLabel.textProperty().bind(javafx.beans.binding.Bindings.when(commanderStatusComponent
                .getIsOnline()).then("SYSTÈME EN LIGNE").otherwise("SYSTÈME HORS LIGNE"));
        // Binding conditionnel pour la couleur du statut
        statusLabel.styleProperty().bind(javafx.beans.binding.Bindings.when(commanderStatusComponent
                        .getIsOnline()).then("-fx-text-fill: #00ff00;") // Vert si en ligne
                .otherwise("-fx-text-fill: #ff0000;") // Rouge si hors ligne
        );

    }
}
