package be.mirooz.elitedangerous.dashboard.controller.ui.component.exploration;

import be.mirooz.elitedangerous.dashboard.controller.IBatchListener;
import be.mirooz.elitedangerous.dashboard.controller.IRefreshable;
import be.mirooz.elitedangerous.dashboard.model.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.model.exploration.ACelesteBody;
import be.mirooz.elitedangerous.dashboard.model.exploration.ExplorationData;
import be.mirooz.elitedangerous.dashboard.model.exploration.ExplorationDataSale;
import be.mirooz.elitedangerous.dashboard.model.exploration.PlaneteDetail;
import be.mirooz.elitedangerous.dashboard.model.exploration.SystemVisited;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.ExplorationDataSaleRegistry;
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

    private final ExplorationDataSaleRegistry registry = ExplorationDataSaleRegistry.getInstance();
    private List<ExplorationData> allSales = new ArrayList<>();
    private int currentIndex = -1;
    private ExplorationData selectedSale;
    private Consumer<SystemVisited> onSystemSelected;
    private Image exobioImage;
    private Image mappedImage;

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
    public void onBatchEnd() {
        refresh();
    }

    public void refresh() {
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
            
            updateUI();
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
        groupNumberLabel.setText(String.format("#%d / %d", currentIndex + 1, allSales.size()));
        currentLabel.setVisible(isCurrent);
        
        // Mettre à jour les informations financières et systèmes
        totalEarningsLabel.setText(String.format("%,d Cr", selectedSale.getTotalEarnings()));
        systemsCountLabel.setText(String.format("%d systèmes", selectedSale.getSystemsVisited().size()));
        
        // Formater les timestamps
        String timeRange = formatTimeRange(selectedSale.getStartTimeStamp(), selectedSale.getEndTimeStamp());
        timeRangeLabel.setText(timeRange);
        
        // Mettre à jour la liste des systèmes visités
        systemsList.getChildren().clear();
        for (SystemVisited system : selectedSale.getSystemsVisited()) {
            VBox card = createSystemCardDirectly(system);
            if (card != null) {
                systemsList.getChildren().add(card);
            }
        }
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
        
        // Placer l'historique sur l'élément 0 (exploration en cours)
        currentIndex = 0;
        updateUI();
        
        // Trouver le système courant dans la liste
        String currentStarSystem = CommanderStatus.getInstance().getCurrentStarSystem();
        if (currentStarSystem != null && !currentStarSystem.isEmpty() && selectedSale != null) {
            // Chercher le système correspondant dans la liste des systèmes visités
            SystemVisited currentSystem = selectedSale.getSystemsVisited().stream()
                    .filter(system -> currentStarSystem.equals(system.getSystemName()))
                    .findFirst()
                    .orElse(null);
            
            // Si le système est trouvé, le sélectionner
            if (currentSystem != null && onSystemSelected != null) {
                onSystemSelected.accept(currentSystem);
            }
        }
    }

    private VBox createSystemCardDirectly(SystemVisited system) {
        // Créer une carte similaire aux cartes de missions
        VBox root = new VBox();
        root.getStyleClass().add("exploration-system-card-compact");
        root.setStyle("-fx-cursor: hand");
        root.setPrefHeight(50);
        root.setMinHeight(50);
        root.setMaxHeight(50);
        root.setPadding(new javafx.geometry.Insets(3, 10, 3, 10));
        
        HBox mainRow = new HBox(15);
        mainRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        mainRow.setFillHeight(false);
        
        // 1. Nom du système
        Label systemNameLabel = new Label(system.getSystemName());
        systemNameLabel.getStyleClass().add("exploration-system-name-compact");
        systemNameLabel.setPrefWidth(180);
        systemNameLabel.setMinWidth(80);
        systemNameLabel.setMaxWidth(180);
        systemNameLabel.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        systemNameLabel.setWrapText(false);
        
        // 2. Nombre de corps
        Label bodiesCountLabel = new Label(system.getNumBodies() + " corps");
        bodiesCountLabel.getStyleClass().add("exploration-bodies-count");
        bodiesCountLabel.setPrefWidth(80);
        bodiesCountLabel.setMinWidth(60);
        bodiesCountLabel.setMaxWidth(80);
        
        // Vérifier et ajouter les icônes exobio/mapped
        boolean[] icons = LabelIconHelper.checkSystemIcons(system.getCelesteBodies());
        boolean hasExobio = icons[0];
        boolean hasMapped = icons[1];
        
        // Calculer le nombre d'espèces collectées et détectées pour TOUTES les planètes du système
        int confirmedSpeciesCount = 0;
        int numSpeciesDetected = 0;
        
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
            }
        }
        
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
        
        // 5. Icône mapped (si nécessaire)
        javafx.scene.image.ImageView mappedIcon = null;
        if (hasMapped && mappedImage != null) {
            mappedIcon = new javafx.scene.image.ImageView(mappedImage);
            mappedIcon.setFitWidth(16);
            mappedIcon.setFitHeight(16);
            mappedIcon.setPreserveRatio(true);
        }
        
        // 6. Valeur en Cr
        long totalValue = system.getCelesteBodies().stream()
                .mapToLong(ACelesteBody::computeValue)
                .sum();
        Label valueLabel = new Label(String.format("%,d Cr", totalValue));
        valueLabel.getStyleClass().add("exploration-system-value");
        valueLabel.setPrefWidth(120);
        valueLabel.setMinWidth(80);
        valueLabel.setMaxWidth(120);
        
        // Ajouter tous les éléments dans l'ordre
        mainRow.getChildren().add(systemNameLabel);
        mainRow.getChildren().add(bodiesCountLabel);
        if (exobioIcon != null) {
            mainRow.getChildren().add(exobioIcon);
        }
        if (speciesCountLabel != null) {
            mainRow.getChildren().add(speciesCountLabel);
        }
        if (mappedIcon != null) {
            mainRow.getChildren().add(mappedIcon);
        }
        mainRow.getChildren().add(valueLabel);
        root.getChildren().add(mainRow);
        
        // Gérer le clic sur la carte
        root.setOnMouseClicked(e -> {
            // Notifier le clic sur le système
            if (onSystemSelected != null) {
                onSystemSelected.accept(system);
            }
        });
        
        return root;
    }

    public void setOnSystemSelected(Consumer<SystemVisited> callback) {
        this.onSystemSelected = callback;
    }
}

