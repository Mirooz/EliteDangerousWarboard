package be.mirooz.elitedangerous.backend;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Charge les propriétés backend partagées depuis
 * {@code elite-backend-client-<app.profile>.properties}.
 */
public final class BackendBundledProperties {

    private static final Properties PROPS = load();

    private BackendBundledProperties() {
    }

    private static Properties load() {
        String profile = System.getProperty("app.profile", "dev");
        Properties p = new Properties();
        String path = "/elite-backend-client-" + profile + ".properties";
        try (InputStream in = BackendBundledProperties.class.getResourceAsStream(path)) {
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
