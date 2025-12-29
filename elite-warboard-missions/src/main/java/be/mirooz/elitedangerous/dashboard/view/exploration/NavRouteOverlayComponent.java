package be.mirooz.elitedangerous.dashboard.view.exploration;

import be.mirooz.elitedangerous.dashboard.model.registries.navigation.NavRouteRegistry;
import be.mirooz.elitedangerous.dashboard.service.PreferencesService;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.view.common.managers.PopupManager;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Composant pour gérer l'overlay de la route de navigation
 * <p>
 * Ce composant gère :
 * - La création et gestion de la fenêtre overlay
 * - Le redimensionnement et déplacement de l'overlay
 * - Le curseur de transparence du background
 * - La sauvegarde/restauration des préférences
 */
public class NavRouteOverlayComponent {

    public static final double MIN_OPACITY = 0.01;
    public static final int MIN_WIDTH_OVERLAY = 600;
    public static final int MIN_HEIGHT_OVERLAY = 90;
    private final PreferencesService preferencesService = PreferencesService.getInstance();
    private final PopupManager popupManager = PopupManager.getInstance();
    private final LocalizationService localizationService = LocalizationService.getInstance();
    private final NavRouteRegistry navRouteRegistry = NavRouteRegistry.getInstance();

    // Clés pour les préférences de l'overlay
    private static final String NAV_ROUTE_OVERLAY_WIDTH_KEY = "nav_route_overlay.width";
    private static final String NAV_ROUTE_OVERLAY_HEIGHT_KEY = "nav_route_overlay.height";
    private static final String NAV_ROUTE_OVERLAY_OPACITY_KEY = "nav_route_overlay.opacity";
    private static final String NAV_ROUTE_OVERLAY_X_KEY = "nav_route_overlay.x";
    private static final String NAV_ROUTE_OVERLAY_Y_KEY = "nav_route_overlay.y";
    private static final String NAV_ROUTE_OVERLAY_TEXT_SCALE_KEY = "nav_route_overlay.text_scale";

    private Stage overlayStage;
    private double overlayOpacity = 0.92;
    private Slider opacitySlider;
    private Label resizeHandle;
    private double textScale = 1.0;
    private StackPane stackPane;
    private VBox overlayContainer;
    private javafx.scene.layout.Pane routeSystemsPane;
    private javafx.scene.control.ScrollPane scrollPane;
    private NavRouteOverlayController overlayController;
    private Runnable onOverlayStateChanged;
    private NavRouteComponent navRouteComponent;
    private Rectangle backgroundRectangle;
    private boolean wasOpenBeforeOnFoot = false; // Pour savoir si l'overlay était ouvert avant d'être "on foot"

