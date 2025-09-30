package be.mirooz.elitedangerous.dashboard.controller;

import be.mirooz.elitedangerous.dashboard.model.MissionStatus;
import be.mirooz.elitedangerous.dashboard.model.MissionsList;
import be.mirooz.elitedangerous.dashboard.service.DashboardService;
import be.mirooz.elitedangerous.dashboard.ui.UIRefreshManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.layout.BorderPane;
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
    
    private HeaderController headerController;
    private MissionListController missionListController;
    private FooterController footerController;
    private DestroyedShipsController destroyedShipsController;
    private UIRefreshManager uiRefreshManager = UIRefreshManager.getInstance();
    
    private DashboardService dashboardService;
    private MissionsList allMissionsList = MissionsList.getInstance();
    private MissionStatus currentFilter = MissionStatus.ACTIVE;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        dashboardService = DashboardService.getInstance();
        loadComponents();
        loadMissions();
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
            uiRefreshManager.registerControllers(
                    headerController,
                    missionListController,
                    footerController);
            uiRefreshManager.registerDestroyedShipsController(destroyedShipsController);
            
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
        missionListController.setFilterChangeCallback(this::onFilterChange);
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
        headerController.setRefreshCallback(this::refreshMissions);
        mainPane.setTop(header);
    }

    private void loadMissions() {
        dashboardService.InitActiveMissions();
        // Mettre à jour le nom du commandant dans le footer
        String commanderName = dashboardService.getCommanderName();
        footerController.updateCommanderName(commanderName);
        
        // Mettre à jour les informations du vaisseau dans le footer
        String currentSystem = dashboardService.getCurrentSystem();
        String currentStation = dashboardService.getCurrentStation();
        String currentShip = dashboardService.getCurrentShip();
        footerController.updateShipInfo(currentSystem, currentStation, currentShip);
        uiRefreshManager.refresh();
    }
    
    private void refreshMissions() {
        headerController.setStatusText("ACTUALISATION...");
        loadMissions();
        headerController.setStatusText("SYSTÈME EN LIGNE");
    }
    
    private void onFilterChange(MissionStatus filter) {
        currentFilter = filter;
        headerController.setCurrentFilter(filter);
        missionListController.setCurrentFilter(filter);
        uiRefreshManager.refresh();
    }
}
