package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.dashboard.service.analytics.dto.LatestVersionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Properties;

/**
 * Service pour vérifier la dernière version disponible de l'application
 */
public class VersionCheckService {
    
    private static VersionCheckService instance;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    
    private VersionCheckService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = new ObjectMapper();
        this.baseUrl = loadBackendUrl();
    }
    
    public static synchronized VersionCheckService getInstance() {
        if (instance == null) {
            instance = new VersionCheckService();
        }
        return instance;
    }
    
    /**
     * Charge l'URL du backend depuis le fichier de propriétés (même logique que AnalyticsClient)
     */
    private String loadBackendUrl() {
        String profile = System.getProperty("app.profile", "dev");
        String fileName = "/analytics-client-" + profile + ".properties";
        
        try (InputStream inputStream = getClass().getResourceAsStream(fileName)) {
            if (inputStream != null) {
                Properties properties = new Properties();
                properties.load(inputStream);
                String url = properties.getProperty("analytics.backend.url");
                if (url != null && !url.isBlank()) {
                    return url;
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de " + fileName + ": " + e.getMessage());
        }
        
        return "http://localhost:8080";
    }
    
    /**
     * Récupère la dernière version disponible depuis l'API
     * @return LatestVersionResponse ou null en cas d'erreur
     */
    public LatestVersionResponse getLatestVersion() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/analytics/version/latest"))
                    .header("Accept", "application/json")
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), LatestVersionResponse.class);
            } else {
                System.err.println("Erreur lors de la vérification de version: " + response.statusCode());
                return null;
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la vérification de version: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Récupère la version actuelle de l'application
     * @return Version actuelle (ex: "1.2.0" ou "1.2.0-SNAPSHOT")
     */
    public String getCurrentVersion() {
        try {
            // Essayer de lire depuis les propriétés système
            String version = System.getProperty("project.version");
            if (version != null && !version.isEmpty()) {
                return normalizeVersion(version);
            }
            
            // Essayer de lire depuis le manifest JAR
            Package pkg = getClass().getPackage();
            if (pkg != null) {
                String implVersion = pkg.getImplementationVersion();
                if (implVersion != null && !implVersion.isEmpty()) {
                    return normalizeVersion(implVersion);
                }
            }
            
            // Essayer de lire depuis les ressources Maven
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
            
            // Valeur par défaut
            return "1.2.0-SNAPSHOT";
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération de la version: " + e.getMessage());
            return "1.2.0-SNAPSHOT";
        }
    }
    
    /**
     * Normalise la version en retirant le préfixe "v" et le suffixe "-SNAPSHOT"
     * @param version Version à normaliser
     * @return Version normalisée
     */
    private String normalizeVersion(String version) {
        if (version == null) return "1.2.0-SNAPSHOT";
        
        // Retirer le préfixe "v" si présent
        if (version.startsWith("v") || version.startsWith("V")) {
            version = version.substring(1);
        }
        
        return version;
    }
    
    /**
     * Compare deux versions
     * @param currentVersion Version actuelle
     * @param latestVersion Version la plus récente
     * @return true si latestVersion est plus récente que currentVersion
     */
    public boolean isNewerVersion(String currentVersion, String latestVersion) {
        if (currentVersion == null || latestVersion == null) {
            return false;
        }
        
        // Normaliser les versions
        currentVersion = normalizeVersion(currentVersion);
        latestVersion = normalizeVersion(latestVersion);
        
        // Retirer les suffixes comme -SNAPSHOT pour la comparaison
        currentVersion = currentVersion.split("-")[0];
        latestVersion = latestVersion.split("-")[0];
        
        // Comparer les versions (format: X.Y.Z)
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
        
        return false; // Versions identiques
    }
}

