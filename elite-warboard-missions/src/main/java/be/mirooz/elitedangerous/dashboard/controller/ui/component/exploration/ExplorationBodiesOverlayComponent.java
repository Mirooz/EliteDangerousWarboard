package be.mirooz.elitedangerous.dashboard.controller.ui.component.exploration;

import be.mirooz.elitedangerous.dashboard.model.exploration.SystemVisited;
import be.mirooz.elitedangerous.dashboard.service.PreferencesService;
import be.mirooz.elitedangerous.dashboard.controller.ui.manager.PopupManager;
import javafx.application.Platform;
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
import java.util.function.Function;

/**
 * Composant pour gérer l'overlay des corps d'exploration (mappables/exobio)
 * <p>
 * Ce composant gère :
 * - La création et gestion de la fenêtre overlay
 * - Le redimensionnement et déplacement de l'overlay
 * - Le curseur de transparence du background
 * - La sauvegarde/restauration des préférences
 */
public class ExplorationBodiesOverlayComponent {

    public static final double MIN_OPPACITY = 0.01;
    public static final int MIN_WIDTH_OVERLAY = 400;
    public static final int MIN_HEIGHT_OVERLAY = 300;
    private final PreferencesService preferencesService = PreferencesService.getInstance();
    private final PopupManager popupManager = PopupManager.getInstance();

    // Clés pour les préférences de l'overlay
    private static final String EXPLORATION_BODIES_OVERLAY_WIDTH_KEY = "exploration_bodies_overlay.width";
    private static final String EXPLORATION_BODIES_OVERLAY_HEIGHT_KEY = "exploration_bodies_overlay.height";
    private static final String EXPLORATION_BODIES_OVERLAY_OPACITY_KEY = "exploration_bodies_overlay.opacity";
    private static final String EXPLORATION_BODIES_OVERLAY_X_KEY = "exploration_bodies_overlay.x";
    private static final String EXPLORATION_BODIES_OVERLAY_Y_KEY = "exploration_bodies_overlay.y";
    private static final String EXPLORATION_BODIES_OVERLAY_TEXT_SCALE_KEY = "exploration_bodies_overlay.text_scale";

    private Stage overlayStage;
    private double overlayOpacity = 0.92;
    private Label resizeHandle;
    private Slider opacitySlider;
    private Slider textScaleSlider;
    private double textScale = 1.0;
    private StackPane stackPane;
    private VBox contentContainer;
    private Function<SystemVisited, VBox> bodyCardFactory;
    @SuppressWarnings("unused")
    private SystemVisited currentSystem;
    @SuppressWarnings("unused")
    private boolean showOnlyHighValue;

