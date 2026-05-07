package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.dashboard.util.NativeVirtualKeyToAwtKeyCode;
import com.fasterxml.jackson.databind.JsonNode;

import java.awt.AWTException;
import java.awt.GraphicsEnvironment;
import java.awt.Robot;
import java.util.Locale;

/**
 * Envoie la touche « sous-système suivant » après un scan complet (ShipTargeted stage 3),
 * si l’option est activée et qu’aucune info de sous-système « générateur » n’est encore présente.
 */
public final class AutoPowerplantKeyService {

    private static final AutoPowerplantKeyService INSTANCE = new AutoPowerplantKeyService();

    private final PreferencesService preferences = PreferencesService.getInstance();

    private AutoPowerplantKeyService() {
    }

    public static AutoPowerplantKeyService getInstance() {
        return INSTANCE;
    }

    public void onShipTargeted(JsonNode json) {
        if (json == null || !json.path("event").asText("").equals("ShipTargeted")) {
            return;
        }
        if (!preferences.isCombatAutoSelectPowerplantEnabled()) {
            return;
        }
        int key = preferences.getCombatAutoSelectPowerplantSubsystemKey();
        if (key <= 0) {
            return;
        }
        if (!json.path("TargetLocked").asBoolean(false)) {
            return;
        }
        if (json.path("ScanStage").asInt(-1) != 3) {
            return;
        }

        if (json.hasNonNull("Subsystem")) {
            String sub = json.get("Subsystem").asText("");
            if (!sub.isEmpty() && subsystemIsPowerplant(sub)) {
                return;
            }
        }

        int awt = NativeVirtualKeyToAwtKeyCode.toAwtKeyCode(key);
        if (awt < 0) {
            return;
        }

        Thread t = new Thread(() -> sendKeyPress(awt), "elite-auto-powerplant-key");
        t.setDaemon(true);
        t.start();
    }

    private static boolean subsystemIsPowerplant(String subsystemToken) {
        if (subsystemToken == null || subsystemToken.isEmpty()) {
            return false;
        }
        String n = subsystemToken.toLowerCase(Locale.ROOT);
        return n.contains("powerplant");
    }

    private static void sendKeyPress(int awtKeyCode) {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        try {
            Robot robot = new Robot();
            robot.keyPress(awtKeyCode);
            robot.keyRelease(awtKeyCode);
        } catch (AWTException ignored) {
        }
    }
}
