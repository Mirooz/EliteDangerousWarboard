package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.analytics.AnalyticsClient;
import be.mirooz.elitedangerous.analytics.dto.LatestVersionResponse;
import be.mirooz.elitedangerous.analytics.dto.spansh.SpanshSearchRequest;
import be.mirooz.elitedangerous.analytics.dto.spansh.SpanshSearchRequestDTO;
import be.mirooz.elitedangerous.analytics.dto.spansh.SpanshSearchResponseDTO;

/**
 * Service pour gérer les analytics de l'application.
 * Utilise le module analytics-backend-client pour communiquer avec le backend.
 */
public class AnalyticsService {
    
    private static final AnalyticsService INSTANCE = new AnalyticsService();
    private final AnalyticsClient analyticsClient;
    
    private AnalyticsService() {
        this.analyticsClient = AnalyticsClient.getInstance();
    }
    
    public static AnalyticsService getInstance() {
        return INSTANCE;
    }
    
    /**
     * Démarre une session analytics pour un commandant
     */
    public void startSession(String commanderName) {
        analyticsClient.startSession(commanderName);
    }
    
    /**
     * Termine la session analytics en cours
     */
    public void endSession() {
        analyticsClient.endSession();
    }
    
    /**
     * Démarre le tracking du temps passé sur un panel
     */
    public void startPanelTime(String panelName) {
        analyticsClient.startPanelTime(panelName);
    }
    
    /**
     * Arrête le tracking du temps passé sur un panel
     */
    public void endPanelTime(String panelName) {
        analyticsClient.endPanelTime(panelName);
    }
    
    /**
     * Récupère la dernière version disponible depuis l'API
     */
    public LatestVersionResponse getLatestVersion() {
        return analyticsClient.getLatestVersion();
    }
    
    /**
     * Récupère la version actuelle de l'application
     */
    public String getCurrentVersion() {
        return analyticsClient.getCurrentVersion();
    }
    
    /**
     * Compare deux versions
     * @return true si latestVersion est plus récente que currentVersion
     */
    public boolean isNewerVersion(String currentVersion, String latestVersion) {
        return analyticsClient.isNewerVersion(currentVersion, latestVersion);
    }
    
    /**
     * Appelle l'endpoint /api/spansh/search du backend analytics
     * @param searchRequest La requête de recherche Spansh
     * @return SpanshSearchResponseDTO contenant la réponse de l'API
     * @throws Exception en cas d'erreur lors de l'appel HTTP
     */
    public SpanshSearchResponseDTO searchSpansh(SpanshSearchRequest searchRequest) throws Exception {
        return analyticsClient.searchSpansh(searchRequest);
    }
    
    /**
     * Appelle l'endpoint /api/spansh/search du backend analytics avec un DTO
     * @param searchRequestDTO Le DTO de requête de recherche Spansh (contient uniquement le système de référence)
     * @return SpanshSearchResponseDTO contenant la réponse de l'API
     * @throws Exception en cas d'erreur lors de l'appel HTTP
     */
    public SpanshSearchResponseDTO searchSpansh(SpanshSearchRequestDTO searchRequestDTO) throws Exception {
        return analyticsClient.searchSpansh(searchRequestDTO);
    }
}
