package be.mirooz.elitedangerous.dashboard.controller.ui.component.exploration;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.TreeCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Cellule personnalisée pour afficher les éléments JSON avec des labels colorés
 * Style Elite Dangerous avec séparation visuelle claire entre clés et valeurs
 */
public class JsonTreeCell extends TreeCell<JsonTreeItem> {
    
    private final HBox container;
    private final javafx.scene.control.Label keyLabel;
    private final javafx.scene.control.Label separatorLabel;
    private final javafx.scene.control.Label valueLabel;
    private final VBox valueListContainer;

    public JsonTreeCell() {
        container = new HBox(8);
        container.setAlignment(Pos.TOP_LEFT);
        container.setPadding(new Insets(4, 8, 4, 0)); // Pas de padding à gauche
        
        keyLabel = new javafx.scene.control.Label();
        keyLabel.getStyleClass().add("json-key-label");
        keyLabel.setAlignment(Pos.CENTER_LEFT); // Aligner à gauche pour un bord gauche uniforme
        keyLabel.setMinWidth(120); // Largeur fixe pour aligner les ":"
        keyLabel.setMaxWidth(120);
        keyLabel.setPrefWidth(120);
        
        // Séparateur visuel élégant
        separatorLabel = new javafx.scene.control.Label(":");
        separatorLabel.getStyleClass().add("json-separator");
        
        valueLabel = new javafx.scene.control.Label();
        valueLabel.getStyleClass().add("json-value-label");
        HBox.setHgrow(valueLabel, Priority.ALWAYS);
        
        // Container pour les listes (multi-lignes)
        valueListContainer = new VBox(2);
        valueListContainer.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(valueListContainer, Priority.ALWAYS);
        
        container.getChildren().addAll(keyLabel, separatorLabel, valueLabel);
        
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
            separatorLabel.setVisible(false);
        } else {
            keyLabel.setText(key);
            separatorLabel.setVisible(true);
        }

        // Nettoyer les styles précédents
        valueLabel.getStyleClass().removeAll("json-value-string", "json-value-number", 
            "json-value-boolean", "json-value-null", "json-value-object", "json-value-array",
            "json-value-special-array");
        
        // Retirer le container de liste s'il était présent
        if (container.getChildren().contains(valueListContainer)) {
            container.getChildren().remove(valueListContainer);
            container.getChildren().add(valueLabel);
        }
        valueListContainer.getChildren().clear();
        
        // Afficher la valeur avec le style approprié
        // Si c'est un formatage spécial (déjà formaté dans JsonTreeItem), afficher en liste
        if (item.isSpecialFormat()) {
            // Diviser la valeur par les retours à la ligne et créer un label pour chaque élément
            String[] lines = displayValue.split("\n");
            if (lines.length > 1) {
                // Liste multi-lignes
                container.getChildren().remove(valueLabel);
                for (String line : lines) {
                    if (!line.trim().isEmpty()) {
                        javafx.scene.control.Label listItem = new javafx.scene.control.Label(line.trim());
                        listItem.getStyleClass().add("json-value-special-array");
                        listItem.setWrapText(true);
                        valueListContainer.getChildren().add(listItem);
                    }
                }
                container.getChildren().add(valueListContainer);
            } else {
                // Une seule ligne
                valueLabel.setText(displayValue);
                valueLabel.getStyleClass().add("json-value-special-array");
            }
        } else {
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
        }

        setGraphic(container);
        setText(null);
    }
}

