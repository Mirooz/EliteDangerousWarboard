package be.mirooz.elitedangerous.dashboard.window;

import be.mirooz.elitedangerous.dashboard.service.PreferencesService;

/**
 * Préférences de cadre fenêtre (barre titre OS vs chrome JavaFX custom).
 */
public final class WindowFramePreferences {

    private WindowFramePreferences() {
    }

    /** Barre titre Windows native (ex. compatibilité VR Quest Link). */
    public static boolean useNativeOsWindowFrame() {
        String p = System.getProperty("elite.dashboard.nativeOsWindow");
        if (p != null) {
            return Boolean.parseBoolean(p.trim());
        }
        return Boolean.parseBoolean(
                PreferencesService.getInstance().getPreference("window.nativeOsWindow", "false"));
    }
}
