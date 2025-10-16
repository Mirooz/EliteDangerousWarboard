package be.mirooz.elitedangerous.dashboard.controller;

import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.CoreMineralType;
import be.mirooz.elitedangerous.dashboard.controller.ui.component.CargoInfoComponent;
import be.mirooz.elitedangerous.dashboard.controller.ui.component.ProspectorCardComponent;
import be.mirooz.elitedangerous.dashboard.controller.ui.manager.UIManager;
import be.mirooz.elitedangerous.dashboard.model.commander.CommanderShip;
import be.mirooz.elitedangerous.dashboard.model.events.ProspectedAsteroid;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.service.MiningService;
import be.mirooz.elitedangerous.lib.edtools.model.MiningHotspot;
import be.mirooz.elitedangerous.lib.inara.model.InaraCommoditiesStats;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.Comparator;
import java.util.Deque;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Contrôleur pour le panneau de mining - Refactorisé
 */
public class MiningController implements Initializable, IRefreshable {

    public static final int MAX_DISTANCE = 100;
    @FXML
    private VBox lastProspectorContainer;
    @FXML
    private VBox lastProspectorContent;
    @FXML
    private VBox prospectorsList;
    @FXML
    private Label cargoUsedLabel;
    @FXML
    private Label limpetsCountLabel;
    @FXML
    private VBox cargoMineralsList;
    @FXML
    private ComboBox<CoreMineralType> mineralComboBox;
    @FXML
    private Label headerPriceLabel;
    @FXML
    private Label headerRingNameLabel;
    @FXML
    private Label headerRingSystemLabel;
    @FXML
    private Label headerDistanceLabel;
    @FXML
    private Label headerStationNameLabel;
    @FXML
    private Label headerStationSystemLabel;
    
    // Labels pour les traductions
    @FXML
    private Label miningTitleLabel;
    @FXML
    private Label mineralTargetLabel;
    @FXML
    private Label priceLabel;
    @FXML
    private Label ringToMineLabel;
    @FXML
    private Label stationToSellLabel;
    @FXML
    private Label lastProspectorsLabel;
    @FXML
    private Label lastProspectorLabel;
    @FXML
    private Label currentCargoLabel;
    @FXML
    private Label cargoLabel;
    @FXML
    private Label limpetsLabel;

    private final MiningService miningService = MiningService.getInstance();
    private final LocalizationService localizationService = LocalizationService.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateProspectors();
        updateCargo();
        initializeMineralComboBox();
        initializeHeaderLabels();
        updateTranslations();
        UIManager.getInstance().register(this);
        
