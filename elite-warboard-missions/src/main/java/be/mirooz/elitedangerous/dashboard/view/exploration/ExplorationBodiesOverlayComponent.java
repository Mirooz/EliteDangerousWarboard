package be.mirooz.elitedangerous.dashboard.view.exploration;

import be.mirooz.elitedangerous.dashboard.model.exploration.SystemVisited;
import be.mirooz.elitedangerous.dashboard.service.PreferencesService;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.view.common.managers.PopupManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

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
    public static final int MIN_WIDTH_OVERLAY = 430;
    public static final int MIN_HEIGHT_OVERLAY = 300;
    private final PreferencesService preferencesService = PreferencesService.getInstance();
    private final PopupManager popupManager = PopupManager.getInstance();
    private final LocalizationService localizationService = LocalizationService.getInstance();

    // Clés pour les préférences de l'overlay
    private static final String EXPLORATION_BODIES_OVERLAY_WIDTH_KEY = "exploration_bodies_overlay.width";
    private static final String EXPLORATION_BODIES_OVERLAY_HEIGHT_KEY = "exploration_bodies_overlay.height";
    private static final String EXPLORATION_BODIES_OVERLAY_OPACITY_KEY = "exploration_bodies_overlay.opacity";
    private static final String EXPLORATION_BODIES_OVERLAY_X_KEY = "exploration_bodies_overlay.x";
    private static final String EXPLORATION_BODIES_OVERLAY_Y_KEY = "exploration_bodies_overlay.y";
    private static final String EXPLORATION_BODIES_OVERLAY_TEXT_SCALE_KEY = "exploration_bodies_overlay.text_scale";

    private Stage overlayStage;
    private Popup overlayPopup;
    private double overlayOpacity = 0.92;
    private Label resizeHandle;
    private Slider opacitySlider;
    private double textScale = 1.0;
    private StackPane stackPane;
    private StackPane popupStackPane;
    private VBox contentCard;
    private Function<SystemVisited, VBox> bodyCardFactory;
    private double popupWidth = 420.0; // Largeur par défaut du popup
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
    public void showOverlay(SystemVisited system, boolean showOnlyHighValue,boolean setSelected) {
        this.currentSystem = system;
        this.showOnlyHighValue = showOnlyHighValue;

        // Si la fenêtre est déjà ouverte, on la ferme
        if (overlayStage != null && overlayStage.isShowing()) {
            saveOverlayPreferences();
            overlayStage.close();
            overlayStage = null;
            return;
        }

        createOverlayStage(system, showOnlyHighValue,setSelected);
    }
    private void makeNodeDraggable(Node node, Stage stage) {
        final double[] offset = new double[2];
        final double[] resizeOffset = new double[2];
        final boolean[] isResizing = {false};

        node.setOnMousePressed(e -> {
            if (e.getButton() != javafx.scene.input.MouseButton.PRIMARY) {
                return;
            }

            Scene scene = stage.getScene();
            double mouseX = e.getSceneX();
            double mouseY = e.getSceneY();
            double sceneWidth = scene.getWidth();
            double sceneHeight = scene.getHeight();

            // 🔧 Détecter la zone de resize (25x25)
            if (mouseX >= sceneWidth - 25 && mouseY >= sceneHeight - 25) {
                isResizing[0] = true;
                resizeOffset[0] = e.getScreenX();
                resizeOffset[1] = e.getScreenY();
                return;
            }

            // 🧲 Sinon → déplacement
            isResizing[0] = false;
            offset[0] = e.getScreenX() - stage.getX();
            offset[1] = e.getScreenY() - stage.getY();
        });

        node.setOnMouseDragged(e -> {
            if (e.getButton() != javafx.scene.input.MouseButton.PRIMARY) {
                return;
            }

            Scene scene = stage.getScene();
            double mouseX = e.getSceneX();
            double mouseY = e.getSceneY();
            double sceneWidth = scene.getWidth();
            double sceneHeight = scene.getHeight();

            if (isResizing[0]) {
                // 🔧 Resize, exactement comme dans setupInteractions()
                double deltaX = e.getScreenX() - resizeOffset[0];
                double deltaY = e.getScreenY() - resizeOffset[1];

                double newWidth = stage.getWidth() + deltaX;
                double newHeight = stage.getHeight() + deltaY;

                if (newWidth >= stage.getMinWidth()) stage.setWidth(newWidth);
                if (newHeight >= stage.getMinHeight()) stage.setHeight(newHeight);

                resizeOffset[0] = e.getScreenX();
                resizeOffset[1] = e.getScreenY();
                return;
            }

            // 🧲 Déplacement normal
            stage.setX(e.getScreenX() - offset[0]);
            stage.setY(e.getScreenY() - offset[1]);
        });

        node.setOnMouseReleased(e -> isResizing[0] = false);
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
        if (overlayStage != null && overlayStage.isShowing() && stackPane != null && bodyCardFactory != null) {
            Platform.runLater(() -> {
                VBox newCard = createOverlayCard(system);
                // contentCard est à l'index 0
                stackPane.getChildren().set(0, newCard);
                contentCard = newCard;
                // Appliquer le scaling actuel à la nouvelle carte
                applyTextScaleToNode(newCard, textScale);
            });
        }
        // Mettre à jour le popup s'il est ouvert
        if (overlayPopup != null && overlayPopup.isShowing() && popupStackPane != null && bodyCardFactory != null) {
            Platform.runLater(() -> {
                updatePopupContent(system);
            });
        }
    }

    /**
     * Met à jour le contenu du popup
     */
    private void updatePopupContent(SystemVisited system) {
        if (popupStackPane == null || popupStackPane.getChildren().isEmpty()) {
            return;
        }
        
        VBox oldCard = (VBox) popupStackPane.getChildren().get(0);
        double currentWidth = oldCard.getPrefWidth();
        if (currentWidth <= 0) {
            currentWidth = popupWidth; // Utiliser la largeur stockée si nécessaire
        }
        
        // Créer la nouvelle carte
        VBox newCard = createOverlayCard(system);
        
        // Récupérer la ScrollPane et extraire le contenu comme dans createPopup
        if (newCard.getChildren().size() > 0) {
            ScrollPane scrollPane = (ScrollPane) newCard.getChildren().get(0);
            Parent content = (Parent) scrollPane.getContent();
            
            // Supprimer la ScrollPane et ajouter le contenu directement
            newCard.getChildren().remove(scrollPane);
            newCard.getChildren().add(content);
            
            // Forcer la largeur du contenu avant le layout
            if (content instanceof Region contentRegion) {
                contentRegion.setPrefWidth(currentWidth);
                contentRegion.setMinWidth(currentWidth);
                contentRegion.setMaxWidth(currentWidth);
            }
            
            // Forcer application du CSS et layout avec la largeur fixe
            content.applyCss();
            content.autosize();
            content.layout();
            
            double contentHeight = content.getBoundsInLocal().getHeight();
            double finalHeight = Math.max(MIN_HEIGHT_OVERLAY, contentHeight + 20);
            
            // Appliquer la taille
            newCard.setPrefSize(currentWidth, finalHeight);
            popupStackPane.setPrefSize(currentWidth, finalHeight);
        }
        
        // Remplacer l'ancienne carte par la nouvelle
        popupStackPane.getChildren().set(0, newCard);
    }

    /**
     * Recalcule et met à jour la taille du popup
     */
    public void resizePopup() {
        if (popupStackPane == null || popupStackPane.getChildren().isEmpty() || currentSystem == null) {
            return;
        }
        
        VBox card = (VBox) popupStackPane.getChildren().get(0);
        double currentWidth = card.getPrefWidth();
        if (currentWidth <= 0) {
            currentWidth = popupWidth;
        }
        
        // Recalculer la hauteur du contenu
        if (card.getChildren().size() > 0) {
            Node content = card.getChildren().get(0);
            
            // Forcer application du CSS et layout
            if (content instanceof Parent contentParent) {
                contentParent.applyCss();
                contentParent.autosize();
                contentParent.layout();
                
                double contentHeight = contentParent.getBoundsInLocal().getHeight();
                double finalHeight = Math.max(MIN_HEIGHT_OVERLAY, contentHeight + 20);
                
                // Appliquer la nouvelle taille
                card.setPrefSize(currentWidth, finalHeight);
                popupStackPane.setPrefSize(currentWidth, finalHeight);
            }
        }
    }

    /**
     * Vérifie si l'overlay est actuellement affiché
     */
    public boolean isShowing() {
        return (overlayStage != null && overlayStage.isShowing()) || 
               (overlayPopup != null && overlayPopup.isShowing());
    }

    /**
     * Vérifie si l'overlay (Stage) est actuellement affiché
     */
    public boolean isOverlayShowing() {
        return overlayStage != null && overlayStage.isShowing();
    }

    /**
     * Vérifie si le popup est actuellement affiché
     */
    public boolean isPopupShowing() {
        return overlayPopup != null && overlayPopup.isShowing();
    }

    /**
     * Affiche un popup pour le système donné avec une largeur spécifique
     * Le popup utilise la largeur du panneau de gauche et calcule la hauteur nécessaire
     * mais n'est pas modifiable ni cliquable
     */
    public void showPopup(SystemVisited system) {
        this.currentSystem = system;
        this.showOnlyHighValue = showOnlyHighValue;

        // Si le popup est déjà ouvert, on le ferme
        if (overlayPopup != null && overlayPopup.isShowing()) {
            closePopup();
            return;
        }

        createPopup(system);
    }

    /**
     * Affiche un popup pour le système donné avec une largeur spécifique
     */
    public void showPopup(SystemVisited system, boolean showOnlyHighValue, double leftPanelWidth) {
        this.currentSystem = system;
        this.showOnlyHighValue = showOnlyHighValue;
        this.popupWidth = leftPanelWidth;

        // Si le popup est déjà ouvert, on le ferme
        if (overlayPopup != null && overlayPopup.isShowing()) {
            closePopup();
        }

        createPopup(system);
    }

    /**
     * Ferme le popup s'il est ouvert
     */
    public void closePopup() {
        if (overlayPopup != null && overlayPopup.isShowing()) {
            overlayPopup.hide();
            // Nettoyer l'enregistrement si nécessaire
            if (overlayPopup.getOwnerWindow() != null) {
                popupManager.unregisterContainer((Stage) overlayPopup.getOwnerWindow());
            }
            overlayPopup = null;
        }
    }

    /**
     * Crée le popup
     */
    private void createPopup(SystemVisited system) {

        overlayPopup = new Popup();
        overlayPopup.setAutoHide(false);
        overlayPopup.setAutoFix(false);

        double width = popupWidth;
        double minHeight = MIN_HEIGHT_OVERLAY;

        VBox popupContentCard = createOverlayCard(system);

        // Récupérer la ScrollPane (tu sais qu’elle est en index 0)
        ScrollPane scrollPane = (ScrollPane) popupContentCard.getChildren().get(0);
        Parent content = (Parent) scrollPane.getContent();

        // On supprime totalement la ScrollPane du popup
        popupContentCard.getChildren().remove(scrollPane);
        popupContentCard.getChildren().add(content);

        // Forcer application du CSS et layout correct
        content.applyCss();
        content.autosize();
        content.layout();

        double contentHeight = content.getBoundsInLocal().getHeight();
        double finalHeight = Math.max(minHeight, contentHeight + 20);

        // Taille finale du popup
        popupContentCard.setPrefSize(width, finalHeight);

        // stackPane = visuel, style, click-through
        popupStackPane = new StackPane(popupContentCard);
        popupStackPane.setPrefSize(width, finalHeight);
        popupStackPane.setMouseTransparent(true);
        popupStackPane.setCursor(Cursor.NONE);
        popupStackPane.getStyleClass().add("mirror-overlay");

        overlayPopup.getContent().add(popupStackPane);

        // Position via préférences
        double posX = Double.parseDouble(preferencesService.getPreference(EXPLORATION_BODIES_OVERLAY_X_KEY, "100"));
        double posY = Double.parseDouble(preferencesService.getPreference(EXPLORATION_BODIES_OVERLAY_Y_KEY, "100"));

        overlayPopup.setX(posX);
        overlayPopup.setY(posY);

        Window ownerWindow = Stage.getWindows()
                .stream()
                .filter(Window::isShowing)
                .findFirst()
                .orElse(null);

        overlayPopup.show(ownerWindow);
    }

    /**
     * Crée la fenêtre overlay
     */
    private void createOverlayStage(SystemVisited system, boolean showOnlyHighValue, boolean setSelected) {
        // Création de la fenêtre overlay
        overlayStage = new Stage();
        overlayStage.initStyle(StageStyle.TRANSPARENT);
        overlayStage.setAlwaysOnTop(true);
        overlayStage.setTitle(localizationService.getString("exploration.exploration_bodies_overlay"));
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
        // Style racine pour cibler les scrollbars overlay
        stackPane.getStyleClass().addAll("overlay-root");
        if (setSelected){
            stackPane.getStyleClass().add("overlay-root-bordered");
        }
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
        // Créer la carte du contenu
        contentCard = createOverlayCard(system);

        // Créer l'icône de redimensionnement
        resizeHandle = createResizeHandle();

        // Créer le curseur de transparence
        opacitySlider = createOpacitySlider();

        
        stackPane = new StackPane();
        makeNodeDraggable(stackPane, overlayStage);

        // Ordre important: contentCard en premier
        stackPane.getChildren().addAll(contentCard, resizeHandle, opacitySlider);
        StackPane.setAlignment(resizeHandle, Pos.BOTTOM_RIGHT);
        StackPane.setAlignment(opacitySlider, Pos.BOTTOM_RIGHT);

        StackPane.setMargin(opacitySlider, new Insets(0, 30, 0, 0));
        stackPane.setPickOnBounds(true);
        
        // S'assurer que les sliders sont cliquables même avec opacity 0
        opacitySlider.setMouseTransparent(false);
        resizeHandle.setMouseTransparent(false);

        // Appliquer le style initial
        updatePaneStyle(overlayOpacity, stackPane);
        
        // Appliquer le scaling initial du texte
        applyTextScaleToNode(contentCard, textScale);

        // Configurer les listeners
        setupOpacitySliderListener();
    }

    /**
     * Crée une carte d'overlay avec le contenu des corps
     */
    private VBox createOverlayCard(SystemVisited system) {
        VBox card;
        if (bodyCardFactory != null && system != null) {
            VBox bodiesContent = bodyCardFactory.apply(system);
            if (bodiesContent != null) {
                javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane();
                // Rendre le VBox transparent aux événements de souris pour permettre le déplacement
                bodiesContent.setMouseTransparent(true);  // OK
                scrollPane.setMouseTransparent(true);

                // Créer un ScrollPane pour le contenu
                scrollPane.setContent(bodiesContent);
                scrollPane.setFitToWidth(true);
                scrollPane.setFitToHeight(true);
                scrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
                scrollPane.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
                scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
                scrollPane.setPannable(false);
                // Rendre le ScrollPane transparent aux événements sauf pour la scrollbar
                scrollPane.setPickOnBounds(false);
                
                card = new VBox();
                card.getChildren().add(scrollPane);
                VBox.setVgrow(scrollPane, Priority.ALWAYS);
            } else {
                card = createEmptyCard();
            }
        } else {
            card = createEmptyCard();
        }
        card.getStyleClass().add("mirror-overlay");
        return card;
    }

    /**
     * Crée une carte vide pour l'overlay
     */
    private VBox createEmptyCard() {
        VBox card = new VBox();
        Label emptyLabel = new Label("");
        emptyLabel.getStyleClass().add("exploration-overlay-title");
        card.getChildren().add(emptyLabel);
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
            VBox card = (VBox) stackPane.getChildren().get(0); // contentCard est à l'index 0
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
            // 🔒 SNAPSHOT pour éviter ConcurrentModificationException
            // La liste des enfants peut être modifiée pendant l'itération
            java.util.List<javafx.scene.Node> childrenSnapshot = new java.util.ArrayList<>(pane.getChildren());
            for (javafx.scene.Node child : childrenSnapshot) {
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
                if (resizeHandle != null) resizeHandle.setOpacity(1.0);
                if (opacitySlider != null) opacitySlider.setOpacity(0.8);
            } else {
                scene.setCursor(javafx.scene.Cursor.DEFAULT);
                if (resizeHandle != null) resizeHandle.setOpacity(0.8);
                if (opacitySlider != null) opacitySlider.setOpacity(0.8);
            }
        });

        // Masquer les contrôles quand la souris quitte la scène
        scene.setOnMouseExited(e -> {
            if (resizeHandle != null) resizeHandle.setOpacity(0.0);
            if (opacitySlider != null) opacitySlider.setOpacity(0.0);
        });

        // Afficher les contrôles quand la souris entre dans la scène
        scene.setOnMouseEntered(e -> {
            if (resizeHandle != null) resizeHandle.setOpacity(0.8);
            if (opacitySlider != null) opacitySlider.setOpacity(0.8);
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
