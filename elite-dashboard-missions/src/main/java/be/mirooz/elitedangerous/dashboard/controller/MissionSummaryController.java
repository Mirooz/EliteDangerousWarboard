package be.mirooz.elitedangerous.dashboard.controller;

import be.mirooz.elitedangerous.dashboard.service.journal.JournalService;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Contrôleur pour afficher le résumé des missions
 */
public class MissionSummaryController extends Application {
    
    private JournalService journalService;
    
    @Override
    public void start(Stage primaryStage) {
        journalService = JournalService.getInstance();
        
        VBox root = new VBox();
        root.setSpacing(10);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #0A0A0A; -fx-font-family: 'Consolas', monospace;");
        
        Label title = new Label("RÉSUMÉ DES MISSIONS ELITE DANGEROUS");
        title.setStyle("-fx-text-fill: #FF6B00; -fx-font-size: 18px; -fx-font-weight: bold;");
        
        Button refreshButton = new Button("ACTUALISER");
        refreshButton.setStyle("-fx-background-color: #1A1A1A; -fx-border-color: #FF6B00; -fx-text-fill: #FF6B00; -fx-font-weight: bold;");
        refreshButton.setOnAction(e -> updateSummary());
        
        TextArea summaryArea = new TextArea();
        summaryArea.setEditable(false);
        summaryArea.setStyle("-fx-background-color: #1A1A1A; -fx-text-fill: #E0E0E0; -fx-font-family: 'Consolas', monospace; -fx-font-size: 12px;");
        summaryArea.setPrefRowCount(25);
        
        root.getChildren().addAll(title, refreshButton, summaryArea);
        
        // Charger le résumé initial
        updateSummary();
        
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("Résumé des Missions Elite Dangerous");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    private void updateSummary() {
        try {
            String summary = journalService.generateMissionSummary();
            // Mettre à jour l'interface
            // Pour l'instant, on affiche dans la console
            System.out.println(summary);
        } catch (Exception e) {
            System.err.println("Erreur lors de la génération du résumé: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
