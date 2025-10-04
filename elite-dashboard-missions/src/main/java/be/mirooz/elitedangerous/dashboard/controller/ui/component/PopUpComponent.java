package be.mirooz.elitedangerous.dashboard.controller.ui.component;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class PopUpComponent extends VBox {

    public PopUpComponent(String message, double xPos, double yPos, StackPane popupContainer) {
        super();
        // Styles du conteneur
        this.getStyleClass().add("system-copied-popup");
        this.setAlignment(Pos.CENTER);
        this.setSpacing(3);
        this.setPadding(new Insets(8, 16, 8, 16));
        // Label de message
        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("popup-title");
        this.getChildren().add(messageLabel);

        // Taille compacte
        this.setMinSize(120, 40);
        this.setPrefSize(120, 40);
        this.setMaxSize(120, 40);

        // Position par rapport à la souris
        this.setTranslateX(xPos);
        this.setTranslateY(yPos);

        // Forcer l’alignement dans le StackPane parent
        StackPane.setAlignment(this, Pos.TOP_LEFT);

        // Ajouter au conteneur
        popupContainer.getChildren().add(this);

        // Animation d’apparition
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), this);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        // Pause visible
        PauseTransition pause = new PauseTransition(Duration.millis(1000));

        // Animation de disparition
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), this);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        // Enchaînement
        SequentialTransition sequence = new SequentialTransition(fadeIn, pause, fadeOut);
        sequence.setOnFinished(e -> popupContainer.getChildren().remove(this));
        sequence.play();
    }
}
