package be.mirooz.elitedangerous.dashboard.view.common;

import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.view.common.managers.NotificationStackManager;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.function.Consumer;

/**
 * Notification en haut à droite : consentement à l’envoi des journaux d’erreur (même emplacement que CAPI / version).
 */
public class ErrorLogsConsentNotificationComponent extends VBox {

    private final VBox notificationStack;

    public ErrorLogsConsentNotificationComponent(StackPane container, Consumer<Boolean> onChoice) {
        this.notificationStack = NotificationStackManager.getInstance().getOrCreateStack(container);
        LocalizationService loc = LocalizationService.getInstance();

        this.getStyleClass().add("capi-auth-notification");
        this.setAlignment(Pos.CENTER_LEFT);
        this.setSpacing(10);
        this.setPadding(new Insets(12, 16, 12, 16));
        this.setMaxWidth(350);
        this.setPrefWidth(320);
        this.setMaxHeight(USE_PREF_SIZE);
        this.setPrefHeight(USE_COMPUTED_SIZE);

        Label headerLabel = new Label(loc.getString("analytics.error.logs.prompt.header"));
        headerLabel.getStyleClass().add("capi-auth-title");
        headerLabel.setWrapText(true);

        Label messageLabel = new Label(loc.getString("analytics.error.logs.prompt.message"));
        messageLabel.getStyleClass().add("capi-auth-message");
        messageLabel.setWrapText(true);

        Button yesButton = new Button(loc.getString("dialog.yes"));
        yesButton.getStyleClass().add("capi-auth-approve-button");
        yesButton.setCursor(Cursor.HAND);
        yesButton.setOnAction(e -> {
            if (onChoice != null) {
                onChoice.accept(true);
            }
            closeNotification();
        });

        Button noButton = new Button(loc.getString("dialog.no"));
        noButton.getStyleClass().add("capi-auth-cancel-button");
        noButton.setCursor(Cursor.HAND);
        noButton.setOnAction(e -> {
            if (onChoice != null) {
                onChoice.accept(false);
            }
            closeNotification();
        });

        HBox buttons = new HBox(10, yesButton, noButton);
        buttons.setAlignment(Pos.CENTER_LEFT);

        this.getChildren().addAll(headerLabel, messageLabel, buttons);
        notificationStack.getChildren().add(this);
        playFadeIn();
    }

    private void playFadeIn() {
        FadeTransition fadeIn = new FadeTransition(Duration.millis(260), this);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    private void closeNotification() {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(180), this);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(event -> notificationStack.getChildren().remove(this));
        fadeOut.play();
    }
}
