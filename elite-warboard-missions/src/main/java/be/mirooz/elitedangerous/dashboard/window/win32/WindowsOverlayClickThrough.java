package be.mirooz.elitedangerous.dashboard.window.win32;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Passe les événements souris au travers de la fenêtre overlay (HWND) sous Windows.
 * <p>
 * Désactivation : {@code -Dwarboard.windows.overlayClickThrough=false}
 */
public final class WindowsOverlayClickThrough {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsOverlayClickThrough.class);
    private static final String DISABLE_PROP = "warboard.windows.overlayClickThrough";

    private WindowsOverlayClickThrough() {
    }

    public static boolean isSupported() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")
                && !Boolean.parseBoolean(System.getProperty(DISABLE_PROP, "false"));
    }

    /**
     * @param passthrough {@code true} : la fenêtre ne reçoit plus la souris (clics vers les fenêtres derrière).
     */
    public static void setMousePassthrough(Stage stage, boolean passthrough) {
        if (!isSupported() || stage == null) {
            return;
        }
        Set<Long> rootPtrs = WindowsJavaFxHwnd.collectRootHwndPointers(stage);
        if (rootPtrs.isEmpty()) {
            LOG.warn(
                    "Clic à travers Windows : aucun HWND trouvé pour l'overlay (titre « {} »). "
                            + "Réflexion peer / titre : en JVM modulaire stricte, essayez --add-opens=javafx.graphics/javafx.stage=ALL-UNNAMED",
                    stage.getTitle());
            return;
        }
        Set<Long> allPtrs = new LinkedHashSet<>();
        for (long rootPtr : rootPtrs) {
            collectSubtreePointers(new WinDef.HWND(Pointer.createConstant(rootPtr)), allPtrs);
        }
        int applied = 0;
        for (long ptr : allPtrs) {
            if (ptr != 0L && applyExStyle(new WinDef.HWND(Pointer.createConstant(ptr)), passthrough)) {
                applied++;
            }
        }
        LOG.debug("Clic à travers Windows : passthrough={}, racines={}, HWND mis à jour={}", passthrough, rootPtrs.size(), applied);
    }

    private static void collectSubtreePointers(WinDef.HWND hwnd, Set<Long> out) {
        if (hwnd == null) {
            return;
        }
        long p = Pointer.nativeValue(hwnd.getPointer());
        if (p == 0L) {
            return;
        }
        out.add(p);
        User32.INSTANCE.EnumChildWindows(hwnd, (child, data) -> {
            collectSubtreePointers(child, out);
            return true;
        }, null);
    }

    private static boolean applyExStyle(WinDef.HWND hwnd, boolean passthrough) {
        BaseTSD.LONG_PTR stylePtr = User32.INSTANCE.GetWindowLongPtr(hwnd, WinUser.GWL_EXSTYLE);
        if (stylePtr == null) {
            return false;
        }
        long exStyle = stylePtr.longValue();
        long newExStyle;
        if (passthrough) {
            newExStyle = exStyle | WinUser.WS_EX_LAYERED | WinUser.WS_EX_TRANSPARENT;
        } else {
            newExStyle = exStyle & ~WinUser.WS_EX_TRANSPARENT;
        }
        if (newExStyle == exStyle) {
            return false;
        }
        User32.INSTANCE.SetWindowLongPtr(hwnd, WinUser.GWL_EXSTYLE, Pointer.createConstant(newExStyle));
        int flags = WinUser.SWP_NOMOVE
                | WinUser.SWP_NOSIZE
                | WinUser.SWP_NOZORDER
                | WinUser.SWP_FRAMECHANGED
                | WinUser.SWP_NOACTIVATE;
        User32.INSTANCE.SetWindowPos(hwnd, null, 0, 0, 0, 0, flags);
        return true;
    }
}
