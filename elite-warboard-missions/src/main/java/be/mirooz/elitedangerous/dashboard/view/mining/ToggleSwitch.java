package be.mirooz.elitedangerous.dashboard.view.mining;

import javafx.animation.TranslateTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class ToggleSwitch extends HBox {
    
    private final BooleanProperty selected = new SimpleBooleanProperty(false);
    private final Label leftLabel = new Label("BEST PRICE");
    private final Label rightLabel = new Label("STATION PRICE");
    private final Rectangle track = new Rectangle();
    private final Circle thumb = new Circle();
    private final StackPane switchPane = new StackPane();
    
    private TranslateTransition thumbTransition;
    
    public ToggleSwitch() {
        initializeComponents();
        setupAnimations();
        setupEventHandlers();
    }
    
    private void initializeComponents() {
        // Configuration du track (fond du switch)
        track.setWidth(50);
        track.setHeight(24); // Réduit de 30 à 24
        track.setArcWidth(24);
        track.setArcHeight(24);
        track.getStyleClass().add("toggle-track");
        
        // Configuration du thumb (boule)
        thumb.setRadius(10); // Réduit de 12 à 10
        thumb.getStyleClass().add("toggle-thumb");
        
        // Configuration des labels
        leftLabel.getStyleClass().addAll("price-mode-label", "price-mode-selected");
        rightLabel.getStyleClass().addAll("price-mode-label", "price-mode-unselected");
        
        // Configuration du switch pane
        switchPane.getChildren().addAll(track, thumb);
        switchPane.setPrefSize(50, 24); // Réduit de 30 à 24
        switchPane.setCursor(javafx.scene.Cursor.HAND); // Curseur hand uniquement sur le switch
        
        // Configuration du container (this = HBox)
        this.setAlignment(Pos.CENTER);
        this.setSpacing(10);
        this.getChildren().addAll(leftLabel, switchPane, rightLabel);
        
        // Appliquer les styles CSS
        this.getStyleClass().add("toggle-switch");
    }
    
    private void setupAnimations() {
        thumbTransition = new TranslateTransition(Duration.millis(200), thumb);
        thumbTransition.setFromX(-13);
        thumbTransition.setToX(13);
    }
    
    private void setupEventHandlers() {
        // Gestionnaire de clic sur le switch
        switchPane.setOnMouseClicked(event -> {
            setSelected(!isSelected());
        });
        
        // Listener pour les changements d'état
        selected.addListener((obs, oldVal, newVal) -> {
            updateVisualState();
            updateLabels();
        });
        
        // État initial
        updateVisualState();
        updateLabels();
    }
    
    private void updateVisualState() {
        if (isSelected()) {
            track.getStyleClass().remove("toggle-track-off");
            track.getStyleClass().add("toggle-track-on");
            thumbTransition.setRate(1);
            thumbTransition.play();
        } else {
            track.getStyleClass().remove("toggle-track-on");
            track.getStyleClass().add("toggle-track-off");
            thumbTransition.setRate(-1);
            thumbTransition.play();
        }
    }
    
    private void updateLabels() {
        // Nettoyer toutes les classes de style des labels
        leftLabel.getStyleClass().removeAll("price-mode-selected", "price-mode-unselected");
        rightLabel.getStyleClass().removeAll("price-mode-selected", "price-mode-unselected");
        
        if (isSelected()) {
            // Mode "Station Price" sélectionné (boule à droite)
            leftLabel.getStyleClass().add("price-mode-unselected");
            rightLabel.getStyleClass().add("price-mode-selected");
        } else {
            // Mode "Best Price" sélectionné (boule à gauche)
            leftLabel.getStyleClass().add("price-mode-selected");
            rightLabel.getStyleClass().add("price-mode-unselected");
        }
    }
    
    // Getters et setters
    public boolean isSelected() {
        return selected.get();
    }
    
    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }
    
    public BooleanProperty selectedProperty() {
        return selected;
    }
    
    public Label getLeftLabel() {
        return leftLabel;
    }
    
    public Label getRightLabel() {
        return rightLabel;
    }
}
