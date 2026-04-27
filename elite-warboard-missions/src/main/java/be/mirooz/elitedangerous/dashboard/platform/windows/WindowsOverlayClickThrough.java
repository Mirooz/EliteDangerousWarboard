package be.mirooz.elitedangerous.dashboard.platform.windows;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.OptionalLong;
import java.util.Set;

/**
 * Passe les événements souris au travers de la fenêtre overlay (HWND) sous Windows.
 * <p>
 * JavaFX/Glass utilise souvent plusieurs HWND (cadre + surface Direct3D). Un seul
 * {@code SetWindowLong} sur le peer racine ne suffit pas : on applique
 * {@code WS_EX_LAYERED | WS_EX_TRANSPARENT} à la racine {@link WinUser#GA_ROOT} et
 * <strong>récursivement à tous les enfants</strong> ({@link User32#EnumChildWindows}).
 * On complète par une recherche {@link User32#EnumWindows} sur le titre de la fenêtre si la
 * réflexion ne donne rien d’exploitable.
 * <p>
 * Désactivation : {@code -Dwarboard.windows.overlayClickThrough=false}
 * <p>
 * Sur une appli classpath classique, la réflexion sur le peer suffit en général. Si un jour la JVM
 * refuse l’accès (modules stricts), ajouter par exemple :
 * {@code --add-opens=javafx.graphics/javafx.stage=ALL-UNNAMED}
 * (et éventuellement {@code com.sun.javafx.tk.quantum}).
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
        Set<Long> rootPtrs = collectRootHwndPointers(stage);
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

    private static Set<Long> collectRootHwndPointers(Stage stage) {
        Set<Long> roots = new LinkedHashSet<>();
        resolveNativeWindowHandle(stage).ifPresent(ptr -> roots.add(toRootPtr(ptr)));
        findTopLevelHwndByTitle(stage.getTitle()).ifPresent(roots::add);
        roots.remove(0L);
        return roots;
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

    /**
     * @return {@code true} si un appel Win32 a modifié ou confirmé le style (utile pour le log).
     */
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
                // suite en getPlatformWindow
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
                    "Clic à travers Windows : réflexion peer refusée ({}). En JVM modulaire stricte, ajoutez --add-opens sur javafx.stage / tk.quantum.",
                    e.getMessage());
        } catch (Throwable t) {
            LOG.debug("Clic à travers Windows : {}", t.toString());
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
