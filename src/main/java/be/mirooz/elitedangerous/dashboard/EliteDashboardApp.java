package be.mirooz.elitedangerous.dashboard;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Application principale du dashboard Elite Dangerous
 */
public class EliteDashboardApp extends Application {
    
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(EliteDashboardApp.class.getResource("/fxml/dashboard.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1200, 800);
        
        // Configuration du style Elite Dangerous
        scene.getStylesheets().add(getClass().getResource("/css/elite-theme.css").toExternalForm());
        
        stage.setTitle("Elite Dangerous - Dashboard des Missions");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.setMinWidth(1000);
        stage.setMinHeight(600);
        
        // Icône de l'application
        try {
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/images/elite-icon.png")));
        } catch (Exception e) {
            // Icône optionnelle, on continue sans
        }
        
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
