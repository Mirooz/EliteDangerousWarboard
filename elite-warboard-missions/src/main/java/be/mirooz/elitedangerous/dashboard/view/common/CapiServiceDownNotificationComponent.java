package be.mirooz.elitedangerous.dashboard.view.common;

import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.view.common.managers.NotificationStackManager;
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
 * Toast en haut à droite lorsque le backend CAPI répond {@code 418} (service indisponible).
 * Même emplacement que {@link CapiAuthConnectedNotificationComponent}, style d’alerte (rouge).
 */
public class CapiServiceDownNotificationComponent extends VBox {

    private static final Duration FADE_IN = Duration.millis(260);
    private static final Duration DISPLAY_DURATION = Duration.seconds(4);
    private static final Duration FADE_OUT = Duration.millis(320);

    private final VBox notificationStack;

    public CapiServiceDownNotificationComponent(StackPane container) {
        this.notificationStack = NotificationStackManager.getInstance().getOrCreateStack(container);
        LocalizationService localizationService = LocalizationService.getInstance();

        this.getStyleClass().add("capi-service-down-notification");
        this.setAlignment(Pos.CENTER_LEFT);
        this.setSpacing(6);
        this.setPadding(new Insets(10, 14, 10, 14));
        this.setMaxWidth(320);
        this.setPrefWidth(300);
        this.setMaxHeight(USE_PREF_SIZE);
        this.setPrefHeight(USE_COMPUTED_SIZE);

        Label title = new Label(localizationService.getString("capi.service.down"));
        title.getStyleClass().add("capi-service-down-title");
        title.setWrapText(true);

        this.getChildren().add(title);

        notificationStack.getChildren().add(this);
        playLifecycle();
    }

    private void playLifecycle() {
        FadeTransition fadeIn = new FadeTransition(FADE_IN, this);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        PauseTransition hold = new PauseTransition(DISPLAY_DURATION);

        FadeTransition fadeOut = new FadeTransition(FADE_OUT, this);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        SequentialTransition sequence = new SequentialTransition(fadeIn, hold, fadeOut);
        sequence.setOnFinished(e -> notificationStack.getChildren().remove(this));
        sequence.play();
    }
}
