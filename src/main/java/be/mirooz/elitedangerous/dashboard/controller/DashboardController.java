package be.mirooz.elitedangerous.dashboard.controller;

import be.mirooz.elitedangerous.dashboard.model.Mission;
import be.mirooz.elitedangerous.dashboard.model.MissionStatus;
import be.mirooz.elitedangerous.dashboard.model.MissionType;
import be.mirooz.elitedangerous.dashboard.service.MissionService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Contrôleur principal du dashboard Elite Dangerous
 */
public class DashboardController implements Initializable {

    @FXML
    private BorderPane mainPane;
    
    private HeaderController headerController;
    private MissionListController missionListController;
    private FooterController footerController;
    
    private MissionService missionService;
    private List<Mission> allMissions = new ArrayList<>();
    private MissionStatus currentFilter = MissionStatus.ACTIVE;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        missionService = new MissionService();
        loadComponents();
        loadMissions();
    }
    
    private void loadComponents() {
        try {
            // Charger le header
            FXMLLoader headerLoader = new FXMLLoader(getClass().getResource("/fxml/header.fxml"));
            VBox header = headerLoader.load();
            headerController = headerLoader.getController();
            headerController.setRefreshCallback(this::refreshMissions);
            mainPane.setTop(header);
            
            // Charger la liste des missions
            FXMLLoader missionListLoader = new FXMLLoader(getClass().getResource("/fxml/mission-list.fxml"));
            VBox missionList = missionListLoader.load();
            missionListController = missionListLoader.getController();
            missionListController.setFilterChangeCallback(this::onFilterChange);
            mainPane.setCenter(missionList);
            
            // Charger le footer
            FXMLLoader footerLoader = new FXMLLoader(getClass().getResource("/fxml/footer.fxml"));
            javafx.scene.layout.HBox footer = footerLoader.load();
            footerController = footerLoader.getController();
            mainPane.setBottom(footer);
            
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement des composants: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void loadMissions() {
        allMissions = missionService.getActiveMissions();
        headerController.setAllMissions(allMissions);
        missionListController.setAllMissions(allMissions);
        applyCurrentFilter();
    }
    
    private void refreshMissions() {
        headerController.setStatusText("ACTUALISATION...");
        loadMissions();
        headerController.setStatusText("SYSTÈME EN LIGNE");
    }
    
    private void onFilterChange(MissionStatus filter) {
        currentFilter = filter;
        headerController.setCurrentFilter(filter);
        applyCurrentFilter();
    }
    
    private void applyCurrentFilter() {
        // Filtrer les missions selon le filtre actuel
        List<Mission> filteredMissions = allMissions.stream()
                .filter(mission -> mission.getType() == MissionType.MASSACRE)
                .filter(mission -> currentFilter == null || mission.getStatus() == currentFilter)
                .sorted((m1, m2) -> m1.getFaction().compareTo(m2.getFaction()))
                .collect(Collectors.toList());
        
        // Mettre à jour les composants
        missionListController.applyFilter(currentFilter);
        headerController.updateStats(filteredMissions);
        
        // Mettre à jour les statistiques par faction (toujours basées sur les missions actives)
        List<Mission> activeMissions = allMissions.stream()
                .filter(mission -> mission.getType() == MissionType.MASSACRE && mission.getStatus() == MissionStatus.ACTIVE)
                .collect(Collectors.toList());
        footerController.updateFactionStats(activeMissions);
    }
}
