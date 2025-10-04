package be.mirooz.elitedangerous.dashboard.controller.ui.manager;

import be.mirooz.elitedangerous.dashboard.controller.Refreshable;
import javafx.application.Platform;
import org.reflections.Reflections;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class UIManager {
    private static final UIManager INSTANCE = new UIManager();

    private final List<Refreshable> refreshables = new ArrayList<>();

    private UIManager() {}

    public static UIManager getInstance() {
        return INSTANCE;
    }

    public void register(Refreshable refreshable) {
        if (!refreshables.contains(refreshable)) {
            refreshables.add(refreshable);
        }
    }

    public void unregister(Refreshable refreshable) {
        refreshables.remove(refreshable);
    }

    public void refreshAllUI() {
        Platform.runLater(() -> {
            for (Refreshable r : refreshables) {
                try {
                    r.refreshUI();
                } catch (Exception e) {
                    System.err.println("[UIRegisterContainer] Erreur refresh " + r.getClass().getSimpleName() + " : " + e.getMessage());
                }
            }
        });
    }
}
