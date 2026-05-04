package be.mirooz.elitedangerous.dashboard.window;

import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.service.PreferencesService;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Barre titre custom (sans décor OS) : drag, min / max / fermer, double-clic plein écran.
 * <p>
 * Maximiser = remplir la zone <em>utilisable</em> de l’écran ({@link StageVisualBounds}), pas
 * {@link Stage#setMaximized(boolean)} (souvent plein moniteur y compris barre des tâches en undecorated).
 */
public final class PrimaryWindowChromeSupport {

    private static final double DRAG_UNMAX_THRESHOLD = 5.0;
    private static final double WORK_AREA_MATCH_EPS = 4.0;

    private final Stage stage;
    private final HBox windowChromeBar;
    private final HBox windowChromeLeft;
    private final HBox dashboardTitleBar;
    private final Label windowChromeTitleLabel;
    private final Button windowMinimizeButton;
    private final Button windowMaxRestoreButton;
    private final Button windowCloseButton;
    private final Node configButton;
    private final ImageView donateButtonImage;
    private final Label systemLabel;
    private final LocalizationService localizationService;

    private final AtomicBoolean installed = new AtomicBoolean();

    private double dragOffsetX;
    private double dragOffsetY;
    private double restoreWinX = 100;
    private double restoreWinY = 100;
    private double restoreWinW = 1200;
    private double restoreWinH = 800;

    /** Clic sur barre en plein écran : attend un vrai glisser avant de restaurer la taille (évite le « 3e clic »). */
    private boolean maximizedPressTracking;
    private double maximizedPressSceneX;
    private double maximizedPressSceneY;

    public PrimaryWindowChromeSupport(
            Stage stage,
            HBox windowChromeBar,
            HBox windowChromeLeft,
            HBox dashboardTitleBar,
            Label windowChromeTitleLabel,
            Button windowMinimizeButton,
            Button windowMaxRestoreButton,
            Button windowCloseButton,
            Node configButton,
            ImageView donateButtonImage,
            Label systemLabel,
            LocalizationService localizationService) {
        this.stage = Objects.requireNonNull(stage, "stage");
        this.windowChromeBar = windowChromeBar;
        this.windowChromeLeft = windowChromeLeft;
        this.dashboardTitleBar = Objects.requireNonNull(dashboardTitleBar, "dashboardTitleBar");
        this.windowChromeTitleLabel = windowChromeTitleLabel;
        this.windowMinimizeButton = windowMinimizeButton;
        this.windowMaxRestoreButton = windowMaxRestoreButton;
        this.windowCloseButton = windowCloseButton;
        this.configButton = configButton;
        this.donateButtonImage = donateButtonImage;
        this.systemLabel = systemLabel;
        this.localizationService = Objects.requireNonNull(localizationService, "localizationService");
    }

    /** Une seule installation effective. */
    public void installIfNeeded() {
        if (!installed.compareAndSet(false, true)) {
            return;
        }
        if (WindowFramePreferences.useNativeOsWindowFrame()) {
            if (windowChromeBar != null) {
                windowChromeBar.setVisible(false);
                windowChromeBar.setManaged(false);
            }
            return;
        }
        if (windowMinimizeButton != null) {
            windowMinimizeButton.setFocusTraversable(false);
            windowMinimizeButton.setText(null);
            windowMinimizeButton.setGraphic(WindowChromeIcons.minimize());
        }
        if (windowMaxRestoreButton != null) {
            windowMaxRestoreButton.setFocusTraversable(false);
            windowMaxRestoreButton.setText(null);
        }
        if (windowCloseButton != null) {
            windowCloseButton.setFocusTraversable(false);
            windowCloseButton.setText(null);
            windowCloseButton.setGraphic(WindowChromeIcons.close());
        }

        if (windowChromeLeft != null) {
            installWindowDragHandlers(windowChromeLeft, false);
            windowChromeLeft.addEventFilter(MouseEvent.MOUSE_CLICKED, this::onWindowHeaderDoubleClick);
        }
        installWindowDragHandlers(dashboardTitleBar, true);
        dashboardTitleBar.addEventFilter(MouseEvent.MOUSE_CLICKED, this::onWindowHeaderDoubleClick);

        seedRestoreBoundsFromPreferencesIfMaximized();
        syncWindowMaxRestoreGlyph();
        refreshLocalizedStrings();
    }

    public void refreshLocalizedStrings() {
        if (windowChromeTitleLabel != null) {
            windowChromeTitleLabel.setText(localizationService.getString("app.title"));
        }
        if (windowMinimizeButton == null || windowMaxRestoreButton == null || windowCloseButton == null) {
            return;
        }
        windowMinimizeButton.setTooltip(new Tooltip(localizationService.getString("window.minimize")));
        windowCloseButton.setTooltip(new Tooltip(localizationService.getString("window.close")));
        syncWindowMaxRestoreGlyph();
    }

    public void minimize() {
        stage.setIconified(true);
    }

    public void toggleMaximized() {
        if (isWorkAreaMaximized()) {
            stage.setMaximized(false);
            stage.setX(restoreWinX);
            stage.setY(restoreWinY);
            stage.setWidth(Math.max(stage.getMinWidth(), restoreWinW));
            stage.setHeight(Math.max(stage.getMinHeight(), restoreWinH));
        } else {
            rememberRestoreBounds();
            StageVisualBounds.fitStageToVisualBounds(stage);
        }
        syncWindowMaxRestoreGlyph();
    }

    /** {@link Stage#close()} n’envoie pas {@code setOnCloseRequest} ; on propage l’événement attendu. */
    public void close() {
        Event.fireEvent(stage, new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
    }

    private boolean isWorkAreaMaximized() {
        return StageVisualBounds.isStageFillingWorkArea(stage, WORK_AREA_MATCH_EPS);
    }

    private void syncWindowMaxRestoreGlyph() {
        if (windowMaxRestoreButton == null) {
            return;
        }
        if (isWorkAreaMaximized()) {
            windowMaxRestoreButton.setGraphic(WindowChromeIcons.restore());
            windowMaxRestoreButton.setTooltip(new Tooltip(localizationService.getString("window.restore")));
        } else {
            windowMaxRestoreButton.setGraphic(WindowChromeIcons.maximize());
            windowMaxRestoreButton.setTooltip(new Tooltip(localizationService.getString("window.maximize")));
        }
    }

    private void installWindowDragHandlers(Node dragHost, boolean titleBarStrip) {
        dragHost.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getButton() != MouseButton.PRIMARY) {
                return;
            }
            if (titleBarStrip && isTitleBarDragExcluded(e.getTarget())) {
                return;
            }
            rememberRestoreBounds();
            if (isWorkAreaMaximized()) {
                maximizedPressTracking = true;
                maximizedPressSceneX = e.getSceneX();
                maximizedPressSceneY = e.getSceneY();
            } else {
                maximizedPressTracking = false;
                dragOffsetX = e.getSceneX();
                dragOffsetY = e.getSceneY();
            }
        });
        dragHost.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            if (!e.isPrimaryButtonDown()) {
                return;
            }
            if (titleBarStrip && isTitleBarDragExcluded(e.getTarget())) {
                return;
            }
            if (isWorkAreaMaximized()) {
                if (maximizedPressTracking) {
                    double dx = e.getSceneX() - maximizedPressSceneX;
                    double dy = e.getSceneY() - maximizedPressSceneY;
                    if (dx * dx + dy * dy > DRAG_UNMAX_THRESHOLD * DRAG_UNMAX_THRESHOLD) {
                        unmaximizeForDrag(e);
                        maximizedPressTracking = false;
                    }
                }
                return;
            }
            stage.setX(e.getScreenX() - dragOffsetX);
            stage.setY(e.getScreenY() - dragOffsetY);
        });
        dragHost.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            if (e.getButton() != MouseButton.PRIMARY) {
                return;
            }
            if (isWorkAreaMaximized()) {
                maximizedPressTracking = false;
            }
        });
    }

    private void rememberRestoreBounds() {
        if (!isWorkAreaMaximized()) {
            restoreWinW = stage.getWidth();
            restoreWinH = stage.getHeight();
            restoreWinX = stage.getX();
            restoreWinY = stage.getY();
        }
    }

    private void seedRestoreBoundsFromPreferencesIfMaximized() {
        if (!StageVisualBounds.isStageFillingWorkArea(stage, WORK_AREA_MATCH_EPS)) {
            return;
        }
        PreferencesService ps = PreferencesService.getInstance();
        if (!Boolean.parseBoolean(ps.getPreference("window.maximized", "false"))) {
            return;
        }
        String sx = ps.getPreference("window.x", null);
        String sy = ps.getPreference("window.y", null);
        String sw = ps.getPreference("window.width", null);
        String sh = ps.getPreference("window.height", null);
        if (sx == null || sy == null || sw == null || sh == null) {
            return;
        }
        try {
            double w = Double.parseDouble(sw);
            double h = Double.parseDouble(sh);
            if (w > 0 && h > 0) {
                restoreWinX = Double.parseDouble(sx);
                restoreWinY = Double.parseDouble(sy);
                restoreWinW = w;
                restoreWinH = h;
            }
        } catch (NumberFormatException ignored) {
            // garder les valeurs par défaut
        }
    }

    private void unmaximizeForDrag(MouseEvent e) {
        double sceneW = stage.getScene().getWidth();
        double sceneH = stage.getScene().getHeight();
        if (sceneW <= 0 || sceneH <= 0) {
            return;
        }
        double ratioX = e.getSceneX() / sceneW;
        double ratioY = e.getSceneY() / sceneH;
        double w = Math.max(stage.getMinWidth(), restoreWinW);
        double h = Math.max(stage.getMinHeight(), restoreWinH);
        stage.setMaximized(false);
        stage.setWidth(w);
        stage.setHeight(h);
        dragOffsetX = ratioX * w;
        dragOffsetY = ratioY * h;
        stage.setX(e.getScreenX() - dragOffsetX);
        stage.setY(e.getScreenY() - dragOffsetY);
    }

    private void onWindowHeaderDoubleClick(MouseEvent e) {
        if (e.getButton() != MouseButton.PRIMARY || e.getClickCount() != 2) {
            return;
        }
        if (e.getSource() == dashboardTitleBar && isTitleBarDragExcluded(e.getTarget())) {
            return;
        }
        toggleMaximized();
    }

    private boolean isTitleBarDragExcluded(Object target) {
        if (!(target instanceof Node n)) {
            return false;
        }
        for (Node c = n; c != null; c = c.getParent()) {
            if (c == configButton || c == donateButtonImage || c == systemLabel) {
                return true;
            }
        }
        return false;
    }
}
