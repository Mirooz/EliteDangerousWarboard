package be.mirooz.elitedangerous.dashboard;

import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.service.WindowToggleService;
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

import java.util.Objects;

public class EliteDashboardApp extends Application {

    private LocalizationService localizationService = LocalizationService.getInstance();
    private WindowToggleService windowToggleService = WindowToggleService.getInstance();
    private Stage mainStage;
    private ComboBox<String> comboBox;
    private StackPane rootPane;

    private double savedWidth = 1200;
    private double savedHeight = 800;

    @Override
    public void start(Stage stage) {
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
                System.out.println("Arrêt des services de journal...");
                JournalTailService.getInstance().stop();
                JournalWatcherService.getInstance().stop();
                windowToggleService.stop();
                Platform.exit();
                System.exit(0);
            });

            stage.show();

            // Initialiser et démarrer le service de toggle de fenêtre
            windowToggleService.initialize(stage, comboBox, rootPane);
            windowToggleService.start();

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

    public static void main(String[] args) {
        launch(args);
    }
}
