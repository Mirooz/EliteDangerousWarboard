package be.mirooz.elitedangerous.dashboard.service.webservice.eddn;

/**
 * Nom et version de l'application pour l'en-tête EDDN ({@code softwareName}/{@code softwareVersion}).
 * La récupération de version reprend la logique d'{@code AnalyticsClient} : system property, manifest,
 * puis {@code pom.properties} généré par Maven, avec un fallback constant.
 */
public final class EddnAppInfo {

    public static final String SOFTWARE_NAME = "Elite Warboard";

    private static final String FALLBACK_VERSION = "1.3.2-SNAPSHOT";

    private static volatile String cachedVersion;

    private EddnAppInfo() {}

    public static String version() {
        String v = cachedVersion;
        if (v != null) {
            return v;
        }
        v = resolveVersion();
        cachedVersion = v;
        return v;
    }

    private static String resolveVersion() {
        try {
            String version = System.getProperty("project.version");
            if (version != null && !version.isEmpty()) {
                return version;
            }
            Package pkg = EddnAppInfo.class.getPackage();
            if (pkg != null) {
                String implVersion = pkg.getImplementationVersion();
                if (implVersion != null && !implVersion.isEmpty()) {
                    return implVersion;
                }
            }
            try (var inputStream = EddnAppInfo.class.getResourceAsStream(
                    "/META-INF/maven/be.mirooz.elitedangerous/elite-warboard-missions/pom.properties")) {
                if (inputStream != null) {
                    var properties = new java.util.Properties();
                    properties.load(inputStream);
                    version = properties.getProperty("version");
                    if (version != null && !version.isEmpty()) {
                        return version;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return FALLBACK_VERSION;
    }
}
