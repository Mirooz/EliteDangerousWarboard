package be.mirooz.elitedangerous.dashboard.controller;

import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.Mineral;
import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.MiningMethod;
import be.mirooz.elitedangerous.dashboard.controller.ui.component.CargoInfoComponent;
import be.mirooz.elitedangerous.dashboard.controller.ui.component.ProspectorCardComponent;
import be.mirooz.elitedangerous.dashboard.controller.ui.component.TooltipComponent;
import be.mirooz.elitedangerous.dashboard.controller.ui.manager.CopyClipboardManager;
import be.mirooz.elitedangerous.dashboard.controller.ui.manager.PopupManager;
import be.mirooz.elitedangerous.dashboard.controller.ui.manager.UIManager;
import be.mirooz.elitedangerous.dashboard.controller.ui.wrapper.MineralListWrapper;
import be.mirooz.elitedangerous.dashboard.model.commander.CommanderShip;
import be.mirooz.elitedangerous.dashboard.model.events.ProspectedAsteroid;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.service.MiningService;
import be.mirooz.elitedangerous.lib.edtools.model.MiningHotspot;
import be.mirooz.elitedangerous.lib.inara.model.InaraCommoditiesStats;
import be.mirooz.elitedangerous.lib.inara.model.StationType;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.*;

/**
 * Contrôleur pour le panneau de mining - Refactorisé
 */
public class MiningController implements Initializable, IRefreshable {

    public static final int MAX_DISTANCE = 100;

    @FXML
    private VBox currentProspectorContent;
    @FXML
    private VBox noProspectorContainer;
    @FXML
    private Button previousProspectorButton;
    @FXML
    private Button nextProspectorButton;
    @FXML
    private Label prospectorCounterLabel;

    // Variables pour la navigation
    private List<ProspectedAsteroid> allProspectors = new ArrayList<>();
    private int currentProspectorIndex = 0;
    @FXML
    private Label cargoUsedLabel;
    @FXML
    private Label limpetsCountLabel;
    @FXML
    private VBox cargoMineralsList;
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
    private ImageView stationTypeImageView;

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
    private Label currentProspectorLabel;
    @FXML
    private Label currentCargoLabel;
    @FXML
    private Label cargoLabel;
    @FXML
    private Label limpetsLabel;
    @FXML
    private Label estimatedCreditsLabel;
    @FXML
    private Label estimatedCreditsTitleLabel;
    @FXML
    private ProgressIndicator loadingIndicator;
    @FXML
    private HBox searchContentHBox;
    @FXML
    private VBox mineralTargetContainer;
    @FXML
    private CheckBox fleetCarrierCheckBox;
    @FXML
    private Label maxDistanceLabel;
    @FXML
    private TextField maxDistanceTextField;
    @FXML
    private Label distanceUnitLabel;

    private final MiningService miningService = MiningService.getInstance();
    private final LocalizationService localizationService = LocalizationService.getInstance();
    private final CopyClipboardManager copyClipboardManager = CopyClipboardManager.getInstance();
    private final PopupManager popupManager = PopupManager.getInstance();

    // Images pour les icônes
    private Image laserImage;
    private Image coreImage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Charger les images
        loadImages();

