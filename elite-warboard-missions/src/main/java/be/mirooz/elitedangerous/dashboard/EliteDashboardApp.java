package be.mirooz.elitedangerous.dashboard;

import be.mirooz.elitedangerous.dashboard.view.common.VersionUpdateNotificationComponent;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.service.LoggingService;
import be.mirooz.elitedangerous.dashboard.service.PreferencesService;
import be.mirooz.elitedangerous.dashboard.service.WindowToggleService;
import be.mirooz.elitedangerous.dashboard.service.AnalyticsService;
import be.mirooz.elitedangerous.analytics.dto.LatestVersionResponse;
import be.mirooz.elitedangerous.dashboard.service.journal.watcher.JournalTailService;
import be.mirooz.elitedangerous.dashboard.service.journal.watcher.JournalWatcherService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.List;
import java.util.Objects;

public class EliteDashboardApp extends Application {

    private LocalizationService localizationService = LocalizationService.getInstance();
    private WindowToggleService windowToggleService = WindowToggleService.getInstance();
    private LoggingService loggingService = LoggingService.getInstance();
    private PreferencesService preferencesService = PreferencesService.getInstance();
    private ComboBox<String> comboBox;
    private StackPane rootPane;
    
    private boolean isRestoringWindow = false; // Flag pour éviter de sauvegarder pendant la restauration

