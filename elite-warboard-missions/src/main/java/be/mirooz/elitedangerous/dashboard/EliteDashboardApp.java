package be.mirooz.elitedangerous.dashboard;

import be.mirooz.elitedangerous.dashboard.window.StageVisualBounds;
import be.mirooz.elitedangerous.dashboard.window.WindowFramePreferences;
import be.mirooz.elitedangerous.dashboard.window.win32.WindowsUndecoratedVrFrameCompat;
import be.mirooz.elitedangerous.dashboard.view.main.DashboardController;
import be.mirooz.elitedangerous.dashboard.service.AppLifecycleService;
import be.mirooz.elitedangerous.dashboard.service.WindowToggleService;
import be.mirooz.elitedangerous.dashboard.service.ErrorLogsConsentService;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.service.LoggingService;
import be.mirooz.elitedangerous.dashboard.service.PreferencesService;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.List;
import java.util.Objects;

public class EliteDashboardApp extends Application {

    private static final double WORK_AREA_MAX_EPS = 4.0;
    /** Marge (px) : au démarrage, {@link StageVisualBounds#isStageFillingWorkArea} peut être en retard ; on garde maximized=true. */
    private static final double NEARLY_FULL_WORK_AREA_SLACK = 120.0;

    private LocalizationService localizationService = LocalizationService.getInstance();
    private LoggingService loggingService = LoggingService.getInstance();
    private PreferencesService preferencesService = PreferencesService.getInstance();
    private ComboBox<String> comboBox;
    private StackPane rootPane;
    
    private boolean isRestoringWindow = false; // Flag pour éviter de sauvegarder pendant la restauration
    /** Évite d’écrire x/y/largeur/hauteur intermédiaires (ex. démaximisation : x/y puis w/h). */
    private boolean saveWindowPositionCoalesceScheduled;
    /**
     * Tant que c’est {@code false}, les listeners de géométrie n’enregistrent pas les prefs : au démarrage,
     * VR Win32 / layout peuvent provoquer des valeurs transitoires et écraser la position « non maximisée ».
     */
    private boolean windowGeometryListenerSavesEnabled;