    public ExplorationBodiesOverlayComponent() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (overlayStage != null && overlayStage.isShowing()) {
                saveOverlayPreferences();
            }
        }));
    }

    /**
     * Définit la fonction factory pour créer les cartes de corps
     */
    public void setBodyCardFactory(Function<SystemVisited, VBox> factory) {
        this.bodyCardFactory = factory;
    }

    /**
     * Affiche l'overlay pour le système donné
     */
    public void showOverlay(SystemVisited system, boolean showOnlyHighValue) {
        this.currentSystem = system;
        this.showOnlyHighValue = showOnlyHighValue;

        // Si la fenêtre est déjà ouverte, on la ferme
        if (overlayStage != null && overlayStage.isShowing()) {
            saveOverlayPreferences();
            overlayStage.close();
            overlayStage = null;
            return;
        }

        createOverlayStage(system, showOnlyHighValue);
    }

    /**
     * Ferme l'overlay s'il est ouvert
     */
    public void closeOverlay() {
        if (overlayStage != null && overlayStage.isShowing()) {
            saveOverlayPreferences();
            popupManager.unregisterContainer(overlayStage);
            overlayStage.close();
            overlayStage = null;
        }
    }

    /**
     * Met à jour le contenu de l'overlay avec un nouveau système
     */
    public void updateContent(SystemVisited system, boolean showOnlyHighValue) {
        this.currentSystem = system;
        this.showOnlyHighValue = showOnlyHighValue;
        if (overlayStage != null && overlayStage.isShowing() && contentContainer != null && bodyCardFactory != null) {
            Platform.runLater(() -> {
                // Le contentContainer contient un ScrollPane, on doit accéder à son contenu
                if (contentContainer.getChildren().size() > 0 && 
                    contentContainer.getChildren().get(0) instanceof javafx.scene.control.ScrollPane) {
                    javafx.scene.control.ScrollPane scrollPane = 
                        (javafx.scene.control.ScrollPane) contentContainer.getChildren().get(0);
                    if (scrollPane.getContent() instanceof VBox) {
                        VBox bodiesList = (VBox) scrollPane.getContent();
                        bodiesList.getChildren().clear();
                        
                        VBox bodiesContent = bodyCardFactory.apply(system);
                        if (bodiesContent != null) {
                            bodiesList.getChildren().add(bodiesContent);
                            applyTextScaleToNode(bodiesContent, textScale);
                        }
                    }
                }
            });
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
    private void createOverlayStage(SystemVisited system, boolean showOnlyHighValue) {
        // Création de la fenêtre overlay
        overlayStage = new Stage();
        overlayStage.initStyle(StageStyle.TRANSPARENT);
        overlayStage.setAlwaysOnTop(true);
        overlayStage.setTitle("Exploration Bodies Overlay");
        overlayStage.setResizable(true);
        overlayStage.setMinWidth(MIN_WIDTH_OVERLAY);
        overlayStage.setMinHeight(MIN_HEIGHT_OVERLAY);

        // Restaurer les préférences sauvegardées
        restoreOverlayPreferences();

        // Créer le contenu de l'overlay
        createOverlayContent(system, showOnlyHighValue);

        // Configurer la scène
        Scene scene = new Scene(stackPane);
        scene.setFill(Color.TRANSPARENT);
        overlayStage.setScene(scene);
        overlayStage.setOpacity(1.0);

        // Appliquer les styles CSS
        scene.getStylesheets().add(getClass().getResource("/css/elite-theme.css").toExternalForm());

        // Enregistrer le container pour les popups
        popupManager.registerContainer(overlayStage, stackPane);
        stackPane.getStyleClass().addAll("overlay-root", "overlay-root-bordered");
        stackPane.setOnMouseExited(event -> {
            stackPane.getStyleClass().remove("overlay-root-bordered");
        });

        // Configurer les interactions (déplacement, redimensionnement)
        setupInteractions();

        // Afficher l'overlay
        overlayStage.show();
        overlayStage.setOnCloseRequest(event -> {
            saveOverlayPreferences();
            popupManager.unregisterContainer(overlayStage);
            overlayStage = null;
        });
    }

    /**
     * Crée le contenu de l'overlay
     */
    private void createOverlayContent(SystemVisited system, boolean showOnlyHighValue) {
        // Créer le conteneur principal pour la liste des corps avec ScrollPane
        VBox bodiesList = new VBox(5);
        bodiesList.setSpacing(5);
        bodiesList.setPadding(new Insets(5));

        // Créer la liste des corps si le factory est disponible
        if (bodyCardFactory != null && system != null) {
            VBox bodiesContent = bodyCardFactory.apply(system);
            if (bodiesContent != null) {
                bodiesList.getChildren().add(bodiesContent);
            }
        } else {
            Label emptyLabel = new Label("Aucun système sélectionné");
            emptyLabel.getStyleClass().add("exploration-overlay-title");
            bodiesList.getChildren().add(emptyLabel);
        }
        
        // Créer un ScrollPane pour permettre le défilement
        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane();
        scrollPane.setContent(bodiesList);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        
        contentContainer = new VBox();
        contentContainer.getChildren().add(scrollPane);
        VBox.setVgrow(scrollPane, javafx.scene.layout.Priority.ALWAYS);

        // Créer l'icône de redimensionnement
        resizeHandle = createResizeHandle();

        // Créer le curseur de transparence
        opacitySlider = createOpacitySlider();

        // Créer le curseur de scaling du texte
        textScaleSlider = createTextScaleSlider();

        stackPane = new StackPane();
        stackPane.getChildren().addAll(contentContainer, resizeHandle, opacitySlider, textScaleSlider);
        StackPane.setAlignment(resizeHandle, Pos.BOTTOM_RIGHT);
        StackPane.setAlignment(opacitySlider, Pos.BOTTOM_RIGHT);
        StackPane.setAlignment(textScaleSlider, Pos.BOTTOM_RIGHT);

        StackPane.setMargin(opacitySlider, new Insets(0, 30, 0, 0));
        StackPane.setMargin(textScaleSlider, new Insets(0, 60, 20, 0));

        // S'assurer que les sliders sont cliquables même avec opacity 0
        opacitySlider.setMouseTransparent(false);
        textScaleSlider.setMouseTransparent(false);
        resizeHandle.setMouseTransparent(false);

        // Appliquer le style initial
        updatePaneStyle(overlayOpacity, stackPane);

        // Appliquer le scaling initial du texte
        if (contentContainer.getChildren().size() > 0) {
            applyTextScaleToNode(contentContainer, textScale);
        }

        // Configurer les listeners
        setupOpacitySliderListener();
        setupTextScaleSliderListener();
    }

    /**
     * Crée l'icône de redimensionnement
     */
    private Label createResizeHandle() {
        Label resizeHandle = new Label("⤡");
        resizeHandle.getStyleClass().add("resize-handle");
        resizeHandle.setStyle("-fx-text-fill: gold;-fx-font-size: 36px; -fx-font-weight: bold; -fx-alignment: center;");
        resizeHandle.setOpacity(0.0);
        return resizeHandle;
    }

    /**
     * Crée le curseur de transparence
     */
    private Slider createOpacitySlider() {
        Slider slider = new Slider(MIN_OPPACITY, 1.0, overlayOpacity);
        slider.setOrientation(javafx.geometry.Orientation.VERTICAL);
        slider.setPrefWidth(20);
        slider.setPrefHeight(100);
        slider.setOpacity(0.0);
        slider.setMajorTickUnit(0.5);
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
        Slider slider = new Slider(0.5, 2.0, textScale);
        slider.setOrientation(javafx.geometry.Orientation.VERTICAL);
        slider.setPrefWidth(20);
        slider.setPrefHeight(100);
        slider.setOpacity(0.0);
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
     * Met à jour le scaling du texte dans le contenu
     */
    private void updateTextScale(double scale) {
        if (contentContainer != null) {
            applyTextScaleToNode(contentContainer, scale);
        }
    }

    /**
     * Applique le scaling du texte récursivement à tous les nœuds de texte
     */
    private void applyTextScaleToNode(javafx.scene.Node node, double scale) {
        if (node instanceof Label) {
            Label label = (Label) node;
            String currentStyle = label.getStyle();
            String scaleStyle = String.format(Locale.ENGLISH, "-fx-font-size: %.1fem;", scale);
            label.setStyle(scaleStyle + (currentStyle != null ? " " + currentStyle : ""));
        } else if (node instanceof Pane) {
            Pane pane = (Pane) node;
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

        scene.setOnMousePressed(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                offset[0] = e.getScreenX() - overlayStage.getX();
                offset[1] = e.getScreenY() - overlayStage.getY();

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
                    overlayStage.setX(e.getScreenX() - offset[0]);
                    overlayStage.setY(e.getScreenY() - offset[1]);
                }
            }
        });

        scene.setOnMouseReleased(e -> {
            isResizing[0] = false;
        });

        scene.setOnMouseMoved(e -> {
            double sceneWidth = scene.getWidth();
            double sceneHeight = scene.getHeight();
            double mouseX = e.getSceneX();
            double mouseY = e.getSceneY();

            if (mouseX >= sceneWidth - 25 && mouseY >= sceneHeight - 25) {
                scene.setCursor(javafx.scene.Cursor.SE_RESIZE);
                if (resizeHandle != null) resizeHandle.setOpacity(1.0);
                if (opacitySlider != null) opacitySlider.setOpacity(0.8);
                if (textScaleSlider != null) textScaleSlider.setOpacity(0.8);
            } else {
                scene.setCursor(javafx.scene.Cursor.DEFAULT);
                if (resizeHandle != null) resizeHandle.setOpacity(0.8);
                if (opacitySlider != null) opacitySlider.setOpacity(0.8);
                if (textScaleSlider != null) textScaleSlider.setOpacity(0.8);
            }
        });

        scene.setOnMouseExited(e -> {
            if (resizeHandle != null) resizeHandle.setOpacity(0.0);
            if (opacitySlider != null) opacitySlider.setOpacity(0.0);
            if (textScaleSlider != null) textScaleSlider.setOpacity(0.0);
        });

        scene.setOnMouseEntered(e -> {
            if (resizeHandle != null) resizeHandle.setOpacity(0.8);
            if (opacitySlider != null) opacitySlider.setOpacity(0.8);
            if (textScaleSlider != null) textScaleSlider.setOpacity(0.8);
        });
    }

    /**
     * Restaure les préférences de l'overlay
     */
    private void restoreOverlayPreferences() {
        String savedWidthStr = preferencesService.getPreference(EXPLORATION_BODIES_OVERLAY_WIDTH_KEY, "500");
        String savedHeightStr = preferencesService.getPreference(EXPLORATION_BODIES_OVERLAY_HEIGHT_KEY, "600");
        String savedOpacityStr = preferencesService.getPreference(EXPLORATION_BODIES_OVERLAY_OPACITY_KEY, "0.92");
        String savedXStr = preferencesService.getPreference(EXPLORATION_BODIES_OVERLAY_X_KEY, "100");
        String savedYStr = preferencesService.getPreference(EXPLORATION_BODIES_OVERLAY_Y_KEY, "100");
        String savedTextScaleStr = preferencesService.getPreference(EXPLORATION_BODIES_OVERLAY_TEXT_SCALE_KEY, "1.0");

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

        overlayStage.setX(finalX);
        overlayStage.setY(finalY);

        if (textScaleSlider != null) {
            textScaleSlider.setValue(textScale);
        }
    }

    /**
     * Sauvegarde les préférences de l'overlay
     */
    private void saveOverlayPreferences() {
        if (overlayStage != null && overlayStage.isShowing()) {
            preferencesService.setPreference(EXPLORATION_BODIES_OVERLAY_WIDTH_KEY, String.valueOf((int) overlayStage.getWidth()));
            preferencesService.setPreference(EXPLORATION_BODIES_OVERLAY_HEIGHT_KEY, String.valueOf((int) overlayStage.getHeight()));
            preferencesService.setPreference(EXPLORATION_BODIES_OVERLAY_OPACITY_KEY, String.valueOf(overlayOpacity));
            preferencesService.setPreference(EXPLORATION_BODIES_OVERLAY_X_KEY, String.valueOf((int) overlayStage.getX()));
            preferencesService.setPreference(EXPLORATION_BODIES_OVERLAY_Y_KEY, String.valueOf((int) overlayStage.getY()));
            preferencesService.setPreference(EXPLORATION_BODIES_OVERLAY_TEXT_SCALE_KEY, String.valueOf(textScale));
        }
    }
}

