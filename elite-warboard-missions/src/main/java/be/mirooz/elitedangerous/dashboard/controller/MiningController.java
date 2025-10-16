package be.mirooz.elitedangerous.dashboard.controller;

import be.mirooz.elitedangerous.commons.lib.models.commodities.ICommodity;
import be.mirooz.elitedangerous.commons.lib.models.commodities.LimpetType;
import be.mirooz.elitedangerous.dashboard.controller.ui.manager.UIManager;
import be.mirooz.elitedangerous.dashboard.model.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.model.events.ProspectedAsteroid;
import be.mirooz.elitedangerous.dashboard.model.registries.ProspectedAsteroidRegistry;
import be.mirooz.elitedangerous.dashboard.service.EdToolsService;
import be.mirooz.elitedangerous.lib.inara.client.InaraClient;
import be.mirooz.elitedangerous.lib.edtools.model.MiningHotspot;
import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.CoreMineralType;
import be.mirooz.elitedangerous.lib.inara.model.InaraCommoditiesStats;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.Comparator;
import java.util.Deque;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

import static be.mirooz.elitedangerous.commons.lib.models.commodities.LimpetType.LIMPET;

/**
 * Contrôleur pour le panneau de mining
 */
public class MiningController implements Initializable, IRefreshable {

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

    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();
    private final ProspectedAsteroidRegistry prospectedRegistry = ProspectedAsteroidRegistry.getInstance();
    private final InaraClient inaraClient = new InaraClient();
    private final EdToolsService edToolsService = EdToolsService.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateProspectors();
        updateCargo();
        initializeMineralComboBox();
        initializeHeaderLabels();
        UIManager.getInstance().register(this);

