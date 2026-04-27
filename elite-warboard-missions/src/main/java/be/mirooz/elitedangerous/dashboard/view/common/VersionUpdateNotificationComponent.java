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

import java.awt.Desktop;
import java.net.URI;

/**
 * Composant de notification pour afficher une nouvelle version disponible
 * S'affiche en haut à droite de l'application
 */
public class VersionUpdateNotificationComponent extends VBox {
    
    private final String downloadUrl;
    private final LocalizationService localizationService = LocalizationService.getInstance();
    private final VBox notificationStack;
    
    public VersionUpdateNotificationComponent(String latestVersion, String releaseBody, String downloadUrl, StackPane container) {
        this.downloadUrl = downloadUrl;
        this.notificationStack = NotificationStackManager.getInstance().getOrCreateStack(container);
        
        // Style du conteneur principal
        this.getStyleClass().add("version-update-notification");
        this.setAlignment(Pos.CENTER_LEFT);
        this.setSpacing(8);
        this.setPadding(new Insets(12, 16, 12, 16));
        this.setMaxWidth(350);
        this.setPrefWidth(320);
        // Limiter la hauteur pour éviter qu'il prenne toute la hauteur
        this.setMaxHeight(USE_PREF_SIZE);
        this.setPrefHeight(USE_COMPUTED_SIZE);
        
        // Message principal avec traduction
        String message = localizationService.getString("version.new_available", latestVersion);
        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("version-notification-message");
        messageLabel.setWrapText(true);

        String detailMessage = (releaseBody != null && !releaseBody.isBlank())
                ? releaseBody
                : localizationService.getString("version.notification.message");
        Label detailLabel = new Label(detailMessage);
        detailLabel.getStyleClass().add("version-notification-detail");
        detailLabel.setWrapText(true);
        
        // Bouton Download avec traduction
        Button downloadButton = new Button(localizationService.getString("version.download_now"));
        downloadButton.getStyleClass().add("version-download-button");
        downloadButton.setCursor(Cursor.HAND);
        downloadButton.setOnAction(e -> openDownloadUrl());
        
        Button hideButton = new Button(localizationService.getString("common.hide"));
        hideButton.getStyleClass().add("version-hide-button");
        hideButton.setCursor(Cursor.HAND);
        hideButton.setOnAction(e -> closeNotification());
        
        // Conteneur pour les boutons
        HBox buttonsContainer = new HBox(8);
        buttonsContainer.setAlignment(Pos.CENTER_LEFT);
        buttonsContainer.getChildren().addAll(downloadButton, hideButton);
        
        // Ajouter les éléments
        this.getChildren().addAll(messageLabel, detailLabel, buttonsContainer);
        
        // Ajouter au conteneur
        notificationStack.getChildren().add(this);
        
        // Animation d'apparition
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), this);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }
    
    /**
     * Ouvre l'URL de téléchargement dans le navigateur
     */
    private void openDownloadUrl() {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(downloadUrl));
            } else {
                System.err.println("Impossible d'ouvrir le navigateur pour: " + downloadUrl);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de l'ouverture de l'URL: " + e.getMessage());
        }
    }
    
    /**
     * Ferme la notification avec animation
     */
    private void closeNotification() {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), this);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> notificationStack.getChildren().remove(this));
        fadeOut.play();
    }
}
