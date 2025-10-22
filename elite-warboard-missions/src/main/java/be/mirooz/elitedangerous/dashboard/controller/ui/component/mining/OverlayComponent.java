package be.mirooz.elitedangerous.dashboard.controller.ui.component.mining;

import be.mirooz.elitedangerous.dashboard.model.events.ProspectedAsteroid;
import be.mirooz.elitedangerous.dashboard.service.PreferencesService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.Locale;

/**
 * Composant pour gérer l'overlay de prospecteur
 * <p>
 * Ce composant gère :
 * - La création et gestion de la fenêtre overlay
 * - Le redimensionnement et déplacement de l'overlay
 * - Le curseur de transparence du background
 * - La sauvegarde/restauration des préférences
 */
public class OverlayComponent {

    public static final double MIN_OPPACITY = 0.01;
    public static final int MIN_WIDTH_OVERLAY = 200;
    public static final int MIN_HEIGHT_OVERLAY = 150;
    private final PreferencesService preferencesService = PreferencesService.getInstance();

    // Clés pour les préférences de l'overlay
    private static final String OVERLAY_WIDTH_KEY = "overlay.width";
    private static final String OVERLAY_HEIGHT_KEY = "overlay.height";
    private static final String OVERLAY_OPACITY_KEY = "overlay.opacity";
    private static final String OVERLAY_X_KEY = "overlay.x";
    private static final String OVERLAY_Y_KEY = "overlay.y";
    private static final String OVERLAY_TEXT_SCALE_KEY = "overlay.text_scale";

    private Stage overlayStage;
    private double overlayOpacity;
    private Slider opacitySlider;
    private Slider textScaleSlider;
    private double textScale = 1.0;
    private StackPane stackPane;

    /**
     * Affiche l'overlay pour le prospecteur donné
     */
    public void showOverlay(ProspectedAsteroid prospector) {
        if (prospector == null) {
            System.out.println("⚠️ Aucun prospecteur à afficher dans l'overlay.");
        }

        // Si la fenêtre est déjà ouverte, on la ferme
        if (overlayStage != null && overlayStage.isShowing()) {
            saveOverlayPreferences();
            overlayStage.close();
            overlayStage = null;
            return;
        }

        createOverlayStage(prospector);
    }

    /**
     * Ferme l'overlay s'il est ouvert
     */
    public void closeOverlay() {
        if (overlayStage != null && overlayStage.isShowing()) {
            saveOverlayPreferences();
            overlayStage.close();
            overlayStage = null;
        }
    }

    /**
     * Met à jour le contenu de l'overlay avec un nouveau prospecteur
     */
    public void updateContent(ProspectedAsteroid prospector) {
        if (overlayStage != null && overlayStage.isShowing() && stackPane != null) {
            VBox newCard = createOverlayCard(prospector);
            stackPane.getChildren().set(0, newCard);
            // Appliquer le scaling actuel à la nouvelle carte
            applyTextScaleToNode(newCard, textScale);
        }
    }

    /**
     * Vide le contenu de l'overlay (affiche une carte vide)
     */
    public void clearContent() {
        if (overlayStage != null && overlayStage.isShowing() && stackPane != null) {
            VBox emptyCard = createEmptyCard();
            stackPane.getChildren().set(0, emptyCard);
            // Appliquer le scaling actuel à la carte vide
            applyTextScaleToNode(emptyCard, textScale);
        }
    }

    /**
     * Vérifie si l'overlay est actuellement affiché
     */
    public boolean isShowing() {
        return overlayStage != null && overlayStage.isShowing();
    }

    /**
     * Crée la fenêtre overlay
     */
    private void createOverlayStage(ProspectedAsteroid prospector) {
        // Création de la fenêtre overlay
        overlayStage = new Stage();
        overlayStage.initStyle(StageStyle.TRANSPARENT);
        overlayStage.setAlwaysOnTop(true);
        overlayStage.setTitle("Prospector Overlay");
        overlayStage.setResizable(true);

        // Définir la taille par défaut et minimale
        overlayStage.setMinWidth(MIN_WIDTH_OVERLAY);
        overlayStage.setMinHeight(MIN_HEIGHT_OVERLAY);

        // Restaurer les préférences sauvegardées
        restoreOverlayPreferences();

        // Créer le contenu de l'overlay
        createOverlayContent(prospector);

        // Configurer la scène
        Scene scene = new Scene(stackPane);
        scene.setFill(Color.TRANSPARENT);
        overlayStage.setScene(scene);
        overlayStage.setOpacity(1.0);

        // Appliquer les styles CSS
        scene.getStylesheets().add(getClass().getResource("/css/elite-theme.css").toExternalForm());
        // Style racine pour cibler les scrollbars overlay
        stackPane.getStyleClass().addAll("overlay-root","overlay-root-bordered");
        stackPane.setOnMouseExited(event -> {
            stackPane.getStyleClass().remove("overlay-root-bordered");
        });
        // Configurer les interactions (déplacement, redimensionnement)
        setupInteractions();

        // Afficher l'overlay
        overlayStage.show();
    }

