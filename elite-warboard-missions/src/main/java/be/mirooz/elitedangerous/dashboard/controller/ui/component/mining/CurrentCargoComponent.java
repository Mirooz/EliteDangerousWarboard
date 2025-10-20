package be.mirooz.elitedangerous.dashboard.controller.ui.component.mining;

import be.mirooz.elitedangerous.dashboard.model.commander.CommanderShip;
import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.Mineral;
import javafx.scene.layout.GridPane;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.service.MiningService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

import java.net.URL;
import java.util.Map;
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
    private ProgressBar cargoProgressBar;
    @FXML
    private GridPane mineralsGridPane;
    @FXML
    private Label estimatedCreditsLabel;
    @FXML
    private Label estimatedCreditsTitleLabel;
    @FXML
    private Label currentCargoLabel;
    @FXML
    private Label limpetsLabel;
    @FXML
    private Label mineralHeaderLabel;
    @FXML
    private Label quantityHeaderLabel;
    @FXML
    private Label unitPriceHeaderLabel;
    @FXML
    private Label totalPriceHeaderLabel;

    Label noMineralsLabel;

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
                clearMineralRows();
                mineralsGridPane.add(createNoMineralsLabel(), 0, 1, 4, 1);
                return;
            }

            // Mettre à jour les statistiques du cargo
            cargoUsedLabel.setText(String.format("%d/%d", cargo.getCurrentUsed(), cargo.getMaxCapacity()));
            limpetsCountLabel.setText(String.valueOf(miningService.getLimpetsCount()));
            
            // Mettre à jour la barre de progression du cargo
            double cargoPercentage = (double) cargo.getCurrentUsed() / cargo.getMaxCapacity();
            cargoProgressBar.setProgress(cargoPercentage);

            // Calculer et afficher les CR estimés
            long estimatedCredits = miningService.calculateEstimatedCredits();
            estimatedCreditsLabel.setText(miningService.formatPrice(estimatedCredits));

            // Afficher les minéraux avec les prix (garder les en-têtes)
            clearMineralRows();
            createMineralsListWithPrices(miningService.getMinerals());

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
        if (limpetsLabel != null) {
            limpetsLabel.setText(getTranslation("mining.limpets"));
        }
        if (estimatedCreditsTitleLabel != null) {
            estimatedCreditsTitleLabel.setText(getTranslation("mining.estimated_credits"));
        }
        if (mineralHeaderLabel != null) {
            mineralHeaderLabel.setText(getTranslation("mining.mineral"));
        }
        if (quantityHeaderLabel != null) {
            quantityHeaderLabel.setText(getTranslation("mining.quantity"));
        }
        if (unitPriceHeaderLabel != null) {
            unitPriceHeaderLabel.setText(getTranslation("mining.unit_price"));
        }
        if (totalPriceHeaderLabel != null) {
            totalPriceHeaderLabel.setText(getTranslation("mining.total_price"));
        }
        if (noMineralsLabel !=null){
            noMineralsLabel.setText(getTranslation("mining.no_minerals"));
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
    private Label createNoMineralsLabel() {
        noMineralsLabel = new Label(getTranslation("mining.no_minerals"));
        noMineralsLabel.getStyleClass().add("no-minerals-label");
        return noMineralsLabel;
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

    public GridPane getMineralsGridPane() {
        return mineralsGridPane;
    }

    /**
     * Nettoie seulement les lignes de minéraux (garde les en-têtes)
     */
    private void clearMineralRows() {
        // Supprimer tous les enfants sauf les en-têtes (ligne 0)
        mineralsGridPane.getChildren().removeIf(node -> {
            Integer rowIndex = GridPane.getRowIndex(node);
            return rowIndex != null && rowIndex > 0;
        });
    }

    /**
     * Crée la liste des minéraux avec les prix unitaires et totaux
     */
    private void createMineralsListWithPrices(Map<Mineral, Integer> minerals) {
        if (minerals == null || minerals.isEmpty()) {
            mineralsGridPane.add(createNoMineralsLabel(), 0, 1, 4, 1);
            return;
        }

        int rowIndex = 1; // Commencer à la ligne 1 (après les en-têtes)
        for (Map.Entry<Mineral, Integer> entry : minerals.entrySet()) {
            Mineral mineral = entry.getKey();
            Integer quantity = entry.getValue();

            if (quantity != null && quantity > 0) {
                createMineralRow(mineral, quantity, rowIndex);
                rowIndex++;
            }
        }
    }

    /**
     * Crée une ligne pour un minéral avec toutes les informations dans le GridPane
     */
    private void createMineralRow(Mineral mineral, Integer quantity, int rowIndex) {
        // Nom du minéral
        Label mineralNameLabel = new Label(mineral.getVisibleName().toUpperCase());
        mineralNameLabel.getStyleClass().add("cargo-mineral-name");

        // Quantité
        Label quantityLabel = new Label(String.valueOf(quantity));
        quantityLabel.getStyleClass().add("cargo-mineral-quantity");

        // Prix unitaire
        long unitPrice = mineral.getPrice();
        Label unitPriceLabel = new Label(formatPriceWithCommas(unitPrice));
        unitPriceLabel.getStyleClass().add("cargo-mineral-unit-price");

        // Prix total
        long totalPrice = unitPrice * quantity;
        Label totalPriceLabel = new Label(formatPriceWithCommas(totalPrice));
        totalPriceLabel.getStyleClass().add("cargo-mineral-total-price");

        // Ajouter au GridPane
        mineralsGridPane.add(mineralNameLabel, 0, rowIndex);
        mineralsGridPane.add(quantityLabel, 1, rowIndex);
        mineralsGridPane.add(unitPriceLabel, 2, rowIndex);
        mineralsGridPane.add(totalPriceLabel, 3, rowIndex);
    }

    /**
     * Formate un prix avec des virgules pour la lisibilité
     */
    private String formatPriceWithCommas(long price) {
        return String.format("%,d Cr", price);
    }
}
