package be.mirooz.elitedangerous.dashboard.controller.ui.component.mining;

import be.mirooz.elitedangerous.dashboard.controller.ui.component.CargoInfoComponent;
import be.mirooz.elitedangerous.dashboard.model.commander.CommanderShip;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.service.MiningService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Composant pour l'affichage du cargo actuel
 * <p>
 * Ce composant gère :
 * - L'affichage de l'utilisation du cargo
 * - Le nombre de limpets
 * - Les crédits estimés
 * - La liste des minéraux dans le cargo
 */
public class CurrentCargoComponent implements Initializable {

    // Services
    private final MiningService miningService = MiningService.getInstance();
    private final LocalizationService localizationService = LocalizationService.getInstance();

    // Composants FXML
    @FXML
    private Label cargoUsedLabel;
    @FXML
    private Label limpetsCountLabel;
    @FXML
    private VBox cargoMineralsList;
    @FXML
    private Label estimatedCreditsLabel;
    @FXML
    private Label estimatedCreditsTitleLabel;
    @FXML
    private Label currentCargoLabel;
    @FXML
    private Label cargoLabel;
    @FXML
    private Label limpetsLabel;

    // Callback pour notifier le parent des changements
    private Runnable onCargoUpdated;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateCargo();
        updateTranslations();
        
        // Écouter les changements de langue
        localizationService.addLanguageChangeListener(locale -> updateTranslations());
    }

    /**
     * Met à jour l'affichage du cargo
     */
    public void updateCargo() {
        Platform.runLater(() -> {
            CommanderShip.ShipCargo cargo = miningService.getCargo();

            if (cargo == null) {
                cargoUsedLabel.setText("0/0");
                limpetsCountLabel.setText("0");
                estimatedCreditsLabel.setText("0");
                cargoMineralsList.getChildren().clear();
                cargoMineralsList.getChildren().add(CargoInfoComponent.createNoMineralsLabel());
                return;
            }

            // Mettre à jour les statistiques du cargo
            cargoUsedLabel.setText(String.format("%d/%d", cargo.getCurrentUsed(), cargo.getMaxCapacity()));
            limpetsCountLabel.setText(String.valueOf(miningService.getLimpetsCount()));

            // Calculer et afficher les CR estimés
            long estimatedCredits = miningService.calculateEstimatedCredits();
            estimatedCreditsLabel.setText(miningService.formatPrice(estimatedCredits));

            // Afficher les minéraux en utilisant le composant
            cargoMineralsList.getChildren().clear();
            VBox mineralsList = CargoInfoComponent.createMineralsList(miningService.getMinerals());
            cargoMineralsList.getChildren().add(mineralsList);

            // Notifier le parent du changement
            if (onCargoUpdated != null) {
                onCargoUpdated.run();
            }
        });
    }

    /**
     * Met à jour toutes les traductions de l'interface
     */
    private void updateTranslations() {
        if (currentCargoLabel != null) {
            currentCargoLabel.setText(getTranslation("mining.current_cargo"));
        }
        if (cargoLabel != null) {
            cargoLabel.setText(getTranslation("mining.cargo"));
        }
        if (limpetsLabel != null) {
            limpetsLabel.setText(getTranslation("mining.limpets"));
        }
        if (estimatedCreditsTitleLabel != null) {
            estimatedCreditsTitleLabel.setText(getTranslation("mining.estimated_credits"));
        }
    }

    /**
     * Récupère une traduction depuis le LocalizationService
     */
    private String getTranslation(String key) {
        return localizationService.getString(key);
    }

    // Getters et setters
    public void setOnCargoUpdated(Runnable onCargoUpdated) {
        this.onCargoUpdated = onCargoUpdated;
    }

    /**
     * Retourne le cargo actuel
     */
    public CommanderShip.ShipCargo getCurrentCargo() {
        return miningService.getCargo();
    }

    /**
     * Retourne le nombre de limpets
     */
    public int getLimpetsCount() {
        return miningService.getLimpetsCount();
    }

    /**
     * Retourne les crédits estimés
     */
    public long getEstimatedCredits() {
        return miningService.calculateEstimatedCredits();
    }

    /**
     * Force le rafraîchissement de l'affichage
     */
    public void refresh() {
        updateCargo();
    }

    // Getters pour accéder aux composants depuis l'extérieur
    public Label getCargoUsedLabel() {
        return cargoUsedLabel;
    }

    public Label getLimpetsCountLabel() {
        return limpetsCountLabel;
    }

    public Label getEstimatedCreditsLabel() {
        return estimatedCreditsLabel;
    }

    public VBox getCargoMineralsList() {
        return cargoMineralsList;
    }
}
