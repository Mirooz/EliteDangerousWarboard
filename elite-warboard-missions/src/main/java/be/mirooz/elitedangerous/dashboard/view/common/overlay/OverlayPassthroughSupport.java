package be.mirooz.elitedangerous.dashboard.view.common.overlay;

import be.mirooz.elitedangerous.dashboard.platform.windows.WindowsOverlayClickThrough;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Verrou « clic à travers » partagé par les fenêtres overlay : {@code mouseTransparent} sur la racine
 * + styles Win32 ({@link WindowsOverlayClickThrough}), avec nouvelles passes après affichage Glass.
 */
public final class OverlayPassthroughSupport {

    private Timeline windowsPassthroughRefreshTimeline;
    private boolean clickThroughLocked;

    public boolean isClickThroughLocked() {
        return clickThroughLocked;
    }

    public void setClickThroughLocked(boolean locked, Stage stage, StackPane stackPane) {
        this.clickThroughLocked = locked;
        if (stackPane != null) {
            stackPane.setMouseTransparent(locked);
        }
        if (stage != null && WindowsOverlayClickThrough.isSupported()) {
            scheduleWindowsPassthroughPasses(stage, locked);
        }
    }

    /** À rappeler après remplacement du contenu pour conserver l’état du verrou. */
    public void reapplyLockedState(Stage stage, StackPane stackPane) {
        setClickThroughLocked(clickThroughLocked, stage, stackPane);
    }

    public void disposeForClose(Stage stage, StackPane stackPane) {
        if (windowsPassthroughRefreshTimeline != null) {
            windowsPassthroughRefreshTimeline.stop();
            windowsPassthroughRefreshTimeline = null;
        }
        clickThroughLocked = false;
        if (stackPane != null) {
            stackPane.setMouseTransparent(false);
        }
        if (stage != null && WindowsOverlayClickThrough.isSupported()) {
            WindowsOverlayClickThrough.setMousePassthrough(stage, false);
        }
    }

    private void scheduleWindowsPassthroughPasses(Stage s, boolean locked) {
        if (windowsPassthroughRefreshTimeline != null) {
            windowsPassthroughRefreshTimeline.stop();
            windowsPassthroughRefreshTimeline = null;
        }
        Runnable once = () -> {
            if (s != null && s.isShowing()) {
                WindowsOverlayClickThrough.setMousePassthrough(s, locked);
            }
        };
        Platform.runLater(() -> {
            once.run();
            Platform.runLater(once);
        });
        windowsPassthroughRefreshTimeline = new Timeline(
                new KeyFrame(Duration.millis(80), e -> once.run()),
                new KeyFrame(Duration.millis(220), e -> once.run()),
                new KeyFrame(Duration.millis(500), e -> once.run()));
        windowsPassthroughRefreshTimeline.setOnFinished(e -> windowsPassthroughRefreshTimeline = null);
        windowsPassthroughRefreshTimeline.play();
    }
}
