package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.analytics.AnalyticsClient;
import be.mirooz.elitedangerous.analytics.dto.LatestVersionResponse;
import be.mirooz.elitedangerous.analytics.dto.spansh.SpanshSearchRequest;
import be.mirooz.elitedangerous.analytics.dto.spansh.SpanshSearchRequestDTO;
import be.mirooz.elitedangerous.analytics.dto.spansh.SpanshSearchResponseDTO;
import be.mirooz.elitedangerous.analytics.dto.spansh.SpanshRouteRequestDTO;
import be.mirooz.elitedangerous.analytics.dto.spansh.SpanshRouteResponseDTO;
import be.mirooz.elitedangerous.analytics.dto.spansh.SpanshRouteResultsResponseDTO;

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
    
    /**
     * Récupère les résultats d'une recherche Spansh via son GUID
     * @param guid Le GUID de la recherche Spansh
     * @return SpanshSearchResponseDTO contenant la réponse de l'API
     * @throws Exception en cas d'erreur lors de l'appel HTTP
     * @deprecated Utiliser getSpanshSearchByGuidAndEndpoint à la place
     */
    @Deprecated
    public SpanshSearchResponseDTO getSpanshSearchByGuid(String guid) throws Exception {
        return analyticsClient.getSpanshSearchByGuid(guid);
    }

    /**
     * Appelle un endpoint Spansh spécifique du backend analytics avec un DTO
     * @param endpoint Le nom de l'endpoint (ex: "stratum-undiscovered", "expressway-to-exomastery", "road-to-riches")
     * @param searchRequestDTO Le DTO de requête de recherche Spansh (contient uniquement le système de référence)
     * @return SpanshSearchResponseDTO contenant la réponse de l'API
     * @throws Exception en cas d'erreur lors de l'appel HTTP
     */
    public SpanshSearchResponseDTO searchSpanshByEndpoint(String endpoint, SpanshSearchRequestDTO searchRequestDTO) throws Exception {
        return analyticsClient.searchSpanshByEndpoint(endpoint, searchRequestDTO);
    }

    /**
     * Récupère les résultats d'une recherche Spansh via son GUID et l'endpoint spécifique
     * @param endpoint Le nom de l'endpoint (ex: "stratum-undiscovered", "expressway-to-exomastery", "road-to-riches")
     * @param guid Le GUID de la recherche Spansh
     * @return SpanshSearchResponseDTO contenant la réponse de l'API
     * @throws Exception en cas d'erreur lors de l'appel HTTP
     */
    public SpanshSearchResponseDTO getSpanshSearchByGuidAndEndpoint(String endpoint, String guid) throws Exception {
        return analyticsClient.getSpanshSearchByGuidAndEndpoint(endpoint, guid);
    }

    /**
     * Appelle un endpoint Spansh spécifique du backend analytics avec un DTO de route (contient maxJumpRange)
     * @param endpoint Le nom de l'endpoint (ex: "expressway-to-exomastery", "road-to-riches")
     * @param routeRequestDTO Le DTO de requête de route Spansh (contient maxJumpRange et systemName)
     * @return SpanshRouteResponseDTO contenant searchReference et spanshResponse avec les résultats
     * @throws Exception en cas d'erreur lors de l'appel HTTP
     */
    public SpanshRouteResponseDTO searchSpanshRouteByEndpoint(String endpoint, SpanshRouteRequestDTO routeRequestDTO) throws Exception {
        return analyticsClient.searchSpanshRouteByEndpoint(endpoint, routeRequestDTO);
    }

    /**
     * Récupère les résultats d'une route Spansh via son GUID (job)
     * @param job Le GUID (job) de la route Spansh
     * @return SpanshRouteResultsResponseDTO contenant les résultats de la route
     * @throws Exception en cas d'erreur lors de l'appel HTTP
     */
    public SpanshRouteResultsResponseDTO getSpanshRouteResultsByJob(String job) throws Exception {
        return analyticsClient.getSpanshRouteResultsByJob(job);
    }

    /**
     * Récupère les résultats d'une route Spansh via son GUID en utilisant /api/spansh/search/{guid}
     * Utilisé pour expressway-to-exomastery et road-to-riches lors du rechargement
     * @param guid Le GUID de la route Spansh
     * @return SpanshRouteResultsResponseDTO contenant les résultats de la route
     * @throws Exception en cas d'erreur lors de l'appel HTTP
     */
    public SpanshRouteResultsResponseDTO getSpanshRouteResultsByGuid(String guid) throws Exception {
        return analyticsClient.getSpanshRouteResultsByGuid(guid);
    }
}
