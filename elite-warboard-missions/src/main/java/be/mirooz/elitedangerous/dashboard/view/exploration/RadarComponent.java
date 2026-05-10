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
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Boussole / mini-radar pour l’analyse exobio : heading depuis Status.json et positions des échantillons.
 * L’instance affichée dans la vue système doit être enregistrée via {@link #setPrimaryInstance(RadarComponent)}
 * pour que {@link DirectionReaderService} puisse l’afficher ou la masquer pendant la surveillance du fichier.
 */
public class RadarComponent {

    private static final double PREF_RADAR_HEIGHT = 200;
    private static final double PREF_RADAR_WIDTH = 350;
    private static final double FALLBACK_LAYOUT_SIZE = 200;
    private static final double LABEL_EXTENSION = 150;
    private static final long UPDATE_INTERVAL_NS = 500_000_000L;
    private static final Color COMPASS_STROKE = Color.rgb(255, 140, 0, 0.5);
    private static final Color AXIS_STROKE = Color.rgb(255, 140, 0, 0.7);
    private static final Color HEADING_FILL = Color.rgb(0, 255, 0);
    private static final Color HEADING_STROKE = Color.rgb(0, 200, 0);

    private final Pane radarContainer;
    private final Group radarGroup;
    private final Pane labelsPane;
    private final DirectionReaderService directionService;
    private final LocalizationService localizationService = LocalizationService.getInstance();
    private AnimationTimer updateTimer;

    /** Instance de la vue principale ; les radars clone pour overlay ne s’y enregistrent pas. */
    private static volatile RadarComponent primaryInstance;

    public RadarComponent() {
        directionService = DirectionReaderService.getInstance();

        radarContainer = new Pane();
        radarContainer.setPrefHeight(PREF_RADAR_HEIGHT);
        radarContainer.setMinHeight(PREF_RADAR_HEIGHT);
        radarContainer.setMaxHeight(PREF_RADAR_HEIGHT);
        radarContainer.setPrefWidth(PREF_RADAR_WIDTH);
        radarContainer.setStyle("-fx-background-color: #000000; -fx-border-color: -fx-elite-orange; -fx-border-width: 1px;");

        radarGroup = new Group();
        radarGroup.setLayoutX(0);
        radarGroup.setLayoutY(0);
        radarGroup.setManaged(false);
        radarGroup.setPickOnBounds(false);

        labelsPane = new Pane();
        labelsPane.setMouseTransparent(true);
        labelsPane.setLayoutX(0);
        labelsPane.setLayoutY(0);
        labelsPane.setManaged(true);
        labelsPane.setPickOnBounds(false);

        radarContainer.getChildren().addAll(radarGroup, labelsPane);
        radarContainer.setClip(new Rectangle());

        radarContainer.widthProperty().addListener((obs, oldVal, newVal) -> {
            updateRadar();
            updateClipping();
        });
        radarContainer.heightProperty().addListener((obs, oldVal, newVal) -> {
            updateRadar();
            updateClipping();
        });

        updateTimer = new AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                if (now - lastUpdate >= UPDATE_INTERVAL_NS) {
                    lastUpdate = now;
                    Platform.runLater(() -> {
                        updateRadar();
                        updateClipping();
                    });
                }
            }
        };
        updateTimer.start();

        radarContainer.setVisible(false);
        radarContainer.setManaged(false);

        Platform.runLater(() -> {
            updateRadar();
            updateClipping();
        });
    }

    /**
     * Enregistre le radar de la vue système (pour {@link DirectionReaderService}).
     */
    public static void setPrimaryInstance(RadarComponent component) {
        primaryInstance = component;
    }

    public static RadarComponent getPrimaryInstance() {
        return primaryInstance;
    }

    public Pane getRadarPane() {
        return radarContainer;
    }

    private void updateClipping() {
        if (radarGroup == null || radarContainer == null) {
            return;
        }

        double width = effectiveWidth();
        double height = effectiveHeight();

        if (width > 0 && height > 0) {
            double centerX = width / 2;
            double centerY = height / 2;
            double radius = Math.min(centerX, centerY) - 20;

            double clipWidth = width + LABEL_EXTENSION;
            Rectangle containerClip = new Rectangle(0, 0, clipWidth, height);
            radarContainer.setClip(containerClip);

            Circle clip = new Circle(centerX, centerY, radius);
            radarGroup.setClip(clip);

            radarGroup.setLayoutX(0);
            radarGroup.setLayoutY(0);
            labelsPane.setLayoutX(0);
            labelsPane.setLayoutY(0);

            labelsPane.setPrefWidth(width + LABEL_EXTENSION);
            labelsPane.setPrefHeight(height);
            labelsPane.setMaxWidth(Double.MAX_VALUE);
            labelsPane.setMaxHeight(height);
        }
    }

    private void updateRadar() {
        if (radarGroup == null) {
            return;
        }
        radarGroup.getChildren().clear();
        drawRadar();
        updateClipping();
    }

    private void drawRadar() {
        if (radarGroup == null) {
            return;
        }

        double width = effectiveWidth();
        double height = effectiveHeight();
        double centerX = width / 2;
        double centerY = height / 2;
        double radius = Math.min(centerX, centerY) - 20;

        drawCompassCircle(centerX, centerY, radius);
        drawCardinalAxes(centerX, centerY, radius);
        labelsPane.getChildren().clear();
        drawCardinalPointLabels(centerX, centerY, radius);

        Position currentPos = directionService.getCurrentPosition();
        if (currentPos == null) {
            return;
        }
        drawHeadingTriangle(currentPos, centerX, centerY, radius);
        drawBiologicalSamples(currentPos, centerX, centerY, radius);
    }

    private double effectiveWidth() {
        double w = radarContainer.getWidth();
        if (w == 0) {
            w = radarContainer.getPrefWidth() > 0 ? radarContainer.getPrefWidth() : FALLBACK_LAYOUT_SIZE;
        }
        return w;
    }

    private double effectiveHeight() {
        double h = radarContainer.getHeight();
        if (h == 0) {
            h = radarContainer.getPrefHeight() > 0 ? radarContainer.getPrefHeight() : FALLBACK_LAYOUT_SIZE;
        }
        return h;
    }

    private void drawCompassCircle(double centerX, double centerY, double radius) {
        Circle compassCircle = new Circle(centerX, centerY, radius);
        compassCircle.setFill(Color.BLACK);
        compassCircle.setStroke(COMPASS_STROKE);
        compassCircle.setStrokeWidth(2);
        radarGroup.getChildren().add(compassCircle);
    }

    private void drawCardinalAxes(double centerX, double centerY, double radius) {
        double lineLength = radius * 0.9;
        Line northLine = new Line(centerX, centerY - radius, centerX, centerY - lineLength);
        northLine.setStroke(AXIS_STROKE);
        northLine.setStrokeWidth(1);
        radarGroup.getChildren().add(northLine);

        Line southLine = new Line(centerX, centerY + radius, centerX, centerY + lineLength);
        southLine.setStroke(AXIS_STROKE);
        southLine.setStrokeWidth(1);
        radarGroup.getChildren().add(southLine);

        Line eastLine = new Line(centerX + radius, centerY, centerX + lineLength, centerY);
        eastLine.setStroke(AXIS_STROKE);
        eastLine.setStrokeWidth(1);
        radarGroup.getChildren().add(eastLine);

        Line westLine = new Line(centerX - radius, centerY, centerX - lineLength, centerY);
        westLine.setStroke(AXIS_STROKE);
        westLine.setStrokeWidth(1);
        radarGroup.getChildren().add(westLine);
    }

    private void drawCardinalPointLabels(double centerX, double centerY, double radius) {
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
    }

    private void drawHeadingTriangle(Position currentPos, double centerX, double centerY, double radius) {
        Integer heading = currentPos.getHeading();
        if (heading == null) {
            return;
        }
        double headingRad = Math.toRadians(90 - heading);
        double triangleLength = radius * 0.2;
        double triangleWidth = radius * 0.06;

        double tipX = centerX + Math.cos(headingRad) * triangleLength;
        double tipY = centerY - Math.sin(headingRad) * triangleLength;

        double baseAngle1 = headingRad + Math.PI / 2;
        double baseAngle2 = headingRad - Math.PI / 2;

        double baseX1 = centerX + Math.cos(baseAngle1) * triangleWidth;
        double baseY1 = centerY - Math.sin(baseAngle1) * triangleWidth;
        double baseX2 = centerX + Math.cos(baseAngle2) * triangleWidth;
        double baseY2 = centerY - Math.sin(baseAngle2) * triangleWidth;

        Polygon directionTriangle = new Polygon(
                tipX, tipY,
                baseX1, baseY1,
                baseX2, baseY2
        );
        directionTriangle.setFill(HEADING_FILL);
        directionTriangle.setStroke(HEADING_STROKE);
        directionTriangle.setStrokeWidth(1.5);
        radarGroup.getChildren().add(directionTriangle);
    }

    private void drawBiologicalSamples(Position currentPos, double centerX, double centerY, double radius) {
        List<Position> samplePositions =
                new ArrayList<>(directionService.getCurrentBiologicalSamplePositions());
        samplePositions.removeIf(Objects::isNull);
        Double colonyRangeMeter = directionService.getColonyRangeMeter();

        if (samplePositions.isEmpty()) {
            return;
        }

        double scaleFactor = 0.85;
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

        double maxSampleDistance = distances.stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0);

        double scaleDistance;
        if (colonyRangeMeter != null && colonyRangeMeter > 0) {
            scaleDistance = colonyRangeMeter / 0.75;
        } else {
            scaleDistance = maxSampleDistance;
            if (scaleDistance == 0) {
                scaleDistance = 1000;
            }
        }

        double exclusionRadiusOnRadar = radius * scaleFactor * 0.75;

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

            double sampleX;
            double sampleY;
            double realSampleX;
            double realSampleY;

            if (distance > 0) {
                double direction = currentPos.calculateDirectionTo(sample);
                double directionRad = Math.toRadians(90 - direction);
                double normalizedDistance = distance / scaleDistance;
                double maxRadius = radius * scaleFactor;
                double pointDistance = normalizedDistance * maxRadius;

                realSampleX = centerX + Math.cos(directionRad) * pointDistance;
                realSampleY = centerY - Math.sin(directionRad) * pointDistance;

                if (pointDistance > maxRadius) {
                    pointDistance = radius;
                }

                sampleX = centerX + Math.cos(directionRad) * pointDistance;
                sampleY = centerY - Math.sin(directionRad) * pointDistance;
            } else {
                sampleX = centerX;
                sampleY = centerY;
                realSampleX = centerX;
                realSampleY = centerY;
            }

            Color sampleColor;
            String colorHex;
            if (sampleIndex == 0) {
                sampleColor = Color.rgb(255, 0, 255);
                colorHex = "#FF00FF";
            } else if (sampleIndex == 1) {
                sampleColor = Color.rgb(0, 255, 255);
                colorHex = "#00FFFF";
            } else {
                sampleColor = Color.rgb(255, 255, 0);
                colorHex = "#FFFF00";
            }

            if (colonyRangeMeter != null && colonyRangeMeter > 0) {
                double exclusionRadius = exclusionRadiusOnRadar;
                Circle exclusionCircle = new Circle(realSampleX, realSampleY, exclusionRadius);
                exclusionCircle.setFill(Color.TRANSPARENT);
                exclusionCircle.setStroke(Color.color(sampleColor.getRed(), sampleColor.getGreen(), sampleColor.getBlue(), 0.6));
                exclusionCircle.setStrokeWidth(1.5);
                exclusionCircle.getStrokeDashArray().addAll(5.0, 5.0);
                radarGroup.getChildren().add(exclusionCircle);
            }

            Circle samplePoint = new Circle(sampleX, sampleY, 4);
            samplePoint.setFill(sampleColor);
            samplePoint.setStroke(Color.WHITE);
            samplePoint.setStrokeWidth(1);
            radarGroup.getChildren().add(samplePoint);

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

    public void stop() {
        if (updateTimer != null) {
            updateTimer.stop();
            updateTimer = null;
        }
    }

    public void showRadar() {
        if (radarContainer != null) {
            radarContainer.setVisible(true);
            radarContainer.setManaged(true);
            syncExobioSectionParentVisibility(true);
        }
    }

    public void hideRadar() {
        if (radarContainer != null) {
            radarContainer.setVisible(false);
            radarContainer.setManaged(false);
            syncExobioSectionParentVisibility(false);
        }
    }

    /**
     * Affiche ou masque le VBox exploration (classe {@code exploration-exobio-radar-section}) parent du radar.
     * Nécessaire car les clones (overlay colonisation) n’ont pas ce parent ; le listener seul peut rater un timing.
     */
    private void syncExobioSectionParentVisibility(boolean visible) {
        javafx.scene.Parent p = radarContainer != null ? radarContainer.getParent() : null;
        if (p != null && p.getStyleClass().contains("exploration-exobio-radar-section")) {
            p.setVisible(visible);
            p.setManaged(visible);
        }
    }

    public void forceUpdate() {
        if (radarContainer != null && radarContainer.isVisible()) {
            Platform.runLater(() -> {
                updateRadar();
                updateClipping();
            });
        }
    }
}
