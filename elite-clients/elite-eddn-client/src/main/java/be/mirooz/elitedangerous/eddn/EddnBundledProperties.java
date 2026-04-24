package be.mirooz.elitedangerous.eddn;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Charge les propriétés EDDN depuis {@code elite-eddn-client-<app.profile>.properties}
 * (convention calquée sur {@code elite-backend-client} pour uniformiser la config multi-profil).
 *
 * <p>Le profil est résolu via la system-property {@code app.profile} (défaut : {@code dev}).
 * Clés exposées :</p>
 * <ul>
 *   <li>{@code eddn.upload-url} — URL HTTPS pour POST gzip des messages EDDN.</li>
 * </ul>
 */
public final class EddnBundledProperties {

    /** URL HTTPS de la gateway EDDN (POST gzip). */
    public static final String KEY_UPLOAD_URL = "eddn.upload-url";

    private static final Properties PROPS = load();

    private EddnBundledProperties() {
    }

    private static Properties load() {
        String profile = System.getProperty("app.profile", "dev");
        Properties p = new Properties();
        String path = "/elite-eddn-client-" + profile + ".properties";
        try (InputStream in = EddnBundledProperties.class.getResourceAsStream(path)) {
            if (in != null) {
                p.load(in);
            }
        } catch (IOException ignored) {
        }
        return p;
    }

    public static String get(String key) {
        String v = PROPS.getProperty(key);
        return v != null && !v.isBlank() ? v.trim() : null;
    }

    public static String get(String key, String defaultValue) {
        String v = get(key);
        return v != null ? v : defaultValue;
    }
}
