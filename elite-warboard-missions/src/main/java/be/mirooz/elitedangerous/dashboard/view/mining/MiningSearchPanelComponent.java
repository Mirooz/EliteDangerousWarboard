package be.mirooz.elitedangerous.dashboard.view.mining;

import be.mirooz.ardentapi.model.CommoditiesStats;
import be.mirooz.ardentapi.model.StationType;
import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.Mineral;
import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.MiningMethod;
import be.mirooz.elitedangerous.dashboard.view.common.IBatchListener;
import be.mirooz.elitedangerous.dashboard.view.common.TooltipComponent;
import be.mirooz.elitedangerous.dashboard.view.mining.wrapper.MineralListWrapper;
import be.mirooz.elitedangerous.dashboard.view.common.context.DashboardContext;
import be.mirooz.elitedangerous.dashboard.view.common.managers.CopyClipboardManager;
import be.mirooz.elitedangerous.dashboard.view.common.managers.PopupManager;
import be.mirooz.elitedangerous.dashboard.service.DashboardService;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.service.MiningService;
import be.mirooz.elitedangerous.dashboard.service.listeners.MineralPriceNotificationService;
import be.mirooz.elitedangerous.lib.edtools.model.MiningHotspot;
import be.mirooz.ardentapi.model.CommodityMaxSell;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
public class MiningSearchPanelComponent implements Initializable, IBatchListener {

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
    private Button previousStationButton;
    @FXML
    private Button nextStationButton;
    @FXML
    private Button previousRingButton;
    @FXML
    private Button nextRingButton;
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
    private Runnable onPriceUpdated;

    public static final int MAX_DISTANCE = 100;

    private final MineralPriceNotificationService priceNotificationService = MineralPriceNotificationService.getInstance();
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadImages();
        initializeMineralComboBox();
        initializeFleetCarrierCheckBox();
        initializeMaxDistanceField();
        initializeHeaderLabels();
        initializeClickHandlers();
        initializeStationNavigation();
        updateTranslations();
        updateSearchResultsVisibility(); // Masquer les résultats par défaut

