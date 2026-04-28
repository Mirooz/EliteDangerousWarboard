package be.mirooz.elitedangerous.dashboard.view.common;

import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.view.common.managers.NotificationStackManager;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Affiche l’URL d’OAuth Frontier lorsque ni {@code Desktop.browse} ni le lanceur système
 * ({@code xdg-open}, etc.) n’ont pu ouvrir le navigateur.
 */
public class CapiAuthBrowserFallbackNotificationComponent extends VBox {

    private final VBox notificationStack;

    public CapiAuthBrowserFallbackNotificationComponent(StackPane container, String loginUrl) {
        this.notificationStack = NotificationStackManager.getInstance().getOrCreateStack(container);
        LocalizationService localizationService = LocalizationService.getInstance();

        this.getStyleClass().add("capi-auth-notification");
        this.setAlignment(Pos.CENTER_LEFT);
        this.setSpacing(10);
        this.setPadding(new Insets(12, 16, 12, 16));
        this.setMaxWidth(440);
        this.setPrefWidth(420);
        this.setMaxHeight(USE_PREF_SIZE);
        this.setPrefHeight(USE_COMPUTED_SIZE);

        Label titleLabel = new Label(localizationService.getString("capi.auth.browser.fallback.title"));
        titleLabel.getStyleClass().add("capi-auth-title");
        titleLabel.setWrapText(true);

        TextField urlField = new TextField(loginUrl);
        urlField.setEditable(false);
        urlField.getStyleClass().add("capi-auth-url-field");
        urlField.setMaxWidth(Double.MAX_VALUE);

        Button copyButton = new Button(localizationService.getString("capi.auth.browser.fallback.copy"));
        copyButton.getStyleClass().add("capi-auth-approve-button");
        copyButton.setCursor(Cursor.HAND);
        copyButton.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(loginUrl);
            Clipboard.getSystemClipboard().setContent(content);
        });

        Button closeButton = new Button(localizationService.getString("capi.auth.browser.fallback.close"));
        closeButton.getStyleClass().add("capi-auth-cancel-button");
        closeButton.setCursor(Cursor.HAND);
        closeButton.setOnAction(e -> closeNotification());

        HBox buttons = new HBox(10, copyButton, closeButton);
        buttons.setAlignment(Pos.CENTER_LEFT);

        this.getChildren().addAll(titleLabel, urlField, buttons);

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
