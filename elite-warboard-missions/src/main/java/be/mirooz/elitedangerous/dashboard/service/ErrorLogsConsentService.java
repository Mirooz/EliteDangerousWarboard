package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.dashboard.view.common.ErrorLogsConsentNotificationComponent;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Consentement unique à l’envoi des journaux d’erreur (notification en haut à droite au premier lancement).
 */
public final class ErrorLogsConsentService {

    private ErrorLogsConsentService() {
    }

    /**
     * Affiche la question si l’utilisateur n’a pas encore répondu (thread JavaFX).
     */
    public static void promptIfNeeded(Stage owner) {
        PreferencesService prefs = PreferencesService.getInstance();
        if (prefs.isErrorLogsConsentPromptCompleted()) {
            return;
        }
        if (owner == null || owner.getScene() == null) {
            return;
        }
        Node root = owner.getScene().getRoot();
        StackPane container = findPopupContainer(root);
        if (container == null && root instanceof StackPane sp) {
            container = sp;
        }
        if (container == null) {
            return;
        }
        new ErrorLogsConsentNotificationComponent(container, accept -> {
            prefs.setSendErrorLogsEnabled(accept);
            prefs.setErrorLogsConsentPromptCompleted(true);
        });
    }

    private static StackPane findPopupContainer(Node node) {
        if (node == null) {
            return null;
        }
        if (node instanceof StackPane stackPane && "popupContainer".equals(node.getId())) {
            return stackPane;
        }
        if (node instanceof Pane pane) {
            for (Node child : pane.getChildren()) {
                StackPane found = findPopupContainer(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
