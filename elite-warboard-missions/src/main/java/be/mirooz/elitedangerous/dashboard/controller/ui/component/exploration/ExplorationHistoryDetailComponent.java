package be.mirooz.elitedangerous.dashboard.controller.ui.component.exploration;

import be.mirooz.elitedangerous.dashboard.controller.IBatchListener;
import be.mirooz.elitedangerous.dashboard.controller.IRefreshable;
import be.mirooz.elitedangerous.dashboard.model.commander.CommanderStatus;
import be.mirooz.elitedangerous.biologic.BioSpecies;
import be.mirooz.elitedangerous.dashboard.model.exploration.ACelesteBody;
import be.mirooz.elitedangerous.dashboard.model.exploration.ExplorationData;
import be.mirooz.elitedangerous.dashboard.model.exploration.ExplorationDataSale;
import be.mirooz.elitedangerous.dashboard.model.exploration.PlaneteDetail;
import be.mirooz.elitedangerous.dashboard.model.exploration.SystemVisited;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.ExplorationDataSaleRegistry;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.OrganicDataSaleRegistry;
import be.mirooz.elitedangerous.dashboard.service.DashboardService;
import be.mirooz.elitedangerous.dashboard.util.DateUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Composant fusionné pour afficher l'historique des groupes d'exploration avec navigation
 * et la liste des systèmes visités du groupe sélectionné
 */
public class ExplorationHistoryDetailComponent implements Initializable, IRefreshable, IBatchListener {

    @FXML
    private Button previousButton;
    @FXML
    private Button nextButton;
    @FXML
    private Button seeCurrentSystemButton;
    @FXML
    private VBox selectedSaleContainer;
    @FXML
    private Label groupNumberLabel;
    @FXML
    private Label currentLabel;
    @FXML
    private Label totalEarningsLabel;
    @FXML
    private Label systemsCountLabel;
    @FXML
    private Label timeRangeLabel;
    @FXML
    private ScrollPane systemsScrollPane;
    @FXML
    private VBox systemsList;
    @FXML
    private VBox onHoldInfoContainer;
    @FXML
    private HBox explorationOnHoldContainer;
    @FXML
    private Label explorationOnHoldLabel;
    @FXML
    private HBox organicOnHoldContainer;
    @FXML
    private Label organicOnHoldLabel;

