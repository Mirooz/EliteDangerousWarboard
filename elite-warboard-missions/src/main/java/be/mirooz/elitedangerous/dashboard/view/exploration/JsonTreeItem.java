package be.mirooz.elitedangerous.dashboard.view.exploration;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe pour stocker les informations d'un nœud JSON avec son type
 */
public class JsonTreeItem {
    private final String key;
    private final JsonNode node;
    private final JsonValueType valueType;
    private final String displayValue;
    private final boolean specialFormat;

    public enum JsonValueType {
        OBJECT, ARRAY, STRING, NUMBER, BOOLEAN, NULL
    }

    public JsonTreeItem(String key, JsonNode node) {
        this.key = key;
        this.node = node;
        boolean isSpecial = false;
        String formattedValue = null;
        String defaultDisplayValue = null;
        
        if (node == null || node.isNull()) {
            this.valueType = JsonValueType.NULL;
            defaultDisplayValue = "null";
        } else if (node.isObject()) {
            this.valueType = JsonValueType.OBJECT;
            // Formatage spécial pour Composition (objet)
            if ("Composition".equalsIgnoreCase(key)) {
                formattedValue = formatCompositionObject(node);
                isSpecial = true;
            } else {
                defaultDisplayValue = "{";
            }
        } else if (node.isArray()) {
            this.valueType = JsonValueType.ARRAY;
            // Formatage spécial pour Materials et AtmosphereComposition (tableaux)
            if ("Materials".equalsIgnoreCase(key)) {
                formattedValue = formatMaterialsArray(node);
                isSpecial = true;
            } else if ("AtmosphereComposition".equalsIgnoreCase(key)) {
                formattedValue = formatAtmosphereCompositionArray(node);
                isSpecial = true;
            } else {
                defaultDisplayValue = "[";
            }
        } else if (node.isTextual()) {
            this.valueType = JsonValueType.STRING;
            String text = node.asText();
            if (text.length() > 80) {
                text = text.substring(0, 77) + "...";
            }
            defaultDisplayValue = text;
        } else if (node.isNumber()) {
            this.valueType = JsonValueType.NUMBER;
            defaultDisplayValue = node.numberValue().toString();
        } else if (node.isBoolean()) {
            this.valueType = JsonValueType.BOOLEAN;
            defaultDisplayValue = String.valueOf(node.booleanValue());
        } else {
            this.valueType = JsonValueType.STRING;
            defaultDisplayValue = node.asText();
        }
        
        this.specialFormat = isSpecial;
        if (isSpecial && formattedValue != null) {
            this.displayValue = formattedValue;
        } else {
            this.displayValue = defaultDisplayValue;
        }
    }
    
    /**
     * Formate le tableau Materials pour afficher uniquement le nom et le pourcentage
     * Format: Liste propre avec chaque élément sur une ligne
     */
    private String formatMaterialsArray(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray() || arrayNode.size() == 0) {
            return "";
        }
        
        List<String> materials = new ArrayList<>();
        for (JsonNode item : arrayNode) {
            if (item.isObject()) {
                String name = item.has("Name") ? item.get("Name").asText() : 
                             (item.has("name") ? item.get("name").asText() : "Unknown");
                double percent = 0.0;
                if (item.has("Percent")) {
                    percent = item.get("Percent").asDouble();
                } else if (item.has("percent")) {
                    percent = item.get("percent").asDouble();
                }
                
                // Formater le nom en majuscules et le pourcentage
                String formattedName = name.toUpperCase();
                materials.add(String.format("%s, %.1f%%", formattedName, percent));
            }
        }
        
        return String.join("\n", materials);
    }
    
    /**
     * Formate l'objet Composition pour afficher uniquement les valeurs
     * Format: Liste propre avec chaque composant sur une ligne
     */
    private String formatCompositionObject(JsonNode objectNode) {
        if (objectNode == null || !objectNode.isObject()) {
            return "";
        }
        
        List<String> components = new ArrayList<>();
        if (objectNode.has("Ice")) {
            components.add(String.format("Ice: %.3f", objectNode.get("Ice").asDouble()));
        }
        if (objectNode.has("Rock")) {
            components.add(String.format("Rock: %.3f", objectNode.get("Rock").asDouble()));
        }
        if (objectNode.has("Metal")) {
            components.add(String.format("Metal: %.3f", objectNode.get("Metal").asDouble()));
        }
        
        return String.join("\n", components);
    }
    
    /**
     * Formate le tableau AtmosphereComposition pour afficher uniquement le nom et le pourcentage
     * Format: Liste propre avec chaque composant sur une ligne
     */
    private String formatAtmosphereCompositionArray(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray() || arrayNode.size() == 0) {
            return "";
        }
        
        List<String> components = new ArrayList<>();
        for (JsonNode item : arrayNode) {
            if (item.isObject()) {
                String name = item.has("Name") ? item.get("Name").asText() : 
                             (item.has("name") ? item.get("name").asText() : "Unknown");
                double percent = 0.0;
                if (item.has("Percent")) {
                    percent = item.get("Percent").asDouble();
                } else if (item.has("percent")) {
                    percent = item.get("percent").asDouble();
                }
                
                components.add(String.format("%s, %.1f%%", name, percent));
            }
        }
        
        return String.join("\n", components);
    }

    public String getKey() {
        return key;
    }

    public JsonNode getNode() {
        return node;
    }

    public JsonValueType getValueType() {
        return valueType;
    }

    public String getDisplayValue() {
        return displayValue;
    }

    public boolean hasChildren() {
        if (node == null) return false;
        if (node.isObject()) return node.size() > 0;
        if (node.isArray()) return node.size() > 0;
        return false;
    }
    
    public boolean isSpecialFormat() {
        return specialFormat;
    }
}

