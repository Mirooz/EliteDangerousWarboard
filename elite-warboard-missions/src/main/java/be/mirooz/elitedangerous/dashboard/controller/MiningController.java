package be.mirooz.elitedangerous.dashboard.controller;

import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.MineralType;
import be.mirooz.elitedangerous.dashboard.controller.ui.component.mining.CurrentCargoComponent;
import be.mirooz.elitedangerous.dashboard.controller.ui.component.mining.CurrentProspectorComponent;
import be.mirooz.elitedangerous.dashboard.controller.ui.component.mining.MiningHistoryComponent;
import be.mirooz.elitedangerous.dashboard.controller.ui.component.mining.MiningSearchPanelComponent;
import be.mirooz.elitedangerous.dashboard.controller.ui.manager.UIManager;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.service.PreferencesService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Contrôleur pour le panneau de mining - Refactorisé avec composants
 * <p>
 * Cette classe a été refactorisée pour utiliser des composants séparés :
 * - MiningSearchPanelComponent : gestion de la recherche de routes
 * - CurrentProspectorComponent : affichage et navigation des prospecteurs
 * - CurrentCargoComponent : affichage du cargo actuel
 * - MiningHistoryComponent : historique des sessions de minage
 * <p>
 * Le contrôleur principal coordonne maintenant ces composants et gère les interactions entre eux.
 */
public class MiningController implements Initializable, IRefreshable {

    // Conteneurs pour les composants
    @FXML
    private VBox miningSearchPanelContainer;
    @FXML
    private VBox currentProspectorContainer;
    @FXML
    private VBox currentCargoContainer;
    @FXML
    private VBox miningHistoryContainer;

    // Labels pour les traductions

    // Services
    private final PreferencesService preferencesService = PreferencesService.getInstance();

    // Composants
    private MiningSearchPanelComponent miningSearchPanel;
    private CurrentProspectorComponent currentProspector;
    private CurrentCargoComponent currentCargo;
    private MiningHistoryComponent miningHistory;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
       //+ initializePricePreference();
        initializeComponents();
        setupComponentCallbacks();
        UIManager.getInstance().register(this);
    }

    /**
     * Initialise les composants en les chargeant depuis leurs fichiers FXML
     */
    private void initializeComponents() {
        try {
            // Charger le composant de recherche de minage
            FXMLLoader searchLoader = new FXMLLoader(getClass().getResource("/fxml/mining/mining-search-panel.fxml"));
            VBox searchPanel = searchLoader.load();
            miningSearchPanel = searchLoader.getController();
            if (miningSearchPanelContainer != null) {
                miningSearchPanelContainer.getChildren().add(searchPanel);
            }

            // Charger le composant de prospecteur actuel
            FXMLLoader prospectorLoader = new FXMLLoader(getClass().getResource("/fxml/mining/current-prospector.fxml"));
            VBox prospectorPanel = prospectorLoader.load();
            currentProspector = prospectorLoader.getController();
            if (currentProspectorContainer != null) {
                currentProspectorContainer.getChildren().add(prospectorPanel);
            }

            // Charger le composant de cargo actuel
            FXMLLoader cargoLoader = new FXMLLoader(getClass().getResource("/fxml/mining/current-cargo.fxml"));
            VBox cargoPanel = cargoLoader.load();
            currentCargo = cargoLoader.getController();
            if (currentCargoContainer != null) {
                currentCargoContainer.getChildren().add(cargoPanel);
            }

            // Charger le composant d'historique de minage
            FXMLLoader historyLoader = new FXMLLoader(getClass().getResource("/fxml/mining/mining-history.fxml"));
            VBox historyPanel = historyLoader.load();
            miningHistory = historyLoader.getController();
            if (miningHistoryContainer != null) {
                miningHistoryContainer.getChildren().add(historyPanel);
            }

            System.out.println("✅ Composants mining chargés avec succès depuis leurs fichiers FXML");
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du chargement des composants mining: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Configure les callbacks entre les composants
     */
    private void setupComponentCallbacks() {
        if (miningSearchPanel != null) {

            // Quand une recherche est terminée, mettre à jour le cargo et l'historique
            miningSearchPanel.setOnSearchCompleted(() -> {
                if (currentCargo != null) {
                    currentCargo.refresh();
                }
                if (miningHistory != null) {
                    miningHistory.refresh();
                }
            });
            miningSearchPanel.setOnPriceUpdated(() -> {
                if (miningHistory != null) {
                    miningHistory.refresh();
                }
            });
        }
    }

    /**
     * Met à jour toutes les traductions de l'interface
     */

    /**
     * Récupère une traduction depuis le LocalizationService
     */


    @Override
    public void refreshUI() {
        Platform.runLater(() -> {
            if (miningSearchPanel != null) {
                // Le composant de recherche se met à jour automatiquement
            }
            if (currentProspector != null) {
                currentProspector.refresh();
            }
            if (currentCargo != null) {
                currentCargo.refresh();
            }
            if (miningHistory != null) {

            }
        });
    }
}