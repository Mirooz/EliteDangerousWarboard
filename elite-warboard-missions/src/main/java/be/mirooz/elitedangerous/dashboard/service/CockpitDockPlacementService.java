package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.dashboard.overlay.panel.CockpitLeftPanelGeometry;
import be.mirooz.elitedangerous.dashboard.overlay.panel.PanelCorners;
import be.mirooz.elitedangerous.dashboard.overlay.panel.PanelPerspective;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.effect.PerspectiveTransform;
import javafx.scene.layout.Background;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * <strong>X</strong> : place la fenêtre sur l’écran du jeu (voir {@link #resolveGameScreen()}), cadre = boîte englobante du panneau, contenu en perspective ;
 * scène, fond du {@link StackPane} racine et fond du {@link javafx.scene.layout.BorderPane} dashboard
 * transparents pour laisser voir le jeu dans les marges du trapèze (le noir venait surtout du dashboard).
 * La fenêtre passe en {@link Stage#setAlwaysOnTop(boolean) alwaysOnTop} jusqu’à <strong>C</strong> (valeur précédente restaurée).
 * <p>
 * <strong>C</strong> : restaure fond, géométrie, écran d’origine et always-on-top.
 */
public final class CockpitDockPlacementService {

    private static final CockpitDockPlacementService INSTANCE = new CockpitDockPlacementService();

    private final ReadOnlyBooleanWrapper dockedMode = new ReadOnlyBooleanWrapper(false);
    private int backupScreenIndex;
    private double backupX;
    private double backupY;
    private double backupWidth;
    private double backupHeight;
    private boolean backupMaximized;
    private boolean backupAlwaysOnTop;
    private Paint backupSceneFill;
    private Background backupRootBackground;
    /** Fond du premier enfant du root (BorderPane dashboard), sauvegardé avant le dock. */
    private Background backupMainDashboardBackground;

    private CockpitDockPlacementService() {
    }

    public static CockpitDockPlacementService getInstance() {
        return INSTANCE;
    }

    public void onDockKeyPressed() {
        if (WindowToggleService.getInstance().isShortcutsPaused()) {
            return;
        }
        Stage stage = WindowToggleService.getInstance().getMainStage();
        if (stage == null) {
            return;
        }
        Platform.runLater(() -> {
            if (dockedMode.get()) {
                return;
            }
            backupScreenIndex = screenIndexContainingStage(stage);
            backupMaximized = stage.isMaximized();
            backupAlwaysOnTop = stage.isAlwaysOnTop();
            backupX = stage.getX();
            backupY = stage.getY();
            backupWidth = stage.getWidth();
            backupHeight = stage.getHeight();

            Scene scene = stage.getScene();
            StackPane root = WindowToggleService.getInstance().getRootPane();
            if (scene != null) {
                backupSceneFill = scene.getFill();
            }
            if (root != null) {
                backupRootBackground = root.getBackground();
                Region main = mainDashboardContent(root);
                backupMainDashboardBackground = main != null ? main.getBackground() : null;
            }

            dockedMode.set(true);
            applyCockpitDock(stage);
            stage.setAlwaysOnTop(true);
        });
    }

    public void onCancelDockPressed() {
        if (WindowToggleService.getInstance().isShortcutsPaused()) {
            return;
        }
        Stage stage = WindowToggleService.getInstance().getMainStage();
        if (stage == null) {
            return;
        }
        Platform.runLater(() -> {
            if (!dockedMode.get()) {
                return;
            }
            dockedMode.set(false);
            stage.setAlwaysOnTop(backupAlwaysOnTop);
            StackPane root = WindowToggleService.getInstance().getRootPane();
            Scene scene = stage.getScene();
            if (root != null) {
                root.setEffect(null);
                root.setCache(false);
                root.getStyleClass().remove("cockpit-dock-scene-root");
                root.setBackground(backupRootBackground);
                Region main = mainDashboardContent(root);
                if (main != null) {
                    main.setBackground(backupMainDashboardBackground);
                }
            }
            if (scene != null) {
                scene.setFill(backupSceneFill != null ? backupSceneFill : Color.web("#0d0d0d"));
            }

            if (backupMaximized) {
                restoreMaximizedOnScreen(stage, backupScreenIndex);
            } else {
                stage.setMaximized(false);
                stage.setX(backupX);
                stage.setY(backupY);
                stage.setWidth(backupWidth);
                stage.setHeight(backupHeight);
            }
        });
    }

    public boolean isDockedMode() {
        return dockedMode.get();
    }

    /**
     * Mode cockpit actif : pour masquer barre de titre / ajuster styles dans l’UI dashboard.
     */
    public ReadOnlyBooleanProperty dockedModeProperty() {
        return dockedMode.getReadOnlyProperty();
    }

    private static void applyCockpitDock(Stage stage) {
        Screen game = resolveGameScreen();
        Rectangle2D b = game.getVisualBounds();

        PanelCorners quadScreen = panelCornersOnScreen(b);
        double minX = min4(quadScreen.topLeft().getX(), quadScreen.topRight().getX(),
                quadScreen.bottomRight().getX(), quadScreen.bottomLeft().getX());
        double maxX = max4(quadScreen.topLeft().getX(), quadScreen.topRight().getX(),
                quadScreen.bottomRight().getX(), quadScreen.bottomLeft().getX());
        double minY = min4(quadScreen.topLeft().getY(), quadScreen.topRight().getY(),
                quadScreen.bottomRight().getY(), quadScreen.bottomLeft().getY());
        double maxY = max4(quadScreen.topLeft().getY(), quadScreen.topRight().getY(),
                quadScreen.bottomRight().getY(), quadScreen.bottomLeft().getY());

        double bbW = Math.max(stage.getMinWidth(), maxX - minX);
        double bbH = Math.max(stage.getMinHeight(), maxY - minY);

        stage.setMaximized(false);
        stage.setX(minX);
        stage.setY(minY);
        stage.setWidth(bbW);
        stage.setHeight(bbH);

        double ox = minX;
        double oy = minY;
        PanelCorners quadLocal = new PanelCorners(
                new Point2D(quadScreen.topLeft().getX() - ox, quadScreen.topLeft().getY() - oy),
                new Point2D(quadScreen.topRight().getX() - ox, quadScreen.topRight().getY() - oy),
                new Point2D(quadScreen.bottomRight().getX() - ox, quadScreen.bottomRight().getY() - oy),
                new Point2D(quadScreen.bottomLeft().getX() - ox, quadScreen.bottomLeft().getY() - oy)
        );

        StackPane root = WindowToggleService.getInstance().getRootPane();
        Scene scene = stage.getScene();
        if (scene != null) {
            scene.setFill(Color.TRANSPARENT);
        }
        if (root != null) {
            if (!root.getStyleClass().contains("cockpit-dock-scene-root")) {
                root.getStyleClass().add("cockpit-dock-scene-root");
            }
            root.setBackground(Background.EMPTY);
            Region main = mainDashboardContent(root);
            if (main != null) {
                main.setBackground(Background.EMPTY);
            }
            root.applyCss();
            root.layout();
            PerspectiveTransform pt = PanelPerspective.createForQuad(quadLocal);
            root.setEffect(pt);
            /* Ne pas mettre en cache : le buffer hors-écran est souvent opaque → rectangle noir autour du trapèze. */
            root.setCache(false);
        }
    }

    /** Premier enfant du {@code StackPane} racine = {@code BorderPane} du dashboard (avant le combo caché). */
    private static Region mainDashboardContent(StackPane root) {
        if (root == null || root.getChildren().isEmpty()) {
            return null;
        }
        Node n = root.getChildren().get(0);
        return n instanceof Region r ? r : null;
    }

    private static PanelCorners panelCornersOnScreen(Rectangle2D visual) {
        return CockpitLeftPanelGeometryService.getInstance().panelCornersOnScreen(visual);
    }

    /**
     * Choisit l’écran où afficher le dock :
     * <ol>
     *   <li>Si la préférence {@link CockpitLeftPanelGeometry#PREF_COCKPIT_DOCK_SCREEN_INDEX} est définie
     *       (entier valide), cet index dans {@link Screen#getScreens()} est utilisé.</li>
     *   <li>Sinon, avec <strong>2</strong> écrans : l’écran <strong>différent du primaire</strong> JavaFX
     *       (souvent le jeu), car l’ordre de la liste ne correspond pas toujours à « écran 1 » Windows.</li>
     *   <li>Avec <strong>3+</strong> écrans : plus grande aire utile parmi les non-primaires.</li>
     *   <li>Un seul écran : primaire.</li>
     * </ol>
     */
    private static Screen resolveGameScreen() {
        List<Screen> screens = Screen.getScreens();
        Screen chosen = pickGameScreen(screens);
        int idx = indexOfScreen(screens, chosen);
        System.out.println("[CockpitDock] Écran dock : index liste JavaFX=" + idx + "/" + screens.size()
                + ", bounds=" + chosen.getVisualBounds()
                + " (préf " + CockpitLeftPanelGeometry.PREF_COCKPIT_DOCK_SCREEN_INDEX + " pour forcer)");
        return chosen;
    }

    private static Screen pickGameScreen(List<Screen> screens) {
        PreferencesService prefs = PreferencesService.getInstance();
        String raw = prefs.getPreference(CockpitLeftPanelGeometry.PREF_COCKPIT_DOCK_SCREEN_INDEX, "");
        if (raw != null && !raw.isBlank()) {
            try {
                int idx = Integer.parseInt(raw.trim());
                if (idx >= 0 && idx < screens.size()) {
                    return screens.get(idx);
                }
                System.err.println("[CockpitDock] Index hors limite : " + idx + " (max " + (screens.size() - 1) + ")");
            } catch (NumberFormatException e) {
                System.err.println("[CockpitDock] Préférence invalide : " + CockpitLeftPanelGeometry.PREF_COCKPIT_DOCK_SCREEN_INDEX + "=" + raw);
            }
        }
        if (screens.size() <= 1) {
            return Screen.getPrimary();
        }
        if (screens.size() == 2) {
            Screen primary = Screen.getPrimary();
            for (int i = 0; i < 2; i++) {
                if (screens.get(i) != primary) {
                    return screens.get(i);
                }
            }
            return screens.get(1);
        }
        Screen primary = Screen.getPrimary();
        Optional<Screen> largestOther = screens.stream()
                .filter(s -> s != primary)
                .max(Comparator.comparingDouble(s -> {
                    Rectangle2D v = s.getVisualBounds();
                    return v.getWidth() * v.getHeight();
                }));
        return largestOther.orElse(screens.get(Math.min(1, screens.size() - 1)));
    }

    private static int indexOfScreen(List<Screen> screens, Screen target) {
        for (int i = 0; i < screens.size(); i++) {
            if (screens.get(i) == target) {
                return i;
            }
        }
        return -1;
    }

    private static int screenIndexContainingStage(Stage stage) {
        double cx = stage.getX() + stage.getWidth() / 2;
        double cy = stage.getY() + stage.getHeight() / 2;
        List<Screen> screens = Screen.getScreens();
        for (int i = 0; i < screens.size(); i++) {
            if (screens.get(i).getVisualBounds().contains(cx, cy)) {
                return i;
            }
        }
        return 0;
    }

    private static void restoreMaximizedOnScreen(Stage stage, int screenIndex) {
        List<Screen> screens = Screen.getScreens();
        Screen s = (screenIndex >= 0 && screenIndex < screens.size()) ? screens.get(screenIndex) : Screen.getPrimary();
        Rectangle2D vb = s.getVisualBounds();
        stage.setMaximized(false);
        stage.setX(vb.getMinX());
        stage.setY(vb.getMinY());
        stage.setWidth(vb.getWidth());
        stage.setHeight(vb.getHeight());
        stage.setMaximized(true);
    }

    private static double min4(double a, double b, double c, double d) {
        return Math.min(Math.min(a, b), Math.min(c, d));
    }

    private static double max4(double a, double b, double c, double d) {
        return Math.max(Math.max(a, b), Math.max(c, d));
    }
}
