package be.mirooz.elitedangerous.capi.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * {@code capi-client-&lt;app.profile&gt;.properties} chargé une fois au chargement de la classe.
 */
public final class CapiBundledProperties {

    private static final Properties PROPS = load();

    private CapiBundledProperties() {
    }

    private static Properties load() {
        String profile = System.getProperty("app.profile", "dev");
        Properties p = new Properties();
        String path = "/capi-client-" + profile + ".properties";
        try (InputStream in = CapiBundledProperties.class.getResourceAsStream(path)) {
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
