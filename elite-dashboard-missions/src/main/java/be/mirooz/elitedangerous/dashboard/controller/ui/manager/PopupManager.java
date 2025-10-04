package be.mirooz.elitedangerous.dashboard.controller.ui.manager;

import be.mirooz.elitedangerous.dashboard.controller.ui.component.PopUpComponent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.HashMap;
import java.util.Map;

public class PopupManager {
    private static final PopupManager INSTANCE = new PopupManager();
    private final Map<Window, StackPane> containers = new HashMap<>();

    private PopupManager() {}

    public static PopupManager getInstance() {
        return INSTANCE;
    }
    public void attachToContainer(StackPane popupContainer) {
        popupContainer.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((o, oldWindow, newWindow) -> {
                    if (newWindow instanceof Stage stage) {
                        // Enregistrer
                        registerContainer(stage, popupContainer);

                        // Nettoyer à la fermeture
                        stage.setOnHidden(e -> unregisterContainer(stage));
                    }
                });
            }
        });
    }
    public void registerContainer(Window window, StackPane container) {
        containers.put(window, container);
    }
    public void unregisterContainer(Stage stage) {
        containers.remove(stage);
    }
    public void showPopup(String message, double x, double y, Window window) {
        StackPane container = containers.get(window);
        if (container == null) {
            throw new IllegalStateException("Pas de container enregistré pour cette fenêtre !");
        }
        new PopUpComponent(message, x, y, container);
    }
}