    public NavRouteOverlayComponent() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (overlayStage != null && overlayStage.isShowing()) {
                saveOverlayPreferences();
            }
        }));

        // Écouter les changements de langue pour mettre à jour l'overlay
        localizationService.addLanguageChangeListener(locale -> {
            if (overlayStage != null && overlayStage.isShowing()) {
                refreshOverlayContent();
            }
        });

        // Écouter les changements de route pour mettre à jour l'overlay
        navRouteRegistry.getCurrentRouteProperty().addListener((obs, oldRoute, newRoute) -> {
            if (overlayStage != null && overlayStage.isShowing()) {
                Platform.runLater(() -> {
                    updateOverlayContent();
                });
            }
        });
        
        // Écouter les changements de lastCopiedSystemName pour mettre à jour l'overlay
        // Cela se fait via l'écoute des changements de route qui se déclenche après updateRouteDisplay
    }

    /**
     * Affiche l'overlay pour la route de navigation
     */
    public void showOverlay() {
        showOverlay(false);
    }
    
    /**
     * Affiche l'overlay pour la route de navigation
     * @param withBordered true pour afficher le cadre orange, false sinon
     */
    public void showOverlay(boolean withBordered) {
        // Si la fenêtre est déjà ouverte, on la ferme
        if (overlayStage != null && overlayStage.isShowing()) {
            saveOverlayPreferences();
            overlayStage.close();
            overlayStage = null;
            if (onOverlayStateChanged != null) {
                onOverlayStateChanged.run();
            }
            return;
        }

        createOverlayStage(withBordered);
        if (onOverlayStateChanged != null) {
            onOverlayStateChanged.run();
        }
    }
    
    /**
     * Affiche l'overlay sans le cadre orange (utilisé après être revenu de "on foot")
     */
    public void showOverlayWithoutBordered() {
        showOverlay(false);
    }
    
    /**
     * Définit si l'overlay était ouvert avant d'être "on foot"
     */
    public void setWasOpenBeforeOnFoot(boolean wasOpen) {
        this.wasOpenBeforeOnFoot = wasOpen;
    }
    
    /**
     * Vérifie si l'overlay était ouvert avant d'être "on foot"
     */
    public boolean wasOpenBeforeOnFoot() {
        return wasOpenBeforeOnFoot;
    }

    /**
     * Définit le callback appelé quand l'état de l'overlay change
     */
    public void setOnOverlayStateChanged(Runnable callback) {
        this.onOverlayStateChanged = callback;
    }

    /**
     * Définit la référence au NavRouteComponent principal pour dessiner les boules
     */
    public void setNavRouteComponent(NavRouteComponent component) {
        this.navRouteComponent = component;
    }

    /**
     * Rafraîchit le contenu de l'overlay avec la nouvelle langue
     */
    private void refreshOverlayContent() {
        if (overlayStage != null && overlayStage.isShowing()) {
            updateOverlayContent();
        }
    }

    /**
     * Met à jour le contenu de l'overlay avec la route actuelle
     * Méthode publique pour permettre la mise à jour depuis NavRouteComponent
     */
    public void updateOverlayContent() {
        if (routeSystemsPane == null || navRouteComponent == null) {
            return;
        }
        
        // Obtenir la route actuelle depuis le registre
        be.mirooz.elitedangerous.dashboard.model.navigation.NavRoute route = navRouteRegistry.getCurrentRoute();
        if (route == null) {
            routeSystemsPane.getChildren().clear();
            return;
        }
        
        // Calculer la largeur disponible
        double availableWidth = 800; // Largeur par défaut
        if (overlayStage != null && overlayStage.getWidth() > 0) {
            availableWidth = overlayStage.getWidth() - 40; // -40 pour padding et marges
        } else if (overlayContainer != null && overlayContainer.getWidth() > 0) {
            availableWidth = overlayContainer.getWidth() - 20; // -20 pour padding
        } else if (routeSystemsPane != null && routeSystemsPane.getWidth() > 0) {
            availableWidth = routeSystemsPane.getWidth() - 20;
        }
        
        // Dessiner les boules dans le Pane en utilisant le NavRouteComponent principal
        navRouteComponent.drawRouteSystems(routeSystemsPane, route, availableWidth);
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
            if (onOverlayStateChanged != null) {
                onOverlayStateChanged.run();
            }
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
     * @param withBordered true pour afficher le cadre orange, false sinon
     */
    private void createOverlayStage(boolean withBordered) {
        // Création de la fenêtre overlay
        overlayStage = new Stage();
        overlayStage.initStyle(StageStyle.TRANSPARENT);
        overlayStage.setAlwaysOnTop(true);
        overlayStage.setTitle(localizationService.getString("nav.route.title"));
        overlayStage.setResizable(true);
        overlayStage.setMinWidth(MIN_WIDTH_OVERLAY);
        overlayStage.setMinHeight(MIN_HEIGHT_OVERLAY);

        // Restaurer les préférences sauvegardées
        restoreOverlayPreferences();

        // Créer le contenu de l'overlay
        createOverlayContent();

        // Configurer la scène
        Scene scene = new Scene(stackPane);
        scene.setFill(Color.TRANSPARENT);
        overlayStage.setScene(scene);
        // Ne pas appliquer l'opacité au Stage, elle est gérée par le rectangle de fond

        // Appliquer les styles CSS
        scene.getStylesheets().add(getClass().getResource("/css/elite-theme.css").toExternalForm());
        stackPane.getStyleClass().add("overlay-root");
        
        // Le cadre orange sera géré dans setupInteractions() pour éviter les conflits

        // Enregistrer le StackPane comme container pour les popups
        popupManager.registerContainer(overlayStage, stackPane);

        // Configurer les interactions (déplacement, redimensionnement)
        setupInteractions();
        
        // Écouter les changements de taille pour mettre à jour le contenu
        overlayStage.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            if (newWidth.doubleValue() > 0) {
                updateOverlayContent();
            }
        });

        // Afficher l'overlay
        overlayStage.show();
        overlayStage.setOnCloseRequest(event -> {
            saveOverlayPreferences();
            popupManager.unregisterContainer(overlayStage);
            overlayStage = null;
            if (onOverlayStateChanged != null) {
                onOverlayStateChanged.run();
            }
        });
    }

    /**
     * Rend un nœud déplaçable
     */
    private void makeNodeDraggable(javafx.scene.Node node, Stage stage) {
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

            // Détecter la zone de resize (25x25)
            if (mouseX >= sceneWidth - 25 && mouseY >= sceneHeight - 25) {
                isResizing[0] = true;
                resizeOffset[0] = e.getScreenX();
                resizeOffset[1] = e.getScreenY();
                return;
            }

            // Sinon → déplacement
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
                // Resize
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

            // Déplacement normal
            stage.setX(e.getScreenX() - offset[0]);
            stage.setY(e.getScreenY() - offset[1]);
        });

        node.setOnMouseReleased(e -> isResizing[0] = false);
    }

    /**
     * Crée le contenu de l'overlay
     */
    private void createOverlayContent() {
        stackPane = new StackPane();
        // S'assurer que le StackPane est transparent pour voir le rectangle de fond
        stackPane.setStyle("-fx-background-color: transparent;");

        try {
            // Charger le FXML de l'overlay (uniquement les boules)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/exploration/nav-route-overlay.fxml"));
            // Charger le ResourceBundle pour la localisation
            ResourceBundle bundle = ResourceBundle.getBundle("messages", localizationService.getCurrentLocale());
            loader.setResources(bundle);
            overlayContainer = loader.load();
            overlayController = loader.getController();
            
            // S'assurer que le VBox est transparent pour voir le rectangle de fond
            overlayContainer.setStyle("-fx-background-color: transparent;");
            
            // Récupérer le Pane pour les systèmes depuis le ScrollPane
            routeSystemsPane = overlayController.getRouteSystemsPane();
            
            // Récupérer le ScrollPane depuis le VBox
            javafx.scene.control.ScrollPane scrollPane = null;
            if (overlayContainer.getChildren().size() > 0 && overlayContainer.getChildren().get(0) instanceof javafx.scene.control.ScrollPane) {
                scrollPane = (javafx.scene.control.ScrollPane) overlayContainer.getChildren().get(0);
            }
            
            // Faire en sorte que le Pane s'adapte à la largeur de l'overlay
            if (routeSystemsPane != null && overlayContainer != null) {
                overlayContainer.widthProperty().addListener((obs, oldWidth, newWidth) -> {
                    if (newWidth.doubleValue() > 0) {
                        Platform.runLater(() -> updateOverlayContent());
                    }
                });
            }

            // Rendre le contenu interne transparent aux événements de souris pour permettre le déplacement
            // SAUF le routeSystemsPane qui doit rester interactif pour les clics sur les cercles
            // Le VBox principal (overlayContainer) reste interactif pour détecter la souris
            // Note: routeSystemsPane n'est PAS rendu transparent pour permettre les clics sur les cercles
            if (scrollPane != null) {
                // Le ScrollPane doit permettre les événements de souris pour son contenu (routeSystemsPane)
                // mais ne doit pas intercepter les événements pour le déplacement
                // On le laisse non-transparent pour permettre les clics sur les cercles
                scrollPane.setMouseTransparent(false);
                scrollPane.setPickOnBounds(true);
            }
            
            // S'assurer que le VBox principal peut recevoir les événements de souris
            overlayContainer.setPickOnBounds(true);
            
            // S'assurer que le StackPane peut recevoir les événements de souris pour le déplacement
            stackPane.setPickOnBounds(true);
            
            // Créer le rectangle de fond avec opacité
            backgroundRectangle = new Rectangle();
            backgroundRectangle.setFill(Color.BLACK);
            backgroundRectangle.setMouseTransparent(true);
            // Lier la taille du rectangle à celle du VBox
            backgroundRectangle.widthProperty().bind(overlayContainer.widthProperty());
            backgroundRectangle.heightProperty().bind(overlayContainer.heightProperty());
            // S'assurer que le rectangle reste visible même à l'opacité minimale
            backgroundRectangle.setOpacity(overlayOpacity);
            
            // Créer le slider d'opacité et le handle de redimensionnement
            opacitySlider = createOpacitySlider();
            resizeHandle = createResizeHandle();
            
            // Ajouter le rectangle de fond en premier, puis le contenu et les contrôles
            stackPane.getChildren().addAll(backgroundRectangle, overlayContainer, resizeHandle, opacitySlider);
            
            // Rendre le StackPane déplaçable (après avoir ajouté les enfants)
            makeNodeDraggable(stackPane, overlayStage);
            
            // Positionner les contrôles
            StackPane.setAlignment(resizeHandle, Pos.BOTTOM_RIGHT);
            StackPane.setAlignment(opacitySlider, Pos.BOTTOM_RIGHT);
            StackPane.setMargin(opacitySlider, new Insets(0, 30, 0, 0));
            StackPane.setMargin(resizeHandle, new Insets(0, 0, 0, 0));
            
            // Rendre les contrôles interactifs
            resizeHandle.setMouseTransparent(false);
            opacitySlider.setMouseTransparent(false);
            
            // Configurer le listener du slider d'opacité
            setupOpacitySliderListener();
            
            // Appliquer l'opacité restaurée au rectangle de fond
            // S'assurer que l'opacité est bien appliquée après la création du rectangle
            updatePaneStyle(overlayOpacity);
            
            // Synchroniser la valeur du slider avec l'opacité restaurée
            // Faire cela après setupOpacitySliderListener pour éviter les conflits
            if (opacitySlider != null) {
                opacitySlider.setValue(overlayOpacity);
            }
            
            // Dessiner les boules initiales
            updateOverlayContent();
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du chargement de l'overlay de route: " + e.getMessage());
            e.printStackTrace();
            
            // Créer un contenu de secours
            Label errorLabel = new Label("Erreur lors du chargement de la route de navigation");
            errorLabel.setStyle("-fx-text-fill: red;");
            stackPane.getChildren().add(errorLabel);
        }
    }

    /**
     * Applique le scaling du texte à un nœud et ses enfants
     */
    private void applyTextScaleToNode(javafx.scene.Node node, double scale) {
        if (node instanceof javafx.scene.control.Label) {
            javafx.scene.control.Label label = (javafx.scene.control.Label) node;
            double originalSize = label.getFont().getSize();
            label.setStyle("-fx-font-size: " + (originalSize * scale) + "px;");
        } else if (node instanceof Parent) {
            Parent parent = (Parent) node;
            for (javafx.scene.Node child : parent.getChildrenUnmodifiable()) {
                applyTextScaleToNode(child, scale);
            }
        }
    }

    /**
     * Configure les interactions (curseur et visibilité des contrôles)
     */
    private void setupInteractions() {
        Scene scene = overlayStage.getScene();

        // Gestion du curseur et de la visibilité des contrôles sur la scène
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

        // Masquer les contrôles et retirer le cadre orange quand la souris quitte la scène
        scene.setOnMouseExited(e -> {
            if (resizeHandle != null) resizeHandle.setOpacity(0.0);
            if (opacitySlider != null) opacitySlider.setOpacity(0.0);
            // Retirer le cadre orange quand la souris sort
            stackPane.getStyleClass().remove("overlay-root-bordered");
        });

        // Afficher les contrôles et ajouter le cadre orange quand la souris entre dans la scène
        scene.setOnMouseEntered(e -> {
            if (resizeHandle != null) resizeHandle.setOpacity(0.8);
            if (opacitySlider != null) opacitySlider.setOpacity(0.8);
            // Ajouter le cadre orange quand la souris entre
            if (!stackPane.getStyleClass().contains("overlay-root-bordered")) {
                stackPane.getStyleClass().add("overlay-root-bordered");
            }
        });
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
        Slider slider = new Slider(MIN_OPACITY, 1.0, overlayOpacity);
        slider.setOrientation(javafx.geometry.Orientation.VERTICAL);
        slider.setPrefWidth(12);
        slider.setPrefHeight(120);
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
     * Configure le listener du curseur de transparence
     */
    private void setupOpacitySliderListener() {
        opacitySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            // Ignorer les changements initiaux (lors de la synchronisation)
            if (oldVal == null) {
                return;
            }
            double opacity = Math.max(newVal.doubleValue(), MIN_OPACITY);
            updatePaneStyle(opacity);
            overlayOpacity = opacity;
        });
    }
    
    /**
     * Met à jour l'opacité du rectangle de fond
     */
    private void updatePaneStyle(double opacity) {
        if (backgroundRectangle == null) {
            return;
        }
        double bgOpacity = Math.max(MIN_OPACITY, opacity);
        overlayOpacity = bgOpacity;
        backgroundRectangle.setOpacity(bgOpacity);
    }

    /**
     * Restaure les préférences de l'overlay
     */
    private void restoreOverlayPreferences() {
        String savedWidthStr = preferencesService.getPreference(NAV_ROUTE_OVERLAY_WIDTH_KEY, "800");
        String savedHeightStr = preferencesService.getPreference(NAV_ROUTE_OVERLAY_HEIGHT_KEY, "150");
        String savedOpacityStr = preferencesService.getPreference(NAV_ROUTE_OVERLAY_OPACITY_KEY, "0.92");
        String savedXStr = preferencesService.getPreference(NAV_ROUTE_OVERLAY_X_KEY, "100");
        String savedYStr = preferencesService.getPreference(NAV_ROUTE_OVERLAY_Y_KEY, "100");
        String savedTextScaleStr = preferencesService.getPreference(NAV_ROUTE_OVERLAY_TEXT_SCALE_KEY, "1.0");

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
            preferencesService.setPreference(NAV_ROUTE_OVERLAY_WIDTH_KEY, String.valueOf((int) overlayStage.getWidth()));
            preferencesService.setPreference(NAV_ROUTE_OVERLAY_HEIGHT_KEY, String.valueOf((int) overlayStage.getHeight()));
            preferencesService.setPreference(NAV_ROUTE_OVERLAY_OPACITY_KEY, String.valueOf(overlayOpacity));
            preferencesService.setPreference(NAV_ROUTE_OVERLAY_X_KEY, String.valueOf((int) overlayStage.getX()));
            preferencesService.setPreference(NAV_ROUTE_OVERLAY_Y_KEY, String.valueOf((int) overlayStage.getY()));
            preferencesService.setPreference(NAV_ROUTE_OVERLAY_TEXT_SCALE_KEY, String.valueOf(textScale));
        }
    }
}