    /**
     * Crée le contenu de l'overlay
     */
    private void createOverlayContent(ProspectedAsteroid prospector) {
        // Créer la carte du prospecteur
        VBox mirrorCard = createOverlayCard(prospector);

        // Créer l'icône de redimensionnement
        Label resizeHandle = createResizeHandle();

        // Créer le curseur de transparence
        opacitySlider = createOpacitySlider();
        
        // Créer le curseur de scaling du texte
        textScaleSlider = createTextScaleSlider();

        // Créer le conteneur principal
        stackPane = new StackPane();
        stackPane.getChildren().addAll(mirrorCard, resizeHandle, opacitySlider, textScaleSlider);
        StackPane.setAlignment(resizeHandle, Pos.BOTTOM_RIGHT);
        StackPane.setAlignment(opacitySlider, Pos.BOTTOM_RIGHT);
        StackPane.setAlignment(textScaleSlider, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(opacitySlider, new Insets(0, 30, 0, 0));
        StackPane.setMargin(textScaleSlider, new Insets(0, 60, 20, 0));
        stackPane.setPickOnBounds(true);

        // Appliquer le style initial
        updatePaneStyle(overlayOpacity, stackPane);
        
        // Appliquer le scaling initial du texte
        applyTextScaleToNode(mirrorCard, textScale);

        // Configurer le listener du curseur
        setupOpacitySliderListener();
        setupTextScaleSliderListener();
    }

    /**
     * Crée une carte de prospecteur avec le style d'overlay
     */
    private VBox createOverlayCard(ProspectedAsteroid prospector) {
        VBox card;
        if (prospector == null) {
            card = createEmptyCard();
        } else {
            card = ProspectorCardComponent.createProspectorCard(prospector);
        }
        card.getStyleClass().add("mirror-overlay");
        return card;
    }

    /**
     * Crée une carte vide pour l'overlay
     */
    private VBox createEmptyCard() {
        VBox card = new VBox();
        card.getStyleClass().add("mirror-overlay");
        return card;
    }

    /**
     * Crée l'icône de redimensionnement
     */
    private Label createResizeHandle() {
        Label resizeHandle = new Label("⤡");
        resizeHandle.getStyleClass().add("resize-handle");
        resizeHandle.setStyle("-fx-text-fill: gold;-fx-font-size: 36px; -fx-font-weight: bold; -fx-alignment: center;");
        resizeHandle.setOpacity(0.0); // Masquer par défaut
        return resizeHandle;
    }

    /**
     * Crée le curseur de transparence
     */
    private Slider createOpacitySlider() {
        Slider slider = new Slider(MIN_OPPACITY, 1.0, overlayOpacity);
        slider.setOrientation(javafx.geometry.Orientation.VERTICAL);
        slider.setPrefWidth(20);
        slider.setPrefHeight(120);
        slider.setPrefWidth(12);
        slider.setOpacity(0.0); // Masquer par défaut
        // Style dédié vertical (étroit) en plus de la classe par défaut "slider"
        slider.getStyleClass().add("opacity-slider");

        // Configuration pour des valeurs plus précises
        slider.setMajorTickUnit(0.2);
        slider.setMinorTickCount(1);
        slider.setShowTickLabels(false);
        slider.setShowTickMarks(false);
        slider.setSnapToTicks(false);

        return slider;
    }
    
    /**
     * Crée le curseur de scaling du texte
     */
    private Slider createTextScaleSlider() {
        Slider slider = new Slider(0.5, 3.0, textScale);
        slider.setOrientation(javafx.geometry.Orientation.HORIZONTAL);
        slider.setPrefWidth(140);
        slider.setOpacity(0.0); // Masquer par défaut
        slider.getStyleClass().add("text-scale-slider");
        
        // Configuration pour des valeurs précises
        slider.setMajorTickUnit(0.5);
        slider.setMinorTickCount(1);
        slider.setShowTickLabels(false);
        slider.setShowTickMarks(false);
        slider.setSnapToTicks(false);
        
        return slider;
    }

    /**
     * Configure le listener du curseur de transparence
     */
    private void setupOpacitySliderListener() {
        opacitySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double opacity = Math.max(newVal.doubleValue(), MIN_OPPACITY);
            updatePaneStyle(opacity, stackPane);
            overlayOpacity = opacity;
        });
    }
    
    /**
     * Configure le listener du curseur de scaling du texte
     */
    private void setupTextScaleSliderListener() {
        textScaleSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            textScale = newVal.doubleValue();
            updateTextScale(textScale);
        });
    }

    /**
     * Met à jour le style du StackPane
     */
    private void updatePaneStyle(double opacity, StackPane stackPane) {
        double stackPaneOpacity = Math.max(MIN_OPPACITY, opacity);
        overlayOpacity = stackPaneOpacity;
        String style = String.format(
                Locale.US,
                "-fx-background-color: rgba(0, 0, 0, %.2f);",
                stackPaneOpacity
        );
        stackPane.setStyle(style);
    }
    
    /**
     * Met à jour le scaling du texte dans la carte
     */
    private void updateTextScale(double scale) {
        if (stackPane != null && stackPane.getChildren().size() > 0) {
            VBox card = (VBox) stackPane.getChildren().get(0);
            applyTextScaleToNode(card, scale);
        }
    }
    
    /**
     * Applique le scaling du texte récursivement à tous les nœuds de texte
     */
    private void applyTextScaleToNode(javafx.scene.Node node, double scale) {
        if (node instanceof Label) {
            Label label = (Label) node;
            String scaleStyle = String.format(Locale.ENGLISH, 
                "-fx-font-size: %.1fem;", scale);
            label.setStyle(scaleStyle);
        } else if (node instanceof javafx.scene.layout.Pane) {
            javafx.scene.layout.Pane pane = (javafx.scene.layout.Pane) node;
            for (javafx.scene.Node child : pane.getChildren()) {
                applyTextScaleToNode(child, scale);
            }
        }
    }

    /**
     * Configure les interactions (déplacement, redimensionnement, survol)
     */
    private void setupInteractions() {
        final double[] offset = new double[2];
        final double[] resizeOffset = new double[2];
        final boolean[] isResizing = {false};

        Scene scene = overlayStage.getScene();

        // Gestion du clic et du glisser
        scene.setOnMousePressed(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                offset[0] = e.getScreenX() - overlayStage.getX();
                offset[1] = e.getScreenY() - overlayStage.getY();

                // Vérifier si on est dans la zone de redimensionnement
                double sceneWidth = scene.getWidth();
                double sceneHeight = scene.getHeight();
                double mouseX = e.getSceneX();
                double mouseY = e.getSceneY();

                if (mouseX >= sceneWidth - 25 && mouseY >= sceneHeight - 25) {
                    isResizing[0] = true;
                    resizeOffset[0] = e.getScreenX();
                    resizeOffset[1] = e.getScreenY();
                }
            }
        });

        scene.setOnMouseDragged(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                if (isResizing[0]) {
                    // Redimensionnement
                    double deltaX = e.getScreenX() - resizeOffset[0];
                    double deltaY = e.getScreenY() - resizeOffset[1];

                    double newWidth = overlayStage.getWidth() + deltaX;
                    double newHeight = overlayStage.getHeight() + deltaY;

                    if (newWidth >= overlayStage.getMinWidth()) {
                        overlayStage.setWidth(newWidth);
                    }
                    if (newHeight >= overlayStage.getMinHeight()) {
                        overlayStage.setHeight(newHeight);
                    }

                    resizeOffset[0] = e.getScreenX();
                    resizeOffset[1] = e.getScreenY();
                } else {
                    // Déplacement
                    overlayStage.setX(e.getScreenX() - offset[0]);
                    overlayStage.setY(e.getScreenY() - offset[1]);
                }
            }
        });

        scene.setOnMouseReleased(e -> {
            isResizing[0] = false;
        });

        // Gestion du curseur et de la visibilité des contrôles
        scene.setOnMouseMoved(e -> {
            double sceneWidth = scene.getWidth();
            double sceneHeight = scene.getHeight();
            double mouseX = e.getSceneX();
            double mouseY = e.getSceneY();

            // Zone de redimensionnement : coin inférieur droit (25x25 pixels)
            if (mouseX >= sceneWidth - 25 && mouseY >= sceneHeight - 25) {
                scene.setCursor(javafx.scene.Cursor.SE_RESIZE);
                if (stackPane.getChildren().size() > 3) {
                    ((Label) stackPane.getChildren().get(1)).setOpacity(1.0); // resizeHandle
                    ((Slider) stackPane.getChildren().get(2)).setOpacity(0.8); // opacitySlider
                    ((Slider) stackPane.getChildren().get(3)).setOpacity(0.8); // textScaleSlider
                }
            } else {
                scene.setCursor(javafx.scene.Cursor.DEFAULT);
                if (stackPane.getChildren().size() > 3) {
                    ((Label) stackPane.getChildren().get(1)).setOpacity(0.8); // resizeHandle
                    ((Slider) stackPane.getChildren().get(2)).setOpacity(0.8); // opacitySlider
                    ((Slider) stackPane.getChildren().get(3)).setOpacity(0.8); // textScaleSlider
                }
            }
        });

        // Masquer les contrôles quand la souris quitte la scène
        scene.setOnMouseExited(e -> {
            if (stackPane.getChildren().size() > 3) {
                ((Label) stackPane.getChildren().get(1)).setOpacity(0.0); // resizeHandle
                ((Slider) stackPane.getChildren().get(2)).setOpacity(0.0); // opacitySlider
                ((Slider) stackPane.getChildren().get(3)).setOpacity(0.0); // textScaleSlider
            }
        });

        // Afficher les contrôles quand la souris entre dans la scène
        scene.setOnMouseEntered(e -> {
            if (stackPane.getChildren().size() > 3) {
                ((Label) stackPane.getChildren().get(1)).setOpacity(0.8); // resizeHandle
                ((Slider) stackPane.getChildren().get(2)).setOpacity(0.8); // opacitySlider
                ((Slider) stackPane.getChildren().get(3)).setOpacity(0.8); // textScaleSlider
            }
        });

        // Listener pour la fermeture de la fenêtre
        overlayStage.setOnCloseRequest(event -> {
            saveOverlayPreferences();
            overlayStage = null;
        });
    }

    /**
     * Restaure les préférences de l'overlay
     */
    private void restoreOverlayPreferences() {
        String savedWidthStr = preferencesService.getPreference(OVERLAY_WIDTH_KEY, "600");
        String savedHeightStr = preferencesService.getPreference(OVERLAY_HEIGHT_KEY, "500");
        String savedOpacityStr = preferencesService.getPreference(OVERLAY_OPACITY_KEY, "0.92");
        String savedXStr = preferencesService.getPreference(OVERLAY_X_KEY, "100");
        String savedYStr = preferencesService.getPreference(OVERLAY_Y_KEY, "100");
        String savedTextScaleStr = preferencesService.getPreference(OVERLAY_TEXT_SCALE_KEY, "1.0");

        double savedWidth = Double.parseDouble(savedWidthStr);
        double savedHeight = Double.parseDouble(savedHeightStr);
        double savedX = Double.parseDouble(savedXStr);
        double savedY = Double.parseDouble(savedYStr);
        overlayOpacity = Double.parseDouble(savedOpacityStr);
        textScale = Double.parseDouble(savedTextScaleStr);

        double width = Math.max(savedWidth, overlayStage.getMinWidth());
        overlayStage.setWidth(width);
        double height = Math.max(savedHeight, overlayStage.getMinHeight());
        overlayStage.setHeight(height);
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double screenWidth = screenBounds.getWidth();
        double screenHeight = screenBounds.getHeight();
        double finalX = Math.max(0, Math.min(savedX, screenWidth - width));
        double finalY = Math.max(0, Math.min(savedY, screenHeight - height));

// Applique la position
        overlayStage.setX(finalX);
        overlayStage.setY(finalY);
        
        // Appliquer le scaling du texte
        if (textScaleSlider != null) {
            textScaleSlider.setValue(textScale);
        }
    }

    /**
     * Sauvegarde les préférences de l'overlay
     */
    private void saveOverlayPreferences() {
        if (overlayStage != null && overlayStage.isShowing()) {
            preferencesService.setPreference(OVERLAY_WIDTH_KEY, String.valueOf((int) overlayStage.getWidth()));
            preferencesService.setPreference(OVERLAY_HEIGHT_KEY, String.valueOf((int) overlayStage.getHeight()));
            preferencesService.setPreference(OVERLAY_OPACITY_KEY, String.valueOf(overlayOpacity));
            preferencesService.setPreference(OVERLAY_X_KEY, String.valueOf((int) overlayStage.getX()));
            preferencesService.setPreference(OVERLAY_Y_KEY, String.valueOf((int) overlayStage.getY()));
            preferencesService.setPreference(OVERLAY_TEXT_SCALE_KEY, String.valueOf(textScale));
            System.out.println("💾 Préférences overlay sauvegardées: " +
                    (int) overlayStage.getWidth() + "x" + (int) overlayStage.getHeight() +
                    " (opacité: " + String.format("%.2f", overlayOpacity) + 
                    ", position: " + (int) overlayStage.getX() + "," + (int) overlayStage.getY() +
                    ", scaling: " + String.format("%.2f", textScale) + ")");
        }
    }
    

}
