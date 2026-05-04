package be.mirooz.elitedangerous.dashboard.window.win32;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.OptionalLong;
import java.util.Set;

/**
 * Résolution du HWND racine d’un {@link Stage} JavaFX (réflexion peer + repli sur le titre).
 */
public final class WindowsJavaFxHwnd {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsJavaFxHwnd.class);

    private WindowsJavaFxHwnd() {
    }

    /**
     * Zéro ou plusieurs racines (réflexion peer + repli titre ; les deux peuvent diverger pour certains stages).
     */
    public static Set<Long> collectRootHwndPointers(Stage stage) {
        Set<Long> roots = new LinkedHashSet<>();
        resolveNativeWindowHandle(stage).ifPresent(ptr -> roots.add(toRootPtr(ptr)));
        findTopLevelHwndByTitle(stage.getTitle()).ifPresent(roots::add);
        roots.remove(0L);
        return roots;
    }

    public static OptionalLong primaryRootHwnd(Stage stage) {
        OptionalLong peer = resolveNativeWindowHandle(stage);
        if (peer.isPresent()) {
            long r = toRootPtr(peer.getAsLong());
            if (r != 0L) {
                return OptionalLong.of(r);
            }
        }
        return findTopLevelHwndByTitle(stage.getTitle());
    }

    private static long toRootPtr(long ptr) {
        if (ptr == 0L) {
            return 0L;
        }
        WinDef.HWND hwnd = new WinDef.HWND(Pointer.createConstant(ptr));
        WinDef.HWND root = User32.INSTANCE.GetAncestor(hwnd, WinUser.GA_ROOT);
        if (root != null && Pointer.nativeValue(root.getPointer()) != 0L) {
            return Pointer.nativeValue(root.getPointer());
        }
        return ptr;
    }

    private static OptionalLong findTopLevelHwndByTitle(String title) {
        if (title == null || title.isEmpty()) {
            return OptionalLong.empty();
        }
        final long[] found = {0L};
        User32.INSTANCE.EnumWindows((hwnd, data) -> {
            char[] buf = new char[512];
            int n = User32.INSTANCE.GetWindowText(hwnd, buf, 512);
            if (n <= 0) {
                return true;
            }
            String t = new String(buf, 0, n);
            if (title.equals(t)) {
                WinDef.HWND root = User32.INSTANCE.GetAncestor(hwnd, WinUser.GA_ROOT);
                WinDef.HWND use = root != null && Pointer.nativeValue(root.getPointer()) != 0L ? root : hwnd;
                found[0] = Pointer.nativeValue(use.getPointer());
                return false;
            }
            return true;
        }, null);
        return found[0] != 0L ? OptionalLong.of(found[0]) : OptionalLong.empty();
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
                // getPlatformWindow
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
            LOG.debug(
                    "HWND JavaFX : réflexion peer refusée ({}). JVM modulaire stricte : --add-opens=javafx.graphics/javafx.stage=ALL-UNNAMED",
                    e.getMessage());
        } catch (Throwable t) {
            LOG.debug("HWND JavaFX : {}", t.toString());
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
