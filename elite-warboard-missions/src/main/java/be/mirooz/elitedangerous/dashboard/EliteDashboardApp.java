package be.mirooz.elitedangerous.dashboard;

import be.mirooz.elitedangerous.dashboard.view.common.VersionUpdateNotificationComponent;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.service.LoggingService;
import be.mirooz.elitedangerous.dashboard.service.VersionCheckService;
import be.mirooz.elitedangerous.dashboard.service.WindowToggleService;
import be.mirooz.elitedangerous.dashboard.service.analytics.AnalyticsClient;
import be.mirooz.elitedangerous.dashboard.service.analytics.dto.LatestVersionResponse;
import be.mirooz.elitedangerous.dashboard.service.journal.watcher.JournalTailService;
import be.mirooz.elitedangerous.dashboard.service.journal.watcher.JournalWatcherService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.File;
import java.util.Objects;

public class EliteDashboardApp extends Application {

    private LocalizationService localizationService = LocalizationService.getInstance();
    private WindowToggleService windowToggleService = WindowToggleService.getInstance();
    private LoggingService loggingService = LoggingService.getInstance();
    private Stage mainStage;
    private ComboBox<String> comboBox;
    private StackPane rootPane;

    private double savedWidth = 1200;
    private double savedHeight = 800;

    @Override
    public void start(Stage stage) {
        // Initialiser le service de logging en premier
        loggingService.initialize();

        this.mainStage = stage;

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

            Scene scene = new Scene(root, savedWidth, savedHeight);

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
            maximizeWindow(stage);

            stage.setOnCloseRequest(event -> {
                stage.hide();
                // Fermer la session analytics
                AnalyticsClient.getInstance().endSession();
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
                VersionCheckService versionService = VersionCheckService.getInstance();
                String currentVersion = versionService.getCurrentVersion();
                LatestVersionResponse latestVersion = versionService.getLatestVersion();
                
                if (latestVersion != null) {
                    String latestVersionTag = latestVersion.getTagName();
                    if (versionService.isNewerVersion(currentVersion, latestVersionTag)) {
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