    @Override
    public void start(Stage stage) {

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
            BorderPane dashboardRoot = loader.load();
            DashboardController dashboardController = loader.getController();

            StackPane root = new StackPane(dashboardRoot);
            root.getStyleClass().add("elite-app-root");
            this.rootPane = root; // Sauvegarder la référence


            stage.initStyle(WindowFramePreferences.useNativeOsWindowFrame() ? StageStyle.DECORATED : StageStyle.UNDECORATED);
            comboBox = new ComboBox<>(FXCollections.observableArrayList(" "));
            comboBox.setPromptText(".");
            comboBox.setPrefWidth(1);
            comboBox.setPrefHeight(1);
            comboBox.setVisible(false);
            comboBox.setManaged(false);
            root.getChildren().add(comboBox);

            // Charger les dimensions depuis les préférences ou utiliser les valeurs par défaut
            String savedWidthStr = preferencesService.getPreference("window.width", "1200");
            String savedHeightStr = preferencesService.getPreference("window.height", "800");
            double initialWidth = 1200;
            double initialHeight = 800;
            try {
                initialWidth = Double.parseDouble(savedWidthStr);
                initialHeight = Double.parseDouble(savedHeightStr);
            } catch (NumberFormatException e) {
                // Utiliser les valeurs par défaut si le parsing échoue
            }
            
            Scene scene = new Scene(root, initialWidth, initialHeight);
            scene.setFill(Color.web("#0A0A0A"));

            scene.getStylesheets().add(
                    Objects.requireNonNull(getClass().getResource("/css/elite-theme.css"))
                            .toExternalForm()
            );

            Image icon = new Image(
                    Objects.requireNonNull(getClass().getResource("/images/elite_dashboard_icon.png"))
                            .toExternalForm()
            );
            stage.getIcons().add(icon);

            String title = localizationService.getString("app.title");
            stage.setTitle(title);
            stage.setScene(scene);
            dashboardController.attachPrimaryStage(stage);
            stage.setResizable(true);
            stage.setMinWidth(1000);
            stage.setMinHeight(600);
            stage.setOpacity(1.0); // Initialiser l'opacité pour les animations
            
            // Restaurer la position et la taille de la fenêtre
            restoreWindowPosition(stage);

            // Écouter les changements de position, taille et état maximisé pour les sauvegarder
            setupWindowListeners(stage);
            
            // Restaurer l'état maximisé après que la fenêtre soit montrée
            stage.setOnShown(event -> {
                String savedMaximized = preferencesService.getPreference("window.maximized", "false");
                boolean maximized = Boolean.parseBoolean(savedMaximized);
                if (maximized && WindowFramePreferences.useNativeOsWindowFrame()) {
                    // Fenêtre décorée : le WM gère la zone au-dessus de la barre des tâches.
                    stage.setMaximized(true);
                } else if (maximized) {
                    // Sans décor : recaler après le premier affichage (layout / peer) puis une frame de plus.
                    Screen targetScreen = resolveWindowScreenFromPreferences();
                    StageVisualBounds.fitStageToVisualBounds(stage, targetScreen);
                    Platform.runLater(() -> StageVisualBounds.fitStageToVisualBounds(stage, targetScreen));
                }
            });

            stage.setOnCloseRequest(event -> {
                // Overlays owned : écrire géométrie dans preferences.properties avant hide du stage principal
                preferencesService.flushOverlayGeometryToPreferencesFile();
                saveWindowPosition(stage);
                stage.hide();
                AppLifecycleService.getInstance().shutdown("window-close-request", null);
            });

            stage.show();

            var windowGeometryStabilizePause = new PauseTransition(Duration.millis(300));
            windowGeometryStabilizePause.setOnFinished(ev -> {
                windowGeometryListenerSavesEnabled = true;
                saveWindowPosition(stage);
            });
            windowGeometryStabilizePause.play();

            if (!WindowFramePreferences.useNativeOsWindowFrame() && WindowsUndecoratedVrFrameCompat.isSupportedOs()) {
                var vrFramePause = new PauseTransition(Duration.millis(100));
                vrFramePause.setOnFinished(e -> {
                    WindowsUndecoratedVrFrameCompat.applyAfterShown(stage);
                    // Ne recaler sur visualBounds que si l’utilisateur a demandé un vrai « plein écran ».
                    // Sinon (maximized=false mais fenêtre grande), fitStageToVisualBounds écrasait x/y/l/h
                    // issus des préférences et cassait max / restore (2ᵉ écran, y négatif, etc.).
                    Platform.runLater(() -> {
                        boolean wantsMax = Boolean.parseBoolean(
                                preferencesService.getPreference("window.maximized", "false"));
                        if (wantsMax) {
                            StageVisualBounds.fitStageToVisualBounds(stage, resolveWindowScreenFromPreferences());
                        }
                    });
                });
                vrFramePause.play();
            }

            AppLifecycleService.getInstance().onStart(stage, comboBox, rootPane);

            // Avant rotation des logs : signalement best-effort du dernier fichier de session
            loggingService.reportSessionLogError();
            loggingService.initialize();
            System.out.println("✅ Application démarrée");

            Platform.runLater(() -> ErrorLogsConsentService.promptIfNeeded(stage));

        } catch (Exception e) {
            AppLifecycleService.getInstance().shutdown("startup-error", e);
            throw new RuntimeException("Erreur lors du chargement du Dashboard", e);
        }
    }

    /**
     * Restaure la position et la taille de la fenêtre depuis les préférences
     */
    private void restoreWindowPosition(Stage stage) {
        isRestoringWindow = true;
        
        try {
            // Récupérer les valeurs sauvegardées
            String savedX = preferencesService.getPreference("window.x", null);
            String savedY = preferencesService.getPreference("window.y", null);
            String savedWidth = preferencesService.getPreference("window.width", null);
            String savedHeight = preferencesService.getPreference("window.height", null);
            String savedMaximized = preferencesService.getPreference("window.maximized", "false");
            String savedScreenIndex = preferencesService.getPreference("window.screen.index", null);
            
            boolean maximized = Boolean.parseBoolean(savedMaximized);
            
            // Si maximisé, positionner sur le bon écran mais ne pas maximiser tout de suite
            // (on le fera dans onShown pour éviter les problèmes)
            if (maximized) {
                StageVisualBounds.fitStageToVisualBounds(stage, resolveWindowScreenFromPreferences());
                isRestoringWindow = false;
                return;
            }
            
            // Vérifier si on a des valeurs sauvegardées pour une fenêtre non maximisée
            if (savedX != null && savedY != null && savedWidth != null && savedHeight != null) {
                try {
                    double x = Double.parseDouble(savedX);
                    double y = StageVisualBounds.clampStageYNonNegative(Double.parseDouble(savedY));
                    double width = Double.parseDouble(savedWidth);
                    double height = Double.parseDouble(savedHeight);
                    
                    // Trouver l'écran sur lequel la fenêtre devrait être
                    Screen targetScreen = getScreenByIndex(savedScreenIndex);
                    if (targetScreen == null) {
                        // Si l'écran n'est pas trouvé, chercher l'écran qui contient la position sauvegardée
                        targetScreen = findScreenContaining(x, y);
                    }
                    
                    // Si toujours pas d'écran, utiliser l'écran principal
                    if (targetScreen == null) {
                        targetScreen = Screen.getPrimary();
                    }
                    
                    Rectangle2D screenBounds = targetScreen.getVisualBounds();
                    
                    // Vérifier que la position est valide (dans les limites de l'écran ou proche)
                    // On accepte une marge pour les fenêtres partiellement hors écran
                    double margin = 100;
                    if (x >= screenBounds.getMinX() - margin && x < screenBounds.getMaxX() + margin &&
                        y >= screenBounds.getMinY() - margin && y < screenBounds.getMaxY() + margin &&
                        width > 0 && height > 0) {
                        
                        // S'assurer que la fenêtre est au moins partiellement visible
                        double finalX = Math.max(screenBounds.getMinX(), Math.min(x, screenBounds.getMaxX() - 100));
                        double finalY = StageVisualBounds.clampStageYNonNegative(
                                Math.max(screenBounds.getMinY(), Math.min(y, screenBounds.getMaxY() - 100)));
                        
                        stage.setX(finalX);
                        stage.setY(finalY);
                        stage.setWidth(width);
                        stage.setHeight(height);
                    } else {
                        // Position invalide, centrer sur l'écran cible
                        centerOnScreen(stage, targetScreen);
                    }
                } catch (NumberFormatException e) {
                    // Si le parsing échoue, utiliser les valeurs par défaut
                    System.err.println("Erreur lors de la restauration de la position de la fenêtre: " + e.getMessage());
                    maximizeWindow(stage);
                }
            } else {
                // Si pas de valeurs sauvegardées, utiliser les valeurs par défaut
                maximizeWindow(stage);
            }
        } finally {
            isRestoringWindow = false;
        }
    }
    
    /**
     * Écran enregistré dans les préférences pour la fenêtre principale, ou écran principal.
     */
    private Screen resolveWindowScreenFromPreferences() {
        Screen targetScreen = getScreenByIndex(preferencesService.getPreference("window.screen.index", null));
        if (targetScreen == null) {
            targetScreen = Screen.getPrimary();
        }
        return targetScreen;
    }

    /**
     * La fenêtre occupe-t-elle presque toute la zone utile de l’écran (tolérance large) ?
     */
    private boolean isStageNearlyFullOnScreen(Stage stage, Screen screen) {
        if (stage == null || screen == null) {
            return false;
        }
        Rectangle2D vb = screen.getVisualBounds();
        return stage.getWidth() >= vb.getWidth() - NEARLY_FULL_WORK_AREA_SLACK
                && stage.getHeight() >= vb.getHeight() - NEARLY_FULL_WORK_AREA_SLACK
                && stage.getX() <= vb.getMinX() + NEARLY_FULL_WORK_AREA_SLACK
                && stage.getY() <= vb.getMinY() + NEARLY_FULL_WORK_AREA_SLACK;
    }

    /**
     * Récupère l'écran par son index
     */
    private Screen getScreenByIndex(String screenIndexStr) {
        if (screenIndexStr == null) {
            return null;
        }
        
        try {
            int screenIndex = Integer.parseInt(screenIndexStr);
            List<Screen> screens = Screen.getScreens();
            if (screenIndex >= 0 && screenIndex < screens.size()) {
                return screens.get(screenIndex);
            }
        } catch (NumberFormatException e) {
            // Ignorer si l'index n'est pas valide
        }
        
        return null;
    }
    
    /**
     * Trouve l'écran qui contient la position donnée
     */
    private Screen findScreenContaining(double x, double y) {
        for (Screen screen : Screen.getScreens()) {
            Rectangle2D bounds = screen.getVisualBounds();
            if (bounds.contains(x, y)) {
                return screen;
            }
        }
        return null;
    }
    
    /**
     * Centre la fenêtre sur l'écran spécifié
     */
    private void centerOnScreen(Stage stage, Screen screen) {
        Rectangle2D bounds = screen.getVisualBounds();
        double width = stage.getWidth() > 0 ? stage.getWidth() : 1200;
        double height = stage.getHeight() > 0 ? stage.getHeight() : 800;
        stage.setX(bounds.getMinX() + (bounds.getWidth() - width) / 2);
        stage.setY(bounds.getMinY() + (bounds.getHeight() - height) / 2);
        stage.setWidth(width);
        stage.setHeight(height);
    }
    
    /**
     * Sauvegarde la géométrie après la fin de la « rafale » de mises à jour du stage (une seule écriture par pulse).
     */
    private void scheduleSaveWindowPosition(Stage stage) {
        if (saveWindowPositionCoalesceScheduled) {
            return;
        }
        saveWindowPositionCoalesceScheduled = true;
        Platform.runLater(() -> {
            saveWindowPositionCoalesceScheduled = false;
            if (!isRestoringWindow && stage.isShowing()) {
                saveWindowPosition(stage);
            }
        });
    }

    /**
     * Sauvegarde la position et la taille de la fenêtre dans les préférences
     */
    private void saveWindowPosition(Stage stage) {
        if (isRestoringWindow) {
            return; // Ne pas sauvegarder pendant la restauration
        }
        if (WindowToggleService.getInstance().isVrWindowGeometryHidden()) {
            // Bind VR : stage 1×1 — ne pas écraser x/y/largeur/hauteur ni le reste à partir de cette géométrie.
            return;
        }

        boolean pseudoMax = !WindowFramePreferences.useNativeOsWindowFrame()
                && StageVisualBounds.isStageFillingWorkArea(stage, WORK_AREA_MAX_EPS);
        boolean prefUndecoratedMax = Boolean.parseBoolean(
                preferencesService.getPreference("window.maximized", "false"));

        boolean skipGeometry;
        boolean persistMaximized;
        if (WindowFramePreferences.useNativeOsWindowFrame()) {
            skipGeometry = stage.isMaximized() || pseudoMax;
            persistMaximized = stage.isMaximized() || pseudoMax;
        } else {
            // Sans décor : tant que window.maximized=true (choix utilisateur via le bouton), ne jamais
            // écraser x/y/largeur/hauteur — ce sont les bornes pour la démaximisation / prochain lancement.
            persistMaximized = stage.isMaximized() || (pseudoMax && prefUndecoratedMax);
            if (prefUndecoratedMax && !persistMaximized) {
                Screen s = getScreenForWindow(stage);
                if (isStageNearlyFullOnScreen(stage, s)) {
                    persistMaximized = true;
                }
            }
            // Inclure pseudoMax : au maximize sans décor, fitStage remplit la zone avant que window.maximized soit true en prefs.
            skipGeometry = stage.isMaximized() || prefUndecoratedMax || pseudoMax;
        }
        // En undecorated « plein zone utile » demandé explicitement, ne pas écraser x/y/largeur/hauteur :
        // ce sont les bornes de restauration pour le bouton restaurer au prochain lancement.
        if (!skipGeometry) {
            preferencesService.setPreference("window.x", String.valueOf(stage.getX()));
            preferencesService.setPreference(
                    "window.y", String.valueOf(StageVisualBounds.clampStageYNonNegative(stage.getY())));
            preferencesService.setPreference("window.width", String.valueOf(stage.getWidth()));
            preferencesService.setPreference("window.height", String.valueOf(stage.getHeight()));
        }
        preferencesService.setPreference("window.maximized", String.valueOf(persistMaximized));
        
        // Sauvegarder l'index de l'écran sur lequel se trouve la fenêtre
        Screen currentScreen = getScreenForWindow(stage);
        if (currentScreen != null) {
            List<Screen> screens = Screen.getScreens();
            int screenIndex = screens.indexOf(currentScreen);
            if (screenIndex >= 0) {
                preferencesService.setPreference("window.screen.index", String.valueOf(screenIndex));
            }
        }
    }
    
    private Screen getScreenForWindow(Stage stage) {
        // Pour une fenêtre maximisée (WM ou zone utilisable), utiliser le centre pour l’écran courant
        double windowX;
        double windowY;
        windowX = stage.getX() + stage.getWidth() / 2;
        windowY = stage.getY() + stage.getHeight() / 2;
        
        // Chercher l'écran qui contient ce point
        for (Screen screen : Screen.getScreens()) {
            Rectangle2D bounds = screen.getVisualBounds();
            if (bounds.contains(windowX, windowY)) {
                return screen;
            }
        }
        
        // Si aucun écran ne contient le point, chercher l'écran le plus proche
        Screen closestScreen = Screen.getPrimary();
        double minDistance = Double.MAX_VALUE;
        
        for (Screen screen : Screen.getScreens()) {
            Rectangle2D bounds = screen.getVisualBounds();
            double centerX = bounds.getMinX() + bounds.getWidth() / 2;
            double centerY = bounds.getMinY() + bounds.getHeight() / 2;
            double distance = Math.sqrt(Math.pow(windowX - centerX, 2) + Math.pow(windowY - centerY, 2));
            
            if (distance < minDistance) {
                minDistance = distance;
                closestScreen = screen;
            }
        }
        
        return closestScreen;
    }
    
    /**
     * Configure les listeners pour sauvegarder automatiquement la position et la taille
     */
    private void setupWindowListeners(Stage stage) {
        stage.xProperty().addListener((obs, oldVal, newVal) -> {
            if (!windowGeometryListenerSavesEnabled) {
                return;
            }
            if (!isRestoringWindow && stage.isShowing()) {
                scheduleSaveWindowPosition(stage);
            }
        });
        stage.yProperty().addListener((obs, oldVal, newVal) -> {
            if (!windowGeometryListenerSavesEnabled) {
                return;
            }
            if (!isRestoringWindow && stage.isShowing()) {
                scheduleSaveWindowPosition(stage);
            }
        });
        stage.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (!windowGeometryListenerSavesEnabled) {
                return;
            }
            if (!isRestoringWindow && stage.isShowing()) {
                scheduleSaveWindowPosition(stage);
            }
        });
        stage.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (!windowGeometryListenerSavesEnabled) {
                return;
            }
            if (!isRestoringWindow && stage.isShowing()) {
                scheduleSaveWindowPosition(stage);
            }
        });
        stage.maximizedProperty().addListener((obs, oldVal, newVal) -> {
            if (!windowGeometryListenerSavesEnabled) {
                return;
            }
            if (!isRestoringWindow && stage.isShowing()) {
                scheduleSaveWindowPosition(stage);
            }
        });
    }
    
    private void maximizeWindow(Stage stage) {
        StageVisualBounds.fitStageToVisualBounds(stage, Screen.getPrimary());
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
