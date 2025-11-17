package be.mirooz.elitedangerous.dashboard.controller;

import be.mirooz.elitedangerous.dashboard.controller.ui.component.exploration.*;
import be.mirooz.elitedangerous.dashboard.controller.ui.manager.UIManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.layout.Region;
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
    private VBox explorationHistoryContainer;
    @FXML
    private VBox explorationDetailContainer;

    // Composants
    private SystemVisualViewComponent systemVisualView;
    private CurrentSystemInfoComponent currentSystemInfo;
    private ExplorationHistoryComponent explorationHistory;
    private ExplorationDetailComponent explorationDetail;

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

            // Charger le composant d'historique d'exploration
            FXMLLoader historyLoader = new FXMLLoader(getClass().getResource("/fxml/exploration/exploration-history.fxml"));
            VBox historyPanel = historyLoader.load();
            explorationHistory = historyLoader.getController();
            if (explorationHistoryContainer != null) {
                explorationHistoryContainer.getChildren().add(historyPanel);
            }

            // Charger le composant de détails d'exploration
            FXMLLoader detailLoader = new FXMLLoader(getClass().getResource("/fxml/exploration/exploration-detail.fxml"));
            VBox detailPanel = detailLoader.load();
            explorationDetail = detailLoader.getController();
            if (explorationDetailContainer != null) {
                explorationDetailContainer.getChildren().add(detailPanel);
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
        if (explorationHistory != null && explorationDetail != null) {
            // Quand un groupe d'exploration est sélectionné, afficher ses détails
            explorationHistory.setOnSaleSelected(explorationDetail::setExplorationDataSale);
        }
        
        if (explorationDetail != null && systemVisualView != null) {
            // Quand un système est cliqué dans le détail, l'afficher dans la vue visuelle
            explorationDetail.setOnSystemSelected(systemVisualView::displaySystem);
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
            if (explorationHistory != null) {
                explorationHistory.refresh();
            }
            if (explorationDetail != null) {
                explorationDetail.refresh();
            }
        });
    }
}

