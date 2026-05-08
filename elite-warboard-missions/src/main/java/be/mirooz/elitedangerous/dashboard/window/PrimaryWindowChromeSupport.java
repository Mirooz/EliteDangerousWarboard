package be.mirooz.elitedangerous.dashboard.window;

import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.service.PreferencesService;
import javafx.animation.PauseTransition;
import javafx.beans.InvalidationListener;
import javafx.event.Event;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
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
import javafx.util.Duration;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Fenêtre sans décor OS : drag et double-clic sur la barre titre dashboard, boutons min / max / fermer
 * (dans la barre titre). La fine barre « chrome » séparée est optionnelle (FXML : null).
 * <p>
 * Maximiser = remplir la zone <em>utilisable</em> de l’écran ({@link StageVisualBounds}), pas
 * {@link Stage#setMaximized(boolean)} (souvent plein moniteur y compris barre des tâches en undecorated).
 */
public final class PrimaryWindowChromeSupport {

    private static final double WORK_AREA_MATCH_EPS = 4.0;
    /** Marge pour considérer que la fenêtre n’est plus « pleine zone utile » (resize manuel, multi-écran). */
    private static final double WORK_AREA_PARTIAL_MARGIN = 24.0;
    private static final int GLYPH_LAYOUT_SYNC_DELAY_MS = 50;

    private final Stage stage;
    private final HBox windowChromeBar;
    private final HBox windowChromeLeft;
    private final HBox dashboardTitleBar;
    /** Optionnel (barre chrome retirée : titre uniquement dans la barre dashboard). */
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
    /**
     * Aligné sur {@code window.maximized} au démarrage et sur les actions max / restore : l’heuristique
     * {@link StageVisualBounds#isStageFillingWorkArea} peut être fausse (2ᵉ écran, DPI, Win32 runLater).
     */
    private boolean chromeWorkAreaExpanded;

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
        /* Main ouverte (pan / drag), comme bodiesScrollPane dans SystemVisualViewComponent — pas équivalent en -fx-cursor CSS. */
        dashboardTitleBar.setCursor(Cursor.OPEN_HAND);
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
        }
        if (windowChromeBar != null) {
            windowChromeBar.addEventFilter(MouseEvent.MOUSE_CLICKED, this::onChromeBarDoubleClick);
        }
        installWindowDragHandlers(dashboardTitleBar, true);
        dashboardTitleBar.addEventFilter(MouseEvent.MOUSE_CLICKED, this::onDashboardTitleBarDoubleClick);

        seedRestoreBoundsFromPreferences();
        boolean prefMaximized = Boolean.parseBoolean(
                PreferencesService.getInstance().getPreference("window.maximized", "false"));
        // Si window.maximized=true au premier runLater, le stage peut encore être à la taille de scène :
        // rememberRestoreBounds() écraserait les x/y/l/h issus des prefs — le bouton restaurer ignorerait alors
        // la géométrie sauvegardée après redémarrage.
        if (!prefMaximized && !isWorkAreaMaximized()) {
            rememberRestoreBounds();
        }
        chromeWorkAreaExpanded = prefMaximized;
        attachStageGeometryGlyphSync();
        syncWindowMaxRestoreGlyph();
        refreshLocalizedStrings();
        if (prefMaximized || isWorkAreaMaximized()) {
            scheduleSyncGlyphAfterLayout();
        }
    }

    /**
     * Recalcule l’icône max / restore quand la géométrie réelle du stage change (ex. déplacement hors
     * zone utilisable alors que l’UI pensait encore « plein écran »).
     */
    private void attachStageGeometryGlyphSync() {
        InvalidationListener l = obs -> onStageGeometryChanged();
        stage.xProperty().addListener(l);
        stage.yProperty().addListener(l);
        stage.widthProperty().addListener(l);
        stage.heightProperty().addListener(l);
        stage.maximizedProperty().addListener(l);
        stage.iconifiedProperty().addListener(l);
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
        boolean expanded = isWorkAreaMaximized() || chromeWorkAreaExpanded;
        if (expanded) {
            chromeWorkAreaExpanded = false;
            stage.setMaximized(false);
            stage.setX(restoreWinX);
            stage.setY(StageVisualBounds.clampStageYNonNegative(restoreWinY));
            stage.setWidth(Math.max(stage.getMinWidth(), restoreWinW));
            stage.setHeight(Math.max(stage.getMinHeight(), restoreWinH));
        } else {
            rememberRestoreBounds();
            StageVisualBounds.fitStageToVisualBounds(stage);
            chromeWorkAreaExpanded = true;
        }
        syncWindowMaxRestoreGlyph();
        scheduleSyncGlyphAfterLayout();
        PreferencesService.getInstance()
                .setPreference("window.maximized", String.valueOf(chromeWorkAreaExpanded));
    }

    private void scheduleSyncGlyphAfterLayout() {
        var pause = new PauseTransition(Duration.millis(GLYPH_LAYOUT_SYNC_DELAY_MS));
        pause.setOnFinished(e -> syncWindowMaxRestoreGlyph());
        pause.play();
    }

    /** {@link Stage#close()} n’envoie pas {@code setOnCloseRequest} ; on propage l’événement attendu. */
    public void close() {
        Event.fireEvent(stage, new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
    }

    private boolean isWorkAreaMaximized() {
        return StageVisualBounds.isStageFillingWorkArea(stage, WORK_AREA_MATCH_EPS);
    }

    private boolean isWorkAreaExpandedForChrome() {
        return isWorkAreaMaximized() || chromeWorkAreaExpanded;
    }

    private void onStageGeometryChanged() {
        if (!isWorkAreaMaximized() && chromeWorkAreaExpanded) {
            Rectangle2D vb = StageVisualBounds.screenForWindowCenter(stage).getVisualBounds();
            boolean clearlyPartial = stage.getWidth() < vb.getWidth() - WORK_AREA_PARTIAL_MARGIN
                    || stage.getHeight() < vb.getHeight() - WORK_AREA_PARTIAL_MARGIN;
            if (clearlyPartial) {
                chromeWorkAreaExpanded = false;
            }
        }
        syncWindowMaxRestoreGlyph();
    }

    private void syncWindowMaxRestoreGlyph() {
        if (windowMaxRestoreButton == null) {
            return;
        }
        if (isWorkAreaExpandedForChrome()) {
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
            if (isWorkAreaExpandedForChrome()) {
                return;
            }
            rememberRestoreBounds();
            dragOffsetX = e.getSceneX();
            dragOffsetY = e.getSceneY();
        });
        dragHost.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            if (!e.isPrimaryButtonDown()) {
                return;
            }
            if (titleBarStrip && isTitleBarDragExcluded(e.getTarget())) {
                return;
            }
            if (isWorkAreaExpandedForChrome()) {
                return;
            }
            stage.setX(e.getScreenX() - dragOffsetX);
            stage.setY(StageVisualBounds.clampStageYNonNegative(e.getScreenY() - dragOffsetY));
        });
    }

    private void rememberRestoreBounds() {
        if (!isWorkAreaMaximized()) {
            restoreWinW = stage.getWidth();
            restoreWinH = stage.getHeight();
            restoreWinX = stage.getX();
            restoreWinY = StageVisualBounds.clampStageYNonNegative(stage.getY());
        }
    }

    /**
     * Initialise {@link #restoreWinX}… depuis les préférences. Toujours appelé (même si
     * {@code window.maximized=false}) : sinon, une fenêtre qui remplit encore la zone utile alors que la
     * préférence est passée à {@code false} gardait les défauts 100×1200 et le bouton « restaurer » sautait
     * n’importe où.
     * <p>
     * Avec {@code window.maximized=true}, ces bornes sont celles utilisées au démaximize après relance
     * (la géométrie « fenêtrée » n’est plus écrasée par une capture du stage encore non calé).
     */
    private void seedRestoreBoundsFromPreferences() {
        PreferencesService ps = PreferencesService.getInstance();
        String sx = ps.getPreference("window.x", null);
        String sy = ps.getPreference("window.y", null);
        String sw = ps.getPreference("window.width", null);
        String sh = ps.getPreference("window.height", null);
        if (sx == null || sy == null || sw == null || sh == null) {
            return;
        }
        try {
            double rx = Double.parseDouble(sx);
            double ry = Double.parseDouble(sy);
            double w = Double.parseDouble(sw);
            double h = Double.parseDouble(sh);
            if (w <= 0 || h <= 0) {
                return;
            }
            restoreWinX = rx;
            restoreWinY = StageVisualBounds.clampStageYNonNegative(ry);
            restoreWinW = w;
            restoreWinH = h;
        } catch (NumberFormatException ignored) {
            // garder les valeurs par défaut
        }
    }

    /** Double-clic sur la barre titre OS custom (ligne tout en haut), hors boutons min / max / fermer. */
    private void onChromeBarDoubleClick(MouseEvent e) {
        if (e.getButton() != MouseButton.PRIMARY || e.getClickCount() != 2) {
            return;
        }
        if (isUnderWindowControls(e.getTarget())) {
            return;
        }
        toggleMaximized();
    }

    private void onDashboardTitleBarDoubleClick(MouseEvent e) {
        if (e.getButton() != MouseButton.PRIMARY || e.getClickCount() != 2) {
            return;
        }
        if (isUnderWindowControls(e.getTarget())) {
            return;
        }
        if (isTitleBarDragExcluded(e.getTarget())) {
            return;
        }
        toggleMaximized();
    }

    private boolean isUnderWindowControls(Object target) {
        if (!(target instanceof Node n)) {
            return false;
        }
        for (Node c = n; c != null; c = c.getParent()) {
            if (c == windowMinimizeButton || c == windowMaxRestoreButton || c == windowCloseButton) {
                return true;
            }
        }
        return false;
    }

    private boolean isTitleBarDragExcluded(Object target) {
        if (!(target instanceof Node n)) {
            return false;
        }
        for (Node c = n; c != null; c = c.getParent()) {
            if (c == configButton || c == donateButtonImage || c == systemLabel) {
                return true;
            }
            if (c == windowMinimizeButton || c == windowMaxRestoreButton || c == windowCloseButton) {
                return true;
            }
        }
        return false;
    }
}
