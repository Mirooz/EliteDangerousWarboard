package be.mirooz.elitedangerous.backend.analytics;

import be.mirooz.elitedangerous.backend.BackendBundledProperties;
import be.mirooz.elitedangerous.backend.generated.ApiClient;
import be.mirooz.elitedangerous.backend.generated.ApiException;
import be.mirooz.elitedangerous.backend.generated.api.AnalyticsControllerApi;
import be.mirooz.elitedangerous.backend.generated.model.EndSessionRequest;
import be.mirooz.elitedangerous.backend.generated.model.LatestVersionResponse;
import be.mirooz.elitedangerous.backend.generated.model.StartSessionRequest;
import be.mirooz.elitedangerous.backend.generated.model.StartSessionResponse;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client OpenAPI pour les endpoints analytics (sessions, version).
 */
public class AnalyticsClient {

    private static AnalyticsClient instance;
    private final AnalyticsControllerApi analyticsApi;
    private volatile Long currentSessionId;

    private final Map<String, Long> panelDurations = new ConcurrentHashMap<>();
    private final Map<String, Long> panelStartTimes = new ConcurrentHashMap<>();

    private AnalyticsClient() {
        String baseUrl = BackendBundledProperties.get("backend.base-url", "http://localhost:8080");
        ApiClient apiClient = new ApiClient();
        apiClient.updateBaseUri(baseUrl);
        apiClient.setReadTimeout(Duration.ofSeconds(10));
        this.analyticsApi = new AnalyticsControllerApi(apiClient);
    }

    public static synchronized AnalyticsClient getInstance() {
        if (instance == null) {
            instance = new AnalyticsClient();
        }
        return instance;
    }

