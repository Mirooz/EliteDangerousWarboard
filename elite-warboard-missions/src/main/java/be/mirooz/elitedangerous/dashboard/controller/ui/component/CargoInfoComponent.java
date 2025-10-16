package be.mirooz.elitedangerous.dashboard.controller.ui.component;

import be.mirooz.elitedangerous.commons.lib.models.commodities.ICommodity;
import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.CoreMineralType;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.service.MiningService;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Map;

/**
 * Composant pour afficher les informations du cargo
 */
public class CargoInfoComponent {
    
    /**
     * Récupère une traduction depuis le LocalizationService
     */
    private static String getTranslation(String key) {
        return LocalizationService.getInstance().getString(key);
    }
    
    
    /**
     * Crée la liste des minéraux dans le cargo
     */
    public static VBox createMineralsList(Map<ICommodity, Integer> minerals) {
        VBox mineralsList = new VBox(5);
        mineralsList.getStyleClass().add("cargo-minerals-list");
        
        if (minerals.isEmpty()) {
            mineralsList.getChildren().add(createNoMineralsLabel());
        } else {
            for (Map.Entry<ICommodity, Integer> entry : minerals.entrySet()) {
                HBox mineralCard = createMineralCard(entry.getKey(), entry.getValue());
                mineralsList.getChildren().add(mineralCard);
            }
        }
        
        return mineralsList;
    }
    
    /**
     * Crée une carte pour un minéral dans le cargo
     */
    public static HBox createMineralCard(ICommodity commodity, Integer quantity) {
        HBox mineralCard = new HBox(10);
        mineralCard.getStyleClass().add("mineral-card");
        mineralCard.setAlignment(Pos.CENTER_LEFT);

        // Utiliser le nom du core mineral en majuscules si c'est un minéral
        String displayName;
        if (commodity instanceof CoreMineralType) {
            CoreMineralType coreMineral = (CoreMineralType) commodity;
            displayName = coreMineral.getInaraName().toUpperCase();
        } else {
            displayName = commodity.toString().toUpperCase();
        }

        Label mineralName = new Label(displayName);
        mineralName.getStyleClass().add("mineral-name");

        Label mineralQuantity = new Label(String.valueOf(quantity));
        mineralQuantity.getStyleClass().add("mineral-quantity");

        mineralCard.getChildren().addAll(mineralName, mineralQuantity);
        return mineralCard;
    }
    
    /**
     * Crée le label "Aucun minéral dans le cargo"
     */
    public static Label createNoMineralsLabel() {
        Label noMineralsLabel = new Label(getTranslation("mining.no_minerals"));
        noMineralsLabel.getStyleClass().add("no-minerals-label");
        return noMineralsLabel;
    }
    
}
