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

public class CapiAuthNotificationComponent extends VBox {

    private final Runnable approveAction;
    private final VBox notificationStack;

    public CapiAuthNotificationComponent(StackPane container, Runnable approveAction) {
        this.approveAction = approveAction;
        this.notificationStack = NotificationStackManager.getInstance().getOrCreateStack(container);
        LocalizationService localizationService = LocalizationService.getInstance();

        this.getStyleClass().add("capi-auth-notification");
        this.setAlignment(Pos.CENTER_LEFT);
        this.setSpacing(10);
        this.setPadding(new Insets(12, 16, 12, 16));
        this.setMaxWidth(350);
        this.setMinWidth(300);
        this.setPrefWidth(320);
        this.setMaxHeight(USE_PREF_SIZE);
        this.setPrefHeight(USE_COMPUTED_SIZE);

        Label headerLabel = new Label(localizationService.getString("capi.auth.header"));
        headerLabel.getStyleClass().add("capi-auth-title");
        headerLabel.setWrapText(true);

        Label messageLabel = new Label(localizationService.getString("capi.auth.message"));
        messageLabel.getStyleClass().add("capi-auth-message");
        messageLabel.setWrapText(true);

        Button approveButton = new Button(localizationService.getString("capi.auth.approve"));
        approveButton.getStyleClass().add("capi-auth-approve-button");
        approveButton.setCursor(Cursor.HAND);
        approveButton.setOnAction(e -> {
            closeNotification();
            if (this.approveAction != null) {
                this.approveAction.run();
            }
        });

        Button closeButton = new Button(localizationService.getString("common.hide"));
        closeButton.getStyleClass().add("capi-auth-cancel-button");
        closeButton.setCursor(Cursor.HAND);
        closeButton.setOnAction(e -> closeNotification());

        HBox buttons = new HBox(10, approveButton, closeButton);
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
