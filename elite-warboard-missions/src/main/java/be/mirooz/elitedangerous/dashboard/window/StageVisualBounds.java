package be.mirooz.elitedangerous.dashboard.window;

import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * Ajuste un {@link Stage} sur la zone <em>utilisable</em> de l’écran (JavaFX {@link Screen#getVisualBounds()}),
 * c’est-à-dire en général <strong>sous</strong> la barre des tâches Windows — contrairement à
 * {@link Stage#setMaximized(boolean)} sur une fenêtre {@link javafx.stage.StageStyle#UNDECORATED}, qui peut
 * occuper tout le moniteur.
 */
public final class StageVisualBounds {

    private StageVisualBounds() {
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
        return Math.abs(stage.getX() - vb.getMinX()) <= epsilon
                && Math.abs(stage.getY() - vb.getMinY()) <= epsilon
                && Math.abs(stage.getWidth() - vb.getWidth()) <= epsilon
                && Math.abs(stage.getHeight() - vb.getHeight()) <= epsilon;
    }

    /** Place le stage sur la zone utilisable de l’écran courant, sans {@link Stage#setMaximized(boolean)}. */
    public static void fitStageToVisualBounds(Stage stage) {
        if (stage == null) {
            return;
        }
        Rectangle2D b = screenForWindowCenter(stage).getVisualBounds();
        stage.setMaximized(false);
        stage.setX(b.getMinX());
        stage.setY(b.getMinY());
        stage.setWidth(b.getWidth());
        stage.setHeight(b.getHeight());
    }
}
