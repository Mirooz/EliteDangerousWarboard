package be.mirooz.elitedangerous.analytics.service;

import be.mirooz.elitedangerous.analytics.model.PanelTime;
import be.mirooz.elitedangerous.analytics.model.UserSession;
import be.mirooz.elitedangerous.analytics.repository.PanelTimeRepository;
import be.mirooz.elitedangerous.analytics.repository.UserSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service Spring pour enregistrer les données d'utilisation de l'application
 * Les données ne sont enregistrées qu'au lancement et à la fermeture de l'application
 */
@Service
public class AnalyticsService {

    private final UserSessionRepository userSessionRepository;
    private final PanelTimeRepository panelTimeRepository;
    
    // Stocke la session en cours pour chaque thread
    private final ThreadLocal<UserSession> currentSession = new ThreadLocal<>();
    
    // Stocke le temps de début pour chaque panel dans la session actuelle (en mémoire)
    private final Map<String, LocalDateTime> panelStartTimes = new ConcurrentHashMap<>();
    
    // Stocke les durées accumulées par panel (en mémoire, jusqu'à la fermeture)
    private final Map<String, Long> panelDurations = new ConcurrentHashMap<>();

    @Autowired
    public AnalyticsService(UserSessionRepository userSessionRepository, 
                           PanelTimeRepository panelTimeRepository) {
        this.userSessionRepository = userSessionRepository;
        this.panelTimeRepository = panelTimeRepository;
    }

    /**
     * Enregistre le début d'une session utilisateur (appelé au lancement de l'app)
     * 
     * @param commanderName Le nom du commandant
     * @param appVersion La version de l'application
     * @return L'ID de la session créée
     */
    @Transactional
    public Long startSession(String commanderName, String appVersion) {
        if (commanderName == null || commanderName.isEmpty()) {
            System.err.println("Le nom du commandant ne peut pas être vide");
            return null;
        }

        try {
            UserSession userSession = UserSession.builder()
                    .commanderName(commanderName)
                    .appVersion(appVersion)
                    .sessionStart(LocalDateTime.now())
                    .build();

            userSession = userSessionRepository.save(userSession);
            currentSession.set(userSession);
            
            // Réinitialiser les compteurs de temps de panel
            panelStartTimes.clear();
            panelDurations.clear();
            
            System.out.println("✅ Session démarrée pour le commandant: " + commanderName + " (ID: " + userSession.getId() + ")");
            return userSession.getId();
        } catch (Exception e) {
            System.err.println("Erreur lors du démarrage de la session: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Enregistre la fin d'une session utilisateur et sauvegarde tous les temps de panel (appelé à la fermeture de l'app)
     * 
     * @param sessionId L'ID de la session à fermer
     */
    @Transactional
    public void endSession(Long sessionId) {
        if (sessionId == null) {
            System.err.println("L'ID de session ne peut pas être null");
            return;
        }

        try {
            UserSession userSession = userSessionRepository.findById(sessionId)
                    .orElse(null);
            
            if (userSession == null) {
                System.err.println("Session non trouvée avec l'ID: " + sessionId);
                return;
            }

            // Calculer et sauvegarder tous les temps de panel accumulés
            saveAllPanelTimes(userSession);

            // Mettre à jour la fin de session
            userSession.setSessionEnd(LocalDateTime.now());
            userSession.calculateDuration();

            userSessionRepository.save(userSession);
            
            currentSession.remove();
            panelStartTimes.clear();
            panelDurations.clear();
            
            System.out.println("✅ Session fermée pour le commandant: " + userSession.getCommanderName() + 
                             " (Durée: " + userSession.getDurationSeconds() + " secondes)");
        } catch (Exception e) {
            System.err.println("Erreur lors de la fermeture de la session: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Enregistre le début du temps passé sur un panel (en mémoire uniquement)
     * 
     * @param panelName Le nom du panel (Missions, Mining, Exploration)
     */
    public void startPanelTime(String panelName) {
        if (panelName == null || panelName.isEmpty()) {
            return;
        }

        UserSession session = currentSession.get();
        if (session == null) {
            return;
        }

        // Si on quitte un panel précédent, accumuler son temps
        if (!panelStartTimes.isEmpty()) {
            accumulateCurrentPanelTime();
        }

        // Démarrer le tracking du nouveau panel
        panelStartTimes.put(panelName, LocalDateTime.now());
    }

    /**
     * Enregistre la fin du temps passé sur un panel (en mémoire uniquement)
     * 
     * @param panelName Le nom du panel (Missions, Mining, Exploration)
     */
    public void endPanelTime(String panelName) {
        if (panelName == null || panelName.isEmpty()) {
            return;
        }

        UserSession session = currentSession.get();
        if (session == null) {
            return;
        }

        LocalDateTime startTime = panelStartTimes.remove(panelName);
        if (startTime == null) {
            return;
        }

        long durationSeconds = java.time.Duration.between(startTime, LocalDateTime.now()).getSeconds();
        if (durationSeconds > 0) {
            // Accumuler le temps en mémoire (ne pas sauvegarder en DB)
            panelDurations.merge(panelName, durationSeconds, Long::sum);
        }
    }

    /**
     * Accumule le temps du panel actuellement actif dans les durées totales
     */
    private void accumulateCurrentPanelTime() {
        for (Map.Entry<String, LocalDateTime> entry : new ArrayList<>(panelStartTimes.entrySet())) {
            String panelName = entry.getKey();
            LocalDateTime startTime = entry.getValue();
            
            long durationSeconds = java.time.Duration.between(startTime, LocalDateTime.now()).getSeconds();
            if (durationSeconds > 0) {
                panelDurations.merge(panelName, durationSeconds, Long::sum);
            }
            
            // Mettre à jour le temps de début pour continuer le tracking
            panelStartTimes.put(panelName, LocalDateTime.now());
        }
    }

    /**
     * Sauvegarde tous les temps de panel accumulés en mémoire dans la base de données
     * (appelé uniquement à la fermeture de l'application)
     */
    @Transactional
    private void saveAllPanelTimes(UserSession userSession) {
        // Accumuler le temps du panel actuellement actif
        accumulateCurrentPanelTime();
        
        // Sauvegarder toutes les durées accumulées
        for (Map.Entry<String, Long> entry : panelDurations.entrySet()) {
            String panelName = entry.getKey();
            Long totalDuration = entry.getValue();
            
            if (totalDuration > 0) {
                PanelTime panelTime = PanelTime.builder()
                        .session(userSession)
                        .panelName(panelName)
                        .durationSeconds(totalDuration)
                        .build();

                panelTimeRepository.save(panelTime);
                System.out.println("  - Panel " + panelName + ": " + totalDuration + " secondes");
            }
        }
    }
}
