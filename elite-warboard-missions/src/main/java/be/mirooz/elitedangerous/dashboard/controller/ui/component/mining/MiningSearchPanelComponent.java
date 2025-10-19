package be.mirooz.elitedangerous.dashboard.controller.ui.component.mining;

import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.Mineral;
import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.MiningMethod;
import be.mirooz.elitedangerous.dashboard.controller.ui.component.TooltipComponent;
import be.mirooz.elitedangerous.dashboard.controller.ui.manager.CopyClipboardManager;
import be.mirooz.elitedangerous.dashboard.controller.ui.manager.PopupManager;
import be.mirooz.elitedangerous.dashboard.controller.ui.wrapper.MineralListWrapper;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.service.MiningService;
import be.mirooz.elitedangerous.lib.edtools.model.MiningHotspot;
import be.mirooz.elitedangerous.lib.inara.model.InaraCommoditiesStats;
import be.mirooz.elitedangerous.lib.inara.model.StationType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Composant pour le panneau de recherche de routes de minage
 * <p>
 * Ce composant gère :
 * - La sélection de minéraux
 * - Les options de recherche (Fleet Carrier, Large pads, distance)
 * - L'affichage des résultats de recherche
 * - La gestion des clics pour copier les noms de systèmes
 */
public class MiningSearchPanelComponent implements Initializable {

    // Services
    private final MiningService miningService = MiningService.getInstance();
    private final LocalizationService localizationService = LocalizationService.getInstance();
    private final CopyClipboardManager copyClipboardManager = CopyClipboardManager.getInstance();
    private final PopupManager popupManager = PopupManager.getInstance();

    // Images pour les icônes
    private Image laserImage;
    private Image coreImage;

    // Composants FXML
    @FXML
    private ComboBox<MineralListWrapper> mineralComboBox;
    @FXML
    private Label headerPriceLabel;
    @FXML
    private Label demandLabel;
    @FXML
    private Label headerDemandLabel;
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
    @FXML
    private Label headerStationDistanceLabel;
    @FXML
    private Label headerStationDistanceTitleLabel;
    @FXML
    private ImageView stationTypeImageView;
    @FXML
    private ProgressIndicator loadingIndicator;
    @FXML
    private HBox searchContentHBox;
    @FXML
    private HBox searchResultsContainer;

    @FXML
    private Label miningTitleLabel;

    @FXML
    private VBox mineralTargetContainer;
    @FXML
    private CheckBox fleetCarrierCheckBox;
    @FXML
    private CheckBox padsCheckBox;
    @FXML
    private Label maxDistanceLabel;
    @FXML
    private TextField maxDistanceTextField;
    @FXML
    private Label distanceUnitLabel;

    // Labels pour les traductions
    @FXML
    private Label mineralTargetLabel;
    @FXML
    private Label priceLabel;
    @FXML
    private Label ringToMineLabel;
    @FXML
    private Label stationToSellLabel;

    // Callback pour notifier le parent des changements
    private Consumer<Mineral> onMineralSelected;
    private Runnable onSearchCompleted;

    public static final int MAX_DISTANCE = 100;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadImages();
        initializeMineralComboBox();
        initializeFleetCarrierCheckBox();
        initializeMaxDistanceField();
        initializeHeaderLabels();
        initializeClickHandlers();
        updateTranslations();
        updateSearchResultsVisibility(); // Masquer les résultats par défaut

