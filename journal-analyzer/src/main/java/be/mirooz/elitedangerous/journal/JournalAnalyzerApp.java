package be.mirooz.elitedangerous.journal;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Standalone Journal Analyzer Application
 * 
 * This utility application allows users to analyze Elite Dangerous journal files
 * by displaying all events with filtering capabilities and JSON visualization.
 */
public class JournalAnalyzerApp extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        // Load FXML
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/journal-analyzer.fxml"));
        
        Scene scene = new Scene(loader.load());
        
        // Apply CSS theme if available
        try {
            java.net.URL cssUrl = getClass().getResource("/css/journal-analyzer.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }
        } catch (Exception e) {
            System.err.println("Could not load CSS: " + e.getMessage());
        }
        
        // Set stage properties
        primaryStage.setTitle("Elite Dangerous Journal Analyzer");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(700);
        
        // Set application icon
        try {
            Image icon = new Image(getClass().getResourceAsStream("/images/elite-icon.png"));
            primaryStage.getIcons().add(icon);
        } catch (Exception e) {
            System.err.println("Could not load application icon: " + e.getMessage());
        }
        
        // Center on screen
        primaryStage.centerOnScreen();
        
        // Show stage
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
