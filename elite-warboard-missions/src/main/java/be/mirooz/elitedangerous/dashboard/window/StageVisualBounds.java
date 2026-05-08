package be.mirooz.elitedangerous.dashboard.window;

import be.mirooz.elitedangerous.dashboard.window.win32.WindowsUndecoratedTightFill;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.Optional;

/**
 * Ajuste un {@link Stage} sur la zone <em>utilisable</em> de l’écran (JavaFX {@link Screen#getVisualBounds()}),
 * c’est-à-dire en général <strong>sous</strong> la barre des tâches Windows — contrairement à
 * {@link Stage#setMaximized(boolean)} sur une fenêtre {@link javafx.stage.StageStyle#UNDECORATED}, qui peut
 * occuper tout le moniteur.
 */
public final class StageVisualBounds {

    private StageVisualBounds() {
    }

    /**
     * Ramène la coordonnée Y écran du stage à au moins 0 (bord haut). Les valeurs négatives arrivent
     * souvent avec le cadre Win32 undecorated ; les préférences et la restauration les éviter ainsi.
     */
    public static double clampStageYNonNegative(double screenY) {
        return Math.max(0.0, screenY);
    }

    /** Écran dont la zone utilisable contient le centre géométrique du stage (ou écran principal). */
    public static Screen screenForWindowCenter(Stage stage) {
        if (stage == null) {
            return Screen.getPrimary();
        }
        double w = stage.getWidth() > 0 ? stage.getWidth() : 1;
        double h = stage.getHeight() > 0 ? stage.getHeight() : 1;
        double cx = stage.getX() + w / 2;
        double cy = stage.getY() + h / 2;
        for (Screen s : Screen.getScreens()) {
            if (s.getVisualBounds().contains(cx, cy)) {
                return s;
            }
        }
        return Screen.getPrimary();
    }

    /** Le stage occupe-t-il (à epsilon près) toute la zone utilisable de l’écran où il se trouve ? */
    public static boolean isStageFillingWorkArea(Stage stage, double epsilon) {
        if (stage == null) {
            return false;
        }
        Rectangle2D vb = screenForWindowCenter(stage).getVisualBounds();
        if (WindowsUndecoratedTightFill.isSupported()) {
            Optional<Rectangle2D> client = WindowsUndecoratedTightFill.getClientAreaScreenBounds(stage);
            if (client.isPresent()) {
                return rectsMatchWithinEpsilon(client.get(), vb, epsilon);
            }
        }
        return rectsMatchWithinEpsilon(
                new Rectangle2D(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight()),
                vb,
                epsilon);
    }

    private static boolean rectsMatchWithinEpsilon(Rectangle2D a, Rectangle2D b, double epsilon) {
        return Math.abs(a.getMinX() - b.getMinX()) <= epsilon
                && Math.abs(a.getMinY() - b.getMinY()) <= epsilon
                && Math.abs(a.getWidth() - b.getWidth()) <= epsilon
                && Math.abs(a.getHeight() - b.getHeight()) <= epsilon;
    }

    /** Place le stage sur la zone utilisable de l’écran courant, sans {@link Stage#setMaximized(boolean)}. */
    public static void fitStageToVisualBounds(Stage stage) {
        fitStageToVisualBounds(stage, screenForWindowCenter(stage));
    }

    /** Place le stage sur la zone utilisable de l’écran donné (redémarrage maximisé sur un moniteur précis). */
    public static void fitStageToVisualBounds(Stage stage, Screen screen) {
        if (stage == null) {
            return;
        }
        Screen s = screen != null ? screen : Screen.getPrimary();
        Rectangle2D b = s.getVisualBounds();
        stage.setMaximized(false);
        stage.setX(b.getMinX());
        stage.setY(b.getMinY());
        stage.setWidth(b.getWidth());
        stage.setHeight(b.getHeight());
        WindowsUndecoratedTightFill.expandOuterWindowToMatchVisualClient(stage, b);
    }
}
