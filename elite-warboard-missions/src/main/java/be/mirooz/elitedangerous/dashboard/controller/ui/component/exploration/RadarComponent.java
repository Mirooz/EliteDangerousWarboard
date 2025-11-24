package be.mirooz.elitedangerous.dashboard.controller.ui.component.exploration;

import be.mirooz.elitedangerous.dashboard.model.exploration.Position;
import be.mirooz.elitedangerous.dashboard.service.DirectionReaderService;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;

import java.util.List;

/**
 * Composant radar (boussole) pour afficher la position actuelle et les échantillons biologiques
 */
public class RadarComponent {
    
    private final Pane radarPane;
    private final DirectionReaderService directionService;
    private AnimationTimer updateTimer;
    
    public RadarComponent() {
        directionService = DirectionReaderService.getInstance();
        
        // Créer le pane pour le radar
        radarPane = new Pane();
        radarPane.setPrefHeight(200);
        radarPane.setMinHeight(200);
        radarPane.setMaxHeight(200);
        radarPane.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5); -fx-border-color: -fx-elite-orange; -fx-border-width: 1px;");
        
        // Redessiner le radar quand la taille change
        radarPane.widthProperty().addListener((obs, oldVal, newVal) -> updateRadar());
        radarPane.heightProperty().addListener((obs, oldVal, newVal) -> updateRadar());
        
        // Utiliser un timer pour mettre à jour périodiquement (toutes les 500ms)
        updateTimer = new AnimationTimer() {
            private long lastUpdate = 0;
            private static final long UPDATE_INTERVAL = 500_000_000L; // 500ms en nanosecondes
            
            @Override
            public void handle(long now) {
                if (now - lastUpdate >= UPDATE_INTERVAL) {
                    lastUpdate = now;
                    Platform.runLater(() -> updateRadar());
                }
            }
        };
        updateTimer.start();
        