        // TODO: Ajouter des listeners pour les mises à jour automatiques
    }

    /**
     * Met à jour l'affichage des prospecteurs
     */
    public void updateProspectors() {
        Platform.runLater(() -> {
            Deque<ProspectedAsteroid> prospectors = prospectedRegistry.getAll();

            // Vider les listes
            lastProspectorContent.getChildren().clear();
            prospectorsList.getChildren().clear();

            if (prospectors.isEmpty()) {
                lastProspectorContainer.setVisible(false);
                return;
            }

            // Afficher le dernier prospecteur (plus visible)
            ProspectedAsteroid lastProspector = prospectors.peekLast();
            if (lastProspector != null) {
                lastProspectorContainer.setVisible(true);
                createProspectorCard(lastProspector, lastProspectorContent, true);
            }

            for (ProspectedAsteroid prospector : prospectors) {
                VBox cardContainer = new VBox();
                cardContainer.getStyleClass().add("prospector-card");
                createProspectorCard(prospector, cardContainer, false);
                prospectorsList.getChildren().add(cardContainer);

            }
        });
    }

    /**
     * Crée une carte de prospecteur avec design Elite Dangerous
     */
    private void createProspectorCard(ProspectedAsteroid prospector, VBox container, boolean isLast) {
        // Conteneur principal de la carte
        VBox cardContainer = new VBox(8);
        cardContainer.getStyleClass().add(isLast ? "elite-prospector-card-large" : "elite-prospector-card");

        // En-tête avec indicateur de core
        HBox headerContainer = new HBox(10);
        headerContainer.setAlignment(Pos.CENTER_LEFT);

        // Icône d'astéroïde
        Label asteroidIcon = new Label("●");
        asteroidIcon.getStyleClass().add("asteroid-icon");
        if (prospector.getCoreMineral() != null) {
            asteroidIcon.getStyleClass().add("core-asteroid");
        }

        // Nom du minéral principal
        String mineralName = prospector.getCoreMineral() != null ?
                prospector.getCoreMineral().getInaraName() :
                (prospector.getMotherlodeMaterial() != null ? prospector.getMotherlodeMaterial() : "Astéroïde");

        Label mineralLabel = new Label(mineralName);
        mineralLabel.getStyleClass().add(isLast ? "elite-mineral-name-large" : "elite-mineral-name");

        // Indicateur de core si présent
        if (prospector.getCoreMineral() != null) {
            Label coreIndicator = new Label("CORE");
            coreIndicator.getStyleClass().add("core-indicator");
            headerContainer.getChildren().addAll(asteroidIcon, mineralLabel, coreIndicator);
        } else {
            headerContainer.getChildren().addAll(asteroidIcon, mineralLabel);
        }

        // Contenu localisé
        String content = prospector.getContentLocalised() != null ?
                prospector.getContentLocalised() :
                (prospector.getContent() != null ? prospector.getContent() : "Contenu inconnu");

        Label contentLabel = new Label(content);
        contentLabel.getStyleClass().add(isLast ? "elite-content-large" : "elite-content");


        // Matériaux avec design amélioré
        VBox materialsContainer = new VBox(4);
        materialsContainer.getStyleClass().add("elite-materials-container");

        if (prospector.getMaterials() != null && !prospector.getMaterials().isEmpty()) {
            Label materialsTitle = new Label("MATÉRIAUX:");
            materialsTitle.getStyleClass().add(isLast ? "elite-materials-title-large" : "elite-materials-title");
            materialsContainer.getChildren().add(materialsTitle);

            for (ProspectedAsteroid.Material material : prospector.getMaterials()) {
                if (material.getProportion() != null) {
                    HBox materialRow = new HBox(10);
                    materialRow.setAlignment(Pos.CENTER_LEFT);

                    String materialName = material.getNameLocalised() != null ?
                            material.getNameLocalised() :
                            (material.getName() != null ? material.getName().toString() : "Inconnu");

                    Label materialLabel = new Label(materialName);
                    materialLabel.getStyleClass().add(isLast ? "elite-material-name-large" : "elite-material-name");

                    Label percentageLabel = new Label(String.format("%.1f%%", material.getProportion()));
                    percentageLabel.getStyleClass().add(isLast ? "elite-material-percent-large" : "elite-material-percent");

                    materialRow.getChildren().addAll(materialLabel, percentageLabel);
                    materialsContainer.getChildren().add(materialRow);
                }
            }
        }

        // Assemblage final
        cardContainer.getChildren().addAll(headerContainer, contentLabel);
        if (!materialsContainer.getChildren().isEmpty()) {
            cardContainer.getChildren().add(materialsContainer);
        }

        container.getChildren().add(cardContainer);
    }

    /**
     * Met à jour l'affichage du cargo
     */
    public void updateCargo() {
        Platform.runLater(() -> {
            if (commanderStatus.getShip() == null || commanderStatus.getShip().getShipCargo() == null) {
                cargoUsedLabel.setText("0/0");
                limpetsCountLabel.setText("0");
                cargoMineralsList.getChildren().clear();
                return;
            }

            var cargo = commanderStatus.getShip().getShipCargo();

            // Mettre à jour les statistiques du cargo au format "x/x"
            cargoUsedLabel.setText(String.format("%d/%d", cargo.getCurrentUsed(), cargo.getMaxCapacity()));

            // Compter les limpets
            int limpetsCount = cargo.getCommodities().getOrDefault(LIMPET, 0);
            limpetsCountLabel.setText(String.valueOf(limpetsCount));

            // Afficher les minéraux
            cargoMineralsList.getChildren().clear();

            for (Map.Entry<ICommodity, Integer> entry : cargo.getCommodities().entrySet()) {
                ICommodity commodity = entry.getKey();
                Integer quantity = entry.getValue();

                // Ne pas afficher les limpets dans la liste des minéraux
                if (commodity instanceof LimpetType) {
                    continue;
                }

                // Créer une carte pour chaque minéral
                HBox mineralCard = new HBox(10);
                mineralCard.getStyleClass().add("mineral-card");

                // Utiliser le nom du core mineral en majuscules si c'est un minéral
                String displayName;
                if (commodity instanceof be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.CoreMineralType) {
                    be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.CoreMineralType coreMineral =
                            (be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.CoreMineralType) commodity;
                    displayName = coreMineral.getInaraName().toUpperCase();
                } else {
                    displayName = commodity.toString().toUpperCase();
                }

                Label mineralName = new Label(displayName);
                mineralName.getStyleClass().add("mineral-name");

                Label mineralQuantity = new Label(String.valueOf(quantity));
                mineralQuantity.getStyleClass().add("mineral-quantity");

                mineralCard.getChildren().addAll(mineralName, mineralQuantity);
                cargoMineralsList.getChildren().add(mineralCard);
            }

            // Si pas de minéraux, afficher un message
            if (cargoMineralsList.getChildren().isEmpty()) {
                Label noMineralsLabel = new Label("Aucun minéral dans le cargo");
                noMineralsLabel.getStyleClass().add("no-minerals-label");
                cargoMineralsList.getChildren().add(noMineralsLabel);
            }
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
            headerRingNameLabel.setText("Non défini");
            headerRingSystemLabel.setText("");
            headerDistanceLabel.setText("--");
            headerStationNameLabel.setText("Non définie");
            headerStationSystemLabel.setText("");
        });
    }

    /**
     * Récupère le système actuel du commandant
     */
    private String getCurrentSystem() {
        if (commanderStatus.getCurrentStarSystem() != null) {
            return commanderStatus.getCurrentStarSystem();
        }
        return "Sol"; // Fallback
    }

    /**
     * Récupère la capacité de cargo actuelle
     */
    private int getCurrentCargoCapacity() {
        if (commanderStatus.getShip() != null && commanderStatus.getShip().getShipCargo() != null) {
            return commanderStatus.getShip().getShipCargo().getMaxCapacity();
        }
        return 100; // Fallback
    }

    /**
     * Initialise le ComboBox avec tous les core minerals et leurs prix
     */
    private void initializeMineralComboBox() {
        mineralComboBox.setItems(FXCollections.observableArrayList(CoreMineralType.all()));

        // CellFactory pour afficher le nom du minéral avec le prix
        mineralComboBox.setCellFactory(listView -> new ListCell<CoreMineralType>() {
            @Override
            protected void updateItem(CoreMineralType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    // Afficher le nom du minéral avec "Recherche..." en attendant le prix
                    setText(item.getInaraName().toUpperCase() + " - Recherche...");
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
                    setText(item.getInaraName().toUpperCase());
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

        // Listener pour recharger les prix quand le ComboBox s'ouvre
        mineralComboBox.setOnShowing(event -> {
            refreshAllMineralPrices();
        });
    }

    /**
     * Charge le prix d'un minéral spécifique et met à jour la cellule
     */
    private void loadMineralPrice(CoreMineralType mineral, ListCell<CoreMineralType> cell) {
        CompletableFuture.supplyAsync(() -> {
                    try {
                        String sourceSystem = getCurrentSystem();
                        int maxDistance = 100;
                        int minDemand = getCurrentCargoCapacity();

                        return inaraClient.fetchMinerMarket(mineral, sourceSystem, maxDistance, minDemand, false);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .thenAccept(commodities -> {
                    Platform.runLater(() -> {
                        if (commodities != null && !commodities.isEmpty()) {
                            // Prendre le meilleur prix (le plus élevé) dans un rayon de 100 AL
                            InaraCommoditiesStats bestPrice = commodities.stream()
                                    .filter(commodity -> commodity.getSystemDistance() <= 100)
                                    .max(Comparator.comparingDouble(InaraCommoditiesStats::getPrice))
                                    .orElse(commodities.get(0));

                            cell.setText(String.format("%s - %s Cr",
                                    mineral.getInaraName().toUpperCase(),
                                    formatPrice(bestPrice.getPrice())));
                        } else {
                            cell.setText(mineral.getInaraName().toUpperCase() + " - Prix non disponible");
                        }
                    });
                })
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        cell.setText(mineral.getInaraName().toUpperCase() + " - Erreur de prix");
                    });
                    return null;
                });
    }

    /**
     * Rafraîchit tous les prix des minéraux dans le ComboBox
     */
    private void refreshAllMineralPrices() {
        // Cette méthode sera appelée quand le ComboBox s'ouvre
        // Les prix seront chargés individuellement par loadMineralPrice
    }

    /**
     * Recherche une route de minage pour un minéral spécifique
     */
    private void searchMiningRouteForMineral(CoreMineralType mineral) {
        headerPriceLabel.setText("Recherche...");
        headerRingNameLabel.setText("Recherche...");
        headerRingSystemLabel.setText("");
        headerDistanceLabel.setText("--");
        headerStationNameLabel.setText("Recherche...");
        headerStationSystemLabel.setText("");

        String sourceSystem = getCurrentSystem();
        int maxDistance = 100;
        int minDemand = getCurrentCargoCapacity();

        // Récupérer le prix et les informations de route
        CompletableFuture.supplyAsync(() -> {
                    try {

                        return inaraClient.fetchMinerMarket(mineral, sourceSystem, maxDistance, minDemand, false);
                    } catch (Exception e) {
                        throw new RuntimeException("Erreur lors de la récupération des données", e);
                    }
                })
                .thenCompose(commodities -> {
                    if (commodities == null || commodities.isEmpty()) {
                        Platform.runLater(() -> {
                            headerPriceLabel.setText("Prix non disponible");
                            headerRingNameLabel.setText("Aucun marché trouvé");
                            headerRingSystemLabel.setText("");
                            headerDistanceLabel.setText("--");
                            headerStationNameLabel.setText("Aucune station trouvée");
                            headerStationSystemLabel.setText("");
                        });
                        return null;
                    }

                    // Prendre le meilleur marché (le plus proche avec le meilleur prix)
                    InaraCommoditiesStats bestMarket = commodities.stream()
                            .filter(commodity -> commodity.getSystemDistance() <= maxDistance)
                            .max(Comparator.comparingDouble(InaraCommoditiesStats::getPrice))
                            .orElse(commodities.get(0));

                    // Mettre à jour le prix et la station
                    Platform.runLater(() -> {
                        headerPriceLabel.setText(String.format("%s Cr", formatPrice(bestMarket.getPrice())));
                        headerStationNameLabel.setText(bestMarket.getStationName());
                        headerStationSystemLabel.setText(bestMarket.getSystemName());
                    });

                    // Rechercher les hotspots de minage
                    return edToolsService.findMiningHotspots(bestMarket.getSystemName(), mineral);
                })
                .thenAccept(hotspots -> {
                    if (hotspots != null && !hotspots.isEmpty()) {
                        // Prendre le hotspot le plus proche
                        MiningHotspot bestHotspot = hotspots.stream()
                                .min((h1, h2) -> Double.compare(h1.getDistanceFromReference(), h2.getDistanceFromReference()))
                                .orElse(hotspots.get(0));

                        Platform.runLater(() -> {
                            headerRingNameLabel.setText(bestHotspot.getRingName());
                            headerRingSystemLabel.setText(bestHotspot.getSystemName());

                            // Calculer la distance entre le ring et la station
                            double totalDistance = bestHotspot.getDistanceFromReference();
                            headerDistanceLabel.setText(String.format("%.1f AL", totalDistance));
                        });
                    } else {
                        Platform.runLater(() -> {
                            headerRingNameLabel.setText("Aucun hotspot trouvé");
                            headerRingSystemLabel.setText("");
                            headerDistanceLabel.setText("--");
                        });
                    }
                })
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        headerPriceLabel.setText("Erreur de prix");
                        headerRingNameLabel.setText("Erreur de recherche");
                        headerRingSystemLabel.setText("");
                        headerDistanceLabel.setText("--");
                        headerStationNameLabel.setText("Erreur de recherche");
                        headerStationSystemLabel.setText("");
                    });
                    return null;
                });
    }

    /**
     * Formate un prix avec des séparateurs de milliers
     */
    private String formatPrice(int price) {
        return String.format("%,d", price).replace(",", ".");
    }

    @Override
    public void refreshUI() {
        updateProspectors();
        updateCargo();
    }
}

