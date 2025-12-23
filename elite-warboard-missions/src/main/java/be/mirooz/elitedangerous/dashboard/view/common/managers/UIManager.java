package be.mirooz.elitedangerous.dashboard.view.common.managers;

import be.mirooz.elitedangerous.dashboard.view.common.IRefreshable;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.List;

public class UIManager {
    private static final UIManager INSTANCE = new UIManager();

    private final List<IRefreshable> IRefreshables = new ArrayList<>();

    private UIManager() {}

    public static UIManager getInstance() {
        return INSTANCE;
    }

    public void register(IRefreshable IRefreshable) {
        if (!IRefreshables.contains(IRefreshable)) {
            IRefreshables.add(IRefreshable);
        }
    }

    public void unregister(IRefreshable IRefreshable) {
        IRefreshables.remove(IRefreshable);
    }

    public void refreshAllUI() {
        Platform.runLater(() -> {
            for (IRefreshable r : IRefreshables) {
                try {
                    r.refreshUI();
                } catch (Exception e) {
                    System.err.println("[UIRegisterContainer] Erreur refresh " + r.getClass().getSimpleName() + " : " + e.getMessage());
                }
            }
        });
    }
}