        // Dessiner le radar initial
        Platform.runLater(() -> updateRadar());
    }
    
    /**
     * Retourne le pane du radar
     */
    public Pane getRadarPane() {
        return radarPane;
    }
    
    /**
     * Met à jour l'affichage du radar
     */
    private void updateRadar() {
        if (radarPane == null) {
            return;
        }
        
        radarPane.getChildren().clear();
        drawRadar();
    }
    
    /**
     * Dessine le radar (boussole) avec la position actuelle et les échantillons biologiques
     */
    private void drawRadar() {
        if (radarPane == null) {
            return;
        }
        
        // Attendre que le pane ait une taille
        double width = radarPane.getWidth();
        double height = radarPane.getHeight();
        
        // Si le pane n'a pas encore de taille, utiliser les valeurs préférées
        if (width == 0) {
            width = radarPane.getPrefWidth() > 0 ? radarPane.getPrefWidth() : 200;
        }
        if (height == 0) {
            height = radarPane.getPrefHeight() > 0 ? radarPane.getPrefHeight() : 200;
        }
        
        double centerX = width / 2;
        double centerY = height / 2;
        double radius = Math.min(centerX, centerY) - 20;
        
        // Dessiner le cercle principal (boussole)
        Circle compassCircle = new Circle(centerX, centerY, radius);
        compassCircle.setFill(Color.TRANSPARENT);
        compassCircle.setStroke(Color.rgb(255, 140, 0, 0.5));
        compassCircle.setStrokeWidth(2);
        radarPane.getChildren().add(compassCircle);
        
        // Dessiner les lignes cardinales (N, S, E, W)
        double lineLength = radius * 0.9;
        // Nord
        Line northLine = new Line(centerX, centerY - radius, centerX, centerY - lineLength);
        northLine.setStroke(Color.rgb(255, 140, 0, 0.7));
        northLine.setStrokeWidth(1);
        radarPane.getChildren().add(northLine);
        
        // Sud
        Line southLine = new Line(centerX, centerY + radius, centerX, centerY + lineLength);
        southLine.setStroke(Color.rgb(255, 140, 0, 0.7));
        southLine.setStrokeWidth(1);
        radarPane.getChildren().add(southLine);
        
        // Est
        Line eastLine = new Line(centerX + radius, centerY, centerX + lineLength, centerY);
        eastLine.setStroke(Color.rgb(255, 140, 0, 0.7));
        eastLine.setStrokeWidth(1);
        radarPane.getChildren().add(eastLine);
        
        // Ouest
        Line westLine = new Line(centerX - radius, centerY, centerX - lineLength, centerY);
        westLine.setStroke(Color.rgb(255, 140, 0, 0.7));
        westLine.setStrokeWidth(1);
        radarPane.getChildren().add(westLine);
        
        // Labels des points cardinaux
        Label northLabel = new Label("N");
        northLabel.setStyle("-fx-text-fill: #FF8C00; -fx-font-size: 12px; -fx-font-weight: bold;");
        northLabel.setLayoutX(centerX - 6);
        northLabel.setLayoutY(centerY - radius - 18);
        radarPane.getChildren().add(northLabel);
        
        Label southLabel = new Label("S");
        southLabel.setStyle("-fx-text-fill: #FF8C00; -fx-font-size: 12px; -fx-font-weight: bold;");
        southLabel.setLayoutX(centerX - 6);
        southLabel.setLayoutY(centerY + radius + 5);
        radarPane.getChildren().add(southLabel);
        
        Label eastLabel = new Label("E");
        eastLabel.setStyle("-fx-text-fill: #FF8C00; -fx-font-size: 12px; -fx-font-weight: bold;");
        eastLabel.setLayoutX(centerX + radius + 5);
        eastLabel.setLayoutY(centerY - 8);
        radarPane.getChildren().add(eastLabel);
        
        Label westLabel = new Label("W");
        westLabel.setStyle("-fx-text-fill: #FF8C00; -fx-font-size: 12px; -fx-font-weight: bold;");
        westLabel.setLayoutX(centerX - radius - 18);
        westLabel.setLayoutY(centerY - 8);
        radarPane.getChildren().add(westLabel);
        
        // Obtenir la position actuelle
        Position currentPos = directionService.getCurrentPosition();
        
        if (currentPos != null) {
            // Dessiner le point central (position actuelle)
            Circle centerPoint = new Circle(centerX, centerY, 5);
            centerPoint.setFill(Color.rgb(0, 255, 0)); // Vert pour la position actuelle
            centerPoint.setStroke(Color.WHITE);
            centerPoint.setStrokeWidth(1);
            radarPane.getChildren().add(centerPoint);
            
            // Dessiner la flèche indiquant le heading
            Integer heading = currentPos.getHeading();
            if (heading != null) {
                // Convertir le heading en radians (0° = Nord, sens horaire)
                // En JavaFX, 0° = Est, sens anti-horaire, donc on doit ajuster
                double headingRad = Math.toRadians(90 - heading); // Ajustement pour que 0° = Nord
                
                double arrowLength = radius * 0.3;
                double arrowX = centerX + Math.cos(headingRad) * arrowLength;
                double arrowY = centerY - Math.sin(headingRad) * arrowLength; // Inverser Y car l'origine est en haut
                
                // Dessiner la ligne du heading
                Line headingLine = new Line(centerX, centerY, arrowX, arrowY);
                headingLine.setStroke(Color.rgb(0, 255, 0));
                headingLine.setStrokeWidth(2);
                radarPane.getChildren().add(headingLine);
                
                // Dessiner la pointe de la flèche
                double arrowSize = 8;
                double angle1 = headingRad + Math.PI - Math.PI / 6;
                double angle2 = headingRad + Math.PI + Math.PI / 6;
                
                double x1 = arrowX + Math.cos(angle1) * arrowSize;
                double y1 = arrowY - Math.sin(angle1) * arrowSize;
                double x2 = arrowX + Math.cos(angle2) * arrowSize;
                double y2 = arrowY - Math.sin(angle2) * arrowSize;
                
                Polygon arrowHead = new Polygon(
                    arrowX, arrowY,
                    x1, y1,
                    x2, y2
                );
                arrowHead.setFill(Color.rgb(0, 255, 0));
                radarPane.getChildren().add(arrowHead);
            }
            
            // Dessiner les points d'échantillons biologiques
            List<Position> samplePositions = directionService.getCurrentBiologicalSamplePositions();
            if (samplePositions != null && !samplePositions.isEmpty()) {
                // Trouver la distance maximale pour l'échelle
                double maxDistance = 0;
                for (Position sample : samplePositions) {
                    if (sample.getDistanceFromCurrent() > 0) {
                        maxDistance = Math.max(maxDistance, sample.getDistanceFromCurrent());
                    }
                }
                
                // Si pas de distance calculée, utiliser une distance par défaut basée sur les coordonnées
                if (maxDistance == 0) {
                    for (Position sample : samplePositions) {
                        double distance = directionService.getDistanceTo(currentPos, sample);
                        if (distance > 0) {
                            maxDistance = Math.max(maxDistance, distance);
                        }
                    }
                }
                
                // Si toujours pas de distance, utiliser une valeur par défaut
                if (maxDistance == 0) {
                    maxDistance = 1000; // 1km par défaut
                }
                
                for (Position sample : samplePositions) {
                    double distance = sample.getDistanceFromCurrent();
                    if (distance <= 0) {
                        distance = directionService.getDistanceTo(currentPos, sample);
                    }
                    
                    if (distance > 0) {
                        // Calculer la direction depuis la position actuelle vers l'échantillon
                        double direction = currentPos.calculateDirectionTo(sample);
                        double directionRad = Math.toRadians(90 - direction); // Même ajustement que pour le heading
                        
                        // Calculer la position sur le radar (normalisée par rapport au rayon)
                        double normalizedDistance = Math.min(distance / maxDistance, 1.0); // Limiter à 1.0
                        double sampleX = centerX + Math.cos(directionRad) * normalizedDistance * radius * 0.9;
                        double sampleY = centerY - Math.sin(directionRad) * normalizedDistance * radius * 0.9;
                        
                        // Dessiner le point d'échantillon
                        Circle samplePoint = new Circle(sampleX, sampleY, 4);
                        samplePoint.setFill(Color.rgb(255, 0, 255)); // Magenta pour les échantillons
                        samplePoint.setStroke(Color.WHITE);
                        samplePoint.setStrokeWidth(1);
                        radarPane.getChildren().add(samplePoint);
                        
                        // Optionnel : dessiner une ligne depuis le centre vers l'échantillon
                        Line sampleLine = new Line(centerX, centerY, sampleX, sampleY);
                        sampleLine.setStroke(Color.rgb(255, 0, 255, 0.3));
                        sampleLine.setStrokeWidth(1);
                        radarPane.getChildren().add(0, sampleLine); // Ajouter en arrière-plan
                    }
                }
            }
        }
    }
    
    /**
     * Arrête le timer de mise à jour
     */
    public void stop() {
        if (updateTimer != null) {
            updateTimer.stop();
            updateTimer = null;
        }
    }
}