        updateProspectors();
        initializeMineralComboBox();
        initializeFleetCarrierCheckBox();
        initializeMaxDistanceField();
        updateCargo();
        initializeHeaderLabels();
        initializeClickHandlers();
        updateTranslations();
        UIManager.getInstance().register(this);

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
            coreImage=null;
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
                return imageView;
            } else {
                return "🧭";
            }
        } else {
            if (laserImage != null) {
                ImageView imageView = new ImageView(laserImage);
                imageView.setFitWidth(25);
                imageView.setFitHeight(25);
                return imageView;
            } else {
                return "🔫";
            }
        }
    }

    /**
     * Met à jour l'affichage des prospecteurs avec navigation
     */
    public void updateProspectors() {
        Platform.runLater(() -> {
            Deque<ProspectedAsteroid> prospectors = miningService.getAllProspectors();

            // Convertir en liste pour faciliter la navigation
            allProspectors.clear();
            allProspectors.addAll(prospectors);

            if (allProspectors.isEmpty()) {
                showNoProspector();
                return;
            }

            // S'assurer que l'index est valide
            if (currentProspectorIndex >= allProspectors.size()) {
                currentProspectorIndex = 0;
            }

            showCurrentProspector();
            updateNavigationButtons();
        });
    }

    /**
     * Affiche le prospecteur actuel
     */
    private void showCurrentProspector() {
        if (allProspectors.isEmpty() || currentProspectorIndex >= allProspectors.size()) {
            showNoProspector();
            return;
        }

        noProspectorContainer.setVisible(false);

        // Vider le contenu actuel
        currentProspectorContent.getChildren().clear();

        // Afficher le prospecteur actuel (toujours en grand format)
        ProspectedAsteroid currentProspector = allProspectors.get(currentProspectorIndex);
        VBox card = ProspectorCardComponent.createProspectorCard(currentProspector, true);

        // Fixer la taille pour éviter les changements de layout
        card.setMinHeight(350);
        card.setPrefHeight(350);
        card.setMaxHeight(350);
        card.setMinWidth(400);
        card.setPrefWidth(400);

        currentProspectorContent.getChildren().add(card);

        // Mettre à jour le compteur
        updateProspectorCounter();
    }

    /**
     * Affiche le message "aucun prospecteur"
     */
    private void showNoProspector() {
        noProspectorContainer.setVisible(false);
        updateNavigationButtons();
    }

    /**
     * Met à jour les boutons de navigation
     */
    private void updateNavigationButtons() {
        boolean hasProspectors = !allProspectors.isEmpty();
        boolean canGoPrevious = hasProspectors && currentProspectorIndex > 0;
        boolean canGoNext = hasProspectors && currentProspectorIndex < allProspectors.size() - 1;

        previousProspectorButton.setDisable(!canGoPrevious);
        nextProspectorButton.setDisable(!canGoNext);

        if (hasProspectors) {
            updateProspectorCounter();
        } else {
            prospectorCounterLabel.setText("0/0");
        }
    }

    /**
     * Met à jour le compteur de prospecteurs
     */
    private void updateProspectorCounter() {
        if (!allProspectors.isEmpty()) {
            prospectorCounterLabel.setText(String.format("%d/%d", currentProspectorIndex + 1, allProspectors.size()));
        }
    }

    /**
     * Affiche le prospecteur précédent
     */
    @FXML
    public void showPreviousProspector() {
        if (currentProspectorIndex > 0) {
            currentProspectorIndex--;
            showCurrentProspector();
            updateNavigationButtons();
        }
    }

    /**
     * Affiche le prospecteur suivant
     */
    @FXML
    public void showNextProspector() {
        if (currentProspectorIndex < allProspectors.size() - 1) {
            currentProspectorIndex++;
            showCurrentProspector();
            updateNavigationButtons();
        }
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
            long estimatedCredits = calculateEstimatedCredits();
            estimatedCreditsLabel.setText(miningService.formatPrice(estimatedCredits));

            // Afficher les minéraux en utilisant le composant
            cargoMineralsList.getChildren().clear();
            VBox mineralsList = CargoInfoComponent.createMineralsList(miningService.getMinerals());
            cargoMineralsList.getChildren().add(mineralsList);
        });
    }

    /**
     * Calcule les CR estimés basés sur les minéraux dans le cargo
     */
    private long calculateEstimatedCredits() {
        try {
            long totalCredits = 0;
            Map<Mineral, Integer> minerals = miningService.getMinerals();
            if (minerals != null && !minerals.isEmpty()) {
                for (Map.Entry<Mineral, Integer> entry : minerals.entrySet()) {
                    Mineral mineralName = entry.getKey();
                    Integer quantity = entry.getValue();

                    if (quantity != null && quantity > 0) {
                        totalCredits += (long) mineralName.getPrice() * quantity;
                    }
                }
            }
            return totalCredits;
        } catch (Exception e) {
            // En cas d'erreur, retourner 0
            return 0;
        }
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
        // Les gestionnaires de clic sont maintenant définis directement dans le FXML
        // via onMouseClicked="#onRingContainerClicked" et onMouseClicked="#onStationContainerClicked"
    }
    
    /**
     * Gestionnaire de clic pour le conteneur de l'anneau (nom + système)
     */
    @FXML
    private void onRingContainerClicked(MouseEvent event) {
        String systemName = headerRingSystemLabel.getText();
        if (systemName != null && !systemName.isEmpty()) {
            copyClipboardManager.copyToClipboard(systemName);
            Stage stage = (Stage) headerRingSystemLabel.getScene().getWindow();
            popupManager.showPopup(getTranslation("system.copied"), event.getSceneX(), event.getSceneY(), stage);
        }
    }
    
    /**
     * Gestionnaire de clic pour le conteneur de la station (nom + système)
     */
    @FXML
    private void onStationContainerClicked(MouseEvent event) {
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
            // Effacer l'image de la station
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
                // Pendant le chargement : masquer tout sauf la ComboBox
                searchContentHBox.getChildren().forEach(child -> {
                    if (child != mineralTargetContainer) {
                        child.setVisible(false);
                        child.setManaged(false);
                    }
                });
            } else {
                // Après le chargement : tout réafficher
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
                    // En cas d'erreur, ne pas afficher d'image
                    stationTypeImageView.setImage(null);
                }
            } else {
                stationTypeImageView.setImage(null);
            }
        });
    }

    /**
     * Initialise le ComboBox avec tous les minéraux organisés par catégorie
     */
    private void initializeMineralComboBox() {
        mineralComboBox.setItems(MineralListWrapper.createOrganizedMineralsList());
        mineralComboBox.setPromptText(getTranslation("mining.mineral_placeholder"));

        mineralComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(MineralListWrapper item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(null);
                setStyle("");
                if (empty || item == null) {
                    setDisable(false);
                    return;
                }

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

                // Élément minéral
                setDisable(false);
                // Texte provisoire (au cas où l'async arrive plus tard)
                setText(item.getMineral().getTitleName());

                loadMineralPriceSafe(item, this);
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
            if (n != null && !n.isSeparator()) searchMiningRouteForMineral(n.getMineral());
            else clearHeaderLabels();
        });
    }

    /** Empêche l'async d'écrire dans une cellule réutilisée. */
    private void loadMineralPriceSafe(MineralListWrapper owningItem, ListCell<MineralListWrapper> cell) {
        Mineral mineral = owningItem.getMineral();
        String sourceSystem = miningService.getCurrentSystem();
        int maxDistance = getMaxDistanceFromField();
        int minDemand = miningService.getCurrentCargoCapacity();

        miningService.findMineralPrice(mineral, sourceSystem, maxDistance, minDemand,true,fleetCarrierCheckBox.isSelected())
                .thenAccept(priceOpt -> Platform.runLater(() -> {
                    // Si la cellule a été recyclée, on ne touche à rien
                    if (cell.getItem() != owningItem || owningItem.isSeparator()) return;

                    if (priceOpt.isPresent()) {
                        var best = priceOpt.get();
                        cell.setGraphic(null);
                        cell.setText(mineral.getTitleName() + " - " + miningService.formatPrice(best.getPrice()) + " Cr");
                        updateCargo();
                    } else {
                        cell.setGraphic(null);
                        cell.setText(mineral.getTitleName() + " - " + getTranslation("mining.price_not_available"));
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        if (cell.getItem() != owningItem || owningItem.isSeparator()) return;
                        cell.setGraphic(null);
                        cell.setText(mineral.getTitleName() + " - " + getTranslation("mining.price_error"));
                    });
                    return null;
                });
    }

    /**
     * Recherche une route de minage pour un minéral spécifique
     */
    private void searchMiningRouteForMineral(Mineral mineral) {
        searchMiningRouteForMineral(mineral, fleetCarrierCheckBox.isSelected());
    }

    /**
     * Recherche une route de minage pour un minéral spécifique avec option Fleet Carrier
     */
    private void searchMiningRouteForMineral(Mineral mineral, boolean includeFleetCarrier) {
        // Afficher l'indicateur de chargement
        setLoadingVisible(true);
        
        // Effacer les labels
        clearHeaderLabels();
        
        String sourceSystem = miningService.getCurrentSystem();
        int maxDistance = getMaxDistanceFromField();
        int minDemand = miningService.getCurrentCargoCapacity();

        // Rechercher le prix et les informations de route
        miningService.findMineralPrice(mineral, sourceSystem, maxDistance, minDemand,true,includeFleetCarrier)
                .thenAccept(priceOpt -> {
                    if (priceOpt.isPresent()) {
                        InaraCommoditiesStats bestMarket = priceOpt.get();
                        // Rechercher les hotspots de minage
                        miningService.getEdToolsService().findMiningHotspots(bestMarket.getSystemName(), mineral)
                                .thenAccept(hotspots -> Platform.runLater(() -> {
                                    // Masquer l'indicateur de chargement
                                    setLoadingVisible(false);
                                    
                                    headerPriceLabel.setText(String.format("%s Cr ", miningService.formatPrice(bestMarket.getPrice())));
                                    headerDemandLabel.setText(String.format("%d T", bestMarket.getDemand()));
                                    headerStationNameLabel.setText(bestMarket.getStationName());
                                    headerStationSystemLabel.setText(bestMarket.getSystemName());
                                    // Mettre à jour l'image du type de station
                                    updateStationTypeImage(bestMarket.getStationType());
                                    if (hotspots != null && !hotspots.isEmpty()) {
                                        // Prendre le hotspot le plus proche
                                        MiningHotspot bestHotspot = hotspots.stream()
                                                .min(Comparator.comparingDouble(MiningHotspot::getDistanceFromReference))
                                                .orElse(hotspots.get(0));

                                        headerRingNameLabel.setText(bestHotspot.getRingName());
                                        headerRingSystemLabel.setText(bestHotspot.getSystemName());
                                        headerDistanceLabel.setText(String.format("%.1f %s", bestHotspot.getDistanceFromReference(), getTranslation("search.distance.unit")));

                                    } else {
                                        headerRingNameLabel.setText(getTranslation("mining.no_hotspot_found"));
                                        headerRingSystemLabel.setText("");
                                        headerDistanceLabel.setText("--");
                                    }
                                }));
                    } else {
                        Platform.runLater(() -> {
                            // Masquer l'indicateur de chargement
                            setLoadingVisible(false);
                            
                            headerPriceLabel.setText(getTranslation("mining.price_not_available"));
                            headerDemandLabel.setText("--");
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
                        // Masquer l'indicateur de chargement
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
        if (demandLabel != null) {
            demandLabel.setText(getTranslation("mining.demand"));
        }
        if (ringToMineLabel != null) {
            ringToMineLabel.setText(getTranslation("mining.ring_to_mine"));
        }
        if (stationToSellLabel != null) {
            stationToSellLabel.setText(getTranslation("mining.station_to_sell"));
        }
        if (currentProspectorLabel != null) {
            currentProspectorLabel.setText(getTranslation("mining.current_prospector"));
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
        if (estimatedCreditsTitleLabel != null) {
            estimatedCreditsTitleLabel.setText(getTranslation("mining.estimated_credits"));
        }

        // Mettre à jour le promptText du ComboBox
        if (mineralComboBox != null) {
            mineralComboBox.setPromptText(getTranslation("mining.mineral_placeholder"));
        }

        // Mettre à jour le tooltip du Fleet Carrier checkbox
        if (fleetCarrierCheckBox != null) {
            fleetCarrierCheckBox.setTooltip(new TooltipComponent(getTranslation("mining.fleet_carrier_hint")));
        }

        // Mettre à jour les labels de distance
        if (maxDistanceLabel != null) {
            maxDistanceLabel.setText(getTranslation("mining.max_distance"));
        }
        if (distanceUnitLabel != null) {
            distanceUnitLabel.setText(getTranslation("mining.distance_unit"));
        }
    }

    /**
     * Récupère une traduction depuis le LocalizationService
     */
    private String getTranslation(String key) {
        return localizationService.getString(key);
    }
    
    /**
     * Récupère une traduction avec paramètres depuis le LocalizationService
     */
    private String getTranslation(String key, String... params) {
        String translation = localizationService.getString(key);
        for (int i = 0; i < params.length; i++) {
            translation = translation.replace("{" + i + "}", params[i]);
        }
        return translation;
    }

    /**
     * Initialise le checkbox Fleet Carrier avec l'image
     */
    private void initializeFleetCarrierCheckBox() {
        if (fleetCarrierCheckBox != null) {
            try {
                // Charger l'image fleet.png
                Image fleetImage = new Image(getClass().getResourceAsStream("/images/fleet.png"));
                ImageView fleetImageView = new ImageView(fleetImage);
                fleetImageView.setFitWidth(16);
                fleetImageView.setFitHeight(16);
                fleetImageView.setPreserveRatio(true);
                
                // Créer un HBox pour contenir l'image et le texte
                HBox contentBox = new HBox(5);
                contentBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                contentBox.getChildren().addAll(fleetImageView, new Label("Fleet Carrier"));
                
                // Définir le contenu du checkbox
                fleetCarrierCheckBox.setGraphic(contentBox);
                fleetCarrierCheckBox.setTooltip(new Tooltip(getTranslation("mining.fleet_carrier_hint")));
            } catch (Exception e) {
                // Si l'image n'est pas trouvée, utiliser juste le texte
                fleetCarrierCheckBox.setText("Fleet Carrier");
                fleetCarrierCheckBox.setTooltip(new Tooltip(getTranslation("mining.fleet_carrier_hint")));
            }
        }
    }

    /**
     * Récupère la distance maximale depuis le champ de saisie
     */
    private int getMaxDistanceFromField() {
        if (maxDistanceTextField != null && !maxDistanceTextField.getText().isEmpty()) {
            try {
                int distance = Integer.parseInt(maxDistanceTextField.getText());
                return Math.max(1, Math.min(distance, 1000)); // Limiter entre 1 et 1000
            } catch (NumberFormatException e) {
                return MAX_DISTANCE; // Valeur par défaut
            }
        }
        return MAX_DISTANCE; // Valeur par défaut
    }

    /**
     * Initialise le champ de distance maximale
     */
    private void initializeMaxDistanceField() {
        if (maxDistanceTextField != null) {
            // Valider que la valeur est numérique
            maxDistanceTextField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue.matches("\\d*")) {
                    maxDistanceTextField.setText(newValue.replaceAll("[^\\d]", ""));
                }
            });
        }
    }

    /**
     * Gère le changement de distance maximale
     */
    @FXML
    private void onMaxDistanceChanged() {
        if (mineralComboBox.getValue() != null) {
            // Rechercher pour le minéral sélectionné avec la nouvelle distance
            MineralListWrapper selectedWrapper = mineralComboBox.getValue();
            if (selectedWrapper.getMineral() != null) {
                searchMiningRouteForMineral(selectedWrapper.getMineral(), fleetCarrierCheckBox.isSelected());
            }
        }
    }

    /**
     * Gère le toggle du checkbox Fleet Carrier
     */
    @FXML
    private void onFleetCarrierToggle() {
        if (mineralComboBox.getValue() != null) {
            // Rechercher pour le minéral sélectionné avec Fleet Carrier
            MineralListWrapper selectedWrapper = mineralComboBox.getValue();
            if (selectedWrapper.getMineral() != null) {
                searchMiningRouteForMineral(selectedWrapper.getMineral(), fleetCarrierCheckBox.isSelected());
            }
        }
    }

    @Override
    public void refreshUI() {
        updateProspectors();
        updateCargo();
    }
}