package be.mirooz.elitedangerous.dashboard.window.win32;

import be.mirooz.elitedangerous.dashboard.window.WindowFramePreferences;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.stage.Stage;

import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Compense les marges non clientes Win32 (p. ex. {@code WS_THICKFRAME} via {@link WindowsUndecoratedVrFrameCompat})
 * pour que la zone <em>cliente</em> remplisse exactement la zone utilisable JavaFX, sans dépasser sur la barre des tâches.
 */
public final class WindowsUndecoratedTightFill {

    /** {@link User32} jna-platform n’expose pas {@code ClientToScreen}. */
    private interface User32Extra extends StdCallLibrary {
        User32Extra INSTANCE = Native.load("user32", User32Extra.class, W32APIOptions.DEFAULT_OPTIONS);

        boolean ClientToScreen(WinDef.HWND hWnd, WinDef.POINT lpPoint);
    }

    private WindowsUndecoratedTightFill() {
    }

    public static boolean isSupported() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")
                && !WindowFramePreferences.useNativeOsWindowFrame();
    }

    public static Optional<Rectangle2D> getClientAreaScreenBounds(Stage stage) {
        if (stage == null || !isSupported()) {
            return Optional.empty();
        }
        OptionalLong opt = WindowsJavaFxHwnd.primaryRootHwnd(stage);
        if (opt.isEmpty()) {
            return Optional.empty();
        }
        WinDef.HWND hwnd = new WinDef.HWND(Pointer.createConstant(opt.getAsLong()));
        WinDef.RECT client = new WinDef.RECT();
        if (!User32.INSTANCE.GetClientRect(hwnd, client)) {
            return Optional.empty();
        }
        WinDef.POINT pt = new WinDef.POINT(0, 0);
        if (!User32Extra.INSTANCE.ClientToScreen(hwnd, pt)) {
            return Optional.empty();
        }
        double cw = client.right - client.left;
        double ch = client.bottom - client.top;
        if (cw <= 0 || ch <= 0) {
            return Optional.empty();
        }
        return Optional.of(new Rectangle2D(pt.x, pt.y, cw, ch));
    }

    public static void expandOuterWindowToMatchVisualClient(Stage stage, Rectangle2D visualWork) {
        if (stage == null || visualWork == null || !isSupported()) {
            return;
        }
        Platform.runLater(() -> applyOuterInsets(stage, visualWork));
    }

    private static void applyOuterInsets(Stage stage, Rectangle2D visualWork) {
        OptionalLong opt = WindowsJavaFxHwnd.primaryRootHwnd(stage);
        if (opt.isEmpty()) {
            return;
        }
        WinDef.HWND hwnd = new WinDef.HWND(Pointer.createConstant(opt.getAsLong()));
        WinDef.RECT client = new WinDef.RECT();
        if (!User32.INSTANCE.GetClientRect(hwnd, client)) {
            return;
        }
        WinDef.POINT pt = new WinDef.POINT(0, 0);
        if (!User32Extra.INSTANCE.ClientToScreen(hwnd, pt)) {
            return;
        }
        WinDef.RECT outer = new WinDef.RECT();
        if (!User32.INSTANCE.GetWindowRect(hwnd, outer)) {
            return;
        }
        int cw = client.right - client.left;
        int ch = client.bottom - client.top;
        if (cw <= 0 || ch <= 0) {
            return;
        }
        int borderLeft = pt.x - outer.left;
        int borderTop = pt.y - outer.top;
        int borderRight = outer.right - (pt.x + cw);
        int borderBottom = outer.bottom - (pt.y + ch);
        if (borderLeft < 0 || borderTop < 0 || borderRight < 0 || borderBottom < 0) {
            return;
        }
        if (borderLeft + borderRight + borderTop + borderBottom == 0) {
            return;
        }
        stage.setX(visualWork.getMinX() - borderLeft);
        stage.setY(visualWork.getMinY() - borderTop);
        stage.setWidth(visualWork.getWidth() + borderLeft + borderRight);
        stage.setHeight(visualWork.getHeight() + borderTop + borderBottom);
    }
}
