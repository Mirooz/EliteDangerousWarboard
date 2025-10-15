package be.mirooz.elitedangerous.dashboard.controller;

import be.mirooz.elitedangerous.dashboard.controller.ui.manager.UIManager;
import be.mirooz.elitedangerous.dashboard.model.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.model.events.ProspectedAsteroid;
import be.mirooz.elitedangerous.dashboard.model.registries.ProspectedAsteroidRegistry;
import be.mirooz.elitedangerous.lib.inara.model.commodities.ICommodity;
import be.mirooz.elitedangerous.lib.inara.model.commodities.LimpetType;
import be.mirooz.elitedangerous.lib.inara.model.commodities.minerals.Mineral;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.Deque;
import java.util.Map;
import java.util.ResourceBundle;

import static be.mirooz.elitedangerous.lib.inara.model.commodities.LimpetType.LIMPET;

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
    private Label cargoMaxLabel;
    
    @FXML
    private Label limpetsCountLabel;
    
    @FXML
    private VBox cargoMineralsList;

    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();
    private final ProspectedAsteroidRegistry prospectedRegistry = ProspectedAsteroidRegistry.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateProspectors();
        updateCargo();
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
            
            // Afficher les 4 autres prospecteurs
            int count = 0;
            for (ProspectedAsteroid prospector : prospectors) {
                if (count < 4) { // Limiter à 4 pour ne pas dépasser 5 au total
                    VBox cardContainer = new VBox();
                    cardContainer.getStyleClass().add("prospector-card");
                    createProspectorCard(prospector, cardContainer, false);
                    prospectorsList.getChildren().add(cardContainer);
                    count++;
                }
            }
        });
    }

    /**
     * Crée une carte de prospecteur
     */
    private void createProspectorCard(ProspectedAsteroid prospector, VBox container, boolean isLast) {
        // Nom du minéral principal
        String mineralName = prospector.getCoreMineral() != null ? 
            prospector.getCoreMineral().getInaraName() : 
            prospector.getMotherlodeMaterial();
        
        Label mineralLabel = new Label(mineralName);
        mineralLabel.getStyleClass().add(isLast ? "last-prospector-mineral" : "prospector-mineral");
        
        // Contenu localisé
        Label contentLabel = new Label(prospector.getContentLocalised() != null ? 
            prospector.getContentLocalised() : prospector.getContent());
        contentLabel.getStyleClass().add(isLast ? "last-prospector-content" : "prospector-content");
        
        // Pourcentage restant
        if (prospector.getRemaining() != null) {
            Label remainingLabel = new Label(String.format("%.1f%% restant", prospector.getRemaining()));
            remainingLabel.getStyleClass().add(isLast ? "last-prospector-remaining" : "prospector-remaining");
            container.getChildren().addAll(mineralLabel, contentLabel, remainingLabel);
        } else {
            container.getChildren().addAll(mineralLabel, contentLabel);
        }
        
        // Matériaux
        if (prospector.getMaterials() != null && !prospector.getMaterials().isEmpty()) {
            VBox materialsContainer = new VBox(2);
            materialsContainer.getStyleClass().add("materials-container");
            
            for (ProspectedAsteroid.Material material : prospector.getMaterials()) {
                if (material.getProportion() != null) {
                    String materialName = material.getNameLocalised() != null ? 
                        material.getNameLocalised() : 
                        (material.getName() != null ? material.getName().toString() : "Inconnu");
                    
                    Label materialLabel = new Label(String.format("%s: %.1f%%", 
                        materialName, material.getProportion()));
                    materialLabel.getStyleClass().add(isLast ? "last-prospector-material" : "prospector-material");
                    materialsContainer.getChildren().add(materialLabel);
                }
            }
            
            if (!materialsContainer.getChildren().isEmpty()) {
                container.getChildren().add(materialsContainer);
            }
        }
    }

    /**
     * Met à jour l'affichage du cargo
     */
    public void updateCargo() {
        Platform.runLater(() -> {
            if (commanderStatus.getShip() == null || commanderStatus.getShip().getShipCargo() == null) {
                cargoUsedLabel.setText("0");
                cargoMaxLabel.setText("0");
                limpetsCountLabel.setText("0");
                cargoMineralsList.getChildren().clear();
                return;
            }
            
            var cargo = commanderStatus.getShip().getShipCargo();
            
            // Mettre à jour les statistiques du cargo
            cargoUsedLabel.setText(String.valueOf(cargo.getCurrentUsed()));
            cargoMaxLabel.setText(String.valueOf(cargo.getMaxCapacity()));
            
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
                
                Label mineralName = new Label(commodity.toString());
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

    @Override
    public void refreshUI() {
        updateProspectors();
        updateCargo();
    }
}

