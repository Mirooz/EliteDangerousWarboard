package be.mirooz.elitedangerous.dashboard.controller;

import be.mirooz.elitedangerous.dashboard.controller.ui.context.DashboardContext;
import be.mirooz.elitedangerous.dashboard.controller.ui.manager.UIManager;
import be.mirooz.elitedangerous.dashboard.service.DashboardService;
import be.mirooz.elitedangerous.dashboard.controller.ui.manager.PopupManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Contrôleur principal du dashboard Elite Dangerous
 */
public class DashboardController implements Initializable {

    @FXML
    private BorderPane mainPane;

    @FXML
    private StackPane popupContainer;

    private HeaderController headerController;
    private MissionListController missionListController;
    private FooterController footerController;
    private DestroyedShipsController destroyedShipsController;
    private final DashboardService dashboardService= DashboardService.getInstance();
    private final DashboardContext dashboardContext= DashboardContext.getInstance();
    private final PopupManager popupManager = PopupManager.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadComponents();
        loadMissions();
        popupManager.attachToContainer(popupContainer);
    }

    private void loadComponents() {
        try {

            // Charger le header
            createHeaderPanel();
            // Charger la liste des missions
            createMissionPanel();
            // Charger le panneau des vaisseaux détruits
            createDestroyedShipsPanel();
            // Charger le footer
            createFooterPanel();

        } catch (IOException e) {
            System.err.println("Erreur lors du chargement des composants: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createFooterPanel() throws IOException {
        FXMLLoader footerLoader = new FXMLLoader(getClass().getResource("/fxml/footer.fxml"));
        javafx.scene.layout.HBox footer = footerLoader.load();
        footerController = footerLoader.getController();
        mainPane.setBottom(footer);
    }

    private void createMissionPanel() throws IOException {
        FXMLLoader missionListLoader = new FXMLLoader(getClass().getResource("/fxml/mission-list.fxml"));
        VBox missionList = missionListLoader.load();
        missionListController = missionListLoader.getController();
        mainPane.setCenter(missionList);
    }

    private void createDestroyedShipsPanel() throws IOException {
        FXMLLoader destroyedShipsLoader = new FXMLLoader(getClass().getResource("/fxml/destroyed-ships.fxml"));
        VBox destroyedShipsPanel = destroyedShipsLoader.load();
        destroyedShipsController = destroyedShipsLoader.getController();
        mainPane.setLeft(destroyedShipsPanel);
    }

    private void createHeaderPanel() throws IOException {
        FXMLLoader headerLoader = new FXMLLoader(getClass().getResource("/fxml/header.fxml"));
        VBox header = headerLoader.load();
        headerController = headerLoader.getController();
        mainPane.setTop(header);
    }
    private void loadMissions() {

        missionListController.preBatch();
        new Thread(() -> {
            try {
                dashboardService.InitActiveMissions();
                Platform.runLater(this::postBatch);
            } catch (Exception ex) {
                ex.printStackTrace();
                postBatch();
            }
        }).start();
    }

    private void postBatch() {
        try {
        missionListController.postBatch();
        footerController.postBatch();
        destroyedShipsController.postBatch();
        headerController.postBatch();
        dashboardContext.refreshUI();
    }catch (Exception e){

        }
    }

}
