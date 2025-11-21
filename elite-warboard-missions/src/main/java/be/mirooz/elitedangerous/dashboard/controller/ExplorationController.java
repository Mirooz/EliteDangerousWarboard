package be.mirooz.elitedangerous.dashboard.controller;

import be.mirooz.elitedangerous.dashboard.controller.ui.component.exploration.*;
import be.mirooz.elitedangerous.dashboard.controller.ui.manager.UIManager;
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
public class ExplorationController implements Initializable, IRefreshable {

    // Conteneurs pour les composants
    @FXML
    private VBox systemVisualViewContainer;
    @FXML
    private VBox currentSystemInfoContainer;
    @FXML
    private VBox explorationHistoryDetailContainer;

    // Composants
    private SystemVisualViewComponent systemVisualView;
    private CurrentSystemInfoComponent currentSystemInfo;
    private ExplorationHistoryDetailComponent explorationHistoryDetail;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeComponents();
        setupComponentCallbacks();
        UIManager.getInstance().register(this);
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

            // Charger le composant d'infos du système actuel (avec statistiques intégrées)
            FXMLLoader systemInfoLoader = new FXMLLoader(getClass().getResource("/fxml/exploration/current-system-info.fxml"));
            VBox systemInfoPanel = systemInfoLoader.load();
            currentSystemInfo = systemInfoLoader.getController();
            if (currentSystemInfoContainer != null) {
                currentSystemInfoContainer.getChildren().add(systemInfoPanel);
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

    @Override
    public void refreshUI() {
        Platform.runLater(() -> {
            if (systemVisualView != null) {
                systemVisualView.refresh();
            }
            if (currentSystemInfo != null) {
                currentSystemInfo.refresh();
            }
            if (explorationHistoryDetail != null) {
                explorationHistoryDetail.refresh();
            }
        });
    }
}

