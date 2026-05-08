package be.mirooz.elitedangerous.dashboard.window.win32;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import javafx.stage.Stage;

import java.util.Locale;
import java.util.OptionalLong;

/**
 * Remonte une fenêtre JavaFX au premier plan sans l’activer (Win32), pour que le bind VR
 * n’arrache pas le focus clavier/souris au jeu.
 */
public final class WindowsBringStageToFrontNoActivate {

    private WindowsBringStageToFrontNoActivate() {
    }

    public static boolean isSupported() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    /**
     * @return {@code true} si {@code SetWindowPos} a été invoqué avec succès sur un HWND résolu
     */
    public static boolean apply(Stage stage) {
        if (!isSupported() || stage == null) {
            return false;
        }
        OptionalLong hwndOpt = WindowsJavaFxHwnd.primaryRootHwnd(stage);
        if (hwndOpt.isEmpty()) {
            return false;
        }
        WinDef.HWND hwnd = new WinDef.HWND(Pointer.createConstant(hwndOpt.getAsLong()));
        int flags = WinUser.SWP_NOMOVE
                | WinUser.SWP_NOSIZE
                | WinUser.SWP_NOACTIVATE
                | WinUser.SWP_SHOWWINDOW;
        /* HWND_TOP : placer au sommet du Z-order sans activer (MSDN : 0). */
        WinDef.HWND insertAfter = new WinDef.HWND(Pointer.createConstant(0));
        return User32.INSTANCE.SetWindowPos(hwnd, insertAfter, 0, 0, 0, 0, flags);
    }
}
