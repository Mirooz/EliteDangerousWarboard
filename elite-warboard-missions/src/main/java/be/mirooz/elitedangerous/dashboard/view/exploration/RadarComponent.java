package be.mirooz.elitedangerous.dashboard.view.exploration;

import be.mirooz.elitedangerous.dashboard.model.exploration.Position;
import be.mirooz.elitedangerous.dashboard.service.DirectionReaderService;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;

import java.util.ArrayList;
import java.util.List;

/**
 * Composant radar (boussole) pour afficher la position actuelle et les échantillons biologiques
 */
public class RadarComponent {
    
    private final Pane radarContainer;
    private final Group radarGroup;
    private final Pane labelsPane;
    private final DirectionReaderService directionService;
    private final LocalizationService localizationService = LocalizationService.getInstance();
    private AnimationTimer updateTimer;
    private static RadarComponent instance;
    
    public RadarComponent() {
        directionService = DirectionReaderService.getInstance();
        // Ne remplacer l'instance statique que si elle n'existe pas encore
        // Cela permet de garder l'instance du panel principal même si on crée des instances pour l'overlay
        if (instance == null) {
            instance = this;
        }
        
        // Créer un conteneur Pane pour superposer le radar clippé et les labels non clippés
        // Utiliser un Pane au lieu d'un StackPane pour éviter le centrage automatique
        radarContainer = new Pane();
        radarContainer.setPrefHeight(200);
        radarContainer.setMinHeight(200);
        radarContainer.setMaxHeight(200);
        // Permettre l'expansion en largeur pour afficher les labels à droite dans le panel
        radarContainer.setPrefWidth(350); // 200 (radar) + 150 (labels)
        radarContainer.setMinWidth(200);
        radarContainer.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5); -fx-border-color: -fx-elite-orange; -fx-border-width: 1px;");
        
        // Créer un Group pour le radar (sera clippé)
        // Un Group ne calcule pas sa taille basée sur ses enfants, donc il ne s'agrandira pas
        radarGroup = new Group();
        // Positionner le Group à (0, 0) pour qu'il ne bouge pas
        radarGroup.setLayoutX(0);
        radarGroup.setLayoutY(0);
        // Empêcher le Group d'influencer la taille du parent
        radarGroup.setManaged(false);
        radarGroup.setPickOnBounds(false); // Ne pas prendre en compte les éléments hors bounds
        
        // Créer le pane pour les labels (ne sera pas clippé)
        labelsPane = new Pane();
        labelsPane.setMouseTransparent(true); // Ne pas intercepter les événements de souris
        labelsPane.setLayoutX(0);
        labelsPane.setLayoutY(0);
        // Le labelsPane doit être géré pour avoir la bonne taille
        labelsPane.setManaged(true);
        labelsPane.setPickOnBounds(false); // Ne pas prendre en compte les éléments hors bounds
        
        // Ajouter les deux au conteneur
        radarContainer.getChildren().addAll(radarGroup, labelsPane);
        
        // Appliquer un clip rectangulaire au conteneur pour empêcher l'expansion
        // Ce clip sera mis à jour dans updateClipping()
        radarContainer.setClip(new Rectangle());
        
        // Redessiner le radar quand la taille change
        radarContainer.widthProperty().addListener((obs, oldVal, newVal) -> {
            updateRadar();
            updateClipping();
        });
        radarContainer.heightProperty().addListener((obs, oldVal, newVal) -> {
            updateRadar();
            updateClipping();
        });
        
        // Utiliser un timer pour mettre à jour périodiquement (toutes les 500ms)
        updateTimer = new AnimationTimer() {
            private long lastUpdate = 0;
            private static final long UPDATE_INTERVAL = 100_000_000L; // 500ms en nanosecondes
            
            @Override
            public void handle(long now) {
                if (now - lastUpdate >= UPDATE_INTERVAL) {
                    lastUpdate = now;
                    Platform.runLater(() -> {
                        updateRadar();
                        updateClipping();
                    });
                }
            }
        };
        updateTimer.start();
        
        // Cacher le radar par défaut
        radarContainer.setVisible(false);
        radarContainer.setManaged(false);
        
