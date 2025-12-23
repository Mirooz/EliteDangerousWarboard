package be.mirooz.elitedangerous.dashboard.view.exploration;

import be.mirooz.elitedangerous.dashboard.view.common.IBatchListener;

import be.mirooz.elitedangerous.dashboard.view.exploration.*;
import be.mirooz.elitedangerous.dashboard.view.common.managers.UIManager;
import be.mirooz.elitedangerous.dashboard.service.DashboardService;
import be.mirooz.elitedangerous.dashboard.service.listeners.ExplorationRefreshNotificationService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Contrôleur pour le panneau d'exploration
 */
public class ExplorationController implements Initializable,IBatchListener {

    // Conteneurs pour les composants
    @FXML
    private VBox systemVisualViewContainer;
    @FXML
    private VBox explorationHistoryDetailContainer;

    // Composants
    private SystemVisualViewComponent systemVisualView;
    private ExplorationHistoryDetailComponent explorationHistoryDetail;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeComponents();
        setupComponentCallbacks();
        DashboardService.getInstance().addBatchListener(this);
    }

    @Override
    public void onBatchEnd(){
        // S'abonner au service de notification pour le refresh
        ExplorationRefreshNotificationService.getInstance().addListener(this::refresh);
    }
    @Override
    public void onBatchStart(){
        // S'abonner au service de notification pour le refresh
        ExplorationRefreshNotificationService.getInstance().clearListeners();
    }
    /**
     * Initialise les composants en les chargeant depuis leurs fichiers FXML
     */
    private void initializeComponents() {
        try {
            // Charger le composant de vue visuelle du système
            FXMLLoader visualLoader = new FXMLLoader(getClass().getResource("/fxml/exploration/system-visual-view.fxml"));
            VBox visualPanel = visualLoader.load();
            systemVisualView = visualLoader.getController();
            if (systemVisualViewContainer != null) {
                systemVisualViewContainer.getChildren().add(visualPanel);
            }

            // Charger le composant fusionné historique/détails d'exploration
            FXMLLoader historyDetailLoader = new FXMLLoader(getClass().getResource("/fxml/exploration/exploration-history-detail.fxml"));
            VBox historyDetailPanel = historyDetailLoader.load();
            explorationHistoryDetail = historyDetailLoader.getController();
            if (explorationHistoryDetailContainer != null) {
                explorationHistoryDetailContainer.getChildren().add(historyDetailPanel);
            }

            System.out.println("✅ Composants exploration chargés avec succès");
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du chargement des composants exploration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Configure les callbacks entre les composants
     */
    private void setupComponentCallbacks() {
        if (explorationHistoryDetail != null && systemVisualView != null) {
            // Quand un système est cliqué dans le détail, l'afficher dans la vue visuelle
            explorationHistoryDetail.setOnSystemSelected(systemVisualView::displaySystem);
        }
    }

    public void refresh() {
        Platform.runLater(() -> {
            if (systemVisualView != null) {
                systemVisualView.refresh();
            }
            if (explorationHistoryDetail != null) {
                explorationHistoryDetail.refresh();
            }
        });
    }
}

