package be.mirooz.elitedangerous.dashboard.view.common.overlay;

import be.mirooz.elitedangerous.dashboard.service.PreferencesService;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.List;

/**
 * Persistance de l’index d’écran et clamp de position en coordonnées écran absolues pour les overlays multi-moniteurs.
 */
public final class OverlayScreenGeometryHelper {

    private OverlayScreenGeometryHelper() {
    }

    /**
     * Écran contenant le centre de la fenêtre (équivalent simplifié de la fenêtre principale, sans branche maximisée).
     */
    public static Screen getScreenForStage(Stage stage) {
        if (stage == null) {
            return null;
        }
        double windowX = stage.getX() + stage.getWidth() / 2;
        double windowY = stage.getY() + stage.getHeight() / 2;

        for (Screen screen : Screen.getScreens()) {
            Rectangle2D bounds = screen.getVisualBounds();
            if (bounds.contains(windowX, windowY)) {
                return screen;
            }
        }

        Screen closestScreen = Screen.getPrimary();
        double minDistance = Double.MAX_VALUE;

        for (Screen screen : Screen.getScreens()) {
            Rectangle2D bounds = screen.getVisualBounds();
            double centerX = bounds.getMinX() + bounds.getWidth() / 2;
            double centerY = bounds.getMinY() + bounds.getHeight() / 2;
            double dx = windowX - centerX;
            double dy = windowY - centerY;
            double distance = dx * dx + dy * dy;
            if (distance < minDistance) {
                minDistance = distance;
                closestScreen = screen;
            }
        }
        return closestScreen;
    }

    public static void persistScreenIndex(PreferencesService preferencesService, String screenIndexKey, Stage stage) {
        if (preferencesService == null || stage == null || screenIndexKey == null) {
            return;
        }
        Screen currentScreen = getScreenForStage(stage);
        if (currentScreen == null) {
            return;
        }
        List<Screen> screens = Screen.getScreens();
        int screenIndex = screens.indexOf(currentScreen);
        if (screenIndex >= 0) {
            preferencesService.setPreference(screenIndexKey, String.valueOf(screenIndex));
        }
    }

    /**
     * Écran cible : préférence d’index si valide, sinon écran contenant le centre de la position sauvegardée, sinon primaire.
     */
    public static Screen resolveScreenForRestore(PreferencesService preferencesService,
                                                 String screenIndexKey,
                                                 double savedX,
                                                 double savedY,
                                                 double width,
                                                 double height) {
        String screenIndexStr = preferencesService != null && screenIndexKey != null
                ? preferencesService.getPreference(screenIndexKey, "")
                : "";
        Screen byIndex = parseScreenIndex(screenIndexStr);
        if (byIndex != null) {
            return byIndex;
        }
        double cx = savedX + Math.max(1.0, width) / 2;
        double cy = savedY + Math.max(1.0, height) / 2;
        for (Screen screen : Screen.getScreens()) {
            if (screen.getVisualBounds().contains(cx, cy)) {
                return screen;
            }
        }
        return Screen.getPrimary();
    }

    private static Screen parseScreenIndex(String screenIndexStr) {
        if (screenIndexStr == null || screenIndexStr.isBlank()) {
            return null;
        }
        try {
            int screenIndex = Integer.parseInt(screenIndexStr.trim());
            List<Screen> screens = Screen.getScreens();
            if (screenIndex >= 0 && screenIndex < screens.size()) {
                return screens.get(screenIndex);
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    public static void applyClampedPosition(Stage overlayStage,
                                            Rectangle2D screenBounds,
                                            double savedX,
                                            double savedY,
                                            double width,
                                            double height) {
        if (overlayStage == null || screenBounds == null) {
            return;
        }
        double margin = 8;
        double minX = screenBounds.getMinX() + margin;
        double minY = screenBounds.getMinY() + margin;
        double maxX = screenBounds.getMaxX() - width - margin;
        double maxY = screenBounds.getMaxY() - height - margin;
        if (maxX < minX) {
            minX = screenBounds.getMinX();
            maxX = screenBounds.getMaxX() - width;
        }
        if (maxY < minY) {
            minY = screenBounds.getMinY();
            maxY = screenBounds.getMaxY() - height;
        }
        double finalX = Math.max(minX, Math.min(savedX, maxX));
        double finalY = Math.max(minY, Math.min(savedY, maxY));
        overlayStage.setX(finalX);
        overlayStage.setY(finalY);
    }
}
