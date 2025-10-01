package be.mirooz.elitedangerous.dashboard;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * Application principale du dashboard Elite Dangerous
 */
public class EliteDashboardApp extends Application {
    
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(EliteDashboardApp.class.getResource("/fxml/dashboard.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1200, 800);
        // Icône de l'application
        Image icon = new Image(Objects.requireNonNull(
                getClass().getResource("/images/614-6140312_elite-dangerous-hd-png-elite-dangerous-logo-transparent.png")
        ).toExternalForm());
        stage.getIcons().add(icon);
        // Configuration du style Elite Dangerous
        scene.getStylesheets().add(getClass().getResource("/css/elite-theme.css").toExternalForm());
        
        stage.setTitle("Elite Dangerous - Dashboard des Missions");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.setMinWidth(1000);
        stage.setMinHeight(600);
        stage.setMaximized(true);
        stage.show();
        stage.setOnCloseRequest(event -> {
            System.out.println("Fermeture demandée, arrêt de l’application...");
            javafx.application.Platform.exit();
            System.exit(0);
        });
    }

    public static void main(String[] args) {
        launch();
    }
}
