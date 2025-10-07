package be.mirooz.elitedangerous.dashboard;

import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Objects;

import be.mirooz.elitedangerous.dashboard.service.journal.watcher.JournalTailService;
import be.mirooz.elitedangerous.dashboard.service.journal.watcher.JournalWatcherService;
public class EliteDashboardApp extends Application {

    LocalizationService localizationService = LocalizationService.getInstance();
    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/dashboard.fxml")
            );
            Scene scene = new Scene(loader.load(), 1200, 800);

            scene.getStylesheets().add(
                    Objects.requireNonNull(getClass().getResource("/css/elite-theme.css"))
                            .toExternalForm()
            );

            // --- Icône ---
            Image icon = new Image(
                    Objects.requireNonNull(
                            getClass().getResource("/images/elite_dashboard_icon.png")
                    ).toExternalForm()
            );
            stage.getIcons().add(icon);

            // --- Stage ---

            String title =localizationService.getString("app.title");
            stage.setTitle(title);
            stage.setScene(scene);
            stage.setResizable(true);
            stage.setMinWidth(1000);
            stage.setMinHeight(600);
            stage.setMaximized(true);

            // --- Gestion de la fermeture ---
            stage.setOnCloseRequest(event -> {
                System.out.println("Fermeture demandée, arrêt de l’application...");
                javafx.application.Platform.exit();
                System.exit(0);
            });

            stage.setOnCloseRequest(event -> {
                System.out.println("Arrêt des services de journal...");
                JournalTailService.getInstance().stop();
                JournalWatcherService.getInstance().stop();

                javafx.application.Platform.exit();
                System.exit(0);
            });
            stage.show();

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du chargement du Dashboard", e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
