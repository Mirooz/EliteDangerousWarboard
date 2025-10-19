package be.mirooz.elitedangerous.dashboard.controller.ui.component.mining;

import be.mirooz.elitedangerous.dashboard.model.events.ProspectedAsteroid;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Composant pour afficher une carte de prospecteur
 */
public class ProspectorCardComponent {
    
    /**
     * Récupère une traduction depuis le LocalizationService
     */
    private static String getTranslation(String key) {
        return LocalizationService.getInstance().getString(key);
    }
    
    /**
     * Crée une carte de prospecteur avec design Elite Dangerous
     */
    public static VBox createProspectorCard(ProspectedAsteroid prospector, boolean isLast) {
        // Conteneur principal de la carte
        VBox cardContainer = new VBox(8);
        cardContainer.getStyleClass().add(isLast ? "elite-prospector-card-large" : "elite-prospector-card");
        
        // Pour les cartes grandes, s'assurer qu'elles utilisent l'espace disponible
        if (isLast) {
            cardContainer.setFillWidth(true);
        }

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
                prospector.getCoreMineral().getVisibleName() :
                (prospector.getMotherlodeMaterial() != null ? prospector.getMotherlodeMaterial().toUpperCase() : getTranslation("mining.asteroid"));

        Label mineralLabel = new Label(mineralName);
        mineralLabel.getStyleClass().add(isLast ? "elite-mineral-name-large" : "elite-mineral-name");

        // Indicateur de core si présent
        if (prospector.getCoreMineral() != null) {
            Label coreIndicator = new Label(getTranslation("mining.core"));
            coreIndicator.getStyleClass().add("core-indicator");
            headerContainer.getChildren().addAll(asteroidIcon, mineralLabel, coreIndicator);
        } else {
            //headerContainer.getChildren().addAll(asteroidIcon, mineralLabel);
        }

        // Contenu localisé
        String content = prospector.getContentLocalised() != null ?
                prospector.getContentLocalised() :
                (prospector.getContent() != null ? prospector.getContent() : getTranslation("mining.unknown_content"));

        Label contentLabel = new Label(content);
        contentLabel.getStyleClass().add(isLast ? "elite-content-large" : "elite-content");

        // Matériaux avec design amélioré
        VBox materialsContainer = new VBox(4);
        materialsContainer.getStyleClass().add("elite-materials-container");

        if (prospector.getMaterials() != null && !prospector.getMaterials().isEmpty()) {
            Label materialsTitle = new Label(getTranslation("mining.materials"));
            materialsTitle.getStyleClass().add(isLast ? "elite-materials-title-large" : "elite-materials-title");
            materialsContainer.getChildren().add(materialsTitle);

            for (ProspectedAsteroid.Material material : prospector.getMaterials()) {
                if (material.getProportion() != null) {
                    HBox materialRow = new HBox(10);
                    materialRow.setAlignment(Pos.CENTER_LEFT);

                    String materialName = material.getNameLocalised() != null ?
                            material.getNameLocalised() :
                            (material.getName() != null ? material.getName().getVisibleName(): getTranslation("mining.unknown_material"));

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
            
            // Pour les cartes grandes, permettre l'expansion des matériaux
            if (isLast) {
                VBox.setVgrow(materialsContainer, javafx.scene.layout.Priority.ALWAYS);
            }
        }

        return cardContainer;
    }
}
