package be.mirooz.elitedangerous.dashboard.window.win32;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.OptionalLong;

/**
 * Fenêtre {@link javafx.stage.StageStyle#UNDECORATED} plus « applicative » côté Win32 (compat VR).
 * <p>
 * Désactivation : {@code -Delite.dashboard.windowsVrFrameCompat=false}
 */
public final class WindowsUndecoratedVrFrameCompat {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsUndecoratedVrFrameCompat.class);
    private static final String ENABLE_PROP = "elite.dashboard.windowsVrFrameCompat";
    private static final long WS_EX_APPWINDOW = 0x00040000L;
    private static final long WS_EX_TOOLWINDOW = 0x00000080L;

    private WindowsUndecoratedVrFrameCompat() {
    }

    public static boolean isSupportedOs() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    public static boolean isEnabledByProperty() {
        String p = System.getProperty(ENABLE_PROP);
        if (p == null) {
            return true;
        }
        return Boolean.parseBoolean(p.trim());
    }

    public static void applyAfterShown(Stage stage) {
        if (!isSupportedOs() || !isEnabledByProperty() || stage == null) {
            return;
        }
        long ptr = resolvePrimaryRoot(stage);
        if (ptr == 0L) {
            LOG.warn(
                    "VR frame compat : aucun HWND pour « {} » — Quest peut ignorer la fenêtre. "
                            + "Réflexion peer ou --add-opens=javafx.graphics/javafx.stage=ALL-UNNAMED",
                    stage.getTitle());
            return;
        }
        WinDef.HWND hwnd = new WinDef.HWND(Pointer.createConstant(ptr));
        if (!applyOverlappedBorderlessStyle(hwnd)) {
            LOG.debug("VR frame compat : styles Win32 inchangés (déjà conformes ou refus OS)");
        } else {
            LOG.debug("VR frame compat : styles Win32 ajustés (HWND={})", Long.toUnsignedString(ptr));
        }
    }

    private static long resolvePrimaryRoot(Stage stage) {
        OptionalLong opt = WindowsJavaFxHwnd.primaryRootHwnd(stage);
        if (opt.isPresent()) {
            return opt.getAsLong();
        }
        for (Long p : WindowsJavaFxHwnd.collectRootHwndPointers(stage)) {
            return p;
        }
        return 0L;
    }

    private static boolean applyOverlappedBorderlessStyle(WinDef.HWND hwnd) {
        BaseTSD.LONG_PTR stylePtr = User32.INSTANCE.GetWindowLongPtr(hwnd, WinUser.GWL_STYLE);
        if (stylePtr == null) {
            return false;
        }
        long style = stylePtr.longValue() & 0xFFFF_FFFFL;
        long newStyle = style;
        newStyle &= ~((long) WinUser.WS_POPUP & 0xFFFF_FFFFL);
        newStyle &= ~((long) WinUser.WS_CHILD & 0xFFFF_FFFFL);
        newStyle &= ~((long) WinUser.WS_CAPTION & 0xFFFF_FFFFL);
        newStyle |= WinUser.WS_THICKFRAME;
        newStyle |= WinUser.WS_SYSMENU;
        newStyle |= WinUser.WS_MINIMIZEBOX;
        newStyle |= WinUser.WS_MAXIMIZEBOX;
        newStyle |= WinUser.WS_CLIPCHILDREN;
        newStyle |= WinUser.WS_CLIPSIBLINGS;

        boolean styleChanged = newStyle != style;
        if (styleChanged) {
            User32.INSTANCE.SetWindowLongPtr(hwnd, WinUser.GWL_STYLE, Pointer.createConstant(newStyle));
        }

        BaseTSD.LONG_PTR exPtr = User32.INSTANCE.GetWindowLongPtr(hwnd, WinUser.GWL_EXSTYLE);
        long exStyle = exPtr == null ? 0L : exPtr.longValue() & 0xFFFF_FFFFL;
        long newEx = exStyle | WS_EX_APPWINDOW;
        newEx &= ~(WS_EX_TOOLWINDOW & 0xFFFF_FFFFL);
        boolean exChanged = newEx != exStyle;
        if (exChanged) {
            User32.INSTANCE.SetWindowLongPtr(hwnd, WinUser.GWL_EXSTYLE, Pointer.createConstant(newEx));
        }

        if (!styleChanged && !exChanged) {
            return false;
        }
        int flags = WinUser.SWP_NOMOVE
                | WinUser.SWP_NOSIZE
                | WinUser.SWP_NOZORDER
                | WinUser.SWP_FRAMECHANGED
                | WinUser.SWP_NOACTIVATE;
        User32.INSTANCE.SetWindowPos(hwnd, null, 0, 0, 0, 0, flags);
        return true;
    }
}