        // Dessiner le radar initial et appliquer le clipping
        Platform.runLater(() -> {
            updateRadar();
            updateClipping();
        });
    }
    
    /**
     * Retourne le conteneur du radar (Pane avec radar clippé et labels non clippés)
     */
    public Pane getRadarPane() {
        return radarContainer;
    }
    
    /**
     * Met à jour le clipping du radar pour que les éléments ne dépassent pas les bords du cercle
     */
    private void updateClipping() {
        if (radarGroup == null || radarContainer == null) {
            return;
        }
        
        double width = radarContainer.getWidth();
        double height = radarContainer.getHeight();
        
        // Si le conteneur n'a pas encore de taille, utiliser les valeurs préférées
        if (width == 0) {
            width = radarContainer.getPrefWidth() > 0 ? radarContainer.getPrefWidth() : 200;
        }
        if (height == 0) {
            height = radarContainer.getPrefHeight() > 0 ? radarContainer.getPrefHeight() : 200;
        }
        
        if (width > 0 && height > 0) {
            double centerX = width / 2;
            double centerY = height / 2;
            // Utiliser exactement le même calcul de rayon que dans drawRadar()
            double radius = Math.min(centerX, centerY) - 20;
            
            // Clipper le conteneur principal avec un rectangle qui permet l'affichage des labels à droite
            // La largeur du clip est augmentée pour permettre l'affichage des distances
            double clipWidth = width + 150; // Espace pour les labels à droite
            Rectangle containerClip = new Rectangle(0, 0, clipWidth, height);
            radarContainer.setClip(containerClip);
            
            // Utiliser un cercle pour le clipping du radarGroup
            // Le clipping s'applique seulement au radarGroup, pas au labelsPane
            Circle clip = new Circle(centerX, centerY, radius);
            radarGroup.setClip(clip);
            
            // S'assurer que le Group et le labelsPane ne bougent pas et ont la bonne taille
            radarGroup.setLayoutX(0);
            radarGroup.setLayoutY(0);
            labelsPane.setLayoutX(0);
            labelsPane.setLayoutY(0);
            
            // Permettre au labelsPane de s'étendre à droite pour afficher les distances
            labelsPane.setPrefWidth(width + 150); // Espace pour les labels à droite
            labelsPane.setPrefHeight(height);
            labelsPane.setMaxWidth(Double.MAX_VALUE); // Pas de limite de largeur
            labelsPane.setMaxHeight(height);
        }
    }
    
    /**
     * Met à jour l'affichage du radar
     */
    private void updateRadar() {
        if (radarGroup == null) {
            return;
        }
        
        radarGroup.getChildren().clear();
        drawRadar();
        // Mettre à jour le clipping après le dessin pour s'assurer qu'il correspond au cercle
        updateClipping();
    }
    
    /**
     * Dessine le radar (boussole) avec la position actuelle et les échantillons biologiques
     */
    private void drawRadar() {
        if (radarGroup == null) {
            return;
        }
        
        // Attendre que le conteneur ait une taille
        double width = radarContainer.getWidth();
        double height = radarContainer.getHeight();
        
        // Si le conteneur n'a pas encore de taille, utiliser les valeurs préférées
        if (width == 0) {
            width = radarContainer.getPrefWidth() > 0 ? radarContainer.getPrefWidth() : 200;
        }
        if (height == 0) {
            height = radarContainer.getPrefHeight() > 0 ? radarContainer.getPrefHeight() : 200;
        }
        
        double centerX = width / 2;
        double centerY = height / 2;
        double radius = Math.min(centerX, centerY) - 20;
        
        // Dessiner le cercle principal (boussole)
        Circle compassCircle = new Circle(centerX, centerY, radius);
        compassCircle.setFill(Color.TRANSPARENT);
        compassCircle.setStroke(Color.rgb(255, 140, 0, 0.5));
        compassCircle.setStrokeWidth(2);
        radarGroup.getChildren().add(compassCircle);
        
        // Dessiner les lignes cardinales (N, S, E, W)
        double lineLength = radius * 0.9;
        // Nord
        Line northLine = new Line(centerX, centerY - radius, centerX, centerY - lineLength);
        northLine.setStroke(Color.rgb(255, 140, 0, 0.7));
        northLine.setStrokeWidth(1);
        radarGroup.getChildren().add(northLine);
        
        // Sud
        Line southLine = new Line(centerX, centerY + radius, centerX, centerY + lineLength);
        southLine.setStroke(Color.rgb(255, 140, 0, 0.7));
        southLine.setStrokeWidth(1);
        radarGroup.getChildren().add(southLine);
        
        // Est
        Line eastLine = new Line(centerX + radius, centerY, centerX + lineLength, centerY);
        eastLine.setStroke(Color.rgb(255, 140, 0, 0.7));
        eastLine.setStrokeWidth(1);
        radarGroup.getChildren().add(eastLine);
        
        // Ouest
        Line westLine = new Line(centerX - radius, centerY, centerX - lineLength, centerY);
        westLine.setStroke(Color.rgb(255, 140, 0, 0.7));
        westLine.setStrokeWidth(1);
        radarGroup.getChildren().add(westLine);
        
        // Labels des points cardinaux (dans le labelsPane non clippé)
        labelsPane.getChildren().clear();
        
        Label northLabel = new Label("N");
        northLabel.setStyle("-fx-text-fill: #FF8C00; -fx-font-size: 12px; -fx-font-weight: bold;");
        northLabel.setLayoutX(centerX - 6);
        northLabel.setLayoutY(centerY - radius - 18);
        labelsPane.getChildren().add(northLabel);
        
        Label southLabel = new Label("S");
        southLabel.setStyle("-fx-text-fill: #FF8C00; -fx-font-size: 12px; -fx-font-weight: bold;");
        southLabel.setLayoutX(centerX - 6);
        southLabel.setLayoutY(centerY + radius + 5);
        labelsPane.getChildren().add(southLabel);
        
        Label eastLabel = new Label("E");
        eastLabel.setStyle("-fx-text-fill: #FF8C00; -fx-font-size: 12px; -fx-font-weight: bold;");
        eastLabel.setLayoutX(centerX + radius + 5);
        eastLabel.setLayoutY(centerY - 8);
        labelsPane.getChildren().add(eastLabel);
        
        Label westLabel = new Label("W");
        westLabel.setStyle("-fx-text-fill: #FF8C00; -fx-font-size: 12px; -fx-font-weight: bold;");
        westLabel.setLayoutX(centerX - radius - 18);
        westLabel.setLayoutY(centerY - 8);
        labelsPane.getChildren().add(westLabel);
        
        // Obtenir la position actuelle
        Position currentPos = directionService.getCurrentPosition();
        
        if (currentPos != null) {
            // Dessiner un triangle indiquant le heading (style radar de jeu vidéo)
            Integer heading = currentPos.getHeading();
            if (heading != null) {
                // Convertir le heading en radians (0° = Nord, sens horaire)
                // En JavaFX, 0° = Est, sens anti-horaire, donc on doit ajuster
                double headingRad = Math.toRadians(90 - heading); // Ajustement pour que 0° = Nord
                
                // Taille du triangle (plus petit)
                double triangleLength = radius * 0.2; // Longueur du triangle (du centre au sommet)
                double triangleWidth = radius * 0.06; // Largeur de la base du triangle
                
                // Sommet du triangle (pointe dans la direction)
                double tipX = centerX + Math.cos(headingRad) * triangleLength;
                double tipY = centerY - Math.sin(headingRad) * triangleLength; // Inverser Y car l'origine est en haut
                
                // Points de la base du triangle (perpendiculaires à la direction)
                double baseAngle1 = headingRad + Math.PI / 2; // Angle perpendiculaire
                double baseAngle2 = headingRad - Math.PI / 2;
                
                double baseX1 = centerX + Math.cos(baseAngle1) * triangleWidth;
                double baseY1 = centerY - Math.sin(baseAngle1) * triangleWidth;
                double baseX2 = centerX + Math.cos(baseAngle2) * triangleWidth;
                double baseY2 = centerY - Math.sin(baseAngle2) * triangleWidth;
                
                // Créer le triangle
                Polygon directionTriangle = new Polygon(
                    tipX, tipY,      // Sommet (pointe dans la direction)
                    baseX1, baseY1,   // Point de base gauche
                    baseX2, baseY2    // Point de base droit
                );
                directionTriangle.setFill(Color.rgb(0, 255, 0)); // Vert vif
                directionTriangle.setStroke(Color.rgb(0, 200, 0)); // Vert plus foncé pour le contour
                directionTriangle.setStrokeWidth(1.5);
                radarGroup.getChildren().add(directionTriangle);
            }
            
            // Dessiner les points d'échantillons biologiques
            List<Position> samplePositions = directionService.getCurrentBiologicalSamplePositions();
            Double colonyRangeMeter = directionService.getColonyRangeMeter();
            
            if (samplePositions != null && !samplePositions.isEmpty()) {
                // Facteur d'échelle pour utiliser 85% du rayon du radar
                double scaleFactor = 0.85;
                
                // Calculer les distances réelles de tous les échantillons
                List<Double> distances = new ArrayList<>();
                for (Position sample : samplePositions) {
                    double distance = sample.getDistanceFromCurrent();
                    if (distance <= 0) {
                        distance = directionService.getDistanceTo(currentPos, sample);
                    }
                    if (distance > 0) {
                        distances.add(distance);
                    }
                }
                
                // Trouver la distance maximale des échantillons
                double maxSampleDistance = distances.stream()
                    .mapToDouble(Double::doubleValue)
                    .max()
                    .orElse(0);
                
                // Si on a colonyRangeMeter, utiliser cette distance comme référence pour l'échelle du cercle
                // Le cercle d'exclusion représente colonyRangeMeter et doit visuellement prendre 3/4 du radar
                // Mais les points peuvent sortir du radar (comme un vrai radar)
                double scaleDistance; // Distance utilisée pour l'échelle
                if (colonyRangeMeter != null && colonyRangeMeter > 0) {
                    // Ajuster scaleDistance pour que le cercle d'exclusion soit à 3/4 du radar
                    // Si le cercle doit être à 3/4, alors quand distance = colonyRangeMeter, 
                    // normalizedDistance doit être 0.75, donc scaleDistance = colonyRangeMeter / 0.75
                    scaleDistance = colonyRangeMeter / 0.75; // = colonyRangeMeter * 4/3
                } else {
                    // Si pas de colonyRangeMeter, utiliser la distance maximale des échantillons
                    scaleDistance = maxSampleDistance;
                    if (scaleDistance == 0) {
                        scaleDistance = 1000; // 1km par défaut
                    }
                }
                
                // Calculer le rayon du cercle d'exclusion pour qu'il soit à 3/4 du radar
                // Le cercle doit toujours visuellement prendre 3/4 du rayon du radar
                double exclusionRadiusOnRadar = radius * scaleFactor * 0.75;
                
                // Afficher colonyRangeMeter comme "Min" à droite du cercle du radar (dans le panel)
                if (colonyRangeMeter != null && colonyRangeMeter > 0) {
                    Label minDistanceLabel = new Label(String.format(localizationService.getString("exploration.min_distance"), colonyRangeMeter));
                    minDistanceLabel.setStyle("-fx-text-fill: #FF8C00; -fx-font-size: 18px; -fx-font-weight: bold;");
                    minDistanceLabel.setLayoutX(centerX + radius + 10);
                    minDistanceLabel.setLayoutY(centerY - radius);
                    labelsPane.getChildren().add(minDistanceLabel);
                }
                
                int sampleIndex = 0;
                for (Position sample : samplePositions) {
                    double distance = sample.getDistanceFromCurrent();
                    if (distance <= 0) {
                        distance = directionService.getDistanceTo(currentPos, sample);
                    }
                    
                    // Calculer la position sur le radar
                    double sampleX;
                    double sampleY;
                    
                    // Calculer la position réelle du sample (pour le cercle d'exclusion)
                    double realSampleX;
                    double realSampleY;
                    
                    if (distance > 0) {
                        // Calculer la direction depuis la position actuelle vers l'échantillon
                        double direction = currentPos.calculateDirectionTo(sample);
                        double directionRad = Math.toRadians(90 - direction); // Même ajustement que pour le heading
                        
                        // Calculer la position sur le radar (normalisée par rapport à scaleDistance)
                        double normalizedDistance = distance / scaleDistance;
                        double maxRadius = radius * scaleFactor; // Rayon maximum normal pour les points
                        
                        // Calculer la distance du point au centre
                        double pointDistance = normalizedDistance * maxRadius;
                        
                        // Calculer la position réelle (sans limitation) pour le cercle d'exclusion
                        realSampleX = centerX + Math.cos(directionRad) * pointDistance;
                        realSampleY = centerY - Math.sin(directionRad) * pointDistance;
                        
                        // Si le point sort du radar, le placer exactement sur la bordure du cercle
                        if (pointDistance > maxRadius) {
                            // Utiliser le rayon exact du cercle du radar (radius, pas radius * scaleFactor)
                            pointDistance = radius;
                        }
                        
                        sampleX = centerX + Math.cos(directionRad) * pointDistance;
                        sampleY = centerY - Math.sin(directionRad) * pointDistance;
                    } else {
                        // Si le sample est à la même position que la currentPosition, le placer au centre
                        sampleX = centerX;
                        sampleY = centerY;
                        realSampleX = centerX;
                        realSampleY = centerY;
                    }
                    
                    // Déterminer la couleur selon l'index du sample
                    Color sampleColor;
                    String colorHex;
                    if (sampleIndex == 0) {
                        // Premier sample : magenta/rose
                        sampleColor = Color.rgb(255, 0, 255);
                        colorHex = "#FF00FF";
                    } else if (sampleIndex == 1) {
                        // Second sample : cyan
                        sampleColor = Color.rgb(0, 255, 255);
                        colorHex = "#00FFFF";
                    } else {
                        // Troisième sample et suivants : jaune
                        sampleColor = Color.rgb(255, 255, 0);
                        colorHex = "#FFFF00";
                    }
                    
                    // Dessiner le cercle d'exclusion (toujours avec la position réelle pour suivre le mouvement normal)
                    if (colonyRangeMeter != null && colonyRangeMeter > 0) {
                        // Le cercle d'exclusion prend toujours 3/4 du rayon du radar
                        // Peu importe la distance réelle, visuellement il occupe 3/4
                        double exclusionRadius = exclusionRadiusOnRadar;
                        
                        // Utiliser la position réelle pour le cercle d'exclusion (continue son mouvement même si le sample est sur la bordure)
                        Circle exclusionCircle = new Circle(realSampleX, realSampleY, exclusionRadius);
                        exclusionCircle.setFill(Color.TRANSPARENT);
                        exclusionCircle.setStroke(Color.color(sampleColor.getRed(), sampleColor.getGreen(), sampleColor.getBlue(), 0.6));
                        exclusionCircle.setStrokeWidth(1.5);
                        exclusionCircle.getStrokeDashArray().addAll(5.0, 5.0); // Ligne pointillée
                        radarGroup.getChildren().add(0, exclusionCircle); // Ajouter en arrière-plan
                    }
                    
                    // Dessiner le point d'échantillon (toujours, même si distance == 0)
                    Circle samplePoint = new Circle(sampleX, sampleY, 4);
                    samplePoint.setFill(sampleColor);
                    samplePoint.setStroke(Color.WHITE);
                    samplePoint.setStrokeWidth(1);
                    radarGroup.getChildren().add(samplePoint);
                    
                    // Afficher la distance du sample à droite du cercle du radar (dans le panel)
                    if (distance > 0) {
                        Label distanceLabel = new Label(String.format(localizationService.getString("exploration.distance"), distance));
                        distanceLabel.setStyle(String.format("-fx-text-fill: %s; -fx-font-size: 18px;", colorHex));
                        distanceLabel.setLayoutX(centerX + radius + 10);
                        distanceLabel.setLayoutY(centerY - radius + 20 + (sampleIndex * 25));
                        labelsPane.getChildren().add(distanceLabel);
                    } else {
                        Label distanceLabel = new Label(localizationService.getString("exploration.zero_distance"));
                        distanceLabel.setStyle(String.format("-fx-text-fill: %s; -fx-font-size: 18px;", colorHex));
                        distanceLabel.setLayoutX(centerX + radius + 10);
                        distanceLabel.setLayoutY(centerY - radius + 20 + (sampleIndex * 25));
                        labelsPane.getChildren().add(distanceLabel);
                    }
                    
                    sampleIndex++;
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
    
    /**
     * Affiche le panel du radar
     */
    public void showRadar() {
        if (radarContainer != null) {
            radarContainer.setVisible(true);
            radarContainer.setManaged(true);
        }
    }
    
    /**
     * Cache le panel du radar
     */
    public void hideRadar() {
        if (radarContainer != null) {
            radarContainer.setVisible(false);
            radarContainer.setManaged(false);
        }
    }
    
    /**
     * Retourne l'instance actuelle du RadarComponent (peut être null)
     */
    public static RadarComponent getInstance() {
        return instance;
    }
}

