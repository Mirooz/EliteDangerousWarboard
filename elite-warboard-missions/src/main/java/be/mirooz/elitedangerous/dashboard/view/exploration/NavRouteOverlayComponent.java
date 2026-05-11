package be.mirooz.elitedangerous.dashboard.view.exploration;

import be.mirooz.elitedangerous.dashboard.service.NavRouteService;
import be.mirooz.elitedangerous.dashboard.service.WindowToggleService;
import be.mirooz.elitedangerous.dashboard.service.PreferencesService;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.view.common.managers.PopupManager;
import be.mirooz.elitedangerous.dashboard.view.common.overlay.OverlayAlwaysOnTopSupport;
import be.mirooz.elitedangerous.dashboard.view.common.overlay.OverlayLockChrome;
import be.mirooz.elitedangerous.dashboard.view.common.overlay.OverlayPassthroughSupport;
import be.mirooz.elitedangerous.dashboard.view.common.overlay.OverlayScreenGeometryHelper;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.Node;
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
    public static final int MIN_HEIGHT_OVERLAY = 100;
    private final PreferencesService preferencesService = PreferencesService.getInstance();
    private final PopupManager popupManager = PopupManager.getInstance();
    private final LocalizationService localizationService = LocalizationService.getInstance();
    private final NavRouteService navRouteService = NavRouteService.getInstance();

    // Clés pour les préférences de l'overlay
    private static final String NAV_ROUTE_OVERLAY_WIDTH_KEY = "nav_route_overlay.width";
    private static final String NAV_ROUTE_OVERLAY_HEIGHT_KEY = "nav_route_overlay.height";
    private static final String NAV_ROUTE_OVERLAY_OPACITY_KEY = "nav_route_overlay.opacity";
    private static final String NAV_ROUTE_OVERLAY_X_KEY = "nav_route_overlay.x";
    private static final String NAV_ROUTE_OVERLAY_Y_KEY = "nav_route_overlay.y";
    private static final String NAV_ROUTE_OVERLAY_TEXT_SCALE_KEY = "nav_route_overlay.text_scale";
    private static final String NAV_ROUTE_OVERLAY_SCREEN_INDEX_KEY = "nav_route_overlay.screen.index";

    /** Aligné sur {@code .overlay-root-bordered} dans elite-theme.css ({@code -fx-border-width: 2px}). */
    private static final double OVERLAY_BORDER_INSET_PX = 2.0;

    private Stage overlayStage;
    private double overlayOpacity = 0.92;
    private Slider opacitySlider;
    private Label resizeHandle;
    private double textScale = 1.0;
    private StackPane stackPane;
    private VBox overlayContainer;
    private javafx.scene.layout.Pane routeSystemsPane;
    private NavRouteOverlayController overlayController;
    private Runnable onOverlayStateChanged;
    private NavRouteComponent navRouteComponent;
    private Rectangle backgroundRectangle;
    private final OverlayPassthroughSupport passthrough = new OverlayPassthroughSupport();
    private final OverlayAlwaysOnTopSupport alwaysOnTop = new OverlayAlwaysOnTopSupport();
    private Runnable onOverlayClosed;

    public NavRouteOverlayComponent() {
        preferencesService.registerOverlayGeometrySaver(this::persistOverlayGeometryForShutdown);

        // Écouter les changements de langue pour mettre à jour l'overlay
        localizationService.addLanguageChangeListener(locale -> {
            if (overlayStage != null && overlayStage.isShowing()) {
                refreshOverlayContent();
            }
        });

        // Écouter les changements de route pour mettre à jour l'overlay
        navRouteService.getCurrentRouteProperty().addListener((obs, oldRoute, newRoute) -> {
            if (overlayStage != null && overlayStage.isShowing()) {
                Platform.runLater(() -> {
                    updateOverlayContent();
                });
            }
        });
        
        // Écouter les changements de RemainingJumpsInRoute : label texte + chiffre au-dessus du segment (redessin route)
        navRouteService.getRemainingJumpsInRouteProperty().addListener((obs, oldValue, newValue) -> {
            if (overlayStage != null && overlayStage.isShowing()) {
                Platform.runLater(() -> {
                    updateRemainingJumpsLabel(newValue.intValue());
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
            closeOverlay();
            return;
        }

        createOverlayStage(withBordered);
        if (onOverlayStateChanged != null) {
            onOverlayStateChanged.run();
        }
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

    public void setOnOverlayClosed(Runnable onOverlayClosed) {
        this.onOverlayClosed = onOverlayClosed;
    }

    public void setClickThroughLocked(boolean locked) {
        passthrough.setClickThroughLocked(locked, overlayStage, stackPane);
        refreshLockChrome();
    }

    private void refreshLockChrome() {
        OverlayLockChrome.apply(passthrough.isClickThroughLocked(), stackPane, resizeHandle, opacitySlider);
    }

    /**
     * Rafraîchit le contenu de l'overlay avec la nouvelle langue
     */
    private void refreshOverlayContent() {
        if (overlayStage != null && overlayStage.isShowing()) {
            updateOverlayContent();
            // Mettre à jour le label des remaining jumps avec la nouvelle langue
            updateRemainingJumpsLabel(navRouteService.getRemainingJumpsInRoute());
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
        be.mirooz.elitedangerous.dashboard.model.navigation.NavRoute route = navRouteService.getCurrentRoute();
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
        navRouteComponent.drawRouteSystems(routeSystemsPane, route, availableWidth, false);
    }

    /**
     * Ferme l'overlay s'il est ouvert
     */
    public void closeOverlay() {
        if (overlayStage == null || !overlayStage.isShowing()) {
            return;
        }
        saveOverlayPreferences();
        if (backgroundRectangle != null) {
            backgroundRectangle.widthProperty().unbind();
            backgroundRectangle.heightProperty().unbind();
        }
        if (overlayContainer != null) {
            overlayContainer.prefWidthProperty().unbind();
            overlayContainer.prefHeightProperty().unbind();
        }
        popupManager.unregisterContainer(overlayStage);
        Stage stage = overlayStage;
        StackPane pane = stackPane;
        overlayStage = null;
        stackPane = null;
        passthrough.disposeForClose(stage, pane);
        alwaysOnTop.dispose();
        if (onOverlayClosed != null) {
            onOverlayClosed.run();
        }
        if (onOverlayStateChanged != null) {
            onOverlayStateChanged.run();
        }
        stage.close();
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
        WindowToggleService.getInstance().bindOverlayOwner(overlayStage);
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
        overlayStage.setOpacity(1.0);

        // Appliquer les styles CSS
        scene.getStylesheets().add(getClass().getResource("/css/elite-theme.css").toExternalForm());
        popupManager.registerContainer(overlayStage, stackPane);
        stackPane.getStyleClass().add("overlay-root");

        // Configurer les interactions (déplacement, redimensionnement)
        setupInteractions();
        refreshLockChrome();

        // Écouter les changements de taille pour mettre à jour le contenu
        overlayStage.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            if (newWidth.doubleValue() > 0) {
                updateOverlayContent();
            }
        });

        // Afficher l'overlay
        overlayStage.show();
        alwaysOnTop.install(overlayStage);
        overlayStage.setOnCloseRequest(event -> closeOverlay());
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

            if (scrollPane != null) {
                scrollPane.setMouseTransparent(false);
                scrollPane.setPickOnBounds(true);
            }
            overlayContainer.setPickOnBounds(true);
            stackPane.setPickOnBounds(true);
            
            // Créer le rectangle de fond avec opacité : lier au StackPane (racine scène), pas au VBox.
            // Sinon le VBox a la hauteur « pref » du contenu et le Rectangle centré laisse des bandes vides
            // (bordure orange overlay-root vs fond noir).
            backgroundRectangle = new Rectangle();
            backgroundRectangle.setFill(Color.BLACK);
            backgroundRectangle.setMouseTransparent(true);
            // Réserver la zone de la bordure orange (sinon le Rectangle la recouvre en bas / à droite).
            double innerInset = 2.0 * OVERLAY_BORDER_INSET_PX;
            backgroundRectangle.widthProperty().bind(stackPane.widthProperty().subtract(innerInset));
            backgroundRectangle.heightProperty().bind(stackPane.heightProperty().subtract(innerInset));
            backgroundRectangle.setOpacity(overlayOpacity);
            StackPane.setAlignment(backgroundRectangle, Pos.TOP_LEFT);
            StackPane.setMargin(backgroundRectangle, new Insets(OVERLAY_BORDER_INSET_PX));

            // Le contenu FXML remplit la zone intérieure (même inset que le fond).
            overlayContainer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            overlayContainer.prefWidthProperty().bind(stackPane.widthProperty().subtract(innerInset));
            overlayContainer.prefHeightProperty().bind(stackPane.heightProperty().subtract(innerInset));
            StackPane.setAlignment(overlayContainer, Pos.TOP_LEFT);
            StackPane.setMargin(overlayContainer, new Insets(OVERLAY_BORDER_INSET_PX));

            // Créer le slider d'opacité et le handle de redimensionnement
            opacitySlider = createOpacitySlider();
            resizeHandle = createResizeHandle();
            
            // Ajouter le rectangle de fond en premier, puis le contenu et les contrôles
            stackPane.getChildren().addAll(backgroundRectangle, overlayContainer, resizeHandle, opacitySlider);

            // Positionner les contrôles
            StackPane.setAlignment(resizeHandle, Pos.BOTTOM_RIGHT);
            StackPane.setAlignment(opacitySlider, Pos.BOTTOM_RIGHT);
            StackPane.setMargin(opacitySlider, new Insets(0, 30, OVERLAY_BORDER_INSET_PX, 0));
            StackPane.setMargin(resizeHandle, new Insets(0, OVERLAY_BORDER_INSET_PX, OVERLAY_BORDER_INSET_PX, 0));
            
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
            
            // Initialiser le label des remaining jumps avec la valeur actuelle
            updateRemainingJumpsLabel(navRouteService.getRemainingJumpsInRoute());
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
     * Filtres en phase capture (comme {@link be.mirooz.elitedangerous.dashboard.view.fleetcarrier.FleetCarrierOverlayComponent}) :
     * reçus avant le {@code ScrollPane}, pour déplacer la fenêtre en cliquant au milieu du panneau route.
     */
    private void setupInteractions() {
        final double[] offset = new double[2];
        final double[] resizeOffset = new double[2];
        final boolean[] isResizing = {false};
        final boolean[] armWindowMove = {false};

        Scene scene = overlayStage.getScene();

        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (passthrough.isClickThroughLocked()) {
                return;
            }
            if (e.getButton() != MouseButton.PRIMARY) {
                return;
            }
            armWindowMove[0] = false;
            Node pick = pickNode(e);
            if (isInsideSliderOrScrollBar(pick)) {
                return;
            }
            double sceneWidth = scene.getWidth();
            double sceneHeight = scene.getHeight();
            double mx = e.getSceneX();
            double my = e.getSceneY();
            if (mx >= sceneWidth - 25 && my >= sceneHeight - 25) {
                isResizing[0] = true;
                resizeOffset[0] = e.getScreenX();
                resizeOffset[1] = e.getScreenY();
            } else {
                isResizing[0] = false;
                armWindowMove[0] = true;
                offset[0] = e.getScreenX() - overlayStage.getX();
                offset[1] = e.getScreenY() - overlayStage.getY();
            }
        });
        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            if (passthrough.isClickThroughLocked()) {
                return;
            }
            if (e.getButton() != MouseButton.PRIMARY) {
                return;
            }
            Node pick = pickNode(e);
            if (isInsideSliderOrScrollBar(pick)) {
                return;
            }
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
                e.consume();
            } else if (armWindowMove[0]) {
                overlayStage.setX(e.getScreenX() - offset[0]);
                overlayStage.setY(e.getScreenY() - offset[1]);
                e.consume();
            }
        });
        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            if (passthrough.isClickThroughLocked()) {
                return;
            }
            isResizing[0] = false;
            armWindowMove[0] = false;
        });
        scene.addEventFilter(MouseEvent.MOUSE_MOVED, e -> {
            if (passthrough.isClickThroughLocked()) {
                return;
            }
            double sceneWidth = scene.getWidth();
            double sceneHeight = scene.getHeight();
            if (e.getSceneX() >= sceneWidth - 25 && e.getSceneY() >= sceneHeight - 25) {
                scene.setCursor(javafx.scene.Cursor.SE_RESIZE);
            } else {
                scene.setCursor(javafx.scene.Cursor.DEFAULT);
            }
        });
    }

    private static Node pickNode(MouseEvent e) {
        if (e.getPickResult() != null && e.getPickResult().getIntersectedNode() != null) {
            return e.getPickResult().getIntersectedNode();
        }
        if (e.getTarget() instanceof Node n) {
            return n;
        }
        return null;
    }

    private static boolean isInsideSliderOrScrollBar(Node node) {
        for (Node n = node; n != null; n = n.getParent()) {
            if (n instanceof ScrollBar || n instanceof Slider) {
                return true;
            }
        }
        return false;
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
        Screen targetScreen = OverlayScreenGeometryHelper.resolveScreenForRestore(
                preferencesService, NAV_ROUTE_OVERLAY_SCREEN_INDEX_KEY, savedX, savedY, width, height);
        Rectangle2D screenBounds = targetScreen.getVisualBounds();
        OverlayScreenGeometryHelper.applyClampedPosition(overlayStage, screenBounds, savedX, savedY, width, height);
    }

    /**
     * Met à jour le label affichant le nombre de sauts restants
     */
    private void updateRemainingJumpsLabel(int remainingJumps) {
        if (overlayController != null) {
            Label remainingJumpsLabel = overlayController.getRemainingJumpsLabel();
            if (remainingJumpsLabel != null) {
                if (remainingJumps > 0) {
                    remainingJumpsLabel.setText(localizationService.getString("nav.route.remaining_jumps", remainingJumps));
                    remainingJumpsLabel.setVisible(true);
                    remainingJumpsLabel.setManaged(true);
                } else {
                    remainingJumpsLabel.setText("");
                    remainingJumpsLabel.setVisible(false);
                    remainingJumpsLabel.setManaged(false);
                }
            }
        }
    }

    public void persistOverlayGeometryForShutdown() {
        if (overlayStage == null) {
            return;
        }
        writeOverlayGeometryPrefs();
    }

    private void saveOverlayPreferences() {
        if (overlayStage == null) {
            return;
        }
        writeOverlayGeometryPrefs();
    }

    private void writeOverlayGeometryPrefs() {
        preferencesService.setPreference(NAV_ROUTE_OVERLAY_WIDTH_KEY, String.valueOf((int) overlayStage.getWidth()));
        preferencesService.setPreference(NAV_ROUTE_OVERLAY_HEIGHT_KEY, String.valueOf((int) overlayStage.getHeight()));
        preferencesService.setPreference(NAV_ROUTE_OVERLAY_OPACITY_KEY, String.valueOf(overlayOpacity));
        preferencesService.setPreference(NAV_ROUTE_OVERLAY_X_KEY, String.valueOf((int) overlayStage.getX()));
        preferencesService.setPreference(NAV_ROUTE_OVERLAY_Y_KEY, String.valueOf((int) overlayStage.getY()));
        preferencesService.setPreference(NAV_ROUTE_OVERLAY_TEXT_SCALE_KEY, String.valueOf(textScale));
        OverlayScreenGeometryHelper.persistScreenIndex(preferencesService, NAV_ROUTE_OVERLAY_SCREEN_INDEX_KEY, overlayStage);
    }
}