        // Écouter les changements de langue
        localizationService.addLanguageChangeListener(locale -> updateTranslations());
    }

    /**
     * Met à jour l'affichage des prospecteurs
     */
    public void updateProspectors() {
        Platform.runLater(() -> {
            Deque<ProspectedAsteroid> prospectors = miningService.getAllProspectors();

            // Vider les listes
            lastProspectorContent.getChildren().clear();
            prospectorsList.getChildren().clear();

            if (prospectors.isEmpty()) {
                lastProspectorContainer.setVisible(false);
                return;
            }

            // Afficher le dernier prospecteur (plus visible)
            Optional<ProspectedAsteroid> lastProspector = miningService.getLastProspector();
            if (lastProspector.isPresent()) {
                lastProspectorContainer.setVisible(true);
                VBox card = ProspectorCardComponent.createProspectorCard(lastProspector.get(), true);
                lastProspectorContent.getChildren().add(card);
            }

            // Afficher tous les prospecteurs dans la liste
            for (ProspectedAsteroid prospector : prospectors) {
                VBox cardContainer = new VBox();
                cardContainer.getStyleClass().add("prospector-card");
                VBox card = ProspectorCardComponent.createProspectorCard(prospector, false);
                cardContainer.getChildren().add(card);
                prospectorsList.getChildren().add(cardContainer);
            }
        });
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
                cargoMineralsList.getChildren().clear();
                cargoMineralsList.getChildren().add(CargoInfoComponent.createNoMineralsLabel());
                return;
            }

            // Mettre à jour les statistiques du cargo
            cargoUsedLabel.setText(String.format("%d/%d", cargo.getCurrentUsed(), cargo.getMaxCapacity()));
            limpetsCountLabel.setText(String.valueOf(miningService.getLimpetsCount()));

            // Afficher les minéraux en utilisant le composant
            cargoMineralsList.getChildren().clear();
            VBox mineralsList = CargoInfoComponent.createMineralsList(miningService.getMinerals());
            cargoMineralsList.getChildren().add(mineralsList);
        });
    }

    /**
     * Initialise les labels du header
     */
    private void initializeHeaderLabels() {
        clearHeaderLabels();
    }

    /**
     * Efface les labels du header
     */
    private void clearHeaderLabels() {
        Platform.runLater(() -> {
            headerPriceLabel.setText("--");
            headerRingNameLabel.setText(getTranslation("mining.undefined"));
            headerRingSystemLabel.setText("");
            headerDistanceLabel.setText("--");
            headerStationNameLabel.setText(getTranslation("mining.undefined_female"));
            headerStationSystemLabel.setText("");
        });
    }

    /**
     * Initialise le ComboBox avec tous les core minerals et leurs prix
     */
    private void initializeMineralComboBox() {
        mineralComboBox.setItems(FXCollections.observableArrayList(CoreMineralType.all()));
        mineralComboBox.setPromptText(getTranslation("mining.mineral_placeholder"));

        // CellFactory pour afficher le nom du minéral avec le prix
        mineralComboBox.setCellFactory(listView -> new ListCell<CoreMineralType>() {
            @Override
            protected void updateItem(CoreMineralType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    // Afficher le nom du minéral avec "Recherche..." en attendant le prix
                    setText(item.getVisibleName().toUpperCase() + " - " + getTranslation("mining.searching"));
                    // Charger le prix en arrière-plan
                    loadMineralPrice(item, this);
                }
            }
        });

        // ButtonCell pour l'affichage du minéral sélectionné
        mineralComboBox.setButtonCell(new ListCell<CoreMineralType>() {
            @Override
            protected void updateItem(CoreMineralType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getVisibleName().toUpperCase());
                }
            }
        });

        // Listener pour mettre à jour les informations quand un minéral est sélectionné
        mineralComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                searchMiningRouteForMineral(newVal);
            } else {
                clearHeaderLabels();
            }
        });
    }

    /**
     * Charge le prix d'un minéral spécifique et met à jour la cellule
     */
    private void loadMineralPrice(CoreMineralType mineral, ListCell<CoreMineralType> cell) {
        String sourceSystem = miningService.getCurrentSystem();
        int maxDistance = MAX_DISTANCE;
        int minDemand = miningService.getCurrentCargoCapacity();
        miningService.findMineralPrice(mineral,sourceSystem,maxDistance,minDemand)
                .thenAccept(priceOpt -> {
                    Platform.runLater(() -> {
                        if (priceOpt.isPresent()) {
                            InaraCommoditiesStats bestPrice = priceOpt.get();
                            cell.setText(String.format("%s - %s Cr",
                                    mineral.getVisibleName().toUpperCase(),
                                    miningService.formatPrice(bestPrice.getPrice())));
                        } else {
                            cell.setText(mineral.getVisibleName().toUpperCase() + " - " + getTranslation("mining.price_not_available"));
                        }
                    });
                })
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        cell.setText(mineral.getVisibleName().toUpperCase() + " - " + getTranslation("mining.price_error"));
                    });
                    return null;
                });
    }

    /**
     * Recherche une route de minage pour un minéral spécifique
     */
    private void searchMiningRouteForMineral(CoreMineralType mineral) {
        headerPriceLabel.setText(getTranslation("mining.searching"));
        headerRingNameLabel.setText(getTranslation("mining.searching"));
        headerRingSystemLabel.setText("");
        headerDistanceLabel.setText("--");
        headerStationNameLabel.setText(getTranslation("mining.searching"));
        headerStationSystemLabel.setText("");
        String sourceSystem = miningService.getCurrentSystem();
        int maxDistance = MAX_DISTANCE;
        int minDemand = miningService.getCurrentCargoCapacity();

        // Rechercher le prix et les informations de route
        miningService.findMineralPrice(mineral,sourceSystem,maxDistance,minDemand)
                .thenAccept(priceOpt -> {
                    if (priceOpt.isPresent()) {
                        InaraCommoditiesStats bestMarket = priceOpt.get();
                        

                        // Rechercher les hotspots de minage
                        miningService.getEdToolsService().findMiningHotspots(bestMarket.getSystemName(), mineral)
                                .thenAccept(hotspots -> Platform.runLater(() -> {
                                    if (hotspots != null && !hotspots.isEmpty()) {
                                        // Prendre le hotspot le plus proche
                                        MiningHotspot bestHotspot = hotspots.stream()
                                                .min(Comparator.comparingDouble(MiningHotspot::getDistanceFromReference))
                                                .orElse(hotspots.get(0));

                                        headerRingNameLabel.setText(bestHotspot.getRingName());
                                        headerRingSystemLabel.setText(bestHotspot.getSystemName());
                                        headerDistanceLabel.setText(String.format("%.1f AL", bestHotspot.getDistanceFromReference()));
                                        headerPriceLabel.setText(String.format("%s Cr", miningService.formatPrice(bestMarket.getPrice())));
                                        headerStationNameLabel.setText(bestMarket.getStationName());
                                        headerStationSystemLabel.setText(bestMarket.getSystemName());

                                    } else {
                                        headerRingNameLabel.setText(getTranslation("mining.no_hotspot_found"));
                                        headerRingSystemLabel.setText("");
                                        headerDistanceLabel.setText("--");
                                        headerRingNameLabel.setText(getTranslation("mining.search_error"));
                                        headerRingSystemLabel.setText("");
                                        headerDistanceLabel.setText("--");
                                    }
                                }));
                    } else {
                        Platform.runLater(() -> {
                            headerPriceLabel.setText(getTranslation("mining.price_not_available"));
                            headerRingNameLabel.setText(getTranslation("mining.no_market_found"));
                            headerRingSystemLabel.setText("");
                            headerDistanceLabel.setText("--");
                            headerStationNameLabel.setText(getTranslation("mining.no_station_found"));
                            headerStationSystemLabel.setText("");
                        });
                    }
                })
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        headerPriceLabel.setText(getTranslation("mining.price_error"));
                        headerRingNameLabel.setText(getTranslation("mining.search_error"));
                        headerRingSystemLabel.setText("");
                        headerDistanceLabel.setText("--");
                        headerStationNameLabel.setText(getTranslation("mining.search_error"));
                        headerStationSystemLabel.setText("");
                    });
                    return null;
                });
    }

    /**
     * Met à jour toutes les traductions de l'interface
     */
    private void updateTranslations() {
        if (miningTitleLabel != null) {
            miningTitleLabel.setText(getTranslation("mining.title"));
        }
        if (mineralTargetLabel != null) {
            mineralTargetLabel.setText(getTranslation("mining.mineral_target"));
        }
        if (priceLabel != null) {
            priceLabel.setText(getTranslation("mining.price"));
        }
        if (ringToMineLabel != null) {
            ringToMineLabel.setText(getTranslation("mining.ring_to_mine"));
        }
        if (stationToSellLabel != null) {
            stationToSellLabel.setText(getTranslation("mining.station_to_sell"));
        }
        if (lastProspectorsLabel != null) {
            lastProspectorsLabel.setText(getTranslation("mining.last_prospectors"));
        }
        if (lastProspectorLabel != null) {
            lastProspectorLabel.setText(getTranslation("mining.last_prospector"));
        }
        if (currentCargoLabel != null) {
            currentCargoLabel.setText(getTranslation("mining.current_cargo"));
        }
        if (cargoLabel != null) {
            cargoLabel.setText(getTranslation("mining.cargo"));
        }
        if (limpetsLabel != null) {
            limpetsLabel.setText(getTranslation("mining.limpets"));
        }
        
        // Mettre à jour le promptText du ComboBox
        if (mineralComboBox != null) {
            mineralComboBox.setPromptText(getTranslation("mining.mineral_placeholder"));
        }
    }

    /**
     * Récupère une traduction depuis le LocalizationService
     */
    private String getTranslation(String key) {
        return localizationService.getString(key);
    }

    @Override
    public void refreshUI() {
        updateProspectors();
        updateCargo();
    }
}