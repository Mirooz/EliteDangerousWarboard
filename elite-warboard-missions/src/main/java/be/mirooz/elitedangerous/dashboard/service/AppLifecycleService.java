package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.backend.generated.model.LatestVersionResponse;
import be.mirooz.elitedangerous.dashboard.model.registries.fleetcarrier.CarrierStatus;
import be.mirooz.elitedangerous.dashboard.service.journal.watcher.JournalTailService;
import be.mirooz.elitedangerous.dashboard.service.journal.watcher.JournalWatcherService;
import be.mirooz.elitedangerous.dashboard.service.persistence.PersistenceService;
import be.mirooz.elitedangerous.dashboard.service.webservice.AnalyticsService;
import be.mirooz.elitedangerous.dashboard.view.common.VersionUpdateNotificationComponent;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Point unique de cycle de vie applicatif (démarrage + fermeture).
 */
public final class AppLifecycleService {

    private static final AppLifecycleService INSTANCE = new AppLifecycleService();

    private final AtomicBoolean startupStarted = new AtomicBoolean(false);
    private final AtomicBoolean shutdownStarted = new AtomicBoolean(false);
    private final AtomicBoolean commanderSwitchPendingFleetRefresh = new AtomicBoolean(false);

    private AppLifecycleService() {
    }

    public static AppLifecycleService getInstance() {
        return INSTANCE;
    }

    /**
     * Démarrage runtime de l'app : services actifs + chargement missions + checks backend.
     */
    public void onStart(Stage stage, ComboBox<String> hiddenFocusComboBox, StackPane rootPane) {
        if (!startupStarted.compareAndSet(false, true)) {
            return;
        }
        WindowToggleService.getInstance().initialize(stage, hiddenFocusComboBox, rootPane);
        WindowToggleService.getInstance().start();
        DashboardService.getInstance().initActiveMissions();
        checkForUpdatesAsync(rootPane);
    }

    /**
     * Étape intermédiaire lors d'un changement de commandant (sans arrêter watchers/lectures).
     * - flush du scope courant
     * - fermeture session analytics courante
     * - reset du marqueur d'activité carrier (timestamp d'insertion)
     * - switch du scope de persistance vers le nouveau FID
     */
    public void onCommanderSwitch(String previousFid, String nextFid) {
        if (previousFid == null || previousFid.isBlank() || previousFid.equals(nextFid)) {
            PersistenceService.getInstance().useCommanderScope(nextFid);
            return;
        }
        try {
            PersistenceService.getInstance().saveAllNow();
        } catch (Exception e) {
            System.err.println("[Lifecycle] Commander switch save failed: " + e.getMessage());
        }
        try {
            AnalyticsService.getInstance().endSession();
        } catch (Exception e) {
            System.err.println("[Lifecycle] Commander switch endSession failed: " + e.getMessage());
        }
        CarrierStatus.getInstance().clearJournalActivityMarker();
        commanderSwitchPendingFleetRefresh.set(true);
        PersistenceService.getInstance().useCommanderScope(nextFid);
    }

    /**
     * @return true une seule fois après un switch commandant (puis reset automatiquement).
     */
    public boolean consumeCommanderSwitchFleetRefreshFlag() {
        return commanderSwitchPendingFleetRefresh.getAndSet(false);
    }

    /**
     * Fermeture propre applicative. Idempotent.
     *
     * @param context message de contexte pour les logs.
     * @param error   erreur éventuelle ayant déclenché la fermeture (nullable).
     */
    public void shutdown(String context, Throwable error) {
        shutdown(context, error, false);
    }

    /**
     * @param skipPersistenceSave si {@code true}, ne pas appeler {@link PersistenceService#saveAllNow()}
     *                            (ex. après suppression du dossier commandant).
     */
    public void shutdown(String context, Throwable error, boolean skipPersistenceSave) {
        if (!shutdownStarted.compareAndSet(false, true)) {
            return;
        }

        String safeContext = (context == null || context.isBlank()) ? "unknown" : context;
        System.out.println("[Lifecycle] Shutdown start: " + safeContext);
        if (error != null) {
            System.err.println("[Lifecycle] Shutdown cause: " + error.getMessage());
        }

        try {
            AnalyticsService.getInstance().endSession();
        } catch (Exception e) {
            System.err.println("[Lifecycle] Analytics stop failed: " + e.getMessage());
        }

        if (!skipPersistenceSave) {
            try {
                PersistenceService.getInstance().saveAllNow();
            } catch (Exception e) {
                System.err.println("[Lifecycle] Persistence save failed: " + e.getMessage());
            }
            PersistenceService.getInstance().unregisterJvmShutdownHook();
        }

        try {
            JournalTailService.getInstance().stop();
        } catch (Exception e) {
            System.err.println("[Lifecycle] Journal tail stop failed: " + e.getMessage());
        }

        try {
            JournalWatcherService.getInstance().stop();
        } catch (Exception e) {
            System.err.println("[Lifecycle] Journal watcher stop failed: " + e.getMessage());
        }

        try {
            WindowToggleService.getInstance().stop();
        } catch (Exception e) {
            System.err.println("[Lifecycle] Window toggle stop failed: " + e.getMessage());
        }

        try {
            LoggingService.getInstance().shutdown();
        } catch (Exception e) {
            System.err.println("[Lifecycle] Logging stop failed: " + e.getMessage());
        }

        Platform.exit();
        System.exit(error == null ? 0 : 1);
    }

    private void checkForUpdatesAsync(StackPane rootPane) {
        new Thread(() -> {
            try {
                AnalyticsService analyticsService = AnalyticsService.getInstance();
                String currentVersion = analyticsService.getCurrentVersion();
                LatestVersionResponse latestVersion = analyticsService.getLatestVersion();
                if (latestVersion == null) {
                    return;
                }
                String latestVersionTag = latestVersion.getTagName();
                if (!analyticsService.isNewerVersion(currentVersion, latestVersionTag)) {
                    return;
                }
                Platform.runLater(() -> {
                    StackPane popupContainer = findPopupContainer(rootPane);
                    StackPane target = popupContainer != null ? popupContainer : rootPane;
                    new VersionUpdateNotificationComponent(
                            latestVersionTag,
                            latestVersion.getName(),
                            latestVersion.getHtmlUrl(),
                            target
                    );
                });
            } catch (Exception e) {
                System.err.println("Erreur lors de la vérification de version: " + e.getMessage());
            }
        }, "AppLifecycle-update-check").start();
    }

    private StackPane findPopupContainer(Node root) {
        if (root instanceof StackPane stackPane) {
            if (root.getId() != null && root.getId().equals("popupContainer")) {
                return stackPane;
            }
        }
        if (root instanceof Pane pane) {
            for (Node child : pane.getChildren()) {
                StackPane found = findPopupContainer(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}

