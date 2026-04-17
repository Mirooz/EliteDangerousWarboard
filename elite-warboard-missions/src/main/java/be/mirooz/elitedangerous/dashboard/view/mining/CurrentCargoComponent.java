package be.mirooz.elitedangerous.dashboard.view.mining;

import be.mirooz.elitedangerous.dashboard.model.registries.commander.CommanderShip;
import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.Mineral;
import be.mirooz.elitedangerous.dashboard.service.listeners.CargoEventNotificationService;
import javafx.scene.layout.GridPane;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.service.MiningService;
import be.mirooz.elitedangerous.dashboard.service.listeners.MineralPriceNotificationService;
import be.mirooz.elitedangerous.dashboard.service.PreferencesService;
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
public class CurrentCargoComponent implements Initializable, MineralPriceNotificationService.MineralPriceListener, CargoEventNotificationService.CargoEventInterface {

    // Services
    private final MiningService miningService = MiningService.getInstance();
    private final LocalizationService localizationService = LocalizationService.getInstance();
    private final MineralPriceNotificationService priceNotificationService = MineralPriceNotificationService.getInstance();
    private final CargoEventNotificationService cargoEventNotificationService = CargoEventNotificationService.getInstance();
    private final PreferencesService preferencesService = PreferencesService.getInstance();

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
    @FXML
    private ToggleSwitch priceModeToggle;

    Label noMineralsLabel;

    // Callback pour notifier le parent des changements
    private Runnable onCargoUpdated;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateCargo();
        updateTranslations();
        
        // Écouter les changements de langue
        localizationService.addLanguageChangeListener(locale -> updateTranslations());
        
        // Écouter les changements de prix des minéraux
        priceNotificationService.addListener(this);
        cargoEventNotificationService.addListener(this);

        
        // Configurer le toggle
        setupPriceModeToggle();
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
            cargoUsedLabel.setText(String.format("%d/%d", cargo.getCurrentUsed(), miningService.getCurrentCargoCapacity()));
            limpetsCountLabel.setText(String.valueOf(miningService.getLimpetsCount()));
            
            // Mettre à jour la barre de progression du cargo
            double cargoPercentage = (double) cargo.getCurrentUsed() / miningService.getCurrentCargoCapacity();
            cargoProgressBar.setProgress(cargoPercentage);

