package be.mirooz.elitedangerous.dashboard.platform.windows;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.OptionalLong;

/**
 * Force le statut « always on top » d'un overlay JavaFX au niveau Win32 (HWND_TOPMOST).
 * <p>
 * Le {@code Stage#setAlwaysOnTop(true)} de JavaFX positionne déjà la fenêtre racine en
 * {@code HWND_TOPMOST}, mais ce statut peut être perdu lorsqu'un jeu DirectX (Elite Dangerous
 * en mode plein écran exclusif ou borderless) prend le focus et installe ses propres fenêtres
 * topmost. Ré-appliquer périodiquement {@code SetWindowPos(HWND_TOPMOST, SWP_NOACTIVATE)}
 * remet l'overlay devant sans lui voler le focus de saisie.
 * <p>
 * Désactivation : {@code -Dwarboard.windows.overlayTopmost=false}
 */
public final class WindowsOverlayTopmost {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsOverlayTopmost.class);
    private static final String DISABLE_PROP = "warboard.windows.overlayTopmost";

    private static final WinDef.HWND HWND_TOPMOST = new WinDef.HWND(Pointer.createConstant(-1L));
    private static final WinDef.HWND HWND_NOTOPMOST = new WinDef.HWND(Pointer.createConstant(-2L));

    private WindowsOverlayTopmost() {
    }

    public static boolean isSupported() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")
                && !Boolean.parseBoolean(System.getProperty(DISABLE_PROP, "false"));
    }

    /**
     * Force (ou retire) le statut topmost sur le HWND racine du stage donné.
     *
     * @param stage   stage JavaFX déjà affiché
     * @param topmost {@code true} pour {@code HWND_TOPMOST}, {@code false} pour {@code HWND_NOTOPMOST}
     * @return {@code true} si l'appel Win32 a été émis avec succès
     */
    public static boolean setTopmost(Stage stage, boolean topmost) {
        if (!isSupported() || stage == null) {
            return false;
        }
        OptionalLong rootPtr = resolveRootHwnd(stage);
        if (rootPtr.isEmpty()) {
            LOG.debug("Topmost Windows : HWND introuvable pour l'overlay « {} »", stage.getTitle());
            return false;
        }
        WinDef.HWND hwnd = new WinDef.HWND(Pointer.createConstant(rootPtr.getAsLong()));
        int flags = WinUser.SWP_NOMOVE | WinUser.SWP_NOSIZE | WinUser.SWP_NOACTIVATE;
        boolean ok = User32.INSTANCE.SetWindowPos(hwnd, topmost ? HWND_TOPMOST : HWND_NOTOPMOST, 0, 0, 0, 0, flags);
        if (!ok) {
            LOG.debug("Topmost Windows : SetWindowPos a échoué pour l'overlay « {} »", stage.getTitle());
        }
        return ok;
    }

    private static OptionalLong resolveRootHwnd(Stage stage) {
        OptionalLong handle = resolveNativeWindowHandle(stage);
        if (handle.isEmpty()) {
            return handle;
        }
        WinDef.HWND hwnd = new WinDef.HWND(Pointer.createConstant(handle.getAsLong()));
        WinDef.HWND root = User32.INSTANCE.GetAncestor(hwnd, WinUser.GA_ROOT);
        if (root != null && Pointer.nativeValue(root.getPointer()) != 0L) {
            return OptionalLong.of(Pointer.nativeValue(root.getPointer()));
        }
        return handle;
    }

    private static OptionalLong resolveNativeWindowHandle(Stage stage) {
        try {
            Method getPeer = Window.class.getDeclaredMethod("getPeer");
            getPeer.setAccessible(true);
            Object tkStage = getPeer.invoke(stage);
            if (tkStage == null) {
                return OptionalLong.empty();
            }
            try {
                Method getRawHandle = tkStage.getClass().getMethod("getRawHandle");
                Object handle = getRawHandle.invoke(tkStage);
                long asLong = toUnsignedLong(handle);
                if (asLong != 0L) {
                    return OptionalLong.of(asLong);
                }
            } catch (NoSuchMethodException ignored) {
                // suite via getPlatformWindow
            }
            Method getPlatformWindow = tkStage.getClass().getDeclaredMethod("getPlatformWindow");
            getPlatformWindow.setAccessible(true);
            Object platformWindow = getPlatformWindow.invoke(tkStage);
            if (platformWindow != null) {
                Method getNativeHandle = platformWindow.getClass().getMethod("getNativeHandle");
                Object nh = getNativeHandle.invoke(platformWindow);
                long asLong = toUnsignedLong(nh);
                if (asLong != 0L) {
                    return OptionalLong.of(asLong);
                }
            }
        } catch (ReflectiveOperationException e) {
            LOG.debug("Topmost Windows : réflexion peer refusée ({}). En JVM modulaire stricte, ajoutez --add-opens sur javafx.stage / tk.quantum.", e.getMessage());
        } catch (Throwable t) {
            LOG.debug("Topmost Windows : {}", t.toString());
        }
        return OptionalLong.empty();
    }

    private static long toUnsignedLong(Object handle) {
        if (handle instanceof Long l) {
            return l;
        }
        if (handle instanceof Integer i) {
            return Integer.toUnsignedLong(i);
        }
        return 0L;
    }
}