        // Écouter les changements de langue
        localizationService.addLanguageChangeListener(locale -> updateTranslations());
        DashboardService.getInstance().addBatchListener(this);
    }
    @Override
    public void onBatchStart() {
        mineralComboBox.getSelectionModel().clearSelection();
        updateSearchResultsVisibility();
    }

    @Override
    public void onBatchEnd() {
        List<MineralListWrapper> items = mineralComboBox.getItems();
        loadMineralPriceSafe(items);
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
        }
    }

    /**
     * Initialise le ComboBox avec tous les minéraux organisés par catégorie
     */
    public void initializeMineralComboBox() {
        var items = MineralListWrapper.createOrganizedMineralsList();
        mineralComboBox.setItems(items);
        mineralComboBox.setPromptText(getTranslation("mining.mineral_placeholder"));

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

                    Node graphic;
                    if (icon instanceof ImageView iv) {
                        graphic = new HBox(5, iv, label);
                    } else {
                        label.setText(icon + " " + label.getText());
                        graphic = label;
                    }

                    // 🔸 Crée une ligne (separator visuel)
                    Region line = new Region();
                    line.setPrefHeight(1);
                    line.setMaxHeight(1);
                    line.setStyle("-fx-background-color: -fx-elite-orange;");

                    VBox content = new VBox(2, graphic, line);
                    setGraphic(content);

                    setDisable(true);
                    setText(null);
                    return;
                }

                setDisable(false);
                setText(item.getMineral().getTitleName() + " - " + item.getDisplayPriceFormatted());
                item.displayPriceProperty().addListener((obs, oldV, newV) -> {
                    if (getItem() == item) {
                        Platform.runLater(() ->
                                setText(item.getMineral().getTitleName() + " - " + newV)
                        );
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
        });
    }

    /**
     * Empêche l'async d'écrire dans une cellule réutilisée.
     */
    private void loadMineralPriceSafe(List<MineralListWrapper> minerals) {
        miningService.fetchCommoditiesMaxSell()
                .thenAccept(prices -> Platform.runLater(() -> {
                    if (prices != null && !prices.isEmpty()) {
                        minerals.stream()
                                .filter(i -> !i.isSeparator())
                                .forEach(mineral -> {
                                    Optional<CommodityMaxSell> commodityMaxSell = prices.stream().filter(
                                                    p -> p.getCommodityName().equals(mineral.getMineral().getCargoJsonName()))
                                            .findFirst();
                                    commodityMaxSell.ifPresent(
                                            maxSell -> mineral.getMineral().setPrice(maxSell.getMaxSellPrice()));
                                });
                        DashboardContext.getInstance().refreshUI();
                        onPriceUpdated.run();
                    }
                }));
    }

    /**
     * Recherche une route de minage pour un minéral spécifique avec un index de station
     *
     * @param mineral      Le minéral à rechercher
     * @param stationIndex L'index de la station à afficher (0 = première station)
     */
    private void searchMiningRouteForMineral(Mineral mineral, int stationIndex) {
        setLoadingVisible(true);
        clearHeaderLabels();

        String sourceSystem = miningService.getCurrentSystem();
        int maxDistance = getMaxDistanceFromField();
        int minDemand = miningService.getCurrentCargoCapacity();
        miningService.findMineralStation(mineral, sourceSystem, maxDistance, minDemand,
                        padsCheckBox.isSelected(), fleetCarrierCheckBox.isSelected())
                .thenAccept(CommoditiesStats -> Platform.runLater(() -> {
                    // Récupérer tous les résultats depuis le cache d'InaraService
                    miningService.getArdentApiService().setSearchResults(CommoditiesStats);
                    // Définir l'index de la station à afficher
                    miningService.getArdentApiService().setCurrentResultIndex(stationIndex);
                    if (!CommoditiesStats.isEmpty()) {
                        CommoditiesStats currentResult = miningService.getArdentApiService().getCurrentResult();
                        // Mettre à jour l'affichage de la station
                        updateStationDisplay(currentResult);
                        updateHotspots(currentResult);
                        updateCurrentStationMarket(currentResult);
                    } else {
                        setNotFoundSearch();
                        updateRingNavigationButtons();
                    }
                    updateStationNavigationButtons();

                    if (onSearchCompleted != null) {
                        onSearchCompleted.run();
                    }
                }))
                .exceptionally(throwable -> {
                    setErrorSearch();
                    return null;
                });
    }

    private void setNotFoundSearch() {
        headerPriceLabel.setText(getTranslation("mining.price_not_available"));
        headerDemandLabel.setText("--");
        headerRingNameLabel.setText(getTranslation("mining.no_market_found"));
        headerRingSystemLabel.setText("");
        headerDistanceLabel.setText("--");
        headerStationNameLabel.setText(getTranslation("mining.no_station_found"));
        headerStationSystemLabel.setText("");
        headerStationDistanceLabel.setText("--");

        miningService.getArdentApiService().setHotspots(null);
        miningService.getArdentApiService().setCurrentHotspotIndex(0);
        setLoadingVisible(false);

    }

    private void setErrorSearch() {
        Platform.runLater(() -> {
            headerPriceLabel.setText(getTranslation("mining.price_error"));
            headerDemandLabel.setText("--");
            headerRingNameLabel.setText(getTranslation("mining.search_error"));
            headerRingSystemLabel.setText("");
            headerDistanceLabel.setText("--");
            headerStationNameLabel.setText(getTranslation("mining.search_error"));
            headerStationSystemLabel.setText("");
            headerStationDistanceLabel.setText("--");
            setLoadingVisible(false);
        });
    }

    /**
     * Recherche une route de minage pour un minéral spécifique (index 0 par défaut)
     */
    private void searchMiningRouteForMineral(Mineral mineral) {
        searchMiningRouteForMineral(mineral, 0);
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
     * Initialise les boutons de navigation des stations et des anneaux
     */
    private void initializeStationNavigation() {
        if (previousStationButton != null && nextStationButton != null) {
            // État initial
            updateStationNavigationButtons();
        }

        if (previousRingButton != null && nextRingButton != null) {
            // État initial
            updateRingNavigationButtons();
        }
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

            searchResultsContainer.setVisible(!visible);
        });
    }

    /**
     * Met à jour l'image du type de station
     */
    private void updateStationTypeImage(StationType stationType) {
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


    public void setOnSearchCompleted(Runnable onSearchCompleted) {
        this.onSearchCompleted = onSearchCompleted;
    }

    // Méthodes de navigation des stations
    @FXML
    private void navigateToPreviousStation() {
        int currentIndex = miningService.getArdentApiService().getCurrentResultIndex();
        if (currentIndex > 0) {
            Mineral selectedMineral = mineralComboBox.getValue() != null ? mineralComboBox.getValue().getMineral() : null;
            if (selectedMineral != null) {
                searchMiningRouteForMineral(selectedMineral, currentIndex - 1);
            }
        }
    }

    @FXML
    private void navigateToNextStation() {
        int currentIndex = miningService.getArdentApiService().getCurrentResultIndex();
        int totalResults = miningService.getArdentApiService().getTotalResults();
        if (currentIndex < totalResults - 1) {
            Mineral selectedMineral = mineralComboBox.getValue() != null ? mineralComboBox.getValue().getMineral() : null;
            if (selectedMineral != null) {
                searchMiningRouteForMineral(selectedMineral, currentIndex + 1);
            }
        }
    }

    @FXML
    private void navigateToPreviousRing() {
        int currentIndex = miningService.getArdentApiService().getCurrentHotspotIndex();
        if (currentIndex > 0) {
            miningService.getArdentApiService().setCurrentHotspotIndex(currentIndex - 1);
            updateCurrentHotspotDisplay();
            updateRingNavigationButtons();
        }
    }

    @FXML
    private void navigateToNextRing() {
        int currentIndex = miningService.getArdentApiService().getCurrentHotspotIndex();
        int totalHotspots = miningService.getArdentApiService().getTotalHotspots();
        if (currentIndex < totalHotspots - 1) {
            miningService.getArdentApiService().setCurrentHotspotIndex(currentIndex + 1);
            updateCurrentHotspotDisplay();
            updateRingNavigationButtons();
        }
    }

    /**
     * Méthode centralisée pour mettre à jour l'affichage de la station et ses hotspots
     * Appelée lors de la sélection d'un minéral ou de la navigation entre stations
     */
    private void updateHotspots(CommoditiesStats currentResult) {
        if (currentResult != null) {

            // Récupérer les hotspots pour cette station
            Mineral selectedMineral = mineralComboBox.getValue() != null ? mineralComboBox.getValue().getMineral() : null;
            if (selectedMineral != null) {
                updateHotspotsForStation(currentResult, selectedMineral);
            }
            // Mettre à jour les boutons de navigation
            updateStationNavigationButtons();
        }
    }

    /**
     * Met à jour l'affichage des informations de la station
     */
    private void updateStationDisplay(CommoditiesStats station) {
        headerStationNameLabel.setText(station.getStationName());
        headerStationSystemLabel.setText(station.getSystemName());
        headerStationDistanceLabel.setText(String.format("%.1f LY", station.getSystemDistance()));
        headerPriceLabel.setText(String.format("%s Cr", miningService.formatPrice(station.getPrice())));
        headerDemandLabel.setText(String.format("%d T", station.getDemand()));
        updateStationTypeImage(station.getStationType());
    }


    /**
     * Met à jour l'affichage des hotspots pour une station donnée
     */
    private void updateHotspotsForStation(CommoditiesStats station, Mineral mineral) {
        miningService.findMiningHotspotsForStation(station, mineral)
                .thenAccept(bestHotspot -> Platform.runLater(() -> {
                    if (bestHotspot != null) {
                        headerRingNameLabel.setText(bestHotspot.getRingName());
                        headerRingSystemLabel.setText(bestHotspot.getSystemName());
                        headerDistanceLabel.setText(String.format("%.1f %s",
                                bestHotspot.getDistanceFromReference(), getTranslation("search.distance.unit")));

                    } else {
                        headerRingNameLabel.setText(getTranslation("mining.no_hotspot_found"));
                        headerRingSystemLabel.setText("");
                        headerDistanceLabel.setText("--");
                    }

                    updateRingNavigationButtons();
                    setLoadingVisible(false);
                }))
                .exceptionally(e -> {
                    setLoadingVisible(false);
                    updateRingNavigationButtons();
                    return null;
                });
    }

    /**
     * Met à jour l'affichage du hotspot actuel (utilisé lors de la navigation)
     */
    private void updateCurrentHotspotDisplay() {
        MiningHotspot currentHotspot = miningService.getArdentApiService().getCurrentHotspot();
        if (currentHotspot != null) {
            headerRingNameLabel.setText(currentHotspot.getRingName());
            headerRingSystemLabel.setText(currentHotspot.getSystemName());
            headerDistanceLabel.setText(String.format("%.1f %s",
                    currentHotspot.getDistanceFromReference(), getTranslation("search.distance.unit")));
        } else {
            headerRingNameLabel.setText(getTranslation("mining.no_hotspot_found"));
            headerRingSystemLabel.setText("");
            headerDistanceLabel.setText("--");
        }
    }

    /**
     * Met à jour le cache de la station
     */
    private void updateCurrentStationMarket(CommoditiesStats currentResult) {
        if (currentResult.getMarketId() != null) {
            // Récupérer le marché de la station depuis Inara de manière asynchrone
            miningService.fetchStationMarket(currentResult.getMarketId())
                    .thenAccept(stationMarket -> Platform.runLater(() -> {
                        // Définir cette station comme station actuelle
                        miningService.getArdentApiService().setCurrentStationMarket(
                                currentResult.getStationName(),
                                currentResult.getSystemName(),
                                stationMarket
                        );

                        priceNotificationService.notifyPriceChanged();

                    }))
                    .exceptionally(e -> {
                        System.err.println("❌ Erreur lors de la récupération du marché de station: " + e.getMessage());
                        e.printStackTrace();
                        return null;
                    });
        }
    }

    private void updateStationNavigationButtons() {
        if (previousStationButton != null && nextStationButton != null) {
            int currentIndex = miningService.getArdentApiService().getCurrentResultIndex();
            int totalResults = miningService.getArdentApiService().getTotalResults();

            previousStationButton.setDisable(currentIndex <= 0);
            nextStationButton.setDisable(currentIndex >= totalResults - 1);
        }
    }

    private void updateRingNavigationButtons() {
        if (previousRingButton != null && nextRingButton != null) {
            int currentIndex = miningService.getArdentApiService().getCurrentHotspotIndex();
            int totalHotspots = miningService.getArdentApiService().getTotalHotspots();

            previousRingButton.setDisable(currentIndex <= 0);
            nextRingButton.setDisable(currentIndex >= totalHotspots - 1);
        }
    }

    public void setOnPriceUpdated(Runnable onPriceUpdated) {
        this.onPriceUpdated = onPriceUpdated;
    }
}
