package be.mirooz.elitedangerous.dashboard.view.common.overlay;

import be.mirooz.elitedangerous.dashboard.platform.windows.WindowsOverlayTopmost;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Maintient un overlay JavaFX réellement au-dessus des autres fenêtres, y compris au-dessus
 * d'Elite Dangerous (DirectX) qui peut faire perdre le statut topmost après un Alt+Tab ou
 * après un changement de mode plein écran.
 * <p>
 * Stratégie :
 * <ul>
 *     <li>{@code Stage#setAlwaysOnTop(true)} en JavaFX (déjà fait à la création) ;</li>
 *     <li>renforcement Win32 via {@link WindowsOverlayTopmost#setTopmost(Stage, boolean)}
 *     (HWND_TOPMOST + SWP_NOACTIVATE) après {@code show()} et sur les événements
 *     {@code showing}/{@code focused}/{@code iconified} du stage et de son owner ;</li>
 *     <li>réapplication périodique (toutes les 2&nbsp;s) tant que l'overlay est visible.</li>
 * </ul>
 */
public final class OverlayAlwaysOnTopSupport {

    private static final Duration REFRESH_PERIOD = Duration.seconds(2);

    private Stage attachedStage;
    private Timeline topmostRefreshTimeline;
    private ChangeListener<Boolean> stageShowingListener;
    private ChangeListener<Boolean> stageFocusListener;
    private ChangeListener<Boolean> stageIconifiedListener;
    private ChangeListener<Boolean> ownerShowingListener;
    private ChangeListener<Boolean> ownerFocusListener;
    private ChangeListener<Boolean> ownerIconifiedListener;
    private Stage ownerStage;

    /**
     * À appeler une fois après {@code overlayStage.show()}. Active la maintenance topmost
     * tant que l'overlay reste affiché. Idempotent.
     */
    public void install(Stage overlayStage) {
        if (overlayStage == null || overlayStage == attachedStage) {
            return;
        }
        dispose();
        this.attachedStage = overlayStage;

        overlayStage.setAlwaysOnTop(true);
        enforceNow();

        stageShowingListener = (obs, was, is) -> {
            if (Boolean.TRUE.equals(is)) {
                enforceSoon();
                startRefreshTimeline();
            } else {
                stopRefreshTimeline();
            }
        };
        overlayStage.showingProperty().addListener(stageShowingListener);

        stageFocusListener = (obs, was, is) -> enforceSoon();
        overlayStage.focusedProperty().addListener(stageFocusListener);

        stageIconifiedListener = (obs, was, is) -> {
            if (Boolean.FALSE.equals(is)) {
                enforceSoon();
            }
        };
        overlayStage.iconifiedProperty().addListener(stageIconifiedListener);

        if (overlayStage.getOwner() instanceof Stage owner) {
            ownerStage = owner;
            ownerShowingListener = (obs, was, is) -> enforceSoon();
            ownerFocusListener = (obs, was, is) -> enforceSoon();
            ownerIconifiedListener = (obs, was, is) -> {
                if (Boolean.FALSE.equals(is)) {
                    enforceSoon();
                }
            };
            owner.showingProperty().addListener(ownerShowingListener);
            owner.focusedProperty().addListener(ownerFocusListener);
            owner.iconifiedProperty().addListener(ownerIconifiedListener);
        }

        if (overlayStage.isShowing()) {
            startRefreshTimeline();
        }
    }

    /**
     * À appeler à la fermeture de l'overlay : retire les listeners et stoppe la timeline.
     */
    public void dispose() {
        stopRefreshTimeline();
        if (attachedStage != null) {
            if (stageShowingListener != null) {
                attachedStage.showingProperty().removeListener(stageShowingListener);
            }
            if (stageFocusListener != null) {
                attachedStage.focusedProperty().removeListener(stageFocusListener);
            }
            if (stageIconifiedListener != null) {
                attachedStage.iconifiedProperty().removeListener(stageIconifiedListener);
            }
        }
        if (ownerStage != null) {
            if (ownerShowingListener != null) {
                ownerStage.showingProperty().removeListener(ownerShowingListener);
            }
            if (ownerFocusListener != null) {
                ownerStage.focusedProperty().removeListener(ownerFocusListener);
            }
            if (ownerIconifiedListener != null) {
                ownerStage.iconifiedProperty().removeListener(ownerIconifiedListener);
            }
        }
        attachedStage = null;
        ownerStage = null;
        stageShowingListener = null;
        stageFocusListener = null;
        stageIconifiedListener = null;
        ownerShowingListener = null;
        ownerFocusListener = null;
        ownerIconifiedListener = null;
    }

    private void startRefreshTimeline() {
        if (!WindowsOverlayTopmost.isSupported() || attachedStage == null) {
            return;
        }
        if (topmostRefreshTimeline != null) {
            return;
        }
        topmostRefreshTimeline = new Timeline(new KeyFrame(REFRESH_PERIOD, e -> enforceNow()));
        topmostRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        topmostRefreshTimeline.play();
    }

    private void stopRefreshTimeline() {
        if (topmostRefreshTimeline != null) {
            topmostRefreshTimeline.stop();
            topmostRefreshTimeline = null;
        }
    }

    private void enforceSoon() {
        Platform.runLater(this::enforceNow);
    }

    private void enforceNow() {
        Stage s = attachedStage;
        if (s == null || !s.isShowing()) {
            return;
        }
        s.setAlwaysOnTop(true);
        WindowsOverlayTopmost.setTopmost(s, true);
    }
}
