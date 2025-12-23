package be.mirooz.elitedangerous.dashboard.view.common;

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
        this(message, xPos, yPos, popupContainer, false);
    }

    public PopUpComponent(String message, double xPos, double yPos, StackPane popupContainer, boolean isWarning) {
        super();
        // Styles du conteneur
        if (isWarning) {
            this.getStyleClass().add("warning-popup");
            // Taille adaptative pour les avertissements
            this.setMinSize(250, 50);
            this.setPrefSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
            this.setMaxSize(500, 100); // Plus de hauteur pour le multiligne
        } else {
            this.getStyleClass().add("system-copied-popup");
            // Taille compacte pour les messages normaux
            this.setMinSize(120, 40);
            this.setPrefSize(120, 40);
            this.setMaxSize(120, 40);
        }
        
        this.setAlignment(Pos.CENTER);
        this.setSpacing(3);
        this.setPadding(new Insets(8, 16, 8, 16));
        
        // Label de message
        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("popup-title");
        if (isWarning) {
            messageLabel.setWrapText(true);
            messageLabel.setMaxWidth(450); // Largeur maximale pour le retour à la ligne
        }
        this.getChildren().add(messageLabel);

        // Position par rapport à la souris (ignorée pour les avertissements)
        if (!isWarning) {
            this.setTranslateX(xPos);
            this.setTranslateY(yPos);
        }

        // Forcer l'alignement dans le StackPane parent
        if (isWarning) {
            StackPane.setAlignment(this, Pos.CENTER);
        } else {
            StackPane.setAlignment(this, Pos.TOP_LEFT);
        }

        // Ajouter au conteneur
        popupContainer.getChildren().add(this);

        // Animation d'apparition
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), this);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        // Pause visible (plus longue pour les avertissements)
        PauseTransition pause = new PauseTransition(Duration.millis(isWarning ? 4000 : 1000));

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
