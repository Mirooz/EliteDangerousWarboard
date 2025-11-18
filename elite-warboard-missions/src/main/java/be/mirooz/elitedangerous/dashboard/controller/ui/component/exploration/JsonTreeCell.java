package be.mirooz.elitedangerous.dashboard.controller.ui.component.exploration;

import javafx.geometry.Insets;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * Cellule personnalisée pour afficher les éléments JSON avec des labels colorés
 */
public class JsonTreeCell extends TreeCell<JsonTreeItem> {
    
    private final HBox container;
    private final javafx.scene.control.Label keyLabel;
    private final javafx.scene.control.Label valueLabel;

    public JsonTreeCell() {
        container = new HBox(8);
        container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        container.setPadding(new Insets(2, 5, 2, 5));
        
        keyLabel = new javafx.scene.control.Label();
        keyLabel.getStyleClass().add("json-key-label");
        
        valueLabel = new javafx.scene.control.Label();
        valueLabel.getStyleClass().add("json-value-label");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        container.getChildren().addAll(keyLabel, valueLabel, spacer);
        
        // S'assurer que le container prend toute la largeur
        container.setMaxWidth(Double.MAX_VALUE);
    }

    @Override
    protected void updateItem(JsonTreeItem item, boolean empty) {
        super.updateItem(item, empty);
        
        if (empty || item == null) {
            setGraphic(null);
            setText(null);
            return;
        }

        String key = item.getKey();
        JsonTreeItem.JsonValueType valueType = item.getValueType();
        String displayValue = item.getDisplayValue();

        // Afficher la clé
        if (key.isEmpty()) {
            keyLabel.setText("");
        } else {
            keyLabel.setText(key);
        }

        // Nettoyer les styles précédents
        valueLabel.getStyleClass().removeAll("json-value-string", "json-value-number", 
            "json-value-boolean", "json-value-null", "json-value-object", "json-value-array");
        
        // Afficher la valeur avec le style approprié (sans guillemets)
        switch (valueType) {
            case OBJECT:
                valueLabel.setText(displayValue);
                valueLabel.getStyleClass().add("json-value-object");
                break;
            case ARRAY:
                int arraySize = item.getNode() != null ? item.getNode().size() : 0;
                valueLabel.setText(displayValue + " (" + arraySize + " items)");
                valueLabel.getStyleClass().add("json-value-array");
                break;
            case STRING:
                valueLabel.setText(displayValue);
                valueLabel.getStyleClass().add("json-value-string");
                break;
            case NUMBER:
                valueLabel.setText(displayValue);
                valueLabel.getStyleClass().add("json-value-number");
                break;
            case BOOLEAN:
                valueLabel.setText(displayValue);
                valueLabel.getStyleClass().add("json-value-boolean");
                break;
            case NULL:
                valueLabel.setText(displayValue);
                valueLabel.getStyleClass().add("json-value-null");
                break;
        }

        setGraphic(container);
        setText(null);
    }
}