        // Écouter les changements de langue
        localizationService.addLanguageChangeListener(locale -> updateTranslations());
    }

    /**
     * Charge les images pour les icônes
     */
    private void loadImages() {
        try {
            laserImage = new Image(getClass().getResourceAsStream("/images/laser.png"));
            coreImage = new Image(getClass().getResourceAsStream("/images/core.png"));
        } catch (Exception e) {
            laserImage = null;
            coreImage = null;
        }
    }

    /**
     * Crée une icône pour une catégorie de minéraux (séparateur)
     */
    private Object createCategoryIcon(MiningMethod miningMethod) {
        if (MiningMethod.CORE.equals(miningMethod)) {
            if (coreImage != null) {
                ImageView imageView = new ImageView(coreImage);
                imageView.setFitWidth(25);
                imageView.setFitHeight(25);
                imageView.setPreserveRatio(true);
                return imageView;
            } else {
                return "🧭";
            }
        } else {
            if (laserImage != null) {
                ImageView imageView = new ImageView(laserImage);
                imageView.setFitWidth(25);
                imageView.setFitHeight(25);
                imageView.setPreserveRatio(true);
                return imageView;
            } else {
                return "🔫";
            }
        }
    }

    /**
     * Gère la visibilité des résultats de recherche
     */
    private void updateSearchResultsVisibility() {
        boolean hasMineralSelected = mineralComboBox.getValue() != null && 
                                   !mineralComboBox.getValue().isSeparator();
        
        if (searchResultsContainer != null) {
            searchResultsContainer.setVisible(hasMineralSelected);
            searchResultsContainer.setManaged(hasMineralSelected);
        }
    }

    /**
     * Initialise le ComboBox avec tous les minéraux organisés par catégorie
     */
    public void initializeMineralComboBox() {
        var items = MineralListWrapper.createOrganizedMineralsList();
        mineralComboBox.setItems(items);
        mineralComboBox.setPromptText(getTranslation("mining.mineral_placeholder"));

        // Préchargement des prix
        items.stream()
                .filter(i -> !i.isSeparator())
                .forEach(this::loadMineralPriceSafe);

        mineralComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(MineralListWrapper item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(null);

                if (empty || item == null) return;

                if (item.isSeparator()) {
                    Object icon = createCategoryIcon(item.getMiningMethod());
                    Label label = new Label(item.getMiningMethod().getMining());
                    if (icon instanceof ImageView iv) {
                        setGraphic(new HBox(5, iv, label));
                    } else {
                        setGraphic(null);
                        setText(icon + " " + label.getText());
                    }
                    setDisable(true);
                    return;
                }

                setDisable(false);
                setText(item.getMineral().getTitleName() + " - " + item.getDisplayPriceFormatted());
                item.displayPriceProperty().addListener((obs, oldV, newV) -> {
                    if (getItem() == item) {
                        setText(item.getMineral().getTitleName() + " - " + newV);
                    }
                });
            }
        });

        // Bouton (valeur sélectionnée)
        mineralComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(MineralListWrapper item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setStyle("");
                setGraphic(null);
                if (empty || item == null || item.isSeparator()) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setGraphic(null);
                    Object icon = createCategoryIcon(item.getMiningMethod());
                    Label label = new Label();
                    if (icon instanceof ImageView iv) {
                        setGraphic(new HBox(5, iv, label));
                        setText(item.getMineral().getVisibleName());
                    } else {
                        setGraphic(null);
                        setText(icon + " " + label.getText());
                    }
                }
            }
        });

        mineralComboBox.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null && !n.isSeparator()) {
                searchMiningRouteForMineral(n.getMineral());
                if (onMineralSelected != null) {
                    onMineralSelected.accept(n.getMineral());
                }
            } else {
                clearHeaderLabels();
            }
            // Mettre à jour la visibilité des résultats
            updateSearchResultsVisibility();
        });
    }

    /**
     * Empêche l'async d'écrire dans une cellule réutilisée.
     */
    private void loadMineralPriceSafe(MineralListWrapper item) {
        Mineral mineral = item.getMineral();
        String sourceSystem = miningService.getCurrentSystem();
        int maxDistance = getMaxDistanceFromField();
        int minDemand = miningService.getCurrentCargoCapacity();

        if (item.getMineral().getPrice() == 0) {
            miningService.findMineralPrice(mineral, sourceSystem, maxDistance, minDemand,
                            padsCheckBox.isSelected(), fleetCarrierCheckBox.isSelected())
                    .thenAccept(priceOpt -> Platform.runLater(() -> {
                        priceOpt.ifPresentOrElse(
                                price -> {
                                    item.getMineral().setPrice(price.getPrice());
                                },
                                () -> item.setDisplayPriceError(getTranslation("mining.price_error"))
                        );
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            item.setDisplayPriceError(getTranslation("mining.price_error"));
                        });
                        return null;
                    });
        }
    }

    /**
     * Recherche une route de minage pour un minéral spécifique
     */
    private void searchMiningRouteForMineral(Mineral mineral) {
        setLoadingVisible(true);
        clearHeaderLabels();

        String sourceSystem = miningService.getCurrentSystem();
        int maxDistance = getMaxDistanceFromField();
        int minDemand = miningService.getCurrentCargoCapacity();

        miningService.searchMiningRoute(mineral, sourceSystem, maxDistance, minDemand,
                        padsCheckBox.isSelected(), fleetCarrierCheckBox.isSelected())
                .thenAccept(routeResult -> Platform.runLater(() -> {
                    setLoadingVisible(false);

                    if (routeResult.hasMarket()) {
                        InaraCommoditiesStats bestMarket = routeResult.getMarket();
                        headerPriceLabel.setText(String.format("%s Cr ", miningService.formatPrice(bestMarket.getPrice())));
                        headerDemandLabel.setText(String.format("%d T", bestMarket.getDemand()));
                        headerStationNameLabel.setText(bestMarket.getStationName());
                        headerStationSystemLabel.setText(bestMarket.getSystemName());
                        headerStationDistanceLabel.setText(String.format("%.1f %s",
                                bestMarket.getSystemDistance(), getTranslation("search.distance.unit")));
                        updateStationTypeImage(bestMarket.getStationType());

                        if (routeResult.hasHotspot()) {
                            MiningHotspot bestHotspot = routeResult.getHotspot();
                            headerRingNameLabel.setText(bestHotspot.getRingName());
                            headerRingSystemLabel.setText(bestHotspot.getSystemName());
                            headerDistanceLabel.setText(String.format("%.1f %s",
                                    bestHotspot.getDistanceFromReference(), getTranslation("search.distance.unit")));
                        } else {
                            headerRingNameLabel.setText(getTranslation("mining.no_hotspot_found"));
                            headerRingSystemLabel.setText("");
                            headerDistanceLabel.setText("--");
                        }
                    } else {
                        headerPriceLabel.setText(getTranslation("mining.price_not_available"));
                        headerDemandLabel.setText("--");
                        headerRingNameLabel.setText(getTranslation("mining.no_market_found"));
                        headerRingSystemLabel.setText("");
                        headerDistanceLabel.setText("--");
                        headerStationNameLabel.setText(getTranslation("mining.no_station_found"));
                        headerStationSystemLabel.setText("");
                    }

                    if (onSearchCompleted != null) {
                        onSearchCompleted.run();
                    }
                }))
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        setLoadingVisible(false);
                        headerPriceLabel.setText(getTranslation("mining.price_error"));
                        headerDemandLabel.setText("--");
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
     * Initialise les labels du header
     */
    private void initializeHeaderLabels() {
        clearHeaderLabels();
    }

    /**
     * Initialise les gestionnaires de clic pour copier les noms de systèmes
     */
    private void initializeClickHandlers() {
        // Les gestionnaires de clic sont définis dans le FXML
    }

    /**
     * Gestionnaire de clic pour le conteneur de l'anneau
     */
    @FXML
    public void onRingContainerClicked(MouseEvent event) {
        String systemName = headerRingSystemLabel.getText();
        if (systemName != null && !systemName.isEmpty()) {
            copyClipboardManager.copyToClipboard(systemName);
            Stage stage = (Stage) headerRingSystemLabel.getScene().getWindow();
            popupManager.showPopup(getTranslation("system.copied"), event.getSceneX(), event.getSceneY(), stage);
        }
    }

    /**
     * Gestionnaire de clic pour le conteneur de la station
     */
    @FXML
    public void onStationContainerClicked(MouseEvent event) {
        String systemName = headerStationSystemLabel.getText();
        if (systemName != null && !systemName.isEmpty()) {
            copyClipboardManager.copyToClipboard(systemName);
            Stage stage = (Stage) headerStationSystemLabel.getScene().getWindow();
            popupManager.showPopup(getTranslation("system.copied"), event.getSceneX(), event.getSceneY(), stage);
        }
    }

    /**
     * Efface les labels du header
     */
    private void clearHeaderLabels() {
        Platform.runLater(() -> {
            headerPriceLabel.setText("--");
            headerDemandLabel.setText("--");
            headerRingNameLabel.setText(getTranslation("mining.undefined"));
            headerRingSystemLabel.setText("");
            headerDistanceLabel.setText("--");
            headerStationNameLabel.setText(getTranslation("mining.undefined_female"));
            headerStationSystemLabel.setText("");
            headerStationDistanceLabel.setText("--");
            stationTypeImageView.setImage(null);
        });
    }

    /**
     * Gère la visibilité de l'indicateur de chargement
     */
    private void setLoadingVisible(boolean visible) {
        Platform.runLater(() -> {
            loadingIndicator.setVisible(visible);
            loadingIndicator.setManaged(visible);

            if (visible) {
                searchContentHBox.getChildren().forEach(child -> {
                    if (child != mineralTargetContainer) {
                        child.setVisible(false);
                        child.setManaged(false);
                    }
                });
            } else {
                searchContentHBox.getChildren().forEach(child -> {
                    child.setVisible(true);
                    child.setManaged(true);
                });
            }
        });
    }

    /**
     * Met à jour l'image du type de station
     */
    private void updateStationTypeImage(StationType stationType) {
        Platform.runLater(() -> {
            if (stationType != null) {
                try {
                    String imagePath = "/images/stations/" + stationType.getImage();
                    Image stationImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream(imagePath)));
                    stationTypeImageView.setImage(stationImage);
                } catch (Exception e) {
                    stationTypeImageView.setImage(null);
                }
            } else {
                stationTypeImageView.setImage(null);
            }
        });
    }

    /**
     * Initialise le checkbox Fleet Carrier avec l'image
     */
    private void initializeFleetCarrierCheckBox() {
        if (fleetCarrierCheckBox != null) {
            try {
                Image fleetImage = new Image(getClass().getResourceAsStream("/images/fleet.png"));
                ImageView fleetImageView = new ImageView(fleetImage);
                fleetImageView.setFitWidth(16);
                fleetImageView.setFitHeight(16);
                fleetImageView.setPreserveRatio(true);

                HBox contentBox = new HBox(5);
                contentBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                contentBox.getChildren().addAll(fleetImageView, new Label("Fleet Carrier"));

                fleetCarrierCheckBox.setGraphic(contentBox);
                fleetCarrierCheckBox.setTooltip(new TooltipComponent(getTranslation("mining.fleet_carrier_hint")));
            } catch (Exception e) {
                fleetCarrierCheckBox.setText("Fleet Carrier");
                fleetCarrierCheckBox.setTooltip(new TooltipComponent(getTranslation("mining.fleet_carrier_hint")));
            }
        }

        if (padsCheckBox != null) {
            padsCheckBox.setText("Large pads");
            padsCheckBox.setTooltip(new TooltipComponent(getTranslation("mining.pads_hint")));
        }
    }

    /**
     * Récupère la distance maximale depuis le champ de saisie
     */
    private int getMaxDistanceFromField() {
        String distanceText = maxDistanceTextField != null ? maxDistanceTextField.getText() : "";
        return miningService.getMaxDistanceFromField(distanceText, MAX_DISTANCE);
    }

    /**
     * Initialise le champ de distance maximale
     */
    private void initializeMaxDistanceField() {
        if (maxDistanceTextField != null) {
            maxDistanceTextField.textProperty().addListener((observable, oldValue, newValue) -> {
                String validatedText = miningService.validateDistanceText(newValue);
                if (!validatedText.equals(newValue)) {
                    maxDistanceTextField.setText(validatedText);
                }
            });

            maxDistanceTextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue && mineralComboBox.getValue() != null) {
                    MineralListWrapper selectedWrapper = mineralComboBox.getValue();
                    if (selectedWrapper.getMineral() != null) {
                        searchMiningRouteForMineral(selectedWrapper.getMineral());
                    }
                }
            });
        }
    }

    /**
     * Gère le toggle du checkbox Fleet Carrier
     */
    @FXML
    public void onFleetCarrierToggle() {
        if (mineralComboBox.getValue() != null) {
            MineralListWrapper selectedWrapper = mineralComboBox.getValue();
            if (selectedWrapper.getMineral() != null) {
                searchMiningRouteForMineral(selectedWrapper.getMineral());
            }
        }
    }

    /**
     * Gère le toggle du checkbox Pads
     */
    @FXML
    public void onPadsToggle() {
        if (mineralComboBox.getValue() != null) {
            MineralListWrapper selectedWrapper = mineralComboBox.getValue();
            if (selectedWrapper.getMineral() != null) {
                searchMiningRouteForMineral(selectedWrapper.getMineral());
            }
        }
    }

    /**
     * Met à jour toutes les traductions de l'interface
     */
    private void updateTranslations() {
        if (mineralTargetLabel != null) {
            mineralTargetLabel.setText(getTranslation("mining.mineral_target"));
        }
        if (priceLabel != null) {
            priceLabel.setText(getTranslation("mining.price"));
        }
        if (demandLabel != null) {
            demandLabel.setText(getTranslation("mining.demand"));
        }
        if (ringToMineLabel != null) {
            ringToMineLabel.setText(getTranslation("mining.ring_to_mine"));
        }
        if (stationToSellLabel != null) {
            stationToSellLabel.setText(getTranslation("mining.station_to_sell"));
        }

        if (mineralComboBox != null) {
            mineralComboBox.setPromptText(getTranslation("mining.mineral_placeholder"));
        }

        if (fleetCarrierCheckBox != null) {
            fleetCarrierCheckBox.setTooltip(new TooltipComponent(getTranslation("mining.fleet_carrier_hint")));
        }

        if (maxDistanceLabel != null) {
            maxDistanceLabel.setText(getTranslation("mining.max_distance"));
        }
        if (distanceUnitLabel != null) {
            distanceUnitLabel.setText(getTranslation("mining.distance_unit"));
        }
        if (headerStationDistanceTitleLabel != null) {
            headerStationDistanceTitleLabel.setText(getTranslation("mining.station_distance_from_system"));
        }
        if (miningTitleLabel != null) {
            miningTitleLabel.setText(getTranslation("mining.title"));
        }
        updateDistanceUnits();
    }

    /**
     * Met à jour les unités de distance dans les labels existants
     */
    private void updateDistanceUnits() {
        if (headerDistanceLabel != null && !headerDistanceLabel.getText().equals("--")) {
            String currentText = headerDistanceLabel.getText();
            if (currentText.contains("AL") || currentText.contains("LY")) {
                String newText = currentText.replaceAll("AL|LY", getTranslation("search.distance.unit"));
                headerDistanceLabel.setText(newText);
            }
        }

        if (headerStationDistanceLabel != null && !headerStationDistanceLabel.getText().equals("--")) {
            String currentText = headerStationDistanceLabel.getText();
            if (currentText.contains("AL") || currentText.contains("LY")) {
                String newText = currentText.replaceAll("AL|LY", getTranslation("search.distance.unit"));
                headerStationDistanceLabel.setText(newText);
            }
        }
    }

    /**
     * Récupère une traduction depuis le LocalizationService
     */
    private String getTranslation(String key) {
        return localizationService.getString(key);
    }

    // Getters et setters pour les callbacks
    public void setOnMineralSelected(Consumer<Mineral> onMineralSelected) {
        this.onMineralSelected = onMineralSelected;
    }

    public void setOnSearchCompleted(Runnable onSearchCompleted) {
        this.onSearchCompleted = onSearchCompleted;
    }

    // Getters pour accéder aux composants depuis l'extérieur
    public ComboBox<MineralListWrapper> getMineralComboBox() {
        return mineralComboBox;
    }

    public CheckBox getFleetCarrierCheckBox() {
        return fleetCarrierCheckBox;
    }

    public CheckBox getPadsCheckBox() {
        return padsCheckBox;
    }

    public TextField getMaxDistanceTextField() {
        return maxDistanceTextField;
    }
}