    private final ExplorationDataSaleRegistry registry = ExplorationDataSaleRegistry.getInstance();
    private final OrganicDataSaleRegistry organicRegistry = OrganicDataSaleRegistry.getInstance();
    private List<ExplorationData> allSales = new ArrayList<>();
    private int currentIndex = -1;
    private ExplorationData selectedSale;
    private Consumer<SystemVisited> onSystemSelected;
    private Image exobioImage;
    private Image mappedImage;
    private SystemVisited selectedSystem; // Système actuellement sélectionné et affiché dans la vue centrale
    private Map<VBox, SystemVisited> systemCardMap = new HashMap<>(); // Map pour associer les cartes aux systèmes

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadImages();
        refresh();
        DashboardService.getInstance().addBatchListener(this);
    }

    private void loadImages() {
        try {
            exobioImage = new Image(getClass().getResourceAsStream("/images/exploration/exobio.png"));
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de l'image exobio.png: " + e.getMessage());
        }
        try {
            mappedImage = new Image(getClass().getResourceAsStream("/images/exploration/mapped.png"));
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de l'image mapped.png: " + e.getMessage());
        }
    }

    @Override
    public void refreshUI() {
        //refresh();
    }
    @Override
    public void onBatchStart() {
        //allSales.clear();
    }
    @Override
    public void onBatchEnd() {
        refresh(() -> {
            // Sélectionner automatiquement le système actuel dans la vue exploration après le refresh
            // Utiliser un Platform.runLater supplémentaire pour s'assurer que l'UI est complètement mise à jour
            Platform.runLater(this::seeCurrentSystem);
        });
    }

    public void refresh() {
        refresh(() -> {
            // Sélectionner automatiquement le système actuel dans la vue exploration après le refresh
            // Utiliser un Platform.runLater supplémentaire pour s'assurer que l'UI est complètement mise à jour
            Platform.runLater(this::seeCurrentSystem);
        });
    }

    private void refresh(Runnable afterUpdate) {
        Platform.runLater(() -> {
            // Obtenir toutes les ventes triées
            allSales.clear();
            if (registry.getExplorationDataOnHold() != null) {
                allSales.add(registry.getExplorationDataOnHold());
            }
            var sortedSales = registry.getAllSales().stream()
                    .filter(sale -> sale != registry.getCurrentSale())
                    .sorted(Comparator.comparing((ExplorationDataSale sale) -> {
                        String ts = sale.getEndTimeStamp();
                        return ts != null ? ts : "";
                    }).reversed())
                    .collect(Collectors.toList());
            allSales.addAll(sortedSales);
            // S'assurer que l'index est valide
            if (currentIndex >= allSales.size()) {
                currentIndex = allSales.size() - 1;
            }
            if (currentIndex < 0 && !allSales.isEmpty()) {
                currentIndex = 0;
            }
            
            // Mettre à jour les informations "on hold" avant updateUI
            updateOnHoldInfo();
            updateUI();
            
            // Exécuter le callback après la mise à jour de l'UI
            if (afterUpdate != null) {
                afterUpdate.run();
            }
        });
    }

    private void updateUI() {
        // Mettre à jour les boutons de navigation
        previousButton.setDisable(allSales.isEmpty() || currentIndex <= 0);
        nextButton.setDisable(allSales.isEmpty() || currentIndex >= allSales.size() - 1);
        
        if (allSales.isEmpty()) {
            groupNumberLabel.setText("Aucun groupe d'exploration");
            currentLabel.setVisible(false);
            totalEarningsLabel.setText("");
            systemsCountLabel.setText("");
            timeRangeLabel.setText("");
            systemsList.getChildren().clear();
            selectedSale = null;
            return;
        }
        
        // Sélectionner la vente à l'index courant
        selectedSale = allSales.get(currentIndex);
        
        // Mettre à jour le numéro du groupe
        boolean isCurrent = selectedSale == registry.getCurrentSale();
        groupNumberLabel.setText(String.format("%d / %d", currentIndex + 1, allSales.size()));
        currentLabel.setVisible(isCurrent);
        
        // Mettre à jour les informations financières et systèmes
        // Calculer le total avec les exobio collectés
        long totalWithExobio = calculateTotalWithExobio(selectedSale);
        totalEarningsLabel.setText(String.format("%,d Cr", totalWithExobio));
        systemsCountLabel.setText(String.format("%d systèmes", selectedSale.getSystemsVisited().size()));
        
        // Formater les timestamps
        String timeRange = formatTimeRange(selectedSale.getStartTimeStamp(), selectedSale.getEndTimeStamp());
        timeRangeLabel.setText(timeRange);
        
        // Mettre à jour les informations "on hold"
        updateOnHoldInfo();
        
        // Mettre à jour la liste des systèmes visités
        systemsList.getChildren().clear();
        systemCardMap.clear(); // Réinitialiser la map
        // Réinitialiser la sélection si le système sélectionné n'est plus dans la nouvelle liste
        if (selectedSystem != null && selectedSale.getSystemsVisited() != null) {
            boolean systemStillExists = selectedSale.getSystemsVisited().stream()
                    .anyMatch(s -> s.equals(selectedSystem));
            if (!systemStillExists) {
                selectedSystem = null;
            }
        }

        selectedSale.getSystemsVisited().stream()
                .sorted(Comparator.comparing(SystemVisited::getLastVisitedTime).reversed())
                .map(this::createSystemCardDirectly)
                .filter(Objects::nonNull)
                .forEach(card -> systemsList.getChildren().add(card));
        
        // Mettre à jour l'affichage de la sélection après avoir créé toutes les cartes
        refreshSystemCardsSelection();
    }
    
    private String formatTimeRange(String startTime, String endTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        
        String startFormatted = "N/A";
        if (startTime != null && !startTime.isEmpty()) {
            try {
                var dateTime = DateUtil.parseTimestamp(startTime);
                startFormatted = dateTime.format(formatter);
            } catch (Exception e) {
                startFormatted = startTime;
            }
        }
        
        String endFormatted = "N/A";
        if (endTime != null && !endTime.isEmpty()) {
            try {
                var dateTime = DateUtil.parseTimestamp(endTime);
                endFormatted = dateTime.format(formatter);
            } catch (Exception e) {
                endFormatted = endTime;
            }
        }
        
        return startFormatted + " → " + endFormatted;
    }

    @FXML
    private void navigatePrevious() {
        if (currentIndex > 0) {
            currentIndex--;
            updateUI();
        }
    }

    @FXML
    private void navigateNext() {
        if (currentIndex < allSales.size() - 1) {
            currentIndex++;
            updateUI();
        }
    }

    @FXML
    private void seeCurrentSystem() {
        // Vérifier qu'il y a au moins un élément (l'exploration en cours en position 0)
        if (allSales.isEmpty()) {
            return;
        }
        
        // Trouver le système courant
        String currentStarSystem = CommanderStatus.getInstance().getCurrentStarSystem();
        if (currentStarSystem == null || currentStarSystem.isEmpty()) {
            // Si pas de système actuel, afficher juste l'exploration en cours
            currentIndex = 0;
            updateUI();
            return;
        }
        
        // Chercher le système dans tous les groupes d'exploration
        SystemVisited currentSystem = null;
        int foundIndex = -1;
        
        for (int i = 0; i < allSales.size(); i++) {
            ExplorationData sale = allSales.get(i);
            if (sale != null && sale.getSystemsVisited() != null) {
                currentSystem = sale.getSystemsVisited().stream()
                        .filter(system -> currentStarSystem.equals(system.getSystemName()))
                        .findFirst()
                        .orElse(null);
                
                if (currentSystem != null) {
                    foundIndex = i;
                    break;
                }
            }
        }
        
        // Si le système est trouvé, naviguer vers le groupe correspondant et le sélectionner
        if (foundIndex >= 0) {
            currentIndex = foundIndex;
            updateUI();
            
            // Mettre à jour le système sélectionné
            selectedSystem = currentSystem;
            refreshSystemCardsSelection();
            
            // Sélectionner le système dans la vue visuelle
            if (currentSystem != null && onSystemSelected != null) {
                onSystemSelected.accept(currentSystem);
            }
        } else {
            // Si le système n'est pas trouvé, afficher au moins l'exploration en cours
            currentIndex = 0;
            updateUI();
        }
    }

    private VBox createSystemCardDirectly(SystemVisited system) {
        // Vérifier si c'est le système actuel
        String currentStarSystem = CommanderStatus.getInstance().getCurrentStarSystem();
        boolean isCurrentSystem = currentStarSystem != null && 
                                  !currentStarSystem.isEmpty() && 
                                  currentStarSystem.equals(system.getSystemName());
        
        // Vérifier si c'est le système sélectionné (affiché dans la vue centrale)
        boolean isSelectedSystem = selectedSystem != null && 
                                   selectedSystem.equals(system);
        
        // Créer une carte similaire aux cartes de missions
        VBox root = new VBox(5);
        root.getStyleClass().add("exploration-system-card-compact");
        if (isCurrentSystem) {
            root.getStyleClass().add("exploration-system-card-current");
        }
        if (isSelectedSystem) {
            root.getStyleClass().add("exploration-system-card-selected");
        }
        root.setStyle("-fx-cursor: hand");
        root.setPrefHeight(javafx.scene.layout.Region.USE_COMPUTED_SIZE);
        root.setMinHeight(javafx.scene.layout.Region.USE_COMPUTED_SIZE);
        root.setMaxHeight(javafx.scene.layout.Region.USE_COMPUTED_SIZE);
        root.setPadding(new javafx.geometry.Insets(5, 10, 5, 10));
        
        // Ligne principale : Nom du système, nombre de corps, Cr
        HBox mainRow = new HBox(15);
        mainRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        mainRow.setFillHeight(false);
        
        // 1. Nom du système
        Label systemNameLabel = new Label(system.getSystemName());
        systemNameLabel.getStyleClass().add("exploration-system-name-compact");
        // Réduire la largeur du nom si on a un badge CURRENT pour faire de la place
        if (isCurrentSystem) {
            systemNameLabel.setPrefWidth(150);
            systemNameLabel.setMinWidth(80);
            systemNameLabel.setMaxWidth(150);
        } else {
            systemNameLabel.setPrefWidth(180);
            systemNameLabel.setMinWidth(80);
            systemNameLabel.setMaxWidth(180);
        }
        systemNameLabel.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        systemNameLabel.setWrapText(false);
        
        // Badge CURRENT si c'est le système actuel
        if (isCurrentSystem) {
            Label currentBadge = new Label("CURRENT");
            currentBadge.getStyleClass().add("exploration-system-current-badge");
            // S'assurer que le texte n'est jamais coupé
            currentBadge.setMinWidth(javafx.scene.control.Label.USE_PREF_SIZE);
            currentBadge.setPrefWidth(javafx.scene.control.Label.USE_PREF_SIZE);
            currentBadge.setMaxWidth(javafx.scene.control.Label.USE_PREF_SIZE);
            currentBadge.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
            currentBadge.setWrapText(false);
            // Empêcher le HBox de réduire la taille du badge
            HBox.setHgrow(currentBadge, javafx.scene.layout.Priority.NEVER);
            mainRow.getChildren().add(systemNameLabel);
            mainRow.getChildren().add(currentBadge);
        } else {
            mainRow.getChildren().add(systemNameLabel);
        }
        
        // 2. Nombre de corps
        Label bodiesCountLabel = new Label(system.getNumBodies() + " corps");
        bodiesCountLabel.getStyleClass().add("exploration-bodies-count");
        bodiesCountLabel.setPrefWidth(80);
        bodiesCountLabel.setMinWidth(60);
        bodiesCountLabel.setMaxWidth(80);
        
        // Vérifier et ajouter les icônes exobio/mapped
        boolean[] icons = LabelIconHelper.checkSystemIcons(system.getCelesteBodies());
        boolean hasExobio = icons[0];
        
        // Calculer le nombre d'espèces collectées et détectées pour TOUTES les planètes du système
        int confirmedSpeciesCount = 0;
        int numSpeciesDetected = 0;
        
        // Calculer le nombre de planètes mapped et totales qui respectent les conditions
        int mappedPlanetsCount = 0;
        int totalMappablePlanetsCount = 0;
        
        for (ACelesteBody body : system.getCelesteBodies()) {
            if (body instanceof PlaneteDetail planet) {
                // Compter les espèces confirmées collectées (avec ANALYSE)
                if (planet.getConfirmedSpecies() != null && !planet.getConfirmedSpecies().isEmpty()) {
                    confirmedSpeciesCount += (int) planet.getConfirmedSpecies().stream()
                            .filter(species -> species.isCollected())
                            .count();
                }
                // Compter le nombre total d'espèces détectées
                if (planet.getNumSpeciesDetected() != null) {
                    numSpeciesDetected += planet.getNumSpeciesDetected();
                }
                
                // Vérifier si la planète respecte les conditions pour mapped
                if (planet.getPlanetClass() != null) {
                    int baseK = planet.getPlanetClass().getBaseK();
                    boolean isMappable = planet.isTerraformable() || baseK > 50000;
                    if (isMappable) {
                        totalMappablePlanetsCount++;
                        if (planet.isMapped()) {
                            mappedPlanetsCount++;
                        }
                    }
                }
            }
        }
        
        boolean hasMappablePlanets = totalMappablePlanetsCount > 0;
        
        // 3. Icône exobio (si nécessaire)
        javafx.scene.image.ImageView exobioIcon = null;
        if (hasExobio && exobioImage != null) {
            exobioIcon = new javafx.scene.image.ImageView(exobioImage);
            exobioIcon.setFitWidth(16);
            exobioIcon.setFitHeight(16);
            exobioIcon.setPreserveRatio(true);
        }
        
        // 4. Compteur X/Y (si exobio est présent) - TOUJOURS afficher si exobio est présent
        Label speciesCountLabel = null;
        if (hasExobio && exobioImage != null) {
            // Déterminer la couleur selon le nombre d'espèces collectées
            String color;
            if (confirmedSpeciesCount == 0) {
                // 0/Y → rouge
                color = "#FF4444";
            } else if (numSpeciesDetected > 0 && confirmedSpeciesCount == numSpeciesDetected) {
                // Y/Y → vert
                color = "#00FF88";
            } else {
                // Entre 1 et Y-1 → orange
                color = "#FF8800";
            }
            
            // Toujours créer le label, même si les valeurs sont 0/0
            String countText = String.format("%d/%d", confirmedSpeciesCount, numSpeciesDetected);
            speciesCountLabel = new Label(countText);
            
            // Appliquer la couleur conditionnelle avec un style très visible
            speciesCountLabel.setStyle(String.format(
                "-fx-text-fill: %s; " +
                "-fx-font-size: 12px; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 2px 4px; " +
                "-fx-background-color: rgba(0, 0, 0, 0.6); " +
                "-fx-background-radius: 3px; " +
                "-fx-border-color: %s; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 3px;",
                color, color));
            
            // Forcer la visibilité et la gestion
            speciesCountLabel.setVisible(true);
            speciesCountLabel.setManaged(true);
            // S'assurer que le label a une taille minimale visible
            speciesCountLabel.setMinWidth(35);
            speciesCountLabel.setPrefWidth(javafx.scene.control.Label.USE_PREF_SIZE);
            speciesCountLabel.setMaxWidth(javafx.scene.control.Label.USE_PREF_SIZE);
        }
        
        // 5. Icône mapped avec compteur X/Y (si nécessaire)
        javafx.scene.image.ImageView mappedIcon = null;
        Label mappedCountLabel = null;
        if (hasMappablePlanets && mappedImage != null) {
            mappedIcon = new javafx.scene.image.ImageView(mappedImage);
            mappedIcon.setFitWidth(16);
            mappedIcon.setFitHeight(16);
            mappedIcon.setPreserveRatio(true);
            
            // Déterminer la couleur selon le nombre de planètes mapped
            String mappedColor;
            if (mappedPlanetsCount == 0) {
                // 0/Y → rouge
                mappedColor = "#FF4444";
            } else if (totalMappablePlanetsCount > 0 && mappedPlanetsCount == totalMappablePlanetsCount) {
                // Y/Y → vert
                mappedColor = "#00FF88";
            } else {
                // Entre 1 et Y-1 → orange
                mappedColor = "#FF8800";
            }
            
            // Créer le label avec le format X/Y
            String mappedCountText = String.format("%d/%d", mappedPlanetsCount, totalMappablePlanetsCount);
            mappedCountLabel = new Label(mappedCountText);
            
            // Appliquer la couleur conditionnelle avec un style très visible
            mappedCountLabel.setStyle(String.format(
                "-fx-text-fill: %s; " +
                "-fx-font-size: 12px; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 2px 4px; " +
                "-fx-background-color: rgba(0, 0, 0, 0.6); " +
                "-fx-background-radius: 3px; " +
                "-fx-border-color: %s; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 3px;",
                mappedColor, mappedColor));
            
            // Forcer la visibilité et la gestion
            mappedCountLabel.setVisible(true);
            mappedCountLabel.setManaged(true);
            // S'assurer que le label a une taille minimale visible
            mappedCountLabel.setMinWidth(35);
            mappedCountLabel.setPrefWidth(javafx.scene.control.Label.USE_PREF_SIZE);
            mappedCountLabel.setMaxWidth(javafx.scene.control.Label.USE_PREF_SIZE);
        }
        
        // 6. Valeur en Cr (corps célestes + exobio collectés)
        long totalValue = system.getCelesteBodies().stream()
                .mapToLong(ACelesteBody::computeBodyValue)
                .sum();
        
        // Calculer le prix total des exobio collectés
        long exobioValue = 0;
        for (ACelesteBody body : system.getCelesteBodies()) {
            if (body instanceof PlaneteDetail planet) {
                if (planet.getConfirmedSpecies() != null && !planet.getConfirmedSpecies().isEmpty()) {
                    for (BioSpecies species : planet.getConfirmedSpecies()) {
                        if (species.isCollected()) {
                            // Utiliser bonusValue si wasFootfalled est false, sinon baseValue
                            if (!planet.isWasFootfalled()) {
                                exobioValue += species.getBonusValue();
                            } else {
                                exobioValue += species.getBaseValue();
                            }
                        }
                    }
                }
            }
        }
        
        // Afficher le total (corps célestes + exobio)
        long grandTotal = totalValue + exobioValue;
        Label valueLabel = new Label(String.format("%,d Cr", grandTotal));
        valueLabel.getStyleClass().add("exploration-system-value");
        valueLabel.setPrefWidth(120);
        valueLabel.setMinWidth(80);
        valueLabel.setMaxWidth(120);
        
        // Ligne principale : Nombre de corps, Cr (le nom du système est déjà ajouté avec le badge si nécessaire)
        mainRow.getChildren().add(bodiesCountLabel);
        mainRow.getChildren().add(valueLabel);
        root.getChildren().add(mainRow);
        
        // Ligne secondaire : Icônes avec compteurs (si présentes)
        if (exobioIcon != null || mappedIcon != null) {
            HBox iconsRow = new HBox(10);
            iconsRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            iconsRow.setPadding(new javafx.geometry.Insets(2, 0, 0, 0));
            
            if (exobioIcon != null) {
                iconsRow.getChildren().add(exobioIcon);
            }
            if (speciesCountLabel != null) {
                iconsRow.getChildren().add(speciesCountLabel);
            }
            if (mappedIcon != null) {
                iconsRow.getChildren().add(mappedIcon);
            }
            if (mappedCountLabel != null) {
                iconsRow.getChildren().add(mappedCountLabel);
            }
            
            root.getChildren().add(iconsRow);
        }
        
        // Associer la carte au système dans la map
        systemCardMap.put(root, system);
        
        // Gérer le clic sur la carte
        root.setOnMouseClicked(e -> {
            // Mettre à jour le système sélectionné
            selectedSystem = system;
            // Mettre à jour toutes les cartes pour refléter la nouvelle sélection
            refreshSystemCardsSelection();
            // Notifier le clic sur le système
            if (onSystemSelected != null) {
                onSystemSelected.accept(system);
            }
        });
        
        return root;
    }

    /**
     * Calcule le total des gains incluant les exobio collectés pour tous les systèmes
     */
    private long calculateTotalWithExobio(ExplorationData sale) {
        long total = sale.getTotalEarnings();
        
        // Ajouter la valeur des exobio collectés pour tous les systèmes
        for (SystemVisited system : sale.getSystemsVisited()) {
            for (ACelesteBody body : system.getCelesteBodies()) {
                if (body instanceof PlaneteDetail planet) {
                    if (planet.getConfirmedSpecies() != null && !planet.getConfirmedSpecies().isEmpty()) {
                        for (BioSpecies species : planet.getConfirmedSpecies()) {
                            if (species.isCollected()) {
                                // Utiliser bonusValue si wasFootfalled est false, sinon baseValue
                                if (!planet.isWasFootfalled()) {
                                    total += species.getBonusValue();
                                } else {
                                    total += species.getBaseValue();
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return total;
    }
    
    /**
     * Met à jour l'état de sélection de toutes les cartes système
     */
    private void refreshSystemCardsSelection() {
        for (Map.Entry<VBox, SystemVisited> entry : systemCardMap.entrySet()) {
            VBox card = entry.getKey();
            SystemVisited system = entry.getValue();
            
            // Retirer la classe selected de toutes les cartes
            card.getStyleClass().remove("exploration-system-card-selected");
            
            // Si c'est le système sélectionné, ajouter la classe
            if (system != null && system.equals(selectedSystem)) {
                card.getStyleClass().add("exploration-system-card-selected");
            }
        }
    }
    
    public void setOnSystemSelected(Consumer<SystemVisited> callback) {
        this.onSystemSelected = callback;
    }
    
    /**
     * Met à jour l'affichage des données "on hold" (exploration et organiques)
     */
    private void updateOnHoldInfo() {
        // Vérifier les données d'exploration on hold
        var explorationOnHold = registry.getExplorationDataOnHold();
        boolean hasExplorationOnHold = explorationOnHold != null && 
                                      explorationOnHold.getTotalEarnings() > 0;
        
        if (hasExplorationOnHold && explorationOnHold != null) {
            long totalEarnings = explorationOnHold.getTotalEarnings();
            int systemsCount = explorationOnHold.getSystemsVisitedMap() != null ? 
                              explorationOnHold.getSystemsVisitedMap().size() : 0;
            explorationOnHoldLabel.setText(String.format("%,d Cr (%d systèmes)", totalEarnings, systemsCount));
            explorationOnHoldContainer.setVisible(true);
            explorationOnHoldContainer.setManaged(true);
        } else {
            explorationOnHoldContainer.setVisible(false);
            explorationOnHoldContainer.setManaged(false);
        }
        
        // Vérifier les données organiques on hold
        var organicOnHold = organicRegistry.getCurrentOrganicDataOnHold();
        boolean hasOrganicOnHold = organicOnHold != null && 
                                   (organicOnHold.getTotalValue() > 0 || organicOnHold.getTotalBonus() > 0);
        
        if (hasOrganicOnHold && organicOnHold != null) {
            long totalValue = organicOnHold.getTotalValue();
            long totalBonus = organicOnHold.getTotalBonus();
            int bioCount = organicOnHold.getBioData() != null ? organicOnHold.getBioData().size() : 0;
            long total = totalValue + totalBonus;
            organicOnHoldLabel.setText(String.format("%,d Cr (%d espèces)", total, bioCount));
            organicOnHoldContainer.setVisible(true);
            organicOnHoldContainer.setManaged(true);
        } else {
            organicOnHoldContainer.setVisible(false);
            organicOnHoldContainer.setManaged(false);
        }
        
        // Afficher le conteneur principal seulement si au moins une info est visible
        boolean hasAnyOnHold = hasExplorationOnHold || hasOrganicOnHold;
        onHoldInfoContainer.setVisible(hasAnyOnHold);
        onHoldInfoContainer.setManaged(hasAnyOnHold);
    }
}

