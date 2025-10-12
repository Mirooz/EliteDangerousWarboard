package be.mirooz.elitedangerous.journal;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Model class representing a journal event item
 */
public class JournalEventItem {
    
    private final StringProperty eventType;
    private final StringProperty timestamp;
    private final StringProperty jsonContent;
    private final BooleanProperty visible;
    private final BooleanProperty expanded;
    
    public JournalEventItem(String eventType, String timestamp, String jsonContent) {
        this.eventType = new SimpleStringProperty(eventType);
        this.timestamp = new SimpleStringProperty(timestamp);
        this.jsonContent = new SimpleStringProperty(jsonContent);
        this.visible = new SimpleBooleanProperty(true);
        this.expanded = new SimpleBooleanProperty(false);
    }
    
    // Getters and setters
    public String getEventType() {
        return eventType.get();
    }
    
    public StringProperty eventTypeProperty() {
        return eventType;
    }
    
    public void setEventType(String eventType) {
        this.eventType.set(eventType);
    }
    
    public String getTimestamp() {
        return timestamp.get();
    }
    
    public StringProperty timestampProperty() {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp) {
        this.timestamp.set(timestamp);
    }
    
    public String getJsonContent() {
        return jsonContent.get();
    }
    
    public StringProperty jsonContentProperty() {
        return jsonContent;
    }
    
    public void setJsonContent(String jsonContent) {
        this.jsonContent.set(jsonContent);
    }
    
    public boolean isVisible() {
        return visible.get();
    }
    
    public BooleanProperty visibleProperty() {
        return visible;
    }
    
    public void setVisible(boolean visible) {
        this.visible.set(visible);
    }
    
    public boolean isExpanded() {
        return expanded.get();
    }
    
    public BooleanProperty expandedProperty() {
        return expanded;
    }
    
    public void setExpanded(boolean expanded) {
        this.expanded.set(expanded);
    }
}
