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
    private final Runnable declineAction;
    private final VBox notificationStack;

    public CapiAuthNotificationComponent(StackPane container, Runnable approveAction, Runnable declineAction) {
        this.approveAction = approveAction;
        this.declineAction = declineAction;
        this.notificationStack = NotificationStackManager.getInstance().getOrCreateStack(container);
        LocalizationService localizationService = LocalizationService.getInstance();

        this.getStyleClass().add("capi-auth-notification");
        this.setAlignment(Pos.CENTER_LEFT);
        this.setSpacing(10);
        this.setPadding(new Insets(12, 16, 12, 16));
        this.setMaxWidth(350);
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

        Button declineButton = new Button(localizationService.getString("capi.auth.decline"));
        declineButton.getStyleClass().add("capi-auth-cancel-button");
        declineButton.setCursor(Cursor.HAND);
        declineButton.setOnAction(e -> {
            closeNotification();
            if (this.declineAction != null) {
                this.declineAction.run();
            }
        });

        HBox buttons = new HBox(10, approveButton, declineButton);
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
