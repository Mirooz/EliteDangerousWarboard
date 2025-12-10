package be.mirooz.elitedangerous.dashboard.service.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Client HTTP pour communiquer avec le backend analytics
 */
public class AnalyticsClient {

    private static AnalyticsClient instance;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private Long currentSessionId;
    
    // Stocke les temps de panel en mémoire
    private final Map<String, Long> panelDurations = new ConcurrentHashMap<>();
    private final Map<String, Long> panelStartTimes = new ConcurrentHashMap<>();

    private AnalyticsClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = new ObjectMapper();
        // URL du backend (chargée depuis le fichier de propriétés, puis propriété système, puis variable d'environnement)
        this.baseUrl = loadBackendUrl();
    }

    /**
     * Charge l'URL du backend analytics depuis le fichier de propriétés
     * Ordre de priorité :
     * 1. Propriété système (analytics.backend.url)
     * 2. Variable d'environnement (ANALYTICS_BACKEND_URL)
     * 3. Fichier analytics-client-dev.properties
     * 4. Valeur par défaut (http://localhost:8080)
     */
    private String loadBackendUrl() {
        // 1. On lit le profil passé en argument JVM (-Dapp.profile=xxx)
        String profile = System.getProperty("app.profile", "dev"); // dev par défaut
        String fileName = "/analytics-client-" + profile + ".properties";

        System.out.println("Chargement du profil : " + profile + " → " + fileName);

        // 2. Lecture du fichier correspondant
        try (InputStream inputStream = getClass().getResourceAsStream(fileName)) {
            if (inputStream != null) {
                Properties properties = new Properties();
                properties.load(inputStream);

                String url = properties.getProperty("analytics.backend.url");
                if (url != null && !url.isBlank()) {
                    return url;
                }
            } else {
                System.err.println("⚠️ Fichier introuvable : " + fileName);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de " + fileName + ": " + e.getMessage());
        }

        // 3. Valeur par défaut
        return "http://localhost:8080";
    }

    public static synchronized AnalyticsClient getInstance() {
        if (instance == null) {
            instance = new AnalyticsClient();
        }
        return instance;
    }
    public void startSession(String commanderName) {
        try {
            String appVersion = getAppVersion();
            String operatingSystem = getOperatingSystem();

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("commanderName", commanderName);
            requestBody.put("appVersion", appVersion);
            requestBody.put("operatingSystem", operatingSystem);

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/analytics/sessions/start"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            // 🔥 FIRE-AND-FORGET — on envoie sans attendre la réponse
            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        try {
                            if (response.statusCode() == 200) {
                                StartSessionResponse sessionResponse =
                                        objectMapper.readValue(response.body(), StartSessionResponse.class);

                                currentSessionId = sessionResponse.getSessionId();
                                panelDurations.clear();
                                panelStartTimes.clear();

                                System.out.println("✅ Session analytics démarrée (ID: " + currentSessionId + ")");
                                startPanelTime("Missions");

                            } else {
                                System.err.println("Erreur lors du démarrage de la session: "
                                        + response.statusCode() + " - " + response.body());
                            }
                        } catch (Exception e) {
                            System.err.println("Erreur traitement async: " + e.getMessage());
                        }
                    });


        } catch (Exception e) {
            System.err.println("Erreur lors de l'appel au backend analytics: " + e.getMessage());
        }
    }


    /**
     * Récupère la version de l'application depuis les propriétés Maven
     */
    private String getAppVersion() {
        try {
            // Essayer de lire depuis les propriétés système (définies par Maven)
            String version = System.getProperty("project.version");
            if (version != null && !version.isEmpty()) {
                return version;
            }

            // Essayer de lire depuis le manifest JAR
            Package pkg = getClass().getPackage();
            if (pkg != null) {
                String implVersion = pkg.getImplementationVersion();
                if (implVersion != null && !implVersion.isEmpty()) {
                    return implVersion;
                }
            }

            // Essayer de lire depuis les ressources Maven (fichier généré par maven-archiver)
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

            // Valeur par défaut
            return "1.2.0-SNAPSHOT";
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération de la version: " + e.getMessage());
            return "1.2.0-SNAPSHOT";
        }
    }

    /**
     * Récupère le système d'exploitation
     */
    private String getOperatingSystem() {
        String osName = System.getProperty("os.name", "Unknown");
        String osVersion = System.getProperty("os.version", "");
        String osArch = System.getProperty("os.arch", "");
        
        // Formater le nom du système d'exploitation de manière lisible
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
            // Accumuler le temps du panel actuellement actif
            accumulateCurrentPanelTime();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("panelTimes", panelDurations);

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/analytics/sessions/" + currentSessionId + "/end"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            System.out.println("Session analytics fermée ");
            // 🔥 FIRE-AND-FORGET — on envoie sans attendre la réponse
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            System.err.println("Erreur lors de l'appel au backend analytics: " + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * Démarre le tracking d'un panel (en mémoire uniquement)
     */
    public void startPanelTime(String panelName) {
        if (panelName == null || panelName.isEmpty() || currentSessionId == null) {
            return;
        }

        // Si on quitte un panel précédent, accumuler son temps
        if (!panelStartTimes.isEmpty()) {
            accumulateCurrentPanelTime();
        }

        panelStartTimes.put(panelName, System.currentTimeMillis());
    }

    /**
     * Arrête le tracking d'un panel (en mémoire uniquement)
     */
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
            panelDurations.merge(panelName, durationSeconds, (a, b) -> a + b);
        }
    }

    /**
     * Accumule le temps du panel actuellement actif
     */
    private void accumulateCurrentPanelTime() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : new HashMap<>(panelStartTimes).entrySet()) {
            String panelName = entry.getKey();
            Long startTime = entry.getValue();
            
            long durationSeconds = (now - startTime) / 1000;
            if (durationSeconds > 0) {
                panelDurations.merge(panelName, durationSeconds, (a, b) -> a + b);
            }
            
            // Mettre à jour le temps de début pour continuer le tracking
            panelStartTimes.put(panelName, now);
        }
    }

    // Classes internes pour les DTOs
    private static class StartSessionResponse {
        private Long sessionId;
        private String message;

        public Long getSessionId() {
            return sessionId;
        }

        public void setSessionId(Long sessionId) {
            this.sessionId = sessionId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    private static class EndSessionResponse {
        private Long sessionId;
        private Long durationSeconds;
        private String message;

        public Long getSessionId() {
            return sessionId;
        }

        public void setSessionId(Long sessionId) {
            this.sessionId = sessionId;
        }

        public Long getDurationSeconds() {
            return durationSeconds;
        }

        public void setDurationSeconds(Long durationSeconds) {
            this.durationSeconds = durationSeconds;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}

