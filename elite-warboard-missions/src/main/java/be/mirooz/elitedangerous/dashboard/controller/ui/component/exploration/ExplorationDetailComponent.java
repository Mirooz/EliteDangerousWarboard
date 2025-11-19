package be.mirooz.elitedangerous.dashboard.controller.ui.component.exploration;

import be.mirooz.elitedangerous.dashboard.controller.IRefreshable;
import be.mirooz.elitedangerous.dashboard.model.exploration.ExplorationDataSale;
import be.mirooz.elitedangerous.dashboard.model.exploration.SystemVisited;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.*;

/**
 * Composant pour afficher les détails d'un groupe d'exploration
 */
public class ExplorationDetailComponent implements Initializable, IRefreshable {

    @FXML
    private VBox generalInfoContainer;
    @FXML
    private Label detailTotalEarningsLabel;
    @FXML
    private Label detailSystemsCountLabel;
    @FXML
    private ScrollPane systemsScrollPane;
    @FXML
    private VBox systemsList;

    private ExplorationDataSale currentSale;
    private java.util.function.Consumer<SystemVisited> onSystemSelected;
    private SystemCardController currentExpandedController;
    private Image exobioImage;
    private Image mappedImage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Charger les images une seule fois pour toutes les cartes
        loadImages();
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
        refresh();
    }

    public void refresh() {
        Platform.runLater(() -> {
            if (currentSale == null) {
                generalInfoContainer.setVisible(false);
                systemsList.getChildren().clear();
                return;
            }

            generalInfoContainer.setVisible(true);
            detailTotalEarningsLabel.setText("Total: " + String.format("%,d Cr", currentSale.getTotalEarnings()));
            detailSystemsCountLabel.setText("Systèmes: " + currentSale.getSystemsVisited().size());

            // Vider la liste
            systemsList.getChildren().clear();
            
            // Créer les cartes directement en Java (beaucoup plus rapide que FXML)
            for (SystemVisited system : currentSale.getSystemsVisited()) {
                VBox card = createSystemCardDirectly(system);
                if (card != null) {
                    systemsList.getChildren().add(card);
                }
            }
        });
    }
    
    private VBox createSystemCardDirectly(SystemVisited system) {
        // Créer directement les composants en Java (pas de FXML = beaucoup plus rapide)
        VBox root = new VBox(5);
        root.getStyleClass().add("exploration-system-card");
        root.setStyle("-fx-cursor: hand");
        root.setPadding(new javafx.geometry.Insets(3));
        
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label systemNameLabel = new Label(system.getSystemName());
        systemNameLabel.getStyleClass().add("exploration-system-card-name");
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        headerBox.getChildren().addAll(systemNameLabel, spacer);
        
        VBox bodiesContainer = new VBox(5);
        bodiesContainer.getStyleClass().add("exploration-system-card-bodies");
        bodiesContainer.setVisible(false);
        bodiesContainer.setManaged(false);
        
        root.getChildren().addAll(headerBox, bodiesContainer);
        
        // Créer le contrôleur et l'initialiser manuellement
        SystemCardController controller = new SystemCardController();
        controller.setRoot(root);
        controller.setSystemNameLabel(systemNameLabel);
        controller.setBodiesContainer(bodiesContainer);
        // Passer les images depuis le parent (évite de les recharger à chaque carte)
        controller.setImages(exobioImage, mappedImage);
        controller.setSystem(system);
        
        // Stocker le contrôleur dans le userData
        root.setUserData(controller);
        
        // Gérer le clic sur la carte
        root.setOnMouseClicked(e -> {
            boolean wasExpanded = controller.isExpanded();
            controller.setExpanded(!wasExpanded);
            
            if (controller.isExpanded() && !wasExpanded) {
                // Fermer les autres cartes
                if (currentExpandedController != null && currentExpandedController != controller) {
                    currentExpandedController.setExpanded(false);
                }
                currentExpandedController = controller;
            }
            
            // Notifier le clic sur le système
            if (onSystemSelected != null) {
                onSystemSelected.accept(system);
            }
        });
        
        return root;
    }

    public void setExplorationDataSale(ExplorationDataSale sale) {
        this.currentSale = sale;
        refresh();
    }
    
    public void setOnSystemSelected(java.util.function.Consumer<SystemVisited> callback) {
        this.onSystemSelected = callback;
    }
}