    @Override
    public void start(Stage stage) {
        // Initialiser le service de logging en premier
        loggingService.initialize();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
            BorderPane dashboardRoot = loader.load();

            StackPane root = new StackPane(dashboardRoot);
            this.rootPane = root; // Sauvegarder la référence

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
                if (maximized) {
                    stage.setMaximized(true);
                }
            });

            stage.setOnCloseRequest(event -> {
                // Sauvegarder la position et la taille avant de fermer
                saveWindowPosition(stage);
                stage.hide();
                // Fermer la session analytics
                AnalyticsService.getInstance().endSession();
                System.out.println("Arrêt des services de journal...");
                JournalTailService.getInstance().stop();
                JournalWatcherService.getInstance().stop();
                windowToggleService.stop();
                
                // Arrêter le service de logging
                loggingService.shutdown();

                Platform.exit();
                System.exit(0);
            });

            stage.show();

            // Initialiser et démarrer le service de toggle de fenêtre
            windowToggleService.initialize(stage, comboBox, rootPane);
            windowToggleService.start();

            // Vérifier la version de l'application de manière asynchrone
            checkForUpdates(root);

            System.out.println("✅ Application démarrée");

        } catch (Exception e) {
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
                // Restaurer l'écran d'abord
                Screen targetScreen = getScreenByIndex(savedScreenIndex);
                if (targetScreen == null) {
                    targetScreen = Screen.getPrimary();
                }
                Rectangle2D screenBounds = targetScreen.getVisualBounds();
                stage.setX(screenBounds.getMinX());
                stage.setY(screenBounds.getMinY());
                stage.setWidth(screenBounds.getWidth());
                stage.setHeight(screenBounds.getHeight());
                isRestoringWindow = false;
                return;
            }
            
            // Vérifier si on a des valeurs sauvegardées pour une fenêtre non maximisée
            if (savedX != null && savedY != null && savedWidth != null && savedHeight != null) {
                try {
                    double x = Double.parseDouble(savedX);
                    double y = Double.parseDouble(savedY);
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
                        double finalY = Math.max(screenBounds.getMinY(), Math.min(y, screenBounds.getMaxY() - 100));
                        
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
     * Sauvegarde la position et la taille de la fenêtre dans les préférences
     */
    private void saveWindowPosition(Stage stage) {
        if (isRestoringWindow) {
            return; // Ne pas sauvegarder pendant la restauration
        }
        
        // Sauvegarder la position et la taille
        preferencesService.setPreference("window.x", String.valueOf(stage.getX()));
        preferencesService.setPreference("window.y", String.valueOf(stage.getY()));
        preferencesService.setPreference("window.width", String.valueOf(stage.getWidth()));
        preferencesService.setPreference("window.height", String.valueOf(stage.getHeight()));
        preferencesService.setPreference("window.maximized", String.valueOf(stage.isMaximized()));
        
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
    
    /**
     * Trouve l'écran sur lequel se trouve la fenêtre
     */
    private Screen getScreenForWindow(Stage stage) {
        // Pour une fenêtre maximisée, utiliser la position X,Y de la fenêtre (coin supérieur gauche)
        // Pour une fenêtre non maximisée, utiliser le centre de la fenêtre
        double windowX, windowY;
        
        if (stage.isMaximized()) {
            windowX = stage.getX();
            windowY = stage.getY();
        } else {
            windowX = stage.getX() + stage.getWidth() / 2;
            windowY = stage.getY() + stage.getHeight() / 2;
        }
        
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
        // Sauvegarder quand la fenêtre est déplacée
        stage.xProperty().addListener((obs, oldVal, newVal) -> {
            if (!isRestoringWindow && stage.isShowing()) {
                saveWindowPosition(stage);
            }
        });
        
        stage.yProperty().addListener((obs, oldVal, newVal) -> {
            if (!isRestoringWindow && stage.isShowing()) {
                saveWindowPosition(stage);
            }
        });
        
        // Sauvegarder quand la fenêtre est redimensionnée
        stage.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (!isRestoringWindow && stage.isShowing() && !stage.isMaximized()) {
                saveWindowPosition(stage);
            }
        });
        
        stage.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (!isRestoringWindow && stage.isShowing() && !stage.isMaximized()) {
                saveWindowPosition(stage);
            }
        });
        
        // Sauvegarder quand l'état maximisé change
        stage.maximizedProperty().addListener((obs, oldVal, newVal) -> {
            if (!isRestoringWindow && stage.isShowing()) {
                // Si on passe de maximisé à non maximisé, sauvegarder la position actuelle
                if (!newVal) {
                    Platform.runLater(() -> saveWindowPosition(stage));
                } else {
                    saveWindowPosition(stage);
                }
            }
        });
    }
    
    private void maximizeWindow(Stage stage) {
        var bounds = Screen.getPrimary().getVisualBounds();
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
    }
    
    /**
     * Vérifie si une nouvelle version est disponible et affiche une notification si nécessaire
     */
    private void checkForUpdates(StackPane rootPane) {
        // Vérifier de manière asynchrone pour ne pas bloquer le démarrage
        new Thread(() -> {
            try {
                AnalyticsService analyticsService = AnalyticsService.getInstance();
                String currentVersion = analyticsService.getCurrentVersion();
                LatestVersionResponse latestVersion = analyticsService.getLatestVersion();
                
                if (latestVersion != null) {
                    String latestVersionTag = latestVersion.getTagName();
                    if (analyticsService.isNewerVersion(currentVersion, latestVersionTag)) {
                        // Afficher la notification sur le thread JavaFX
                        Platform.runLater(() -> {
                            // Chercher le popupContainer dans la hiérarchie
                            StackPane popupContainer = findPopupContainer(rootPane);
                            if (popupContainer != null) {
                                new VersionUpdateNotificationComponent(
                                    latestVersionTag,
                                    latestVersion.getHtmlUrl(),
                                    popupContainer
                                );
                            } else {
                                // Fallback: utiliser rootPane directement
                                new VersionUpdateNotificationComponent(
                                    latestVersionTag,
                                    latestVersion.getHtmlUrl(),
                                    rootPane
                                );
                            }
                        });
                    }
                }
            } catch (Exception e) {
                // Ignorer silencieusement les erreurs de vérification de version
                System.err.println("Erreur lors de la vérification de version: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Trouve le popupContainer dans la hiérarchie des nœuds
     */
    private StackPane findPopupContainer(javafx.scene.Node root) {
        if (root instanceof StackPane stackPane) {
            // Vérifier si c'est le popupContainer (chercher par ID ou par structure)
            if (root.getId() != null && root.getId().equals("popupContainer")) {
                return stackPane;
            }
        }
        
        // Parcourir récursivement les enfants
        if (root instanceof javafx.scene.layout.Pane pane) {
            for (javafx.scene.Node child : pane.getChildren()) {
                StackPane found = findPopupContainer(child);
                if (found != null) {
                    return found;
                }
            }
        }
        
        return null;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
