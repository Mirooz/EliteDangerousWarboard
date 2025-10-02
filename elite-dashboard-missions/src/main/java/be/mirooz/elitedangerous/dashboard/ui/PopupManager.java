package be.mirooz.elitedangerous.dashboard.ui;

import be.mirooz.elitedangerous.dashboard.ui.component.PopUpComponent;
import javafx.scene.layout.StackPane;

public class PopupManager {
    private static PopupManager instance;
    private StackPane container;

    private PopupManager() {}

    public static PopupManager getInstance() {
        if (instance == null) {
            instance = new PopupManager();
        }
        return instance;
    }

    public void setContainer(StackPane container) {
        this.container = container;
    }

    public void showPopup(String message, double x, double y) {
        if (this.container == null) return;
        new PopUpComponent(message, x, y, this.container);
    }
    public void showPopup(String message, double x, double y, StackPane container) {
        if (container == null) return;
        new PopUpComponent(message, x, y, container);
    }

}