            // Calculer et afficher les CR estimés selon le mode
            long estimatedCredits;
            if (isPriceStationMode()) {
                estimatedCredits = calculateStationCredits();
                // Appliquer la couleur verte pour le mode station
                estimatedCreditsLabel.getStyleClass().clear();
                estimatedCreditsLabel.getStyleClass().add("cargo-stat-price-number-station");
            } else {
                estimatedCredits = miningService.calculateEstimatedCredits();
                // Appliquer la couleur normale pour le mode best price
                estimatedCreditsLabel.getStyleClass().clear();
                estimatedCreditsLabel.getStyleClass().add("cargo-stat-price-number");
            }
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
            updatePriceHeaders();
        }
        if (totalPriceHeaderLabel != null) {
            updatePriceHeaders();
        }
        if (priceModeToggle != null) {
            priceModeToggle.getLeftLabel().setText(getTranslation("mining.best_price"));
            priceModeToggle.getRightLabel().setText(getTranslation("mining.price_station"));
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


    /**
     * Force le rafraîchissement de l'affichage
     */
    public void refresh() {
        updateCargo();
    }

    /**
     * Nettoie les ressources (à appeler lors de la destruction du composant)
     */
    public void cleanup() {
        priceNotificationService.removeListener(this);
    }

    /**
     * Configure le toggle de mode prix
     */
    private void setupPriceModeToggle() {
        if (priceModeToggle != null) {
            priceModeToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
                updatePriceHeaders(); // Mettre à jour les en-têtes
                updateCargo(); // Rafraîchir l'affichage
            });
        }
    }

    /**
     * Retourne true si le mode "price station" est sélectionné
     */
    private boolean isPriceStationMode() {
        return priceModeToggle != null && priceModeToggle.isSelected();
    }

    /**
     * Met à jour les en-têtes des colonnes de prix selon le mode sélectionné
     */
    private void updatePriceHeaders() {
        if (unitPriceHeaderLabel != null && totalPriceHeaderLabel != null) {
            if (isPriceStationMode()) {
                unitPriceHeaderLabel.setText(getTranslation("mining.station_price"));
                totalPriceHeaderLabel.setText(getTranslation("mining.station_total"));
            } else {
                unitPriceHeaderLabel.setText(getTranslation("mining.unit_price"));
                totalPriceHeaderLabel.setText(getTranslation("mining.total_price"));
            }
        }
    }

    /**
     * Calcule les crédits estimés avec les prix de station
     */
    private long calculateStationCredits() {
        Map<Mineral, Integer> minerals = miningService.getMinerals();
        return minerals.entrySet().stream()
                .mapToLong(entry -> {
                    Mineral mineral = entry.getKey();
                    Integer quantity = entry.getValue();
                    long stationPrice = getStationPriceForMineral(mineral);
                    return stationPrice * quantity;
                })
                .sum();
    }
    private Label createNoMineralsLabel() {
        noMineralsLabel = new Label(getTranslation("mining.no_minerals"));
        noMineralsLabel.getStyleClass().add("no-minerals-label");
        return noMineralsLabel;
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

        // Déterminer les prix selon le mode sélectionné
        long unitPrice, totalPrice;
        String unitPriceStyle, totalPriceStyle;
        
        if (isPriceStationMode()) {
            // Mode "Price Station" - utiliser les prix de station
            unitPrice = getStationPriceForMineral(mineral);
            totalPrice = unitPrice * quantity;
            unitPriceStyle = unitPrice <= 0 ? "cargo-mineral-null-price" : "cargo-mineral-station-price-success";
            totalPriceStyle = totalPrice <= 0 ? "cargo-mineral-null-price" : "cargo-mineral-station-total-success";
        } else {
            // Mode "Best Price" - utiliser les prix stockés
            unitPrice = mineral.getPrice();
            totalPrice = unitPrice * quantity;
            unitPriceStyle = unitPrice <= 0 ? "cargo-mineral-null-price" : "cargo-mineral-unit-price";
            totalPriceStyle = totalPrice <= 0 ? "cargo-mineral-null-price" : "cargo-mineral-total-price";
        }

        // Prix unitaire
        Label unitPriceLabel = new Label(formatPriceWithoutCr(unitPrice));
        unitPriceLabel.getStyleClass().add(unitPriceStyle);

        // Prix total
        Label totalPriceLabel = new Label(formatPriceWithoutCr(totalPrice));
        totalPriceLabel.getStyleClass().add(totalPriceStyle);

        // Ajouter au GridPane (seulement 4 colonnes maintenant)
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

    /**
     * Formate un prix sans "Cr" et avec "/" pour les valeurs nulles
     */
    private String formatPriceWithoutCr(long price) {
        if (price <= 0) {
            return "/";
        }
        return String.format("%,d", price);
    }

    /**
     * Récupère le prix de station pour un minéral depuis la station actuelle
     */
    private long getStationPriceForMineral(Mineral mineral) {
        // Utiliser la station actuellement sélectionnée
        return miningService.getArdentApiService().getMineralPriceInCurrentStation(mineral.getInaraName());
    }

    // Implémentation de MineralPriceNotificationService.MineralPriceListener
    
    @Override
    public void onMineralPriceChanged() {
        Platform.runLater(() -> {
            System.out.println("💰 Prix  changé");
            // Rafraîchir l'affichage du cargo pour refléter le nouveau prix
            updateCargo();
        });
    }

    @Override
    public void onReadCargoJson() {
        updateCargo();
    }
}
