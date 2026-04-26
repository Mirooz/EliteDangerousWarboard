package be.mirooz.elitedangerous.dashboard.service.webservice;

import be.mirooz.elitedangerous.backend.analytics.AnalyticsClient;
import be.mirooz.elitedangerous.backend.generated.model.LatestVersionResponse;
import be.mirooz.elitedangerous.backend.generated.model.SpanshSearchResponseDTO;
import be.mirooz.elitedangerous.backend.generated.model.SpanshRouteResponseDTO;
import be.mirooz.elitedangerous.backend.generated.model.SpanshRouteResultsResponse;
import be.mirooz.elitedangerous.backend.generated.model.SpanshRouteRequestDTO;
import be.mirooz.elitedangerous.backend.generated.model.SpanshSearchRequestDTO;
import be.mirooz.elitedangerous.backend.spansh.ExplorationMode;
import be.mirooz.elitedangerous.backend.spansh.SpanshFacade;

/**
 * Service pour gérer les analytics de l'application.
 * Utilise le module analytics-backend-client pour communiquer avec le backend.
 */
public class AnalyticsService {

    private static final AnalyticsService INSTANCE = new AnalyticsService();
    private final CapiApiService capiApiService;
    private final AnalyticsClient analyticsClient;
    private final SpanshFacade spanshFacade;
    
    private AnalyticsService() {
        this.analyticsClient = AnalyticsClient.getInstance();
        this.spanshFacade = SpanshFacade.getInstance();
        this.capiApiService = CapiApiService.getInstance();
    }
    
    public static AnalyticsService getInstance() {
        return INSTANCE;
    }
    
    /**
     * Démarre une session analytics pour un commandant.
     *
     * @return {@code true} si le contrôle profil CAPI est OK (authentification Frontier valide).
     */
    public boolean startSession(String commanderName) {
        boolean profileOk = capiApiService.checkCapiAuthentication();
        analyticsClient.startSession(commanderName);
        return profileOk;
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
        return analyticsClient.getAppVersion();
    }
    
    /**
     * Compare deux versions
     * @return true si latestVersion est plus récente que currentVersion
     */
    public boolean isNewerVersion(String currentVersion, String latestVersion) {
        return analyticsClient.isNewerVersion(currentVersion, latestVersion);
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
        return spanshFacade.getSpanshSearchByGuid(guid);
    }

    /**
     * Appelle un endpoint Spansh spécifique du backend analytics avec un DTO
     * @param endpoint Le nom de l'endpoint (ex: "stratum-undiscovered", "expressway-to-exomastery", "road-to-riches")
     * @param searchRequestDTO Le DTO de requête de recherche Spansh (contient uniquement le système de référence)
     * @return SpanshSearchResponseDTO contenant la réponse de l'API
     * @throws Exception en cas d'erreur lors de l'appel HTTP
     */
    public SpanshSearchResponseDTO searchSpansh(ExplorationMode mode, SpanshSearchRequestDTO searchRequestDTO) throws Exception {
        return spanshFacade.searchSpansh(mode, searchRequestDTO);
    }

    /**
     * Récupère les résultats d'une recherche Spansh via son GUID et l'endpoint spécifique
     * @param endpoint Le nom de l'endpoint (ex: "stratum-undiscovered", "expressway-to-exomastery", "road-to-riches")
     * @param guid Le GUID de la recherche Spansh
     * @return SpanshSearchResponseDTO contenant la réponse de l'API
     * @throws Exception en cas d'erreur lors de l'appel HTTP
     */
    public SpanshSearchResponseDTO getSpanshSearchByGuidAndMode(ExplorationMode mode, String guid) throws Exception {
        return spanshFacade.getSpanshSearchByGuid(guid);
    }

    /**
     * Appelle un endpoint Spansh spécifique du backend analytics avec un DTO de route (contient maxJumpRange)
     * @param endpoint Le nom de l'endpoint (ex: "expressway-to-exomastery", "road-to-riches")
     * @param routeRequestDTO Le DTO de requête de route Spansh (contient maxJumpRange et systemName)
     * @return SpanshRouteResponseDTO contenant searchReference et spanshResponse avec les résultats
     * @throws Exception en cas d'erreur lors de l'appel HTTP
     */
    public SpanshRouteResponseDTO searchSpanshRoute(ExplorationMode mode, SpanshRouteRequestDTO routeRequestDTO) throws Exception {
        return spanshFacade.searchSpanshRoute(mode, routeRequestDTO);
    }


    /**
     * Récupère les résultats d'une route Spansh via son GUID en utilisant /api/spansh/search/{guid}
     * Utilisé pour expressway-to-exomastery et road-to-riches lors du rechargement
     * @param guid Le GUID de la route Spansh
     * @return SpanshRouteResultsResponseDTO contenant les résultats de la route
     * @throws Exception en cas d'erreur lors de l'appel HTTP
     */
    public SpanshRouteResultsResponse getSpanshRouteResultsByGuid(String guid) throws Exception {
        return spanshFacade.getSpanshRouteResultsByGuid(guid);
    }
}