    public void startSession(String commanderName) {
        try {
            if (currentSessionId != null) {
                return;
            }
            String appVersion = getAppVersion();
            String operatingSystem = getOperatingSystem();
            StartSessionRequest body = new StartSessionRequest()
                    .commanderName(commanderName)
                    .appVersion(appVersion)
                    .operatingSystem(operatingSystem);

            CompletableFuture.runAsync(() -> {
                try {
                    StartSessionResponse sessionResponse = analyticsApi.apiAnalyticsSessionsStartPost(body);
                    currentSessionId = sessionResponse.getSessionId();
                    panelDurations.clear();
                    panelStartTimes.clear();
                    startPanelTime("Missions");
                } catch (ApiException e) {
                    System.err.println("Erreur lors du démarrage de la session: " + e.getCode() + " - " + e.getMessage());
                } catch (Exception e) {
                    System.err.println("Erreur traitement async: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            System.err.println("Erreur lors de l'appel au backend analytics: " + e.getMessage());
        }
    }

    private String getAppVersion() {
        try {
            String version = System.getProperty("project.version");
            if (version != null && !version.isEmpty()) {
                return version;
            }

            Package pkg = getClass().getPackage();
            if (pkg != null) {
                String implVersion = pkg.getImplementationVersion();
                if (implVersion != null && !implVersion.isEmpty()) {
                    return implVersion;
                }
            }

            try (var inputStream = getClass().getResourceAsStream("/META-INF/maven/be.mirooz.elitedangerous/elite-warboard-missions/pom.properties")) {
                if (inputStream != null) {
                    var properties = new java.util.Properties();
                    properties.load(inputStream);
                    version = properties.getProperty("version");
                    if (version != null && !version.isEmpty()) {
                        return version;
                    }
                }
            }

            return "1.2.0-SNAPSHOT";
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération de la version: " + e.getMessage());
            return "1.2.0-SNAPSHOT";
        }
    }

    private String getOperatingSystem() {
        String osName = System.getProperty("os.name", "Unknown");
        String osVersion = System.getProperty("os.version", "");
        String osArch = System.getProperty("os.arch", "");

        String os = osName;
        if (!osVersion.isEmpty()) {
            os += " " + osVersion;
        }
        if (!osArch.isEmpty()) {
            os += " (" + osArch + ")";
        }

        return os;
    }

    public void endSession() {
        if (currentSessionId == null) {
            return;
        }

        try {
            accumulateCurrentPanelTime();
            analyticsApi.apiAnalyticsSessionsSessionIdEndPost(
                    currentSessionId,
                    new EndSessionRequest().panelTimes(panelDurations));
        } catch (ApiException e) {
            System.err.println("Erreur lors de l'appel au backend analytics: " + e.getCode() + " - " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erreur lors de l'appel au backend analytics: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void startPanelTime(String panelName) {
        if (panelName == null || panelName.isEmpty() || currentSessionId == null) {
            return;
        }
        if (!panelStartTimes.isEmpty()) {
            accumulateCurrentPanelTime();
        }
        panelStartTimes.put(panelName, System.currentTimeMillis());
    }

    public void endPanelTime(String panelName) {
        if (panelName == null || panelName.isEmpty() || currentSessionId == null) {
            return;
        }

        Long startTime = panelStartTimes.remove(panelName);
        if (startTime == null) {
            return;
        }

        long durationSeconds = (System.currentTimeMillis() - startTime) / 1000;
        if (durationSeconds > 0) {
            panelDurations.merge(panelName, durationSeconds, Long::sum);
        }
    }

    private void accumulateCurrentPanelTime() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : new HashMap<>(panelStartTimes).entrySet()) {
            String panelName = entry.getKey();
            Long startTime = entry.getValue();

            long durationSeconds = (now - startTime) / 1000;
            if (durationSeconds > 0) {
                panelDurations.merge(panelName, durationSeconds, Long::sum);
            }
            panelStartTimes.put(panelName, now);
        }
    }

    public LatestVersionResponse getLatestVersion() {
        try {
            return analyticsApi.apiAnalyticsVersionLatestGet();
        } catch (ApiException e) {
            System.err.println("Erreur lors de la vérification de version: " + e.getCode());
            return null;
        } catch (Exception e) {
            System.err.println("Erreur lors de la vérification de version: " + e.getMessage());
            return null;
        }
    }

    public String getCurrentVersion() {
        try {
            String version = System.getProperty("project.version");
            if (version != null && !version.isEmpty()) {
                return normalizeVersion(version);
            }

            Package pkg = getClass().getPackage();
            if (pkg != null) {
                String implVersion = pkg.getImplementationVersion();
                if (implVersion != null && !implVersion.isEmpty()) {
                    return normalizeVersion(implVersion);
                }
            }

            try (var inputStream = getClass().getResourceAsStream("/META-INF/maven/be.mirooz.elitedangerous/elite-warboard-missions/pom.properties")) {
                if (inputStream != null) {
                    var properties = new java.util.Properties();
                    properties.load(inputStream);
                    version = properties.getProperty("version");
                    if (version != null && !version.isEmpty()) {
                        return normalizeVersion(version);
                    }
                }
            }

            return "1.2.0-SNAPSHOT";
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération de la version: " + e.getMessage());
            return "1.2.0-SNAPSHOT";
        }
    }

    private String normalizeVersion(String version) {
        if (version == null) {
            return "1.2.0-SNAPSHOT";
        }
        if (version.startsWith("v") || version.startsWith("V")) {
            version = version.substring(1);
        }
        return version;
    }

    public boolean isNewerVersion(String currentVersion, String latestVersion) {
        if (currentVersion == null || latestVersion == null) {
            return false;
        }

        currentVersion = normalizeVersion(currentVersion);
        latestVersion = normalizeVersion(latestVersion);

        currentVersion = currentVersion.split("-")[0];
        latestVersion = latestVersion.split("-")[0];

        String[] currentParts = currentVersion.split("\\.");
        String[] latestParts = latestVersion.split("\\.");
        int maxLength = Math.max(currentParts.length, latestParts.length);

        for (int i = 0; i < maxLength; i++) {
            int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
            int latestPart = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;
            if (latestPart > currentPart) {
                return true;
            } else if (latestPart < currentPart) {
                return false;
            }
        }

        return false;
    }
}
