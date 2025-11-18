package be.mirooz.elitedangerous.dashboard.controller.ui.component.exploration;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Classe pour stocker les informations d'un nœud JSON avec son type
 */
public class JsonTreeItem {
    private final String key;
    private final JsonNode node;
    private final JsonValueType valueType;
    private final String displayValue;

    public enum JsonValueType {
        OBJECT, ARRAY, STRING, NUMBER, BOOLEAN, NULL
    }

    public JsonTreeItem(String key, JsonNode node) {
        this.key = key;
        this.node = node;
        
        if (node == null || node.isNull()) {
            this.valueType = JsonValueType.NULL;
            this.displayValue = "null";
        } else if (node.isObject()) {
            this.valueType = JsonValueType.OBJECT;
            this.displayValue = "{";
        } else if (node.isArray()) {
            this.valueType = JsonValueType.ARRAY;
            this.displayValue = "[";
        } else if (node.isTextual()) {
            this.valueType = JsonValueType.STRING;
            String text = node.asText();
            if (text.length() > 80) {
                text = text.substring(0, 77) + "...";
            }
            this.displayValue = text;
        } else if (node.isNumber()) {
            this.valueType = JsonValueType.NUMBER;
            this.displayValue = node.numberValue().toString();
        } else if (node.isBoolean()) {
            this.valueType = JsonValueType.BOOLEAN;
            this.displayValue = String.valueOf(node.booleanValue());
        } else {
            this.valueType = JsonValueType.STRING;
            this.displayValue = node.asText();
        }
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
}

