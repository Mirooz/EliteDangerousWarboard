package be.mirooz.elitedangerous.analytics.controller;

import be.mirooz.elitedangerous.analytics.dto.*;
import be.mirooz.elitedangerous.analytics.model.PanelTime;
import be.mirooz.elitedangerous.analytics.model.UserSession;
import be.mirooz.elitedangerous.analytics.repository.PanelTimeRepository;
import be.mirooz.elitedangerous.analytics.repository.UserSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Controller REST pour les endpoints analytics
 */
@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*") // Permettre les appels depuis n'importe quelle origine
public class AnalyticsController {

    private final UserSessionRepository userSessionRepository;
    private final PanelTimeRepository panelTimeRepository;

    @Autowired
    public AnalyticsController(UserSessionRepository userSessionRepository,
                              PanelTimeRepository panelTimeRepository) {
        this.userSessionRepository = userSessionRepository;
        this.panelTimeRepository = panelTimeRepository;
    }

    /**
     * Endpoint pour démarrer une nouvelle session
     * POST /api/analytics/sessions/start
     */
    @PostMapping("/sessions/start")
    @Transactional
    public ResponseEntity<StartSessionResponse> startSession(@RequestBody StartSessionRequest request) {
        try {
            if (request.getCommanderName() == null || request.getCommanderName().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new StartSessionResponse(null, "Le nom du commandant est requis"));
            }

            UserSession userSession = UserSession.builder()
                    .commanderName(request.getCommanderName())
                    .appVersion(request.getAppVersion())
                    .sessionStart(LocalDateTime.now())
                    .build();

            userSession = userSessionRepository.save(userSession);

            return ResponseEntity.ok(new StartSessionResponse(
                    userSession.getId(),
                    "Session démarrée avec succès"
            ));
        } catch (Exception e) {
            System.err.println("Erreur lors du démarrage de la session: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StartSessionResponse(null, "Erreur lors du démarrage de la session: " + e.getMessage()));
        }
    }

    /**
     * Endpoint pour fermer une session et enregistrer les temps de panel
     * POST /api/analytics/sessions/{sessionId}/end
     */
    @PostMapping("/sessions/{sessionId}/end")
    @Transactional
    public ResponseEntity<EndSessionResponse> endSession(
            @PathVariable Long sessionId,
            @RequestBody EndSessionRequest request) {
        try {
            UserSession userSession = userSessionRepository.findById(sessionId)
                    .orElse(null);

            if (userSession == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new EndSessionResponse(null, null, "Session non trouvée avec l'ID: " + sessionId));
            }

            // Enregistrer les temps de panel
            if (request.getPanelTimes() != null) {
                for (Map.Entry<String, Long> entry : request.getPanelTimes().entrySet()) {
                    String panelName = entry.getKey();
                    Long durationSeconds = entry.getValue();

                    if (durationSeconds != null && durationSeconds > 0) {
                        PanelTime panelTime = PanelTime.builder()
                                .session(userSession)
                                .panelName(panelName)
                                .durationSeconds(durationSeconds)
                                .build();

                        panelTimeRepository.save(panelTime);
                    }
                }
            }

            // Mettre à jour la fin de session
            userSession.setSessionEnd(LocalDateTime.now());
            userSession.calculateDuration();
            userSessionRepository.save(userSession);

            return ResponseEntity.ok(new EndSessionResponse(
                    userSession.getId(),
                    userSession.getDurationSeconds(),
                    "Session fermée avec succès"
            ));
        } catch (Exception e) {
            System.err.println("Erreur lors de la fermeture de la session: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new EndSessionResponse(null, null, "Erreur lors de la fermeture de la session: " + e.getMessage()));
        }
    }

    /**
     * Endpoint de santé pour vérifier que l'API est accessible
     * GET /api/analytics/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "OK", "service", "analytics-backend"));
    }
}

