package be.mirooz.elitedangerous.dashboard.view.exploration;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Toast d’erreur (même look que {@link be.mirooz.elitedangerous.dashboard.view.common.CapiServiceDownNotificationComponent}),
 * ancré dans le {@link StackPane} de la zone route — pas au centre de la fenêtre.
 */
public final class NavRouteInlineErrorToastComponent extends VBox {

    private static final Duration FADE_IN = Duration.millis(260);
    private static final Duration DISPLAY_DURATION = Duration.seconds(4);
    private static final Duration FADE_OUT = Duration.millis(320);

    public NavRouteInlineErrorToastComponent(StackPane host, String message) {
        if (host == null || message == null || message.isBlank()) {
            throw new IllegalArgumentException("host et message requis");
        }

        getStyleClass().add("capi-service-down-notification");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(6);
        setPadding(new Insets(8, 10, 8, 10));
        setMaxWidth(280);
        setPrefWidth(USE_COMPUTED_SIZE);
        setMaxHeight(USE_PREF_SIZE);
        setPickOnBounds(false);
        setMouseTransparent(true);

        Label title = new Label(message);
        title.getStyleClass().add("capi-service-down-title");
        title.setWrapText(true);
        title.setMaxWidth(260);
        getChildren().add(title);

        host.getChildren().add(this);
        StackPane.setAlignment(this, Pos.TOP_LEFT);
        StackPane.setMargin(this, new Insets(4, 0, 0, 8));

        playLifecycle(host);
    }

    private void playLifecycle(StackPane host) {
        FadeTransition fadeIn = new FadeTransition(FADE_IN, this);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        PauseTransition hold = new PauseTransition(DISPLAY_DURATION);

        FadeTransition fadeOut = new FadeTransition(FADE_OUT, this);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        SequentialTransition sequence = new SequentialTransition(fadeIn, hold, fadeOut);
        sequence.setOnFinished(e -> host.getChildren().remove(this));
        sequence.play();
    }
}
