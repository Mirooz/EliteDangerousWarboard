package be.mirooz.elitedangerous.dashboard.controller.ui.component.mining;

import be.mirooz.elitedangerous.dashboard.model.events.ProspectedAsteroid;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.service.MiningService;
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
     * Calcule l'estimation de valeur d'un matériau basé sur son pourcentage
     * Formule: 0.28 × pourcentage × prix_du_minéral
     */
    private static long calculateMaterialEstimation(double percentage, be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.Mineral mineral) {
        if (mineral == null || mineral.getPrice() == 0) {
            return 0;
        }
        
        // Calculer la quantité extraite en tonnes: 0.28 × pourcentage
        double extractedTons = 0.28 * percentage;
        
        // Multiplier par le prix du minéral
        return Math.round(extractedTons * mineral.getPrice());
    }

    /**
     * Calcule l'estimation de valeur d'un core
     * Formule: 18 × prix_du_minéral
     */
    private static long calculateCoreEstimation(be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.MineralType coreMineral) {
        if (coreMineral == null || coreMineral.getPrice() == 0) {
            return 0;
        }
        
        // Pour un core: 18 tonnes × prix du minéral
        return Math.round(18 * coreMineral.getPrice());
    }

    /**
     * Crée une carte de prospecteur avec design Elite Dangerous
     */
    public static VBox createProspectorCard(ProspectedAsteroid prospector) {
        // Conteneur principal de la carte
        VBox cardContainer = new VBox(8);
        cardContainer.getStyleClass().add("elite-prospector-card-large");

        // Pour les cartes grandes, s'assurer qu'elles utilisent l'espace disponible
        cardContainer.setFillWidth(true);


        // En-tête d'informations (icône + éventuel core)
        HBox headerContainer = new HBox();
        headerContainer.setAlignment(Pos.CENTER_LEFT);
        headerContainer.setSpacing(10);

        // Indicateur de core si présent
        if (prospector.getCoreMineral() != null) {
            // Icône d'astéroïde core
            Label asteroidIcon = new Label("●");
            asteroidIcon.getStyleClass().add("asteroid-icon");
            asteroidIcon.getStyleClass().add("core-asteroid");
            
            // Nom du minéral core
            String mineralName = prospector.getCoreMineral().getVisibleName();
            Label mineralLabel = new Label(mineralName);
            mineralLabel.getStyleClass().add("elite-mineral-name-large");
            
            Label coreIndicator = new Label(getTranslation("mining.core"));
            coreIndicator.getStyleClass().add("core-indicator");
            
            headerContainer.getChildren().addAll(asteroidIcon, mineralLabel, coreIndicator);
        }
        // Pas de core, ne rien afficher dans l'en-tête

        // Calculer l'estimation totale
        long totalEstimation = 0;
        if (prospector.getMaterials() != null) {
            for (ProspectedAsteroid.Material material : prospector.getMaterials()) {
                if (material.getProportion() != null) {
                    long estimation = calculateMaterialEstimation(material.getProportion(), material.getName());
                    totalEstimation += estimation;
                }
            }
        }
        if (prospector.getCoreMineral() != null) {
            long coreEstimation = calculateCoreEstimation(prospector.getCoreMineral());
            totalEstimation += coreEstimation;
        }

        // Afficher l'estimation totale AU-DESSUS de l'astéroïde (aligné à gauche)
        if (totalEstimation > 0) {
            HBox estimationBar = new HBox();
            estimationBar.setAlignment(Pos.CENTER_LEFT);
            Label totalEstimationLabel = new Label("~" + MiningService.getInstance().formatPrice(totalEstimation) + " Cr");
            totalEstimationLabel.getStyleClass().add("asteroid-total-estimation");
            estimationBar.getChildren().add(totalEstimationLabel);
            cardContainer.getChildren().add(estimationBar);
        }

        // Contenu localisé
        String content = prospector.getContentLocalised() != null ?
                prospector.getContentLocalised() :
                (prospector.getContent() != null ? prospector.getContent() : getTranslation("mining.unknown_content"));

        Label contentLabel = new Label(content);
        contentLabel.getStyleClass().add("elite-content-large");

        // Matériaux avec design amélioré
        VBox materialsContainer = new VBox(4);
        materialsContainer.getStyleClass().add("elite-materials-container");

        if (prospector.getMaterials() != null && !prospector.getMaterials().isEmpty()) {
            Label materialsTitle = new Label(getTranslation("mining.materials"));
            materialsTitle.getStyleClass().add("elite-materials-title-large");
            materialsContainer.getChildren().add(materialsTitle);

            for (ProspectedAsteroid.Material material : prospector.getMaterials()) {
                if (material.getProportion() != null) {
                    HBox materialRow = new HBox(10);
                    materialRow.setAlignment(Pos.CENTER_LEFT);

                    String materialName = material.getNameLocalised() != null ?
                            material.getNameLocalised().toUpperCase() :
                            (material.getName() != null ? material.getName().getVisibleName() : getTranslation("mining.unknown_material"));

                    Label materialLabel = new Label(materialName);
                    materialLabel.getStyleClass().add("elite-material-name-large");

                    Label percentageLabel = new Label(String.format("%.1f%%", material.getProportion()));
                    percentageLabel.getStyleClass().add("elite-material-percent-large");

                    double proportion = material.getProportion();

                    if (proportion <= 20) {
                        percentageLabel.getStyleClass().add("low");
                    } else if (proportion <= 35) {
                        percentageLabel.getStyleClass().add("medium");
                    } else {
                        percentageLabel.getStyleClass().add("high");
                    }

                    materialRow.getChildren().addAll(materialLabel, percentageLabel);
                    materialsContainer.getChildren().add(materialRow);
                }
            }
        }

        // Ajouter l'estimation du core au total si présent
        if (prospector.getCoreMineral() != null) {
            long coreEstimation = calculateCoreEstimation(prospector.getCoreMineral());
            totalEstimation += coreEstimation;
        }

        // Assemblage final
        cardContainer.getChildren().addAll(headerContainer, contentLabel);
        if (!materialsContainer.getChildren().isEmpty()) {
            cardContainer.getChildren().add(materialsContainer);
            VBox.setVgrow(materialsContainer, javafx.scene.layout.Priority.ALWAYS);
        }

        return cardContainer;
    }
}
