package be.mirooz.elitedangerous.dashboard.view.exploration;

import be.mirooz.elitedangerous.biologic.BioSpecies;
import be.mirooz.elitedangerous.biologic.BodyType;
import be.mirooz.elitedangerous.biologic.StarType;
import be.mirooz.elitedangerous.dashboard.view.common.IRefreshable;
import be.mirooz.elitedangerous.dashboard.view.common.TooltipComponent;
import be.mirooz.elitedangerous.dashboard.model.registries.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.service.listeners.ExplorationRefreshNotificationService;
import be.mirooz.elitedangerous.dashboard.model.colonisation.ColonisationArchitectMapCaptionLine;
import be.mirooz.elitedangerous.dashboard.model.colonisation.ConstructionStatus;
import be.mirooz.elitedangerous.dashboard.model.exploration.ACelesteBody;
import be.mirooz.elitedangerous.dashboard.model.exploration.PlaneteDetail;
import be.mirooz.elitedangerous.dashboard.model.exploration.Scan;
import be.mirooz.elitedangerous.dashboard.model.exploration.SpeciesProbability;
import be.mirooz.elitedangerous.dashboard.model.exploration.StarDetail;
import be.mirooz.elitedangerous.dashboard.model.exploration.SystemVisited;
import be.mirooz.elitedangerous.dashboard.service.ExplorationService;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.service.PreferencesService;
import be.mirooz.elitedangerous.dashboard.service.SpanshSystemVisitedService;
import be.mirooz.elitedangerous.dashboard.view.common.context.DashboardContext;
import com.fasterxml.jackson.databind.JsonNode;
import be.mirooz.elitedangerous.dashboard.view.common.managers.CopyClipboardManager;
import be.mirooz.elitedangerous.dashboard.view.common.managers.PopupManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Circle;
import javafx.scene.transform.Scale;
import javafx.util.Duration;
import javafx.stage.Window;

import java.net.URL;
import java.util.*;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import java.util.stream.Collectors;

/**
 * Composant pour la vue visuelle du système (orrery)
 */
public class SystemVisualViewComponent implements Initializable, IRefreshable,
        ExplorationRefreshNotificationService.BodyFilterListener {

    @FXML
    private VBox bodiesListPanel;
    @FXML
    private VBox bodiesListContainer;
    @FXML
    private Label systemTitleLabel;
    @FXML
    private Label systemBodiesLabel;

    private RadarComponent radarComponent;
    private RadarComponent overlayRadarComponent; // Cache pour le RadarComponent de l'overlay
    @FXML
    private CheckBox showOnlyHighValueBodiesCheckBox;
    @FXML
    private ScrollPane bodiesScrollPane;
    @FXML
    private Group bodiesGroup;
    @FXML
    private Pane bodiesPane;
    @FXML
    private VBox jsonDetailPanel;
    @FXML
    private Label jsonBodyNameLabel;
    @FXML
    private TreeView<JsonTreeItem> jsonTreeView;
    @FXML
    private Button bodiesOverlayButton;
    @FXML
    private Label spacingTitleLabel;
    @FXML
    private Label spacingHorizontalLabel;
    @FXML
    private Label spacingVerticalLabel;
    @FXML
    private Slider spacingHorizontalSlider;
    @FXML
    private Slider spacingVerticalSlider;
    @FXML
    private Button resetViewParametersButton;

    private ExplorationBodiesOverlayComponent bodiesOverlayComponent;
    private Image gasImage;
    private Image starImage; // Image par défaut pour les étoiles (fallback)
    // Images par type de planète
    private final Map<be.mirooz.elitedangerous.biologic.BodyType, Image> planetImages = new HashMap<>();
    private Image ringImageTop;
    private Image ringImageBack;
    // Images par type d'étoile
    private final Map<StarType, Image> starImages = new HashMap<>();
    private Image exobioImage;
    private Image mappedImage;
    /**
     * Icônes chantier colonisation (vue architecte) : surface {@code settlement.png}, orbital {@code orbital.png}.
     */
    private Image colonisationStationPortOrbitalImage;
    private Image colonisationStationPortSurfaceImage;
    /**
     * Icône unique sur la carte dès qu’il y a au moins une colonie (orbital et/ou surface).
     */
    private Image colonisationSettlementImage;
    /**
     * Évite une boucle infinie lors du re-display après hydratation Spansh.
     */
    private boolean spanshOnlineHydrationSuppressed;

    /**
     * Chargement Spansh : placé sur le {@link StackPane} qui enveloppe le {@link #bodiesScrollPane},
     * <strong>en dehors</strong> de {@link #bodiesGroup} pour ne pas subir zoom / pan.
     */
    private StackPane spanshLoadingLayer;

    /**
     * Si {@code true}, attend la réponse Spansh avant d'afficher l'orrery et la liste des corps (chargement).
     */
    public static final boolean SEARCH_SPANSH_EXPLORATION = true;

    /** Appelé sur le thread JavaFX juste après {@link #mergeOnlineDataFromSpansh} (liste d’historique, etc.). */
    private Runnable afterSpanshBodiesMerged;

    private SystemVisited currentSystem;
    private Map<Integer, BodyPosition> bodyPositions = new HashMap<>();
    private Scale zoomTransform;
    private double currentZoom = 1.0;
    private static final double MIN_ZOOM = 0.1;
    private static final double MAX_ZOOM = 5.0;
    private static final double ZOOM_FACTOR = 0.1;
    private boolean mapPanning;
    private double lastPanSceneX;
    private double lastPanSceneY;
    private double panTranslateX;
    private double panTranslateY;

    /**
     * Facteur sur les espacements de la vue système (orrery) : chaîne des planètes, lunes, étoiles, marges.
     * {@code 0.85} ≈ réduction de 15 % des distances (horizontal et vertical).
     */
    private static final double SYSTEM_VIEW_ORRERY_BASE_SPACING_SCALE = 0.85;
    private static final double USER_SPACING_MIN = 0.4;
    private static final double USER_SPACING_MAX = 1.875;
    private static final double USER_SPACING_RANGE = USER_SPACING_MAX - USER_SPACING_MIN;
    private double userHorizontalSpacingScale = 1.0;
    private double userVerticalSpacingScale = 1.0;

    private int orrerySpacingX(int basePixels) {
        return Math.max(1, (int) Math.round(basePixels * SYSTEM_VIEW_ORRERY_BASE_SPACING_SCALE * userHorizontalSpacingScale));
    }

    private int orrerySpacingY(int basePixels) {
        return Math.max(1, (int) Math.round(basePixels * SYSTEM_VIEW_ORRERY_BASE_SPACING_SCALE * userVerticalSpacingScale));
    }

    public void setOrrerySpacingUserScale(double horizontalScale, double verticalScale) {
        userHorizontalSpacingScale = Math.max(USER_SPACING_MIN, Math.min(USER_SPACING_MAX, horizontalScale));
        userVerticalSpacingScale = Math.max(USER_SPACING_MIN, Math.min(USER_SPACING_MAX, verticalScale));
        if (currentSystem != null) {
            displaySystem(currentSystem);
        }
    }

    private ACelesteBody currentJsonBody; // Corps actuellement affiché dans le panneau JSON
    private Integer filteredBodyID; // BodyID à filtrer (null = pas de filtre)
    private final LocalizationService localizationService = LocalizationService.getInstance();
    private final PreferencesService preferencesService = PreferencesService.getInstance();
    private final CopyClipboardManager copyClipboardManager = CopyClipboardManager.getInstance();
    private final PopupManager popupManager = PopupManager.getInstance();
    /** Vue exploration principale uniquement (évite d’écraser l’instance en ouvrant d’autres FXML colonisation / dialogues). */
    private static volatile SystemVisualViewComponent primaryInstance;

    /**
     * Enregistre la vue système du module exploration pour {@link #getInstance()} (zoom fenêtre, etc.).
     */
    public static void setPrimaryInstance(SystemVisualViewComponent view) {
        primaryInstance = view;
    }
    private static final String PREF_SYSTEM_VIEW_SPACING_X = "exploration.systemView.spacing.horizontal";
    private static final String PREF_SYSTEM_VIEW_SPACING_Y = "exploration.systemView.spacing.vertical";
    private static final double DEFAULT_SPACING_X = USER_SPACING_MIN + (USER_SPACING_RANGE / 2.0);
    private static final double DEFAULT_SPACING_Y = USER_SPACING_MIN + (USER_SPACING_RANGE / 2.0);

    /**
     * Si non null, clic sur une puce station (carte architecte) → détail (marketId).
     */
    private LongConsumer colonisationStationClickHandler;
    /**
     * Étiquettes chantier colonisation par {@code bodyID} (vue architecte).
     */
    private Map<Integer, List<ColonisationArchitectMapCaptionLine>> colonisationBodyCaptionLines = Map.of();

    /**
     * Vues colonisation / recherche colonisable : pas de pastilles exobio ni « high value / mappable » sur la carte
     * ni dans les cartes liste (ni filtre liste sur ces corps).
     */
    private boolean suppressExplorationValueIndicators;

    private boolean isHighValueBodyListFilterActive() {
        if (suppressExplorationValueIndicators) {
            return false;
        }
        return showOnlyHighValueBodiesCheckBox != null && showOnlyHighValueBodiesCheckBox.isSelected();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // S'abonner au service de notification
        ExplorationRefreshNotificationService notificationService = ExplorationRefreshNotificationService.getInstance();
        notificationService.addBodyFilterListener(this);
        // S'abonner aux changements d'état "à pied" pour gérer la transition overlay/popup
        notificationService.addOnFootStateListener(this::handleOnFootStateChanged);
        // Initialiser le composant overlay
        bodiesOverlayComponent = new ExplorationBodiesOverlayComponent();
        bodiesOverlayComponent.setBodyCardFactory(this::createBodiesListForOverlay);
        initSystemTitleClipboardAction();

        // Mettre à jour le texte du bouton overlay
        Platform.runLater(() -> {
            updateBodiesOverlayButtonText();
            updateTranslations();
        });

        // Écouter les changements de langue
        localizationService.addLanguageChangeListener(locale -> {
            Platform.runLater(() -> {
                updateTranslations();
                updateBodiesOverlayButtonText();
            });
        });

        // Charger les images
        try {
            gasImage = new Image(getClass().getResourceAsStream("/images/exploration/gas.png"));
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de l'image gas.png: " + e.getMessage());
        }
        try {
            starImage = new Image(getClass().getResourceAsStream("/images/exploration/star.png"));
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de l'image star.png: " + e.getMessage());
        }
        try {
            exobioImage = new Image(getClass().getResourceAsStream("/images/exploration/exobio.png"));
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de l'image exobio.png: " + e.getMessage());
        }
        try {
            mappedImage = new Image(getClass().getResourceAsStream("/images/exploration/mapped.png"));
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de l'image mapped.png: " + e.getMessage());
        }
        try {
            var settlement = getClass().getResourceAsStream("/images/colonisation/settlement.png");
            if (settlement != null) {
                Image sImg = new Image(settlement);
                colonisationSettlementImage = sImg;
                colonisationStationPortSurfaceImage = sImg;
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement images/colonisation/settlement.png: " + e.getMessage());
        }
        try {
            var orbital = getClass().getResourceAsStream("/images/colonisation/orbital.png");
            if (orbital != null) {
                colonisationStationPortOrbitalImage = new Image(orbital);
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement images/colonisation/orbital.png: " + e.getMessage());
        }

        // Charger les images par type de planète
        loadPlanetImages();

        loadRingImage();
        // Charger les images par type d'étoile
        loadStarImages();

        // Initialiser la transformation de zoom
        zoomTransform = new Scale(1.0, 1.0);
        bodiesGroup.getTransforms().add(zoomTransform);

        // Désactiver les scrollbars
        bodiesScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        bodiesScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        // Le déplacement (pan) est géré manuellement pour fonctionner partout
        bodiesScrollPane.setPannable(false);
        bodiesScrollPane.setFitToWidth(true);
        bodiesScrollPane.setFitToHeight(true);

        // Bloquer tous les événements de défilement qui pourraient causer un scroll
        bodiesScrollPane.addEventFilter(ScrollEvent.ANY, event -> {
            // Consommer l'événement pour empêcher le défilement par défaut
            event.consume();
            // Gérer le zoom manuellement
            handleScroll(event);
        });
        initMapPanControls();

        // Cocher la checkbox par défaut
        if (showOnlyHighValueBodiesCheckBox != null) {
            showOnlyHighValueBodiesCheckBox.setSelected(true);
        }

        initSpacingControls();

        // Initialiser le radar
        initializeRadar();
    }

    private void initSystemTitleClipboardAction() {
        if (systemTitleLabel == null) {
            return;
        }
        systemTitleLabel.setPickOnBounds(true);
        systemTitleLabel.setCursor(Cursor.HAND);
        Tooltip tip = new TooltipComponent(localizationService.getString("colonisation.buy.tooltipCopySystem"));
        tip.setShowDelay(Duration.millis(180));
        Tooltip.install(systemTitleLabel, tip);
        systemTitleLabel.setOnMouseEntered(e -> systemTitleLabel.setUnderline(true));
        systemTitleLabel.setOnMouseExited(e -> systemTitleLabel.setUnderline(false));
        systemTitleLabel.setOnMouseClicked(event -> {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }
            String name = null;
            if (currentSystem != null && currentSystem.getSystemName() != null && !currentSystem.getSystemName().isBlank()) {
                name = currentSystem.getSystemName().trim();
            } else if (systemTitleLabel.getText() != null && !systemTitleLabel.getText().isBlank()) {
                name = systemTitleLabel.getText().trim();
            }
            if (name == null || name.isBlank()) {
                return;
            }
            Window win = systemTitleLabel.getScene() != null ? systemTitleLabel.getScene().getWindow() : null;
            if (win == null) {
                return;
            }
            copyClipboardManager.copyToClipboard(name);
            popupManager.showPopup(localizationService.getString("system.copied"), event.getSceneX(), event.getSceneY(), win);
            event.consume();
        });
    }

    private void initMapPanControls() {
        if (bodiesScrollPane == null) {
            return;
        }
        bodiesScrollPane.setCursor(Cursor.OPEN_HAND);
        bodiesScrollPane.setOnMouseEntered(event -> {
            if (!mapPanning) {
                bodiesScrollPane.setCursor(Cursor.OPEN_HAND);
            }
        });
        bodiesScrollPane.setOnMouseExited(event -> {
            if (!mapPanning) {
                bodiesScrollPane.setCursor(Cursor.DEFAULT);
            }
        });
        bodiesScrollPane.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (!event.isPrimaryButtonDown()) {
                return;
            }
            if (!isPanStartAllowed(event)) {
                mapPanning = false;
                bodiesScrollPane.setCursor(Cursor.OPEN_HAND);
                return;
            }
            mapPanning = true;
            lastPanSceneX = event.getSceneX();
            lastPanSceneY = event.getSceneY();
            bodiesScrollPane.setCursor(Cursor.CLOSED_HAND);
        });
        bodiesScrollPane.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (!mapPanning) {
                return;
            }
            double deltaX = event.getSceneX() - lastPanSceneX;
            double deltaY = event.getSceneY() - lastPanSceneY;
            lastPanSceneX = event.getSceneX();
            lastPanSceneY = event.getSceneY();
            panTranslateX += deltaX;
            panTranslateY += deltaY;
            if (bodiesGroup != null) {
                bodiesGroup.setTranslateX(panTranslateX);
                bodiesGroup.setTranslateY(panTranslateY);
            }
            event.consume();
        });
        bodiesScrollPane.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if (!mapPanning) {
                return;
            }
            mapPanning = false;
            bodiesScrollPane.setCursor(Cursor.OPEN_HAND);
        });
    }

    private boolean isPanStartAllowed(MouseEvent event) {
        Node node = event.getPickResult() != null ? event.getPickResult().getIntersectedNode() : null;
        if (node == null && event.getTarget() instanceof Node n) {
            node = n;
        }
        while (node != null) {
            if (node == bodiesPane || node == bodiesGroup || node == bodiesScrollPane) {
                return true;
            }
            if (isInteractiveMapNode(node)) {
                return false;
            }
            node = node.getParent();
        }
        return true;
    }

    private boolean isInteractiveMapNode(Node node) {
        if (node.getOnMouseClicked() != null || node.getOnMousePressed() != null || node.getOnMouseReleased() != null) {
            return true;
        }
        if (node.getCursor() == Cursor.HAND) {
            return true;
        }
        return node.getStyleClass().contains("exploration-visual-colonisation-caption-chip-interactive");
    }

    private void initSpacingControls() {
        if (spacingHorizontalSlider == null || spacingVerticalSlider == null) {
            return;
        }
        spacingHorizontalSlider.setValue(parseSpacingPreference(PREF_SYSTEM_VIEW_SPACING_X, DEFAULT_SPACING_X));
        spacingVerticalSlider.setValue(parseSpacingPreference(PREF_SYSTEM_VIEW_SPACING_Y, DEFAULT_SPACING_Y));

        Runnable apply = () -> setOrrerySpacingUserScale(spacingHorizontalSlider.getValue(), spacingVerticalSlider.getValue());
        Runnable persist = () -> {
            preferencesService.setPreference(PREF_SYSTEM_VIEW_SPACING_X, String.format(Locale.ROOT, "%.3f", spacingHorizontalSlider.getValue()));
            preferencesService.setPreference(PREF_SYSTEM_VIEW_SPACING_Y, String.format(Locale.ROOT, "%.3f", spacingVerticalSlider.getValue()));
        };

        spacingHorizontalSlider.valueProperty().addListener((obs, oldV, newV) -> apply.run());
        spacingVerticalSlider.valueProperty().addListener((obs, oldV, newV) -> apply.run());

        spacingHorizontalSlider.valueChangingProperty().addListener((obs, oldV, changing) -> {
            if (!changing) {
                persist.run();
            }
        });
        spacingVerticalSlider.valueChangingProperty().addListener((obs, oldV, changing) -> {
            if (!changing) {
                persist.run();
            }
        });

        spacingHorizontalSlider.setOnMouseReleased(e -> persist.run());
        spacingVerticalSlider.setOnMouseReleased(e -> persist.run());
        if (resetViewParametersButton != null) {
            Tooltip tooltip = new Tooltip("Reset view parameters");
            tooltip.setShowDelay(Duration.millis(120));
            tooltip.setHideDelay(Duration.millis(80));
            resetViewParametersButton.setTooltip(tooltip);
            resetViewParametersButton.setOnAction(e -> {
                spacingHorizontalSlider.setValue(DEFAULT_SPACING_X);
                spacingVerticalSlider.setValue(DEFAULT_SPACING_Y);
                persist.run();
                panTranslateX = 0.0;
                panTranslateY = 0.0;
                if (bodiesGroup != null) {
                    bodiesGroup.setTranslateX(0.0);
                    bodiesGroup.setTranslateY(0.0);
                }
                if (currentSystem != null) {
                    displaySystem(currentSystem);
                }
            });
        }

        apply.run();
    }

    private double parseSpacingPreference(String key, double defaultValue) {
        String raw = preferencesService.getPreference(key, String.valueOf(defaultValue));
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            double v = Double.parseDouble(raw.trim());
            return Math.max(USER_SPACING_MIN, Math.min(USER_SPACING_MAX, v));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Initialise le composant radar
     */
    private void initializeRadar() {
        // Créer le composant radar
        radarComponent = new RadarComponent();

        // Ajouter un listener sur la visibilité du radar pour mettre à jour l'overlay
        if (radarComponent.getRadarPane() != null) {
            radarComponent.getRadarPane().visibleProperty().addListener((obs, oldVal, newVal) -> {
                // Mettre à jour l'overlay/popup si ouvert
                if (bodiesOverlayComponent != null && bodiesOverlayComponent.isShowing() && currentSystem != null) {
                    boolean showOnlyHighValue = isHighValueBodyListFilterActive();
                    bodiesOverlayComponent.updateContent(currentSystem, showOnlyHighValue);
                    // Recalculer la taille du popup si ouvert
                    if (bodiesOverlayComponent.isPopupShowing()) {
                        Platform.runLater(() -> {
                            bodiesOverlayComponent.resizePopup();
                        });
                    }
                }
            });
        }

        // Ajouter le radar au-dessus de la liste des corps
        if (bodiesListPanel != null) {
            // Insérer le radar après le titre et la checkbox, avant le ScrollPane
            // Le titre est à l'index 0, la checkbox à l'index 1, le ScrollPane à l'index 2
            // On insère le radar à l'index 2, ce qui déplace le ScrollPane à l'index 3
            if (bodiesListPanel.getChildren().size() >= 2) {
                bodiesListPanel.getChildren().add(2, radarComponent.getRadarPane());
            } else {
                bodiesListPanel.getChildren().add(radarComponent.getRadarPane());
            }
        }
    }

    /**
     * Gère le zoom avec la molette de la souris
     * Zoom simple sans tenir compte de la position de la souris
     */
    private void handleScroll(ScrollEvent event) {
        // L'événement est déjà consommé par l'EventFilter, pas besoin de le consommer à nouveau

        double deltaY = event.getDeltaY();
        if (deltaY == 0) {
            return;
        }
        if (bodiesGroup == null) {
            return;
        }

        // Point visé par la souris dans le repère local du groupe (avant zoom)
        Point2D mouseInGroupBeforeZoom = bodiesGroup.sceneToLocal(event.getSceneX(), event.getSceneY());

        // Calculer le nouveau niveau de zoom
        double zoomChange = deltaY > 0 ? (1 + ZOOM_FACTOR) : (1 - ZOOM_FACTOR);
        double newZoom = currentZoom * zoomChange;

        // Limiter le zoom entre MIN_ZOOM et MAX_ZOOM
        newZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, newZoom));

        if (newZoom != currentZoom) {
            // Appliquer le zoom simple
            zoomTransform.setPivotX(0);
            zoomTransform.setPivotY(0);
            zoomTransform.setX(newZoom);
            zoomTransform.setY(newZoom);
            currentZoom = newZoom;

            // Après zoom, recalculer la position écran de ce même point logique
            // puis translater pour que la souris reste "ancrée" sur ce point.
            Point2D mouseInSceneAfterZoom = bodiesGroup.localToScene(mouseInGroupBeforeZoom);
            double adjustX = event.getSceneX() - mouseInSceneAfterZoom.getX();
            double adjustY = event.getSceneY() - mouseInSceneAfterZoom.getY();
            panTranslateX += adjustX;
            panTranslateY += adjustY;
            bodiesGroup.setTranslateX(panTranslateX);
            bodiesGroup.setTranslateY(panTranslateY);
        }
    }

    /**
     * Calcule et applique le zoom optimal pour afficher tout le contenu dans la vue
     * Le coin haut gauche est toujours visible
     */
    private void calculateAndApplyOptimalZoom() {
        // Obtenir les dimensions du viewport
        double viewportWidth = bodiesScrollPane.getViewportBounds().getWidth();
        double viewportHeight = bodiesScrollPane.getViewportBounds().getHeight();

        if (viewportWidth <= 0 || viewportHeight <= 0) {
            // Réessayer plus tard si les dimensions ne sont pas encore disponibles
            Platform.runLater(() -> calculateAndApplyOptimalZoom());
            return;
        }

        // Calculer les dimensions réelles du contenu en parcourant les positions des corps
        double minX = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE;
        double maxY = Double.MIN_VALUE;

        // Parcourir toutes les positions des corps pour trouver les limites réelles
        boolean hasBodies = false;
        for (BodyPosition pos : bodyPositions.values()) {
            hasBodies = true;
            // Prendre en compte la taille réelle du corps (centrée)
            double bodyRadius = pos.size / 2;
            minX = Math.min(minX, pos.x - bodyRadius);
            maxX = Math.max(maxX, pos.x + bodyRadius);
            minY = Math.min(minY, pos.y - bodyRadius);
            maxY = Math.max(maxY, pos.y + bodyRadius);
        }

        // Si aucun corps n'a été trouvé, utiliser les dimensions du pane
        double contentWidth;
        double contentHeight;
        double offsetX = 0;
        double offsetY = 0;

        if (hasBodies && minX != Double.MAX_VALUE) {
            // Utiliser les dimensions réelles du contenu avec une marge
            // Le minX et minY représentent le coin haut gauche réel
            offsetX = minX - 50; // Marge de 50px à gauche
            offsetY = minY - 50; // Marge de 50px en haut
            contentWidth = (maxX - minX) + 100; // Marge de 50px de chaque côté
            contentHeight = (maxY - minY) + 50;
        } else {
            // Fallback sur les dimensions du pane
            contentWidth = Math.max(bodiesPane.getPrefWidth(), bodiesPane.getMinWidth());
            contentHeight = Math.max(bodiesPane.getPrefHeight(), bodiesPane.getMinHeight());

            if (contentWidth <= 0) {
                contentWidth = bodiesPane.getWidth();
            }
            if (contentHeight <= 0) {
                contentHeight = bodiesPane.getHeight();
            }
        }

        if (contentWidth <= 0 || contentHeight <= 0) {
            // Réessayer plus tard si les dimensions ne sont pas encore disponibles
            Platform.runLater(() -> calculateAndApplyOptimalZoom());
            return;
        }

        // Calculer le zoom nécessaire pour que tout le contenu soit visible
        // Avec une marge de 15% de chaque côté pour être sûr que tout est visible
        double zoomX = (viewportWidth * 0.95) / contentWidth;
        double zoomY = (viewportHeight * 0.95) / contentHeight;

        // Prendre le minimum pour que tout soit visible
        double optimalZoom = Math.min(zoomX, zoomY);

        // S'assurer qu'on peut dézoomer suffisamment (ne pas limiter trop haut)
        // Limiter le zoom entre MIN_ZOOM et MAX_ZOOM, mais permettre un zoom plus faible si nécessaire
        optimalZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, optimalZoom));

        // Si le zoom calculé est trop grand (contenu plus petit que le viewport), prendre un zoom qui affiche tout
        if (optimalZoom > 1.0 && (contentWidth * optimalZoom < viewportWidth * 0.9 || contentHeight * optimalZoom < viewportHeight * 0.9)) {
            // Recalculer avec une marge plus grande
            zoomX = (viewportWidth * 0.95) / contentWidth;
            zoomY = (viewportHeight * 0.95) / contentHeight;
            optimalZoom = Math.min(zoomX, zoomY);
            optimalZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, optimalZoom));
        }

        // Appliquer le zoom optimal
        // Le pivot doit être à (0, 0) pour que le zoom se fasse depuis le coin haut gauche
        zoomTransform.setPivotX(0);
        zoomTransform.setPivotY(0);
        zoomTransform.setX(optimalZoom);
        zoomTransform.setY(optimalZoom);
        currentZoom = optimalZoom;

        // Positionner le coin haut gauche visible
        // Toujours positionner le scroll à 0,0 pour que la première étoile soit en haut à gauche
        bodiesScrollPane.setHvalue(0.0);
        bodiesScrollPane.setVvalue(0.0);
    }

    /**
     * Méthode publique pour recalculer le zoom optimal
     * Peut être appelée depuis d'autres composants (ex: WindowToggleService)
     */
    public void recalculateOptimalZoom() {
        Platform.runLater(() -> {
            Platform.runLater(() -> {
                calculateAndApplyOptimalZoom();
            });
        });
    }

    /**
     * Instance « principale » exploration ({@link #setPrimaryInstance}) ; peut être null.
     */
    public static SystemVisualViewComponent getInstance() {
        return primaryInstance;
    }

    /**
     * Liste + orrery : libellé Spansh + indicateur (même rendu que l’attente intégrée au module exploration).
     * À appeler sur le thread JavaFX avant un {@code fetchSystemVisited} asynchrone hors de ce composant.
     */
    public void showSpanshLoadingPlaceholder() {
        Runnable r = () -> {
            bodyPositions.clear();
            if (systemBodiesLabel != null) {
                systemBodiesLabel.setText(localizationService.getString("exploration.spansh_loading"));
            }
            showSpanshExplorationLoadingState();
        };
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }

    @Override
    public void refreshUI() {
        //refresh();
    }

    public void refresh() {
        if (currentSystem != null) {
            displaySystem(currentSystem);
        } else {
            clearDisplay();
        }
    }

    /**
     * Étiquettes chantiers colonisation sur la carte (nom + statut pour le style). Passer une map vide pour masquer.
     */
    public void setColonisationArchitectBodyCaptions(Map<Integer, List<ColonisationArchitectMapCaptionLine>> linesByBodyId) {
        if (linesByBodyId == null || linesByBodyId.isEmpty()) {
            this.colonisationBodyCaptionLines = Map.of();
        } else {
            Map<Integer, List<ColonisationArchitectMapCaptionLine>> copy = new HashMap<>();
            for (Map.Entry<Integer, List<ColonisationArchitectMapCaptionLine>> e : linesByBodyId.entrySet()) {
                copy.put(e.getKey(), List.copyOf(e.getValue()));
            }
            this.colonisationBodyCaptionLines = copy;
        }
    }

    /**
     * Clic sur l’étiquette d’une station en chantier (vue architecte). {@code null} désactive.
     */
    public void setColonisationArchitectStationClickHandler(LongConsumer handler) {
        this.colonisationStationClickHandler = handler;
    }

    /**
     * Masque indicateurs d’exploration (exobio, planète « mappable » / forte valeur) sur la carte et dans les cartes corps.
     * À activer pour les vues système intégrées au module colonisation.
     */
    public void setExplorationValueIndicatorsSuppressed(boolean suppressed) {
        this.suppressExplorationValueIndicators = suppressed;
    }

    /**
     * Met à jour le titre du système tout de suite (ex. pendant un chargement asynchrone).
     */
    public void setPendingSystemTitle(String systemName) {
        Runnable r = () -> {
            if (systemTitleLabel == null) {
                return;
            }
            if (systemName != null && !systemName.isBlank()) {
                systemTitleLabel.setText(systemName.trim());
            } else {
                systemTitleLabel.setText(localizationService.getString("exploration.system_visual_view"));
            }
        };
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }
    public void displaySystem(SystemVisited system){
        displaySystem(system,false);
    }
    /**
     * Affiche les corps célestes d'un système dans la vue visuelle avec tri orrery et liens visuels
     */
    public void displaySystem(SystemVisited system,boolean forceRefresh) {
        Platform.runLater(() -> {
            hideSpanshLoadingLayer();
            // Fermer le panneau JSON si un nouveau système est sélectionné
            if (this.currentSystem != null && system != null &&
                    !this.currentSystem.equals(system)) {
                closeJsonPanel();
            }

            // Réinitialiser le corps JSON affiché si on change de système
            if (system == null || (this.currentSystem != null && system != null &&
                    !this.currentSystem.equals(system))) {
                currentJsonBody = null;
            }

            boolean systemSelectionChanged = this.currentSystem != system;
            this.currentSystem = system;

            // On reset le déplacement manuel uniquement quand on change de système sélectionné.
            // (Un refresh/re-render du même système, ex. slider spacing, garde la position courante.)
            if (systemSelectionChanged || forceRefresh) {
                panTranslateX = 0.0;
                panTranslateY = 0.0;
                if (bodiesGroup != null) {
                    bodiesGroup.setTranslateX(0.0);
                    bodiesGroup.setTranslateY(0.0);
                }
            }

            // Mettre à jour le titre avec le nom du système
            if (systemTitleLabel != null) {
                if (system != null && system.getSystemName() != null) {
                    systemTitleLabel.setText(system.getSystemName());
                } else {
                    systemTitleLabel.setText(localizationService.getString("exploration.system_visual_view"));
                }
            }

            bodiesPane.getChildren().clear();
            bodyPositions.clear();

            // Mettre à jour l'overlay si il est ouvert
            if (bodiesOverlayComponent != null && bodiesOverlayComponent.isShowing()) {
                boolean showOnlyHighValue = isHighValueBodyListFilterActive();
                bodiesOverlayComponent.updateContent(system, showOnlyHighValue);
            }

            if (system == null) {
                restoreExplorationBodiesHeader();
                updateBodiesList(null);
                return;
            }

            boolean awaitSpansh = SEARCH_SPANSH_EXPLORATION
                    && systemSelectionChanged
                    && !forceRefresh
                    && !DashboardContext.getInstance().isBatchLoading();

            if (awaitSpansh) {
                showSpanshLoadingPlaceholder();
                scheduleSpanshOnlineDataHydration(system);
                return;
            }

            restoreExplorationBodiesHeader();
            // Mettre à jour la liste des corps à gauche
            updateBodiesList(system);

            // Le zoom optimal sera calculé après le positionnement des corps

            if (systemSelectionChanged) {
                scheduleSpanshOnlineDataHydration(system);
            }

            // Créer une map pour lookup rapide
            Map<Integer, ACelesteBody> bodiesMap = system.getCelesteBodies().stream()
                    .collect(Collectors.toMap(ACelesteBody::getBodyID, body -> body));

            // Trier selon la hiérarchie orrery
            List<ACelesteBody> sortedBodies = sortBodiesHierarchically(system.getCelesteBodies());

            // Disposer les corps : étoiles verticalement à gauche, planètes en chaîne horizontale, lunes verticalement
            // Ajouter un gap pour que les planètes ne collent pas au bord (espacements réduits ~15 % via
            // {@link #SYSTEM_VIEW_ORRERY_BASE_SPACING_SCALE} + échelles utilisateur horizontale/verticale.
            int gapLeft = orrerySpacingX(50);
            int gapTop = orrerySpacingY(58);
            int startX = gapLeft;
            int startY = gapTop;
            int starVerticalSpacing = orrerySpacingY(165);
            int horizontalSpacing = orrerySpacingX(120);
            int moonVerticalSpacing = orrerySpacingY(80);
            /* Lunes de lune : encore un peu plus serrées horizontalement. */
            int subMoonHorizontalSpacing = Math.max(1,
                    (int) Math.round(horizontalSpacing * 0.9));

            // Organiser la hiérarchie : étoiles -> planètes directes -> lunes -> lunes de lune
            List<ACelesteBody> stars = new ArrayList<>();
            Map<ACelesteBody, List<ACelesteBody>> starToDirectPlanets = new HashMap<>();
            Map<ACelesteBody, List<ACelesteBody>> planetToMoons = new HashMap<>();
            Map<ACelesteBody, List<ACelesteBody>> moonToSubMoons = new HashMap<>();

            // Identifier les étoiles qui ont une autre étoile comme parent (étoiles en orbite)
            // Ces étoiles doivent être traitées comme des planètes en orbite autour de l'étoile parente
            // On les identifie d'abord pour éviter de les ajouter à la liste des étoiles principales
            Set<Integer> orbitingStarIds = new HashSet<>();
            Map<Integer, ACelesteBody> orbitingStarToParent = new HashMap<>();

            for (ACelesteBody body : sortedBodies) {
                if (body instanceof StarDetail) {
                    var parents = body.getParents();
                    if (parents != null && !parents.isEmpty()) {
                        // Chercher si l'un des parents est une étoile (pas "Null")
                        ACelesteBody parentStar = null;
                        int maxBodyID = -1;

                        for (var parent : parents) {
                            if ("Null".equalsIgnoreCase(parent.getType())) {
                                continue; // Ignorer les parents "Null"
                            }
                            ACelesteBody parentBody = bodiesMap.get(parent.getBodyID());
                            if (parentBody != null && parentBody instanceof StarDetail) {
                                // Vérifier que cette étoile parente n'a pas elle-même une autre étoile comme parent
                                // (on cherche les étoiles principales, pas les étoiles en orbite)
                                var parentStarParents = parentBody.getParents();
                                boolean isMainStar = parentStarParents == null || parentStarParents.isEmpty() ||
                                        parentStarParents.stream().anyMatch(p -> "Null".equalsIgnoreCase(p.getType()));

                                if (isMainStar && parent.getBodyID() > maxBodyID) {
                                    parentStar = parentBody;
                                    maxBodyID = parent.getBodyID();
                                }
                            }
                        }

                        // Si cette étoile a une étoile principale comme parent, c'est une étoile en orbite
                        if (parentStar != null) {
                            orbitingStarIds.add(body.getBodyID());
                            orbitingStarToParent.put(body.getBodyID(), parentStar);
                        }
                    }
                }
            }

            // Identifier les étoiles principales (sans parent ou avec parent "Null")
            // Exclure les étoiles en orbite identifiées précédemment
            for (ACelesteBody body : sortedBodies) {
                if (body instanceof StarDetail) {
                    // Ne pas ajouter les étoiles en orbite à la liste des étoiles principales
                    if (orbitingStarIds.contains(body.getBodyID())) {
                        continue;
                    }

                    var parents = body.getParents();
                    if (parents == null || parents.isEmpty() ||
                            parents.stream().anyMatch(p -> "Null".equalsIgnoreCase(p.getType()))) {
                        stars.add(body);
                        starToDirectPlanets.put(body, new ArrayList<>());
                    }
                }
            }

            // Maintenant ajouter les étoiles en orbite comme "planètes" de leur étoile parente
            for (Map.Entry<Integer, ACelesteBody> entry : orbitingStarToParent.entrySet()) {
                ACelesteBody orbitingStar = bodiesMap.get(entry.getKey());
                ACelesteBody parentStar = entry.getValue();

                if (orbitingStar != null && parentStar != null && stars.contains(parentStar)) {
                    // Ajouter cette étoile comme "planète" en orbite autour de l'étoile parente
                    starToDirectPlanets.computeIfAbsent(parentStar, k -> new ArrayList<>()).add(orbitingStar);
                    planetToMoons.put(orbitingStar, new ArrayList<>());
                }
            }

            // Identifier les planètes directes des étoiles, les lunes des planètes, et les lunes de lune
            for (ACelesteBody body : sortedBodies) {
                if (body instanceof PlaneteDetail) {
                    var parents = body.getParents();
                    if (parents != null && !parents.isEmpty()) {
                        // Trouver le parent direct en utilisant le type
                        // Le parent direct est le parent avec le bodyID le plus élevé de type "Planet" ou "Star" (pas "Null")
                        ACelesteBody directParent = null;
                        String directParentType = null;
                        int maxBodyID = -1;

                        Integer nullParentBodyID = null; // BodyID du parent "Null" si présent

                        for (var parent : parents) {
                            if ("Null".equalsIgnoreCase(parent.getType())) {
                                // Sauvegarder le bodyID du parent "Null" pour créer un fake soleil partagé
                                if (nullParentBodyID == null || parent.getBodyID() < nullParentBodyID) {
                                    nullParentBodyID = parent.getBodyID();
                                }
                                continue; // Ignorer les parents "Null" pour la recherche du parent réel
                            }
                            ACelesteBody parentBody = bodiesMap.get(parent.getBodyID());
                            if (parentBody != null && parent.getBodyID() > maxBodyID) {
                                directParent = parentBody;
                                directParentType = parent.getType();
                                maxBodyID = parent.getBodyID();
                            }
                        }

                        // Si aucune planète n'a de parent réel (seulement "Null"), créer ou réutiliser un fake soleil
                        if (directParent == null && nullParentBodyID != null) {
                            // Créer un bodyID unique pour le fake soleil basé sur le bodyID du parent "Null"
                            // Utiliser un préfixe négatif pour éviter les conflits avec les vrais bodyIDs
                            int fakeStarBodyID = -1000000 - nullParentBodyID;

                            // Vérifier si ce fake soleil existe déjà (partagé par plusieurs planètes avec le même parent "Null")
                            StarDetail fakeStar = (StarDetail) bodiesMap.get(fakeStarBodyID);
                            if (fakeStar == null) {
                                // Extraire le nom de l'étoile à partir du nom de la planète
                                String starName = extractStarNameFromPlanetName(body);

                                // Créer le fake soleil
                                fakeStar = StarDetail.builder()
                                        .bodyName(starName)
                                        .bodyID(fakeStarBodyID)
                                        .starSystem(body.getStarSystem())
                                        .systemAddress(body.getSystemAddress())
                                        .starType(StarType.NULL)
                                        .starTypeString("null")
                                        .stellarMass(1.0)
                                        .wasDiscovered(false)
                                        .wasMapped(false)
                                        .wasFootfalled(false)
                                        .build();

                                // Ajouter le fake soleil à la map et à la liste des étoiles
                                bodiesMap.put(fakeStarBodyID, fakeStar);
                                stars.add(fakeStar);
                                starToDirectPlanets.put(fakeStar, new ArrayList<>());
                            }

                            // Associer la planète au fake soleil (partagé avec d'autres planètes ayant le même parent "Null")
                            directParent = fakeStar;
                            directParentType = "Star";
                        }

                        if (directParent != null && directParentType != null) {
                            if ("Star".equalsIgnoreCase(directParentType) && directParent instanceof StarDetail) {
                                // C'est une planète directe de l'étoile
                                starToDirectPlanets.computeIfAbsent(directParent, k -> new ArrayList<>()).add(body);
                                planetToMoons.put(body, new ArrayList<>());
                            } else if ("Planet".equalsIgnoreCase(directParentType) && directParent instanceof PlaneteDetail) {
                                // C'est une lune ou une lune de lune
                                // Vérifier si le parent direct est une planète directe (a une étoile comme parent)
                                // ou si c'est déjà une lune (dans planetToMoons d'une autre planète)
                                boolean isDirectPlanet = true;
                                boolean isMoon = false;

                                // Vérifier si le parent direct est déjà une lune (dans planetToMoons)
                                for (var entry : planetToMoons.entrySet()) {
                                    if (entry.getValue().contains(directParent)) {
                                        isMoon = true;
                                        isDirectPlanet = false;
                                        break;
                                    }
                                }

                                // Si ce n'est pas déjà une lune, vérifier les parents du parent
                                if (!isMoon) {
                                    var parentParents = directParent.getParents();
                                    if (parentParents != null && !parentParents.isEmpty()) {
                                        for (var pp : parentParents) {
                                            if ("Planet".equalsIgnoreCase(pp.getType())) {
                                                ACelesteBody ppBody = bodiesMap.get(pp.getBodyID());
                                                if (ppBody instanceof PlaneteDetail) {
                                                    isDirectPlanet = false;
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }

                                if (isDirectPlanet && !isMoon) {
                                    // C'est une lune d'une planète directe
                                    planetToMoons.computeIfAbsent(directParent, k -> new ArrayList<>()).add(body);
                                    moonToSubMoons.put(body, new ArrayList<>());
                                } else {
                                    // C'est une lune de lune (sub-lune) ou une lune d'une lune
                                    moonToSubMoons.computeIfAbsent(directParent, k -> new ArrayList<>()).add(body);
                                }
                            }
                        }
                    }
                }
            }

            // Passe supplémentaire : réidentifier les sub-lunes pour s'assurer qu'elles sont toutes correctement identifiées
            // (certaines peuvent avoir été manquées si leur parent lune n'était pas encore dans planetToMoons)
            for (ACelesteBody body : sortedBodies) {
                if (body instanceof PlaneteDetail) {
                    var parents = body.getParents();
                    if (parents != null && !parents.isEmpty()) {
                        // Trouver le parent direct
                        ACelesteBody directParent = null;
                        int maxBodyID = -1;

                        for (var parent : parents) {
                            if ("Null".equalsIgnoreCase(parent.getType())) {
                                continue;
                            }
                            ACelesteBody parentBody = bodiesMap.get(parent.getBodyID());
                            if (parentBody != null && parent.getBodyID() > maxBodyID) {
                                directParent = parentBody;
                                maxBodyID = parent.getBodyID();
                            }
                        }

                        // Si le parent direct est une lune (dans planetToMoons), alors ce corps est une sub-lune
                        if (directParent != null && directParent instanceof PlaneteDetail) {
                            for (var entry : planetToMoons.entrySet()) {
                                if (entry.getValue().contains(directParent)) {
                                    // Le parent est une lune, donc ce corps est une sub-lune
                                    // Vérifier qu'il n'est pas déjà dans moonToSubMoons
                                    List<ACelesteBody> existingSubMoons = moonToSubMoons.get(directParent);
                                    if (existingSubMoons == null || !existingSubMoons.contains(body)) {
                                        moonToSubMoons.computeIfAbsent(directParent, k -> new ArrayList<>()).add(body);
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            // Disposer chaque étoile avec ses planètes et lunes
            int currentStarY = startY;
            int maxX = startX;
            stars.sort(Comparator.comparing(ACelesteBody::getBodyName));
            for (ACelesteBody star : stars) {
                // Positionner l'étoile verticalement à gauche
                double starSize = getBodySize(BodyHierarchyType.STAR);
                bodyPositions.put(star.getBodyID(), new BodyPosition(startX, currentStarY, star, starSize));
                Node starView = createBodyImageView(star, startX, currentStarY, BodyHierarchyType.STAR);
                bodiesPane.getChildren().add(starView);
                addBodyNameLabel(star, startX, currentStarY, BodyHierarchyType.STAR);

                // Positionner les planètes directes de cette étoile en chaîne horizontale à droite
                List<ACelesteBody> directPlanets = starToDirectPlanets.get(star);
                if (directPlanets != null && !directPlanets.isEmpty()) {
                    // Trier les planètes par numéro extrait du nom, puis par bodyID en cas d'égalité
                    // Cela respecte l'ordre orbital naturel (1, 2, 3, etc.)
                    directPlanets.sort(Comparator
                            .comparing((ACelesteBody body) -> extractBodyNumber(body))
                            .thenComparing(ACelesteBody::getBodyID));
                    int planetX = startX + horizontalSpacing;
                    int planetY = currentStarY;

                    // Ensuite, positionner chaque planète et ses lunes
                    for (int i = 0; i < directPlanets.size(); i++) {
                        ACelesteBody planet = directPlanets.get(i);

                        // Positionner la planète (ou étoile en orbite)
                        double planetSize = getBodySize(BodyHierarchyType.PLANET);
                        bodyPositions.put(planet.getBodyID(), new BodyPosition(planetX, planetY, planet, planetSize));
                        // Si c'est une étoile en orbite, utiliser le type STAR pour le rendu visuel
                        BodyHierarchyType visualType = (planet instanceof StarDetail) ? BodyHierarchyType.STAR : BodyHierarchyType.PLANET;
                        Node planetView = createBodyImageView(planet, planetX, planetY, visualType);
                        bodiesPane.getChildren().add(planetView);
                        addBodyNameLabel(planet, planetX, planetY, visualType);

                        // Ajouter les icônes sous la planète (exobio et mapped)
                        addPlanetIcons(planet, planetX, planetY);

                        // Positionner les lunes de cette planète verticalement en dessous
                        // et calculer la position X maximale atteinte par les sub-lunes
                        int maxSubMoonX = planetX; // Position X maximale des sub-lunes

                        // Récupérer les lunes de cette planète
                        List<ACelesteBody> moons = planetToMoons.get(planet);

                        // Si c'est une étoile en orbite, récupérer aussi ses planètes depuis starToDirectPlanets
                        // et les traiter comme des "lunes" visuellement
                        if (planet instanceof StarDetail orbitingStar) {
                            List<ACelesteBody> orbitingStarPlanets = starToDirectPlanets.get(orbitingStar);
                            if (orbitingStarPlanets != null && !orbitingStarPlanets.isEmpty()) {
                                // Créer une liste combinée pour positionner à la fois les lunes et les planètes de l'étoile en orbite
                                List<ACelesteBody> allMoons = new ArrayList<>();
                                if (moons != null) {
                                    allMoons.addAll(moons);
                                }
                                allMoons.addAll(orbitingStarPlanets);
                                moons = allMoons;
                            }
                        }

                        if (moons != null && !moons.isEmpty()) {
                            int moonY = planetY + moonVerticalSpacing;
                            for (ACelesteBody moon : moons) {
                                // Positionner la lune
                                double moonSize = getBodySize(BodyHierarchyType.MOON);
                                bodyPositions.put(moon.getBodyID(), new BodyPosition(planetX, moonY, moon, moonSize));
                                Node moonView = createBodyImageView(moon, planetX, moonY, BodyHierarchyType.MOON);
                                bodiesPane.getChildren().add(moonView);
                                addBodyNameLabel(moon, planetX, moonY, BodyHierarchyType.MOON);

                                // Ajouter les icônes sous la lune (exobio et mapped)
                                addPlanetIcons(moon, planetX, moonY);

                                // Positionner les lunes de lune (sub-lunes) horizontalement à droite de la lune
                                List<ACelesteBody> subMoons = moonToSubMoons.get(moon);

                                // Si cette "lune" est en fait une planète (peut arriver pour les planètes d'étoiles en orbite),
                                // récupérer aussi ses lunes depuis planetToMoons
                                if (moon instanceof PlaneteDetail) {
                                    List<ACelesteBody> planetMoons = planetToMoons.get(moon);
                                    if (planetMoons != null && !planetMoons.isEmpty()) {
                                        // Combiner les sub-lunes existantes avec les lunes de la planète
                                        if (subMoons == null) {
                                            subMoons = new ArrayList<>();
                                        }
                                        subMoons.addAll(planetMoons);
                                    }
                                }

                                if (subMoons != null && !subMoons.isEmpty()) {
                                    int subMoonX = planetX + subMoonHorizontalSpacing;
                                    for (ACelesteBody subMoon : subMoons) {
                                        double subMoonSize = getBodySize(BodyHierarchyType.SUB_MOON);
                                        bodyPositions.put(subMoon.getBodyID(), new BodyPosition(subMoonX, moonY, subMoon, subMoonSize));
                                        Node subMoonView = createBodyImageView(subMoon, subMoonX, moonY, BodyHierarchyType.SUB_MOON);
                                        bodiesPane.getChildren().add(subMoonView);
                                        addBodyNameLabel(subMoon, subMoonX, moonY, BodyHierarchyType.SUB_MOON);

                                        // Ajouter les icônes sous la lune de lune (exobio et mapped)
                                        addPlanetIcons(subMoon, subMoonX, moonY);

                                        subMoonX += subMoonHorizontalSpacing;
                                        maxSubMoonX = Math.max(maxSubMoonX, subMoonX);
                                    }
                                    for (ACelesteBody subMoon : subMoons) {
                                        BodyPosition smPos = bodyPositions.get(subMoon.getBodyID());
                                        if (smPos != null) {
                                            addColonisationConstructionCaption(
                                                    subMoon, smPos.x, smPos.y, BodyHierarchyType.SUB_MOON);
                                        }
                                    }
                                    maxX = Math.max(maxX, subMoonX);
                                }
                                BodyPosition moonPos = bodyPositions.get(moon.getBodyID());
                                if (moonPos != null) {
                                    addColonisationConstructionCaption(moon, moonPos.x, moonPos.y, BodyHierarchyType.MOON);
                                }

                                moonY += moonVerticalSpacing;
                            }
                        }

                        // Calculer la position X pour la planète suivante
                        // Si cette planète a des sub-lunes, décaler la planète suivante pour qu'elle soit
                        // au-delà de la position maximale des sub-lunes, avec une marge de sécurité
                        if (i + 1 < directPlanets.size()) {
                            if (maxSubMoonX > planetX) {
                                // Il y a des sub-lunes, positionner la planète suivante après la dernière sub-lune
                                // avec une marge de sécurité (horizontalSpacing) pour éviter les superpositions
                                planetX = maxSubMoonX;
                            } else {
                                // Pas de sub-lunes, espacement normal
                                planetX += horizontalSpacing;
                            }
                        }
                    }
                    for (ACelesteBody planet : directPlanets) {
                        BodyPosition pos = bodyPositions.get(planet.getBodyID());
                        if (pos == null) {
                            continue;
                        }
                        BodyHierarchyType vt = (planet instanceof StarDetail)
                                ? BodyHierarchyType.STAR
                                : BodyHierarchyType.PLANET;
                        addColonisationConstructionCaption(planet, pos.x, pos.y, vt);
                    }
                    maxX = Math.max(maxX, planetX);
                }

                // Calculer la prochaine position Y pour l'étoile suivante
                // Prendre en compte la hauteur nécessaire pour cette étoile et ses planètes/lunes
                int maxHeightForThisStar = currentStarY;
                if (directPlanets != null && !directPlanets.isEmpty()) {
                    for (ACelesteBody planet : directPlanets) {
                        List<ACelesteBody> moons = planetToMoons.get(planet);

                        // Si c'est une étoile en orbite, récupérer aussi ses planètes
                        if (planet instanceof StarDetail orbitingStar) {
                            List<ACelesteBody> orbitingStarPlanets = starToDirectPlanets.get(orbitingStar);
                            if (orbitingStarPlanets != null && !orbitingStarPlanets.isEmpty()) {
                                if (moons == null) {
                                    moons = new ArrayList<>();
                                }
                                moons.addAll(orbitingStarPlanets);
                            }
                        }

                        if (moons != null && !moons.isEmpty()) {
                            // Calculer la position Y de la dernière lune
                            int lastMoonY = currentStarY + moonVerticalSpacing * moons.size();
                            maxHeightForThisStar = Math.max(maxHeightForThisStar, lastMoonY);
                        }
                    }
                }
                // Positionner la prochaine étoile en dessous de tout ce qui précède
                currentStarY = maxHeightForThisStar + starVerticalSpacing;
            }

            // Deuxième passe : traiter les lunes de lune qui n'ont pas été positionnées
            // (peut arriver si elles n'ont pas été correctement détectées dans la première passe)
            for (ACelesteBody body : sortedBodies) {
                if (body instanceof PlaneteDetail && !bodyPositions.containsKey(body.getBodyID())) {
                    var parents = body.getParents();
                    if (parents != null && !parents.isEmpty()) {
                        for (var parent : parents) {
                            if ("Planet".equalsIgnoreCase(parent.getType()) && !"Null".equalsIgnoreCase(parent.getType())) {
                                ACelesteBody parentBody = bodiesMap.get(parent.getBodyID());
                                if (parentBody instanceof PlaneteDetail) {
                                    // Vérifier si le parent est une lune (a un parent de type "Planet", pas "Star")
                                    var parentParents = parentBody.getParents();
                                    boolean parentIsMoon = false;
                                    if (parentParents != null && !parentParents.isEmpty()) {
                                        for (var pp : parentParents) {
                                            if ("Planet".equalsIgnoreCase(pp.getType()) && !"Null".equalsIgnoreCase(pp.getType())) {
                                                parentIsMoon = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (parentIsMoon && bodyPositions.containsKey(parentBody.getBodyID())) {
                                        // C'est une lune de lune, l'ajouter à la map et la positionner
                                        moonToSubMoons.computeIfAbsent(parentBody, k -> new ArrayList<>()).add(body);
                                        BodyPosition parentPos = bodyPositions.get(parentBody.getBodyID());
                                        if (parentPos != null) {
                                            int subMoonX = (int) parentPos.x + subMoonHorizontalSpacing;
                                            int subMoonY = (int) parentPos.y;
                                            double subMoonSize = getBodySize(BodyHierarchyType.SUB_MOON);
                                            bodyPositions.put(body.getBodyID(), new BodyPosition(subMoonX, subMoonY, body, subMoonSize));
                                            Node subMoonView = createBodyImageView(body, subMoonX, subMoonY, BodyHierarchyType.SUB_MOON);
                                            bodiesPane.getChildren().add(subMoonView);
                                            addBodyNameLabel(body, subMoonX, subMoonY, BodyHierarchyType.SUB_MOON);

                                            // Ajouter les icônes sous la lune de lune (exobio et mapped)
                                            addPlanetIcons(body, subMoonX, subMoonY);
                                            addColonisationConstructionCaption(body, subMoonX, subMoonY, BodyHierarchyType.SUB_MOON);

                                            maxX = Math.max(maxX, subMoonX + subMoonHorizontalSpacing);
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Dessiner les lignes de connexion
            drawConnectionsHierarchical(bodiesMap, stars, starToDirectPlanets, planetToMoons, moonToSubMoons);


            // Fixer une taille constante pour le pane (ne pas adapter au contenu)
            // Utiliser une taille fixe de 800x450 pour tous les systèmes
            double fixedWidth = 800;
            double fixedHeight = 450;
            bodiesPane.setMinWidth(600);
            bodiesPane.setMinHeight(300);
            bodiesPane.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
            bodiesPane.setMinSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);

            // Calculer et appliquer le zoom optimal pour afficher tout le contenu
            // Utiliser plusieurs Platform.runLater pour s'assurer que les dimensions sont mises à jour
/*            bodiesScrollPane.viewportBoundsProperty().addListener((obs, oldVal, newVal) -> {
                Platform.runLater(() -> calculateAndApplyOptimalZoom());
            });*/
            Platform.runLater(() -> {
                Platform.runLater(() -> {
                    calculateAndApplyOptimalZoom();
                });
            });
        });
    }

    private void restoreExplorationBodiesHeader() {
        if (systemBodiesLabel != null) {
            systemBodiesLabel.setText(localizationService.getString("exploration.system_bodies"));
        }
    }

    public void setAfterSpanshBodiesMerged(Runnable afterSpanshBodiesMerged) {
        this.afterSpanshBodiesMerged = afterSpanshBodiesMerged;
    }

    private void notifyAfterSpanshBodiesMerged() {
        if (afterSpanshBodiesMerged == null) {
            return;
        }
        try {
            afterSpanshBodiesMerged.run();
        } catch (Exception ignored) {
            // ne pas bloquer l’UI exploration
        }
    }

    private void showSpanshExplorationLoadingState() {
        if (bodiesPane != null) {
            bodiesPane.getChildren().clear();
        }
        if (bodiesListContainer != null) {
            bodiesListContainer.getChildren().clear();
            Label loadingLabel = new Label(localizationService.getString("exploration.spansh_loading"));
            loadingLabel.setWrapText(true);
            bodiesListContainer.getChildren().add(loadingLabel);
        }
        showSpanshLoadingLayerOnHost();
    }

    private void hideSpanshLoadingLayer() {
        if (spanshLoadingLayer != null) {
            spanshLoadingLayer.setVisible(false);
            spanshLoadingLayer.setManaged(false);
        }
    }

    /**
     * Couvre le panneau droit (scroll + fond) et centre le spinner, sans passer par le zoom de l’orrery.
     */
    private void showSpanshLoadingLayerOnHost() {
        if (bodiesScrollPane == null) {
            return;
        }
        Parent rawParent = bodiesScrollPane.getParent();
        if (!(rawParent instanceof StackPane host)) {
            return;
        }
        if (spanshLoadingLayer == null) {
            ProgressIndicator indicator = new ProgressIndicator();
            indicator.setMaxSize(96, 96);
            spanshLoadingLayer = new StackPane(indicator);
            StackPane.setAlignment(indicator, Pos.CENTER);
            spanshLoadingLayer.setPickOnBounds(true);
            spanshLoadingLayer.setStyle("-fx-background-color: transparent;");
            spanshLoadingLayer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            int scrollIdx = host.getChildren().indexOf(bodiesScrollPane);
            if (scrollIdx < 0) {
                host.getChildren().add(spanshLoadingLayer);
            } else {
                host.getChildren().add(scrollIdx + 1, spanshLoadingLayer);
            }
        }
        spanshLoadingLayer.setVisible(true);
        spanshLoadingLayer.setManaged(true);
    }

    private static void mergeOnlineDataFromSpansh(SystemVisited target, SystemVisited spanshSource) {
        if (target == null || spanshSource == null || spanshSource.getCelesteBodies() == null) {
            return;
        }
        if (spanshSource.getCelesteBodies().isEmpty()) {
            return;
        }
        Map<Integer, ACelesteBody> spanshByBodyId = spanshSource.getCelesteBodies().stream()
                .filter(Objects::nonNull)
                .filter(b -> b.getBodyID() != 0)
                .collect(Collectors.toMap(ACelesteBody::getBodyID, b -> b, (a, b) -> a));

        if (target.getCelesteBodies() == null || target.getCelesteBodies().isEmpty()) {
            target.setCelesteBodies(new ArrayList<>(spanshSource.getCelesteBodies()));
            return;
        }
        Set<Integer> spanshIds = spanshSource.getCelesteBodies().stream()
                .filter(Objects::nonNull)
                .map(ACelesteBody::getBodyID)
                .collect(Collectors.toSet());
        if (spanshIds.isEmpty()) {
            return;
        }
        List<ACelesteBody> merged = new ArrayList<>(target.getCelesteBodies());
        Set<Integer> targetIds = merged.stream()
                .filter(Objects::nonNull)
                .map(ACelesteBody::getBodyID)
                .collect(Collectors.toSet());
        for (ACelesteBody fromSpansh : spanshSource.getCelesteBodies()) {
            if (fromSpansh != null && !targetIds.contains(fromSpansh.getBodyID())) {
                merged.add(fromSpansh);
            }
        }
        for (ACelesteBody body : merged) {
            if (body instanceof PlaneteDetail targetPlanet) {
                ACelesteBody spanshBody = spanshByBodyId.get(body.getBodyID());
                if (spanshBody instanceof PlaneteDetail spanshPlanet) {
                    targetPlanet.mergeSpanshEnrichmentIfAbsent(spanshPlanet);
                }
            }
        }
        target.setCelesteBodies(merged);
    }

    /**
     * Extrait l'identifiant 64 bits du système ({@code SystemAddress} des journaux Elite) à partir
     * du premier corps céleste connu. Retourne {@code null} si aucun corps n'a d'adresse valide.
     */
    private static Long extractSystemAddress(SystemVisited sv) {
        if (sv == null || sv.getCelesteBodies() == null) {
            return null;
        }
        for (var body : sv.getCelesteBodies()) {
            if (body == null) {
                continue;
            }
            long addr = body.getSystemAddress();
            if (addr != 0L) {
                return addr;
            }
        }
        return null;
    }

    private void scheduleSpanshOnlineDataHydration(SystemVisited displayed) {
        if (spanshOnlineHydrationSuppressed) {
            return;
        }
        if (DashboardContext.getInstance().isBatchLoading()) {
            return;
        }
        if (displayed == null || displayed.getSystemName() == null || displayed.getSystemName().isBlank()) {
            return;
        }
        final String sysKey = displayed.getSystemName().trim();
        final Long sysId64 = extractSystemAddress(displayed);
        Thread t = new Thread(() -> {
            try {
                SystemVisited spanshSv = SpanshSystemVisitedService.getInstance().fetchSystemVisited(sysKey, sysId64);
                Platform.runLater(() -> {
                    if (currentSystem == null || currentSystem.getSystemName() == null) {
                        return;
                    }
                    if (!sysKey.equalsIgnoreCase(currentSystem.getSystemName().trim())) {
                        return;
                    }
                    mergeOnlineDataFromSpansh(currentSystem, spanshSv);
                    notifyAfterSpanshBodiesMerged();
                    spanshOnlineHydrationSuppressed = true;
                    try {
                        displaySystem(currentSystem);
                    } finally {
                        spanshOnlineHydrationSuppressed = false;
                    }
                    // Second passage après le prochain pulse layout : corps ajoutés par Spansh + zoom
                    Platform.runLater(() -> {
                        spanshOnlineHydrationSuppressed = true;
                        try {
                            displaySystem(currentSystem,true);
                        } finally {
                            spanshOnlineHydrationSuppressed = false;
                        }
                    });
                });
            } catch (Exception ignored) {
                if (!SEARCH_SPANSH_EXPLORATION) {
                    return;
                }
                Platform.runLater(() -> {
                    if (displayed == null || currentSystem == null || currentSystem.getSystemName() == null) {
                        return;
                    }
                    if (!sysKey.equalsIgnoreCase(currentSystem.getSystemName().trim())) {
                        return;
                    }
                    spanshOnlineHydrationSuppressed = true;
                    try {
                        displaySystem(currentSystem, true);
                    } finally {
                        spanshOnlineHydrationSuppressed = false;
                    }
                    Platform.runLater(() -> {
                        spanshOnlineHydrationSuppressed = true;
                        try {
                            displaySystem(currentSystem, true);
                        } finally {
                            spanshOnlineHydrationSuppressed = false;
                        }
                    });
                });
            }
        }, "spansh-bodies-online");
        t.setDaemon(true);
        t.start();
    }

    private Node maybeWrapOnlineDataRing(ACelesteBody body, double innerVisualSize, Node inner) {
        return inner;
    }

    /**
     * Retire le nom du système du début du bodyName pour l'affichage (comme le journal : « A 1 »).
     * <p>
     * Spansh renvoie souvent le nom complet (« NomDuSystème A 1 ») alors que le journal met déjà {@code BodyName}
     * court. Le préfixe doit matcher de façon insensible à la casse : {@code startsWith} strict échouait si la
     * casse diffère entre {@code starSystem} et le début de {@code bodyName}.
     */
    private String getBodyNameWithoutSystem(ACelesteBody body) {
        String bodyName = body.getBodyName();
        String systemName = body.getStarSystem();

        if (systemName == null || bodyName == null) {
            return bodyName;
        }
        String sys = systemName.trim();
        String bn = bodyName.trim();
        if (sys.isEmpty()) {
            return bodyName;
        }
        if (bn.length() >= sys.length() && bn.regionMatches(true, 0, sys, 0, sys.length())) {
            String nameWithoutSystem = bn.substring(sys.length()).trim();
            if (nameWithoutSystem.startsWith(" ")) {
                nameWithoutSystem = nameWithoutSystem.substring(1).trim();
            }
            if (nameWithoutSystem.startsWith("-")) {
                nameWithoutSystem = nameWithoutSystem.substring(1).trim();
            }
            return nameWithoutSystem.isEmpty() ? bodyName : nameWithoutSystem;
        }

        return bodyName;
    }

    /**
     * Charge les images par type de planète
     */
    private void loadPlanetImages() {
        be.mirooz.elitedangerous.biologic.BodyType[] types = be.mirooz.elitedangerous.biologic.BodyType.values();
        for (be.mirooz.elitedangerous.biologic.BodyType type : types) {
            String imageName = getImageNameForBodyType(type);
            try {
                Image image = new Image(getClass().getResourceAsStream("/images/exploration/" + imageName));
                planetImages.put(type, image);
            } catch (Exception e) {
                // Si l'image n'existe pas, utiliser l'image par défaut (gas.png)
                System.err.println("Image non trouvée pour " + type + " (" + imageName + "), utilisation de gas.png par défaut");
            }
        }
    }

    private void loadRingImage() {
        try {
            ringImageBack = new Image(getClass().getResourceAsStream("/images/exploration/ringback2.png"));
            ringImageTop = new Image(getClass().getResourceAsStream("/images/exploration/ringtop2.png"));
        } catch (Exception e) {
            System.err.println("Ring.png introuvable !");
        }
    }

    /**
     * Retourne le nom de l'image correspondant au type de planète
     */
    private String getImageNameForBodyType(be.mirooz.elitedangerous.biologic.BodyType type) {
        return switch (type) {
            case METAL_RICH -> "metal-rich.png";
            case HIGH_METAL_CONTENT -> "high-metal-content.png";
            case ROCKY -> "rocky.png";
            case ROCKY_ICE -> "rocky-ice.png";
            case ICY -> "icy.png";
            case WATER_WORLD -> "water-world.png";
            case EARTHLIKE -> "earthlike.png";
            case AMMONIA -> "ammonia.png";
            case GAS_GIANT, GAS_GIANT_I, GAS_GIANT_II, GAS_GIANT_III, GAS_GIANT_IV, GAS_GIANT_V -> "gas.png";
            case HELIUM_RICH_GG, HELIUM_GG -> "helium-gas.png";
            case ICE_GIANT -> "ice-giant.png";
            case CLUSTER -> "cluster.png";
            case UNKNOWN -> "gas.png"; // Par défaut
        };
    }

    /**
     * Charge les images pour chaque type d'étoile
     */
    private void loadStarImages() {
        StarType[] types = StarType.values();
        for (StarType type : types) {
            String imageName = type.getImageName();
            try {
                Image image = new Image(getClass().getResourceAsStream("/images/exploration/" + imageName));
                starImages.put(type, image);
            } catch (Exception e) {
                // Si l'image n'existe pas, utiliser l'image par défaut (star.png)
                System.err.println("Image non trouvée pour " + type + " (" + imageName + "), utilisation de star.png par défaut");
            }
        }
    }


    /**
     * Enum pour le type de corps dans la hiérarchie
     */
    private enum BodyHierarchyType {
        STAR,      // Étoile
        PLANET,    // Planète directe d'une étoile
        MOON,      // Lune d'une planète
        SUB_MOON   // Lune de lune
    }

    /**
     * Calcule la taille d'un corps céleste selon sa position dans la hiérarchie
     */
    private double getBodySize(BodyHierarchyType hierarchyType) {
        return switch (hierarchyType) {
            case STAR -> 60.0;        // Étoiles : 60px
            case PLANET -> 50.0;      // Planètes : 50px (un peu plus petites)
            case MOON -> 40.0;        // Lunes : 40px (encore plus petites)
            case SUB_MOON -> 30.0;    // Lunes de lune : 30px (encore plus petites)
        };
    }

    /**
     * Crée une ImageView pour un corps céleste
     */
    private Node createBodyImageView(ACelesteBody body, double x, double y, BodyHierarchyType hierarchyType) {
        double size = getBodySize(hierarchyType);

        // image de base (planète seule)
        Image planetBase = getImageForBaseBody(body);

        Node coreNode;

        // -----------------------------------------
        // 🔥 Ajouter les anneaux uniquement si body.isRings()
        // -----------------------------------------
        if (body instanceof PlaneteDetail planet && planet.isRings()) {
            coreNode = createPlanetWithRings(planetBase, ringImageBack, ringImageTop, size);
        } else {
            ImageView iv = new ImageView(planetBase);
            iv.setPreserveRatio(true);
            iv.setFitWidth(size);
            iv.setFitHeight(size);
            coreNode = iv;
        }

        Node planetNode = maybeWrapOnlineDataRing(body, size, coreNode);

        if (body instanceof PlaneteDetail planet && planet.isRings()) {
            Platform.runLater(() -> {
                if (!(planetNode instanceof StackPane sp)) {
                    return;
                }
                double w = sp.getWidth();
                double h = sp.getHeight();
                sp.setLayoutX(x - size / 2 - (w - size) / 2);
                sp.setLayoutY(y - size / 2 - (h - size) / 2);
            });
        } else {
            planetNode.setLayoutX(x - size / 2);
            planetNode.setLayoutY(y - size / 2);
        }

        // Tooltip
        String bodyName = getBodyNameWithoutSystem(body);
        Tooltip tooltip = new TooltipComponent(bodyName);
        Tooltip.install(planetNode, tooltip);

        // Classes CSS
        planetNode.getStyleClass().add("exploration-visual-body");
        if (body instanceof StarDetail)
            planetNode.getStyleClass().add("exploration-visual-star");
        else if (body instanceof PlaneteDetail)
            planetNode.getStyleClass().add("exploration-visual-planet");

        // Clic → JSON (sauf en vue architecte station : étoiles / planètes / lunes non cliquables)
        planetNode.setOnMouseClicked(event -> {
            if (colonisationStationClickHandler != null) {
                event.consume();
                return;
            }
            showJsonDialog(body, event);
        });

        return planetNode;
    }

    /**
     * Marqueur colonisation (vue architecte) : nombre de colonies puis icône {@code settlement.png} en haut à droite du corps.
     */
    private void addColonisationConstructionCaption(ACelesteBody body, double cx, double cy, BodyHierarchyType hierarchyType) {
        if (!(body instanceof PlaneteDetail)) {
            return;
        }
        int bodyId = body.getBodyID();
        List<ColonisationArchitectMapCaptionLine> lines = colonisationBodyCaptionLines.get(bodyId);
        if (lines == null || lines.isEmpty()) {
            return;
        }
        BodyPosition bodyPos = bodyPositions.get(bodyId);
        double bodySize = bodyPos != null ? bodyPos.size : getBodySize(hierarchyType);
        double bodyRadius = bodySize / 2;

        List<Long> orbitalMarketIds = new ArrayList<>();
        List<Long> surfaceMarketIds = new ArrayList<>();
        List<String> tooltipParts = new ArrayList<>();
        for (ColonisationArchitectMapCaptionLine entry : lines) {
            String nm = entry.siteLabel() != null ? entry.siteLabel().strip() : "";
            if (!nm.isEmpty()) {
                tooltipParts.add(nm);
            }
            if (entry.surfacePort()) {
                surfaceMarketIds.add(entry.marketId());
            } else {
                orbitalMarketIds.add(entry.marketId());
            }
        }
        int colonyCount = orbitalMarketIds.size() + surfaceMarketIds.size();
        if (colonyCount <= 0) {
            return;
        }

        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("exploration-visual-colonisation-caption-chip");
        boolean anyInProgress = false;
        for (ColonisationArchitectMapCaptionLine l : lines) {
            if (l.status() == ConstructionStatus.IN_PROGRESS) {
                anyInProgress = true;
                break;
            }
        }
        row.getStyleClass().add(anyInProgress
                ? "exploration-visual-colonisation-caption-chip-inprogress"
                : "exploration-visual-colonisation-caption-chip-idle");

        row.getChildren().add(buildColonisationSettlementChip(colonyCount));

        for (Node child : row.getChildren()) {
            child.setMouseTransparent(true);
        }

        if (colonisationStationClickHandler != null) {
            row.getStyleClass().add("exploration-visual-colonisation-caption-chip-interactive");
            row.setCursor(Cursor.HAND);
            ContextMenu sitePicker = buildColonisationBodySitesContextMenu(lines);
            row.setOnMouseClicked(ev -> {
                ev.consume();
                if (sitePicker.isShowing()) {
                    sitePicker.hide();
                } else {
                    sitePicker.show(row, Side.RIGHT, 2, 0);
                }
            });
            String tip = localizationService.getString("colonisation.architect.mapChipTooltip");
            if (!tooltipParts.isEmpty()) {
                tip = tip + "\n" + String.join("\n", tooltipParts);
            }
            Tooltip.install(row, new TooltipComponent(tip));
        } else if (!tooltipParts.isEmpty()) {
            Tooltip.install(row, new TooltipComponent(String.join("\n", tooltipParts)));
        }

        double layH = 30;
        double pad = 0;
        double diag = 1;
        double captionHorizontalLeftPx = 8;
        /* Décalage vers le bas pour mieux caler la pastille sur le disque (évite trop « collée » au bord haut). */
        double captionVerticalDownPx = 10;
        row.setLayoutX(cx + bodyRadius + pad + diag - captionHorizontalLeftPx);
        row.setLayoutY(cy - bodyRadius - pad - layH - diag + captionVerticalDownPx);
        bodiesPane.getChildren().add(row);
    }

    private ContextMenu buildColonisationBodySitesContextMenu(List<ColonisationArchitectMapCaptionLine> lines) {
        ContextMenu cm = new ContextMenu();
        cm.setAutoHide(true);
        cm.getStyleClass().add("colonisation-colony-cascade-popup");
        String orb = localizationService.getString("colonisation.architect.siteOrbital");
        String surf = localizationService.getString("colonisation.architect.siteSurface");

        List<ColonisationArchitectMapCaptionLine> surface = new ArrayList<>();
        List<ColonisationArchitectMapCaptionLine> orbital = new ArrayList<>();
        for (ColonisationArchitectMapCaptionLine line : lines) {
            if (line == null) {
                continue;
            }
            if (line.surfacePort()) {
                surface.add(line);
            } else {
                orbital.add(line);
            }
        }

        boolean bothKinds = !surface.isEmpty() && !orbital.isEmpty();

        for (int i = 0; i < surface.size(); i++) {
            SitePickerRowBorder border = i < surface.size() - 1 ? SitePickerRowBorder.THIN_BELOW : SitePickerRowBorder.NONE;
            cm.getItems().add(createColonisationSitePickerMenuItem(cm, surface.get(i), orb, surf, border));
        }
        if (bothKinds) {
            SeparatorMenuItem sep = new SeparatorMenuItem();
            sep.getStyleClass().add("colonisation-site-picker-surface-orbital-separator");
            cm.getItems().add(sep);
        }
        for (int i = 0; i < orbital.size(); i++) {
            ColonisationArchitectMapCaptionLine line = orbital.get(i);
            SitePickerRowBorder border = i < orbital.size() - 1
                    ? SitePickerRowBorder.THIN_BELOW
                    : SitePickerRowBorder.NONE;
            cm.getItems().add(createColonisationSitePickerMenuItem(cm, line, orb, surf, border));
        }
        return cm;
    }

    private enum SitePickerRowBorder {
        /** Pas de trait sous la ligne. */
        NONE,
        /** Trait fin noir entre deux entrées du même type. */
        THIN_BELOW
    }

    private CustomMenuItem createColonisationSitePickerMenuItem(
            ContextMenu cm,
            ColonisationArchitectMapCaptionLine line,
            String orb,
            String surf,
            SitePickerRowBorder bottomBorder) {
        Node row = buildColonisationSitePickerMenuRow(line, orb, surf, bottomBorder);
        CustomMenuItem it = new CustomMenuItem(row, true);
        it.getStyleClass().addAll("colonisation-colony-cascade-menu-item", "colonisation-site-picker-menu-item");
        it.setMnemonicParsing(false);
        long mid = line.marketId();
        it.setOnAction(e -> {
            if (colonisationStationClickHandler != null) {
                colonisationStationClickHandler.accept(mid);
            }
            cm.hide();
        });
        return it;
    }

    /**
     * Une ligne du menu « sites sur ce corps » : pastille port + libellé (survol géré en CSS sur le MenuItem parent).
     */
    private Node buildColonisationSitePickerMenuRow(
            ColonisationArchitectMapCaptionLine line,
            String orb,
            String surf,
            SitePickerRowBorder bottomBorder) {
        String name = line.siteLabel() != null ? line.siteLabel().strip() : "";
        if (name.isEmpty()) {
            name = "—";
        }
        String kind = line.surfacePort() ? surf : orb;

        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("colonisation-site-picker-row");
        if (bottomBorder == SitePickerRowBorder.THIN_BELOW) {
            row.getStyleClass().add("colonisation-site-picker-row-gap-thin-below");
        }
        if (line.status() == ConstructionStatus.COMPLETE) {
            row.getStyleClass().add("colonisation-site-picker-row-status-complete");
        } else {
            row.getStyleClass().add("colonisation-site-picker-row-status-incomplete");
        }
        row.setMaxWidth(Double.MAX_VALUE);
        row.setCursor(Cursor.HAND);

        StackPane iconWrap = new StackPane();
        iconWrap.getStyleClass().add("colonisation-site-picker-icon-wrap");
        iconWrap.setCursor(Cursor.HAND);
        Image img = line.surfacePort() ? colonisationStationPortSurfaceImage : colonisationStationPortOrbitalImage;
        if (img != null) {
            ImageView iv = new ImageView(img);
            iv.setPreserveRatio(true);
            iv.setFitHeight(20);
            iv.setFitWidth(20);
            iv.setCursor(Cursor.HAND);
            iconWrap.getChildren().add(iv);
        } else {
            Label fb = new Label(line.surfacePort() ? "P" : "O");
            fb.getStyleClass().add("exploration-visual-colonisation-port-count");
            fb.setCursor(Cursor.HAND);
            iconWrap.getChildren().add(fb);
        }

        Label nameLbl = new Label(name);
        nameLbl.setMnemonicParsing(false);
        nameLbl.getStyleClass().add("colonisation-site-picker-name");
        nameLbl.setCursor(Cursor.HAND);
        Label kindLbl = new Label(" · " + kind);
        kindLbl.setMnemonicParsing(false);
        kindLbl.getStyleClass().add("colonisation-site-picker-kind");
        kindLbl.setCursor(Cursor.HAND);

        HBox text = new HBox(0);
        text.setAlignment(Pos.CENTER_LEFT);
        text.getChildren().addAll(nameLbl, kindLbl);
        HBox.setHgrow(text, Priority.ALWAYS);

        row.getChildren().addAll(iconWrap, text);
        return row;
    }

    /**
     * Pastille carte : « N » + icône settlement.
     */
    private Node buildColonisationSettlementChip(int colonyCount) {
        HBox hb = new HBox(6);
        hb.setAlignment(Pos.CENTER_LEFT);
        Label count = new Label(Integer.toString(colonyCount));
        count.getStyleClass().addAll(
                "exploration-visual-colonisation-port-count",
                "exploration-visual-colonisation-settlement-count");
        hb.getChildren().add(count);

        StackPane iconWrap = new StackPane();
        iconWrap.getStyleClass().add("exploration-visual-colonisation-settlement-icon-wrap");
        if (colonisationSettlementImage != null) {
            ImageView iv = new ImageView(colonisationSettlementImage);
            iv.setPreserveRatio(true);
            iv.setFitHeight(28);
            iv.setFitWidth(28);
            iconWrap.getChildren().add(iv);
        } else {
            Label fb = new Label("·");
            fb.getStyleClass().add("exploration-visual-colonisation-port-count");
            iconWrap.getChildren().add(fb);
        }
        hb.getChildren().add(iconWrap);
        return hb;
    }

    private Image getImageForBaseBody(ACelesteBody body) {

        if (body instanceof StarDetail star) {
            StarType starType = star.getStarType();
            return (starType != null && starImages.containsKey(starType))
                    ? starImages.get(starType)
                    : starImage;
        }

        if (body instanceof PlaneteDetail planet) {
            BodyType planetClass = planet.getPlanetClass();
            return (planetClass != null && planetImages.containsKey(planetClass))
                    ? planetImages.get(planetClass)
                    : gasImage;
        }

        return gasImage;
    }

    private Node createPlanetWithRings(Image planet, Image ringBack, Image ringFront, double size) {

        // Conteneur principal (centrage automatique)
        StackPane root = new StackPane();
        root.setPickOnBounds(false); // Ne bloque pas les clics

        // --- Planète ---
        ImageView body = new ImageView(planet);
        body.setPreserveRatio(true);
        body.setFitWidth(size);

        // --- Anneau arrière ---
        ImageView back = new ImageView(ringBack);
        back.setPreserveRatio(true);
        back.setFitWidth(size * 2.2);

        // --- Anneau avant ---
        ImageView front = new ImageView(ringFront);
        front.setPreserveRatio(true);
        front.setFitWidth(size * 2.2);

        // Ordre des layers
        root.getChildren().addAll(back, body, front);

        return root;
    }


    /**
     * Ajoute les icônes exobio et mapped en haut à droite d'une planète dans un badge
     */
    private void addPlanetIcons(ACelesteBody body, double planetX, double planetY) {
        if (suppressExplorationValueIndicators) {
            return;
        }
        if (!(body instanceof PlaneteDetail planet)) {
            return;
        }
        // Vérifier quelles icônes afficher
        boolean hasExobio = exobioImage != null &&
                ((planet.getBioSpecies() != null && !planet.getBioSpecies().isEmpty()) ||
                        (planet.getConfirmedSpecies() != null && !planet.getConfirmedSpecies().isEmpty()));

        // Vérifier si la planète respecte les conditions pour mapped
        // Si la planète est déjà mapped, on affiche toujours l'icône + V vert
        // Sinon, on affiche l'icône seulement si elle respecte les conditions (terraformable OU baseK > 50000)
        boolean shouldShowMappedIcon = false;
        boolean isMapped = planet.isMapped();

        if (isMapped) {
            // Si la planète est déjà mapped, toujours afficher l'icône + V vert
            shouldShowMappedIcon = true;
        } else if (planet.getPlanetClass() != null) {
            // Sinon, vérifier si elle respecte les conditions
            int baseK = planet.getPlanetClass().getBaseK();
            shouldShowMappedIcon = planet.isTerraformable() || baseK > 50000;
        }

        if (!hasExobio && !shouldShowMappedIcon) {
            return; // Pas d'icônes à afficher
        }

        // Taille des icônes
        double iconSize = 14; // Taille des icônes
        double iconSpacing = 2; // Espacement vertical entre les icônes
        double padding = 3; // Padding du badge

        // Créer un conteneur vertical pour les icônes
        VBox iconsContainer = new VBox(iconSpacing);
        iconsContainer.setAlignment(javafx.geometry.Pos.CENTER);

        // Ajouter les icônes
        if (hasExobio) {
            // Créer un HBox pour l'icône exobio et le compteur
            HBox exobioContainer = new HBox(3);
            exobioContainer.setAlignment(javafx.geometry.Pos.CENTER);

            ImageView exobioIcon = new ImageView(exobioImage);
            exobioIcon.setFitWidth(iconSize);
            exobioIcon.setFitHeight(iconSize);
            exobioIcon.setPreserveRatio(true);
            exobioContainer.getChildren().add(exobioIcon);

            // Calculer le nombre d'espèces collectées et détectées
            int confirmedSpeciesCount = 0;
            int numSpeciesDetected = 0;

            if (planet.getConfirmedSpecies() != null && !planet.getConfirmedSpecies().isEmpty()) {
                confirmedSpeciesCount = (int) planet.getConfirmedSpecies().stream()
                        .filter(species -> species.isCollected())
                        .count();
            }
            if (planet.getNumSpeciesDetected() != null) {
                numSpeciesDetected = planet.getNumSpeciesDetected();
            }

            // Déterminer la couleur selon le nombre d'espèces collectées
            String color;
            if (confirmedSpeciesCount == 0) {
                // 0/Y → rouge
                color = "#FF4444";
            } else if (numSpeciesDetected > 0 && confirmedSpeciesCount == numSpeciesDetected) {
                // Y/Y → vert
                color = "#00FF88";
            } else {
                // Entre 1 et Y-1 → orange
                color = "#FF8800";
            }

            // Ajouter le label avec le format X/Y
            Label speciesCountLabel = new Label(String.format("%d/%d", confirmedSpeciesCount, numSpeciesDetected));
            speciesCountLabel.setStyle(String.format("-fx-text-fill: %s; -fx-font-size: 9px; -fx-font-weight: bold;", color));
            exobioContainer.getChildren().add(speciesCountLabel);

            iconsContainer.getChildren().add(exobioContainer);
        }

        if (shouldShowMappedIcon && mappedImage != null) {
            // Créer un HBox pour l'icône mapped et l'indicateur de statut
            HBox mappedContainer = new HBox(3);
            mappedContainer.setAlignment(javafx.geometry.Pos.CENTER);

            ImageView mappedIcon = new ImageView(mappedImage);
            mappedIcon.setFitWidth(iconSize);
            mappedIcon.setFitHeight(iconSize);
            mappedIcon.setPreserveRatio(true);
            mappedContainer.getChildren().add(mappedIcon);

            // Ajouter l'indicateur de statut (V vert si mapped, croix rouge si non mapped)
            Label statusLabel = new Label();
            if (isMapped) {
                statusLabel.setText("✓");
                statusLabel.setStyle("-fx-text-fill: #00FF00; -fx-font-size: 10px; -fx-font-weight: bold;");
            } else {
                statusLabel.setText("✗");
                statusLabel.setStyle("-fx-text-fill: #FF0000; -fx-font-size: 10px; -fx-font-weight: bold;");
            }
            mappedContainer.getChildren().add(statusLabel);

            iconsContainer.getChildren().add(mappedContainer);
        }

        // Créer un badge avec fond semi-transparent
        StackPane badge = new StackPane();
        badge.getChildren().add(iconsContainer);

        // Fond du badge avec coins arrondis
        // Si exobio est présent, le badge doit être plus large pour accommoder l'icône + le compteur
        // Si mapped est présent, le badge doit être plus large pour accommoder l'icône + le statut
        double badgeWidth = hasExobio ?
                (iconSize + 25 + (padding * 2)) : // icône + texte (~25px) + padding
                (shouldShowMappedIcon ? (iconSize + 12 + (padding * 2)) : (iconSize + (padding * 2))); // icône + statut (~12px) + padding
        double badgeHeight = ((hasExobio ? 1 : 0) + (shouldShowMappedIcon ? 1 : 0) > 1 ?
                (iconSize * 2) + iconSpacing : iconSize) + (padding * 2);

        Rectangle background = new Rectangle(badgeWidth, badgeHeight);
        background.setArcWidth(8);
        background.setArcHeight(8);
        background.setFill(Color.rgb(0, 0, 0, 0.7)); // Fond noir semi-transparent
        background.setStroke(Color.rgb(255, 255, 255, 0.3)); // Bordure blanche légère
        background.setStrokeWidth(1);

        badge.getChildren().add(0, background); // Ajouter le fond en premier

        // Position du badge : en haut à droite de la planète
        // Les planètes sont rondes, donc on positionne le badge de manière à suivre la courbe
        // Récupérer la taille réelle du corps depuis bodyPositions
        BodyPosition bodyPos = bodyPositions.get(body.getBodyID());
        double bodySize = bodyPos != null ? bodyPos.size : 60.0; // Fallback à 60px si non trouvé
        double bodyRadius = bodySize / 2;
        double offsetX = 0; // Plus proche du bord de la planète
        double offsetY = 0; // Plus proche du bord de la planète

        // Pour une forme ronde, utiliser un point légèrement en dessous de 45° pour un meilleur alignement visuel
        // Un angle d'environ 30-35° donne un meilleur positionnement pour le badge
        double angle = Math.PI / 3.5; // Environ 51° pour un positionnement plus naturel
        double tangentX = bodyRadius * Math.cos(angle);
        double tangentY = bodyRadius * Math.sin(angle);

        // Positionner le badge de manière à ce que son coin bas gauche soit tangent au cercle
        // Le point de référence sur la planète est à : (planetX + tangentX, planetY - tangentY)
        // Le coin bas gauche du badge doit être légèrement décalé de ce point
        double badgeX = planetX + tangentX + offsetX; // Le badge commence à droite du point tangent
        double badgeY = planetY - tangentY - badgeHeight - offsetY; // Le badge est au-dessus du point tangent

        badge.setLayoutX(badgeX);
        badge.setLayoutY(badgeY);

        bodiesPane.getChildren().add(badge);
    }

    /**
     * Ajoute un label avec le nom du body en bas à droite de chaque corps céleste
     */
    private void addBodyNameLabel(ACelesteBody body, double x, double y, BodyHierarchyType hierarchyType) {
        if (body == null) {
            return;
        }

        // Obtenir le nom du body sans le nom du système
        String bodyName = getBodyNameWithoutSystem(body);
        // Ne pas afficher si le nom est null, vide, ou la chaîne "null"
        if (bodyName == null || bodyName.isEmpty() || "null".equalsIgnoreCase(bodyName.trim())) {
            return;
        }

        // Pour les étoiles : si le nom est équivalent au nom du système, remplacer par "A"
        if (body instanceof StarDetail) {
            String systemName = body.getStarSystem();
            if (systemName != null && bodyName.equalsIgnoreCase(systemName)) {
                bodyName = "A";
            }
        }

        // Créer le label
        Label nameLabel = new Label(bodyName);
        nameLabel.setStyle("-fx-text-fill: #FF8C00; -fx-font-size: 15px; -fx-font-weight: bold;");

        // Calculer la position : en bas à droite du cercle, mais remonté vers le haut-gauche
        // Pour un cercle, utiliser un angle légèrement supérieur à 45° pour remonter
        double size = getBodySize(hierarchyType);
        double radius = size / 2;

        // Angle d'environ 50-55° pour remonter vers le haut-gauche tout en restant en bas-droite
        double angle = Math.PI / 3.5; // Environ 51° pour remonter un peu
        double offsetX = radius * Math.cos(angle);
        double offsetY = radius * Math.sin(angle);

        // Position du label : point sur le cercle + très petit décalage pour coller à la planète
        double labelX = x + offsetX + 2; // 2px d'espacement à droite (réduit pour coller)
        double labelY = y + offsetY - 2; // -2px pour remonter vers le haut

        nameLabel.setLayoutX(labelX);
        nameLabel.setLayoutY(labelY);

        bodiesPane.getChildren().add(nameLabel);
    }

    /**
     * Affiche le panneau JSON pour un corps céleste
     */
    private void showJsonDialog(ACelesteBody body, MouseEvent event) {
        if (body == null || jsonDetailPanel == null) {
            return;
        }

        // Si on clique sur le même corps et que le panneau est ouvert, le fermer
        if (currentJsonBody != null && currentJsonBody.getBodyID() == body.getBodyID() &&
                jsonDetailPanel.isVisible()) {
            closeJsonPanel();
            return;
        }

        // Mémoriser le corps actuellement affiché
        currentJsonBody = body;

        // Définir le nom du corps (sans icônes)
        String bodyName = body.getBodyName() != null ? body.getBodyName() : "Corps céleste";
        jsonBodyNameLabel.setText(bodyName);

        // Configurer la cellule personnalisée pour le TreeView
        jsonTreeView.setCellFactory(treeView -> new JsonTreeCell());

        // Construire le TreeView à partir du JSON
        JsonNode jsonNode = body.getJsonNode();
        if (jsonNode != null) {
            TreeItem<JsonTreeItem> root = buildJsonTree(jsonNode, "");
            jsonTreeView.setRoot(root);
            jsonTreeView.setShowRoot(false); // Masquer le root pour un affichage plus propre
            // Développer seulement le premier niveau (pas les arrays)
            root.setExpanded(true);
            if (root.getChildren() != null && !root.getChildren().isEmpty()) {
                root.getChildren().forEach(child -> {
                    JsonTreeItem childValue = child.getValue();
                    // Ne pas développer les arrays par défaut, sauf « Rings » (anneaux visibles tout de suite)
                    if (childValue != null && childValue.getValueType() != JsonTreeItem.JsonValueType.ARRAY) {
                        child.setExpanded(true);
                        // Ne pas développer le niveau suivant
                        if (child.getChildren() != null && !child.getChildren().isEmpty()) {
                            child.getChildren().forEach(grandChild -> grandChild.setExpanded(false));
                        }
                    } else {
                        boolean ringsArray = childValue != null && "Rings".equalsIgnoreCase(childValue.getKey());
                        child.setExpanded(ringsArray);
                        if (ringsArray && child.getChildren() != null && !child.getChildren().isEmpty()) {
                            child.getChildren().forEach(ringChild -> ringChild.setExpanded(true));
                        }
                    }
                });
            }
        } else {
            JsonTreeItem emptyItem = new JsonTreeItem("", null);
            TreeItem<JsonTreeItem> root = new TreeItem<>(emptyItem);
            root.setValue(new JsonTreeItem("No JSON available", null));
            jsonTreeView.setRoot(root);
            jsonTreeView.setShowRoot(true);
        }

        // Afficher le panneau
        jsonDetailPanel.setVisible(true);
        jsonDetailPanel.setManaged(true);
    }

    /**
     * Construit un TreeItem à partir d'un JsonNode avec un affichage plus joli
     */
    private TreeItem<JsonTreeItem> buildJsonTree(JsonNode node, String key) {
        JsonTreeItem jsonItem = new JsonTreeItem(key, node);
        TreeItem<JsonTreeItem> item = new TreeItem<>(jsonItem);

        if (node == null || node.isNull()) {
            return item;
        }

        // Ne pas développer les tableaux/objets formatés spécialement (affichés directement)
        if (jsonItem.isSpecialFormat()) {
            return item; // Pas d'enfants pour les éléments formatés spécialement
        }

        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String fieldKey = field.getKey();
                // Filtrer les champs event, timestamp, scantype, parents
                if (!"event".equalsIgnoreCase(fieldKey) &&
                        !"timestamp".equalsIgnoreCase(fieldKey) &&
                        !"scantype".equalsIgnoreCase(fieldKey) &&
                        !"parents".equalsIgnoreCase(fieldKey)) {
                    TreeItem<JsonTreeItem> child = buildJsonTree(field.getValue(), fieldKey);
                    item.getChildren().add(child);
                }
            }
        } else if (node.isArray()) {
            if ("Rings".equalsIgnoreCase(key)) {
                int ringCount = node.size();
                for (int i = 0; i < ringCount; i++) {
                    JsonNode ring = node.get(i);
                    if (ring == null || ring.isNull()) {
                        continue;
                    }
                    if (ring.isObject()) {
                        Iterator<Map.Entry<String, JsonNode>> ringFields = ring.fields();
                        while (ringFields.hasNext()) {
                            Map.Entry<String, JsonNode> e = ringFields.next();
                            String fk = e.getKey();
                            String label = ringCount <= 1 ? fk : ("Ring " + (i + 1) + " — " + fk);
                            item.getChildren().add(buildJsonTree(e.getValue(), label));
                        }
                    } else {
                        String label = ringCount <= 1 ? "Ring" : ("Ring " + (i + 1));
                        item.getChildren().add(buildJsonTree(ring, label));
                    }
                }
            } else {
                for (int i = 0; i < node.size(); i++) {
                    TreeItem<JsonTreeItem> child = buildJsonTree(node.get(i), "[" + i + "]");
                    item.getChildren().add(child);
                }
            }
        }

        return item;
    }

    /**
     * Ferme le panneau JSON
     */
    @FXML
    private void closeJsonPanel() {
        if (jsonDetailPanel != null) {
            jsonDetailPanel.setVisible(false);
            jsonDetailPanel.setManaged(false);
            currentJsonBody = null; // Réinitialiser le corps affiché
        }
    }

    /**
     * Dessine les lignes de connexion hiérarchique : étoile -> planètes (chaîne) et planète -> lunes (vertical) et lune -> lunes de lune (horizontal)
     */
    private void drawConnectionsHierarchical(Map<Integer, ACelesteBody> bodiesMap,
                                             List<ACelesteBody> stars,
                                             Map<ACelesteBody, List<ACelesteBody>> starToPlanets,
                                             Map<ACelesteBody, List<ACelesteBody>> planetToMoons,
                                             Map<ACelesteBody, List<ACelesteBody>> moonToSubMoons) {
        for (ACelesteBody star : stars) {
            List<ACelesteBody> planets = starToPlanets.get(star);
            if (planets == null || planets.isEmpty()) {
                continue;
            }

            BodyPosition starPos = bodyPositions.get(star.getBodyID());
            if (starPos == null) continue;

            // Ligne de l'étoile vers la première planète
            ACelesteBody firstPlanet = planets.get(0);
            BodyPosition firstPlanetPos = bodyPositions.get(firstPlanet.getBodyID());
            if (firstPlanetPos != null) {
                Line connection = new Line();
                connection.setStartX(starPos.x);
                connection.setStartY(starPos.y);
                connection.setEndX(firstPlanetPos.x);
                connection.setEndY(firstPlanetPos.y);
                connection.getStyleClass().add("exploration-visual-connection");
                bodiesPane.getChildren().add(0, connection);
            }

            // Lignes entre planètes consécutives (chaîne horizontale)
            for (int i = 1; i < planets.size(); i++) {
                BodyPosition prevPos = bodyPositions.get(planets.get(i - 1).getBodyID());
                BodyPosition currPos = bodyPositions.get(planets.get(i).getBodyID());
                if (prevPos != null && currPos != null) {
                    Line connection = new Line();
                    connection.setStartX(prevPos.x);
                    connection.setStartY(prevPos.y);
                    connection.setEndX(currPos.x);
                    connection.setEndY(currPos.y);
                    connection.getStyleClass().add("exploration-visual-connection");
                    bodiesPane.getChildren().add(0, connection);
                }
            }

            // Lignes entre planètes et leurs lunes (vertical)
            for (ACelesteBody planet : planets) {
                List<ACelesteBody> moons = planetToMoons.get(planet);
                if (moons != null && !moons.isEmpty()) {
                    BodyPosition planetPos = bodyPositions.get(planet.getBodyID());
                    if (planetPos != null) {
                        // Ligne de la planète vers sa première lune
                        ACelesteBody firstMoon = moons.get(0);
                        BodyPosition firstMoonPos = bodyPositions.get(firstMoon.getBodyID());
                        if (firstMoonPos != null) {
                            Line connection = new Line();
                            connection.setStartX(planetPos.x);
                            connection.setStartY(planetPos.y);
                            connection.setEndX(firstMoonPos.x);
                            connection.setEndY(firstMoonPos.y);
                            connection.getStyleClass().add("exploration-visual-connection");
                            bodiesPane.getChildren().add(0, connection);
                        }

                        // Lignes entre lunes consécutives (chaîne verticale)
                        for (int i = 1; i < moons.size(); i++) {
                            BodyPosition prevMoonPos = bodyPositions.get(moons.get(i - 1).getBodyID());
                            BodyPosition currMoonPos = bodyPositions.get(moons.get(i).getBodyID());
                            if (prevMoonPos != null && currMoonPos != null) {
                                Line connection = new Line();
                                connection.setStartX(prevMoonPos.x);
                                connection.setStartY(prevMoonPos.y);
                                connection.setEndX(currMoonPos.x);
                                connection.setEndY(currMoonPos.y);
                                connection.getStyleClass().add("exploration-visual-connection");
                                bodiesPane.getChildren().add(0, connection);
                            }
                        }

                        // Lignes entre lunes et leurs sub-lunes (horizontal)
                        for (ACelesteBody moon : moons) {
                            List<ACelesteBody> subMoons = moonToSubMoons.get(moon);

                            // Si cette "lune" est en fait une planète (peut arriver pour les planètes d'étoiles en orbite),
                            // récupérer aussi ses lunes depuis planetToMoons
                            if (moon instanceof PlaneteDetail) {
                                List<ACelesteBody> planetMoons = planetToMoons.get(moon);
                                if (planetMoons != null && !planetMoons.isEmpty()) {
                                    // Combiner les sub-lunes existantes avec les lunes de la planète
                                    if (subMoons == null) {
                                        subMoons = new ArrayList<>();
                                    }
                                    subMoons.addAll(planetMoons);
                                }
                            }

                            if (subMoons != null && !subMoons.isEmpty()) {
                                BodyPosition moonPos = bodyPositions.get(moon.getBodyID());
                                if (moonPos != null) {
                                    // Ligne de la lune vers sa première sub-lune
                                    ACelesteBody firstSubMoon = subMoons.get(0);
                                    BodyPosition firstSubMoonPos = bodyPositions.get(firstSubMoon.getBodyID());
                                    if (firstSubMoonPos != null) {
                                        Line connection = new Line();
                                        connection.setStartX(moonPos.x);
                                        connection.setStartY(moonPos.y);
                                        connection.setEndX(firstSubMoonPos.x);
                                        connection.setEndY(firstSubMoonPos.y);
                                        connection.getStyleClass().add("exploration-visual-connection");
                                        bodiesPane.getChildren().add(0, connection);
                                    }

                                    // Lignes entre sub-lunes consécutives (chaîne horizontale)
                                    for (int i = 1; i < subMoons.size(); i++) {
                                        BodyPosition prevSubMoonPos = bodyPositions.get(subMoons.get(i - 1).getBodyID());
                                        BodyPosition currSubMoonPos = bodyPositions.get(subMoons.get(i).getBodyID());
                                        if (prevSubMoonPos != null && currSubMoonPos != null) {
                                            Line connection = new Line();
                                            connection.setStartX(prevSubMoonPos.x);
                                            connection.setStartY(prevSubMoonPos.y);
                                            connection.setEndX(currSubMoonPos.x);
                                            connection.setEndY(currSubMoonPos.y);
                                            connection.getStyleClass().add("exploration-visual-connection");
                                            bodiesPane.getChildren().add(0, connection);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Trouve l'étoile parente d'un corps (remonte la chaîne de parents)
     */
    private ACelesteBody findParentStar(ACelesteBody body, Map<Integer, ACelesteBody> bodiesMap) {
        var parents = body.getParents();
        if (parents == null || parents.isEmpty()) {
            return null;
        }

        for (var parent : parents) {
            ACelesteBody parentBody = bodiesMap.get(parent.getBodyID());
            if (parentBody != null) {
                if (parentBody instanceof StarDetail) {
                    var starParents = parentBody.getParents();
                    if (starParents == null || starParents.isEmpty() ||
                            starParents.stream().anyMatch(p -> "Null".equalsIgnoreCase(p.getType()))) {
                        return parentBody;
                    }
                }
                // Récursivement chercher l'étoile parente
                ACelesteBody star = findParentStar(parentBody, bodiesMap);
                if (star != null) {
                    return star;
                }
            }
        }
        return null;
    }

    /**
     * Trie les corps selon la hiérarchie orrery (comme dans SystemCardController)
     */
    private List<ACelesteBody> sortBodiesHierarchically(Collection<ACelesteBody> bodies) {
        List<ACelesteBody> result = new ArrayList<>();
        Set<Integer> processed = new HashSet<>();

        // 🔒 SNAPSHOT complet pour éviter ConcurrentModificationException
        // La collection originale peut être modifiée pendant l'itération (ObservableCollection, etc.)
        List<ACelesteBody> bodiesSnapshot = new ArrayList<>(bodies);

        // Identifier les soleils
        List<ACelesteBody> stars = bodiesSnapshot.stream()
                .filter(body -> body instanceof StarDetail)
                .filter(body -> {
                    var parents = body.getParents();
                    return parents == null || parents.isEmpty() ||
                            parents.stream().anyMatch(p -> "Null".equalsIgnoreCase(p.getType()));
                })
                .sorted(Comparator.comparing(ACelesteBody::getBodyID))
                .collect(Collectors.toList());

        // Pour chaque soleil, ajouter le soleil puis ses enfants récursivement
        for (ACelesteBody star : stars) {
            if (!processed.contains(star.getBodyID())) {
                result.add(star);
                processed.add(star.getBodyID());
                addChildrenRecursively(star, bodiesSnapshot, result, processed);
            }
        }

        // Ajouter les corps orphelins
        bodiesSnapshot.stream()
                .filter(body -> !processed.contains(body.getBodyID()))
                .sorted(Comparator.comparing(ACelesteBody::getBodyID))
                .forEach(body -> {
                    if (processed.contains(body.getBodyID())) {
                        return; // skip, déjà ajouté
                    }
                    result.add(body);
                    processed.add(body.getBodyID());
                    addChildrenRecursively(body, bodiesSnapshot, result, processed);
                });

        return result;
    }

    /**
     * Ajoute récursivement les enfants d'un parent
     */
    private void addChildrenRecursively(
            ACelesteBody parent,
            Collection<ACelesteBody> allBodies,
            List<ACelesteBody> result,
            Set<Integer> processed) {

        // 🔒 SNAPSHOT pour éviter ConcurrentModificationException
        List<ACelesteBody> snapshot = new ArrayList<>(allBodies);

        List<ACelesteBody> children = snapshot.stream()
                .filter(body -> !processed.contains(body.getBodyID()))
                .filter(body -> {
                    var parents = body.getParents();
                    if (parents == null || parents.isEmpty()) {
                        return false;
                    }
                    return parents.stream()
                            .anyMatch(p -> p.getBodyID() == parent.getBodyID());
                })
                .sorted(Comparator.comparing(ACelesteBody::getBodyID))
                .toList(); // Java 16+

        for (ACelesteBody child : children) {
            if (processed.add(child.getBodyID())) { // micro-optimisation
                result.add(child);
                // Passer le snapshot au lieu de allBodies pour éviter ConcurrentModificationException
                addChildrenRecursively(child, snapshot, result, processed);
            }
        }
    }


    /**
     * Calcule la profondeur hiérarchique d'un corps
     */
    private int calculateDepth(ACelesteBody body, Map<Integer, ACelesteBody> bodiesMap) {
        var parents = body.getParents();
        if (parents == null || parents.isEmpty()) {
            return 0;
        }

        for (var parent : parents) {
            ACelesteBody parentBody = bodiesMap.get(parent.getBodyID());
            if (parentBody != null) {
                return calculateDepth(parentBody, bodiesMap) + 1;
            }
        }

        return 0;
    }

    /**
     * Met à jour la liste des corps à gauche
     */
    private void updateBodiesList(SystemVisited system) {
        if (bodiesListContainer == null) {
            return;
        }

        bodiesListContainer.getChildren().clear();

        if (system == null || system.getCelesteBodies() == null || system.getCelesteBodies().isEmpty()) {
            return;
        }

        // Créer une map pour lookup rapide
        Map<Integer, ACelesteBody> bodiesMap = system.getCelesteBodies().stream()
                .collect(Collectors.toMap(ACelesteBody::getBodyID, body -> body));

        // Trier les corps hiérarchiquement
        List<ACelesteBody> sortedBodies = sortBodiesHierarchically(system.getCelesteBodies());

        // Créer une map pour savoir si un corps a des frères suivants au même niveau
        Map<Integer, Boolean> hasNextSibling = new HashMap<>();

        // Pour chaque corps, trouver son parent direct et vérifier s'il a des frères suivants
        for (int i = 0; i < sortedBodies.size(); i++) {
            ACelesteBody body = sortedBodies.get(i);
            int depth = calculateBodyDepth(body, bodiesMap);

            // Trouver le parent direct (celui qui est à depth-1)
            ACelesteBody directParent = findDirectParent(body, bodiesMap, depth);

            if (directParent != null) {
                // Trouver tous les frères de ce corps (enfants du même parent au même niveau)
                List<ACelesteBody> siblings = sortedBodies.stream()
                        .filter(b -> {
                            int bDepth = calculateBodyDepth(b, bodiesMap);
                            if (bDepth != depth) return false;
                            ACelesteBody bParent = findDirectParent(b, bodiesMap, bDepth);
                            return bParent != null && bParent.getBodyID() == directParent.getBodyID();
                        })
                        .sorted(Comparator.comparing(ACelesteBody::getBodyID))
                        .collect(Collectors.toList());

                // Trouver l'index de ce corps dans ses frères
                int siblingIndex = siblings.indexOf(body);
                hasNextSibling.put(body.getBodyID(), siblingIndex >= 0 && siblingIndex < siblings.size() - 1);
            } else {
                // Pas de parent, donc pas de frère suivant
                hasNextSibling.put(body.getBodyID(), false);
            }
        }

        // Créer une map pour savoir si un parent a des frères suivants (pour les lignes verticales)
        Map<Integer, Boolean> parentHasNextSibling = new HashMap<>();
        for (ACelesteBody body : sortedBodies) {
            int depth = calculateBodyDepth(body, bodiesMap);
            for (int d = 0; d < depth; d++) {
                final int level = d; // Variable finale pour la lambda
                // Pour chaque niveau, trouver le parent à ce niveau
                ACelesteBody parentAtLevel = findParentAtDepth(body, bodiesMap, level);
                if (parentAtLevel != null && !parentHasNextSibling.containsKey(parentAtLevel.getBodyID())) {
                    // Vérifier si ce parent a un frère suivant
                    ACelesteBody parentParent = findDirectParent(parentAtLevel, bodiesMap, level);
                    if (parentParent != null) {
                        List<ACelesteBody> parentSiblings = sortedBodies.stream()
                                .filter(b -> {
                                    int bDepth = calculateBodyDepth(b, bodiesMap);
                                    if (bDepth != level) return false;
                                    ACelesteBody bParent = findDirectParent(b, bodiesMap, bDepth);
                                    return bParent != null && bParent.getBodyID() == parentParent.getBodyID();
                                })
                                .sorted(Comparator.comparing(ACelesteBody::getBodyID))
                                .collect(Collectors.toList());
                        int parentSiblingIndex = parentSiblings.indexOf(parentAtLevel);
                        parentHasNextSibling.put(parentAtLevel.getBodyID(),
                                parentSiblingIndex >= 0 && parentSiblingIndex < parentSiblings.size() - 1);
                    }
                }
            }
        }

        // Filtrer les corps si la checkbox est cochée
        List<ACelesteBody> filteredBodies = sortedBodies;
        if (isHighValueBodyListFilterActive()) {
            filteredBodies = sortedBodies.stream()
                    .filter(this::isHighValueBody)
                    .collect(Collectors.toList());
        }

        // Appliquer le filtre de bodyID si actif (pour n'afficher que le corps approché avec exobio non collecté)
        if (filteredBodyID != null && CommanderStatus.getInstance().getCurrentStarSystem().equals(system.getSystemName())) {
            filteredBodies = filteredBodies.stream()
                    .filter(body -> body.getBodyID() == filteredBodyID)
                    .collect(Collectors.toList());
        }

        // Créer les cartes avec les lignes hiérarchiques
        for (int i = 0; i < filteredBodies.size(); i++) {
            ACelesteBody body = filteredBodies.get(i);
            int depth = calculateBodyDepth(body, bodiesMap);
            boolean hasNext = hasNextSibling.getOrDefault(body.getBodyID(), false);

            // Créer une map pour chaque niveau de profondeur
            Map<Integer, Boolean> levelHasNext = new HashMap<>();
            for (int d = 0; d < depth; d++) {
                ACelesteBody parentAtLevel = findParentAtDepth(body, bodiesMap, d);
                if (parentAtLevel != null) {
                    levelHasNext.put(d, parentHasNextSibling.getOrDefault(parentAtLevel.getBodyID(), false));
                }
            }
            levelHasNext.put(depth, hasNext);

            VBox card = createBodyCard(body, depth, levelHasNext, bodiesMap);
            if (card != null) {
                bodiesListContainer.getChildren().add(card);
            }
        }

        // Mettre à jour l'overlay et le popup si ils sont ouverts
        if (bodiesOverlayComponent != null && bodiesOverlayComponent.isShowing() && system != null) {
            boolean showOnlyHighValue = isHighValueBodyListFilterActive();
            bodiesOverlayComponent.updateContent(system, showOnlyHighValue);
            // Recalculer la taille du popup si ouvert
            if (bodiesOverlayComponent.isPopupShowing()) {
                Platform.runLater(() -> {
                    bodiesOverlayComponent.resizePopup();
                });
            }
        }
    }

    /**
     * Calcule la profondeur hiérarchique d'un corps (nombre de niveaux de parents)
     */
    private int calculateBodyDepth(ACelesteBody body, Map<Integer, ACelesteBody> bodiesMap) {
        var parents = body.getParents();
        if (parents == null || parents.isEmpty()) {
            return 0;
        }

        int maxDepth = 0;
        for (var parent : parents) {
            if ("Null".equalsIgnoreCase(parent.getType())) {
                continue;
            }
            ACelesteBody parentBody = bodiesMap.get(parent.getBodyID());
            if (parentBody != null) {
                int parentDepth = calculateBodyDepth(parentBody, bodiesMap);
                maxDepth = Math.max(maxDepth, parentDepth + 1);
            }
        }

        return maxDepth;
    }

    /**
     * Trouve le parent direct d'un corps (celui qui est à depth-1)
     */
    private ACelesteBody findDirectParent(ACelesteBody body, Map<Integer, ACelesteBody> bodiesMap, int depth) {
        if (depth == 0) {
            return null;
        }

        var parents = body.getParents();
        if (parents == null || parents.isEmpty()) {
            return null;
        }

        // Trouver le parent qui a la profondeur depth-1
        for (var parent : parents) {
            if ("Null".equalsIgnoreCase(parent.getType())) {
                continue;
            }
            ACelesteBody parentBody = bodiesMap.get(parent.getBodyID());
            if (parentBody != null) {
                int parentDepth = calculateBodyDepth(parentBody, bodiesMap);
                if (parentDepth == depth - 1) {
                    return parentBody;
                }
            }
        }

        return null;
    }

    /**
     * Trouve le parent d'un corps à un niveau de profondeur donné
     */
    private ACelesteBody findParentAtDepth(ACelesteBody body, Map<Integer, ACelesteBody> bodiesMap, int targetDepth) {
        if (targetDepth < 0) {
            return null;
        }

        int currentDepth = calculateBodyDepth(body, bodiesMap);
        if (targetDepth >= currentDepth) {
            return null;
        }

        // Remonter la chaîne de parents jusqu'au niveau cible
        ACelesteBody current = body;
        int currentD = currentDepth;

        while (currentD > targetDepth && current != null) {
            ACelesteBody parent = findDirectParent(current, bodiesMap, currentD);
            if (parent == null) {
                break;
            }
            current = parent;
            currentD--;
        }

        return (currentD == targetDepth) ? current : null;
    }

    /**
     * Crée une carte pour un corps céleste avec toutes les informations
     */
    private VBox createBodyCard(ACelesteBody body, int depth, Map<Integer, Boolean> levelHasNext, Map<Integer, ACelesteBody> bodiesMap) {
        VBox card = new VBox(5);
        card.getStyleClass().add("exploration-body-card");

        // Largeur adaptative : évite que le fond de carte dépasse visuellement le cadre orange
        // quand la scrollbar/paddings/indentation réduisent l'espace réel.
        card.setFillWidth(true);
        card.setMinWidth(Region.USE_COMPUTED_SIZE);
        card.setPrefWidth(Region.USE_COMPUTED_SIZE);
        card.setMaxWidth(Double.MAX_VALUE);

        // Créer un conteneur pour les lignes hiérarchiques et le contenu
        HBox mainContainer = new HBox(0);
        mainContainer.setAlignment(javafx.geometry.Pos.TOP_LEFT);

        // Créer les lignes verticales pour la hiérarchie
        VBox hierarchyLines = new VBox(0);
        hierarchyLines.setAlignment(javafx.geometry.Pos.TOP_LEFT);
        hierarchyLines.setMinWidth(20 * depth);
        hierarchyLines.setPrefWidth(20 * depth);
        hierarchyLines.setMaxWidth(20 * depth);


        // Contenu de la carte
        VBox cardContent = new VBox(5);
        cardContent.setPadding(new javafx.geometry.Insets(8));
        // S'assurer que le contenu prend toute la largeur disponible
        cardContent.setMinWidth(0);
        cardContent.setPrefWidth(Region.USE_COMPUTED_SIZE);
        HBox.setHgrow(cardContent, Priority.ALWAYS);

        // Nom du corps
        HBox headerRow = new HBox(5);
        headerRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        String bodyName = getBodyNameWithoutSystem(body);
        Label nameLabel = new Label(bodyName);
        nameLabel.getStyleClass().add("exploration-body-card-name");
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 15px;");
        headerRow.getChildren().add(nameLabel);

        // Ajouter l'icône mapped si nécessaire (pour les planètes)
        if (body instanceof PlaneteDetail planet && !suppressExplorationValueIndicators) {
            // Vérifier si la planète respecte les conditions pour mapped
            boolean shouldShowMappedIcon = false;
            boolean isMapped = planet.isMapped();

            if (isMapped) {
                shouldShowMappedIcon = true;
            } else if (planet.getPlanetClass() != null) {
                int baseK = planet.getPlanetClass().getBaseK();
                shouldShowMappedIcon = planet.isTerraformable() || baseK > 50000;
            }

            if (shouldShowMappedIcon && mappedImage != null) {
                ImageView mappedIconView = new ImageView(mappedImage);
                mappedIconView.setFitWidth(20);
                mappedIconView.setFitHeight(20);
                mappedIconView.setPreserveRatio(true);
                headerRow.getChildren().add(mappedIconView);

                // Ajouter l'indicateur de statut
                Label statusLabel = new Label();
                if (isMapped) {
                    statusLabel.setText("✓");
                    statusLabel.setStyle("-fx-text-fill: #00FF00; -fx-font-size: 15px; -fx-font-weight: bold;");
                } else {
                    statusLabel.setText("✗");
                    statusLabel.setStyle("-fx-text-fill: #FF0000; -fx-font-size: 15px; -fx-font-weight: bold;");
                }
                headerRow.getChildren().add(statusLabel);

                // Ajouter "FIRST" si firstMapped (wasMapped est false)
                if (!planet.isWasMapped()) {
                    Label firstMappedLabel = new Label(localizationService.getString("exploration.first"));
                    firstMappedLabel.getStyleClass().add("exploration-body-first-discovery");
                    headerRow.getChildren().add(firstMappedLabel);
                }
                //PRICE
                //Simule la planete mappé
                boolean mappedTemp = planet.isMapped();
                planet.setMapped(true);
                long bodyValue = planet.computeBodyValue();
                planet.setMapped(mappedTemp);
                Label price = new Label(String.format("%,d Cr", bodyValue));
                price.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 14px; -fx-font-weight: bold;");
                headerRow.getChildren().add(price);
            }

            // Informations exobio (X/Y) - ajoutées dans le headerRow
            // Calculer le nombre d'espèces collectées et détectées
            int confirmedSpeciesCount = 0;
            int numSpeciesDetected = 0;

            if (planet.getConfirmedSpecies() != null && !planet.getConfirmedSpecies().isEmpty()) {
                confirmedSpeciesCount = (int) planet.getConfirmedSpecies().stream()
                        .filter(species -> species.isCollected())
                        .count();
            }
            if (planet.getNumSpeciesDetected() != null) {
                numSpeciesDetected = planet.getNumSpeciesDetected();
            }

            // Afficher l'info exobio si nécessaire
            if (exobioImage != null &&
                    ((planet.getBioSpecies() != null && !planet.getBioSpecies().isEmpty()) ||
                            (planet.getConfirmedSpecies() != null && !planet.getConfirmedSpecies().isEmpty()))) {

                // Espaceur pour pousser les éléments exobio vers la droite
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                headerRow.getChildren().add(spacer);

                ImageView exobioIconView = new ImageView(exobioImage);
                exobioIconView.setFitWidth(20);
                exobioIconView.setFitHeight(20);
                exobioIconView.setPreserveRatio(true);
                headerRow.getChildren().add(exobioIconView);

                // Déterminer la couleur selon le nombre d'espèces collectées
                String color;
                if (confirmedSpeciesCount == 0) {
                    color = "#FF4444";
                } else if (numSpeciesDetected > 0 && confirmedSpeciesCount == numSpeciesDetected) {
                    color = "#00FF88";
                } else {
                    color = "#FF8800";
                }

                Label speciesCountLabel = new Label(String.format("%d/%d", confirmedSpeciesCount, numSpeciesDetected));
                speciesCountLabel.setStyle(String.format(
                        "-fx-text-fill: %s; " +
                                "-fx-font-size: 15px; " +
                                "-fx-font-weight: bold; " +
                                "-fx-padding: 5px 10px;",
                        color));
                speciesCountLabel.getStyleClass().add("exploration-body-exobio-count");
                headerRow.getChildren().add(speciesCountLabel);

                // Ajouter un label si wasFootfalled est false (première découverte)
                if (!planet.isWasFootfalled()) {
                    Label firstDiscoveryLabel = new Label(localizationService.getString("exploration.first"));
                    firstDiscoveryLabel.getStyleClass().add("exploration-body-first-discovery");
                    headerRow.getChildren().add(firstDiscoveryLabel);
                }
            }
        }

        cardContent.getChildren().add(headerRow);

        // Informations exobio (X/Y) - seulement pour les planètes
        if (body instanceof PlaneteDetail planet && !suppressExplorationValueIndicators) {
            // Liste des BioSpecies avec probabilités
            // Afficher si on a des bioSpecies OU des confirmedSpecies
            if ((planet.getBioSpecies() != null && !planet.getBioSpecies().isEmpty()) ||
                    (planet.getConfirmedSpecies() != null && !planet.getConfirmedSpecies().isEmpty())) {
                VBox speciesList = new VBox(4);
                speciesList.setPadding(new javafx.geometry.Insets(8, 8, 8, 8));
                speciesList.getStyleClass().add("exploration-body-species-list");

                // Créer une map des confirmedSpecies par nom pour lookup rapide
                Map<String, BioSpecies> confirmedSpeciesMap = new HashMap<>();
                if (planet.getConfirmedSpecies() != null && !planet.getConfirmedSpecies().isEmpty()) {
                    for (BioSpecies confirmed : planet.getConfirmedSpecies()) {
                        if (confirmed.getName() != null) {
                            confirmedSpeciesMap.put(confirmed.getName(), confirmed);
                        }
                    }
                }

                // Set pour tracker les noms déjà traités avec confirmedSpecies
                Set<String> processedConfirmedNames = new HashSet<>();

                // Prendre le scan le plus récent (niveau le plus élevé)
                Scan latestScan = null;
                if (planet.getBioSpecies() != null && !planet.getBioSpecies().isEmpty()) {
                    latestScan = planet.getBioSpecies().stream()
                            .max(Comparator.comparingInt(Scan::getScanNumber))
                            .orElse(null);
                }

                if (latestScan != null && latestScan.getSpeciesProbabilities() != null) {
                    for (SpeciesProbability sp : latestScan.getSpeciesProbabilities()) {
                        BioSpecies bioSpecies = sp.getBioSpecies();
                        BioSpecies confirmedSpecies = null;
                        String bioSpeciesName = bioSpecies.getName();

                        // Vérifier si une confirmedSpecies a le même nom
                        if (bioSpeciesName != null && confirmedSpeciesMap.containsKey(bioSpeciesName)) {
                            // Si on n'a pas encore traité ce nom avec une confirmedSpecies, on le remplace
                            if (!processedConfirmedNames.contains(bioSpeciesName)) {
                                confirmedSpecies = confirmedSpeciesMap.get(bioSpeciesName);
                                processedConfirmedNames.add(bioSpeciesName);
                            } else {
                                // Sinon, on ignore ce bioSpecies (doublon)
                                continue;
                            }
                        }

                        HBox speciesRow = new HBox(8);
                        speciesRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                        speciesRow.setPadding(new javafx.geometry.Insets(4, 6, 4, 6));
                        speciesRow.getStyleClass().add("exploration-body-species-row");

                        // Afficher le nom (confirmedSpecies si disponible, sinon bioSpecies)
                        String speciesName;
                        if (confirmedSpecies != null) {
                            speciesName = confirmedSpecies.getFullName();
                        } else {
                            speciesName = bioSpecies.getFullName();
                        }

                        Label speciesNameLabel = new Label(speciesName);
                        speciesNameLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: -fx-elite-text;");
                        speciesNameLabel.setMaxWidth(Double.MAX_VALUE);
                        speciesNameLabel.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
                        HBox.setHgrow(speciesNameLabel, javafx.scene.layout.Priority.ALWAYS);

                        // Afficher ✓ ou ✗ si confirmedSpecies, sinon le pourcentage
                        Label statusLabel = new Label();
                        if (confirmedSpecies != null) {
                            if (confirmedSpecies.isCollected()) {
                                statusLabel.setText("✓");
                                statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #00FF00; -fx-font-weight: bold;");
                            } else if (confirmedSpecies.getSampleNumber() != 0) {
                                statusLabel.setText(confirmedSpecies.getSampleNumber() + "/3");
                                statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: -fx-elite-orange; -fx-font-weight: bold;");
                            } else {
                                statusLabel.setText("✗");
                                statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #FF0000; -fx-font-weight: bold;");
                            }
                        } else {
                            double prob = sp.getProbability();
                            statusLabel.setText(String.format("%.1f%%", prob));

                            String color = "#00FF88"; // vert par défaut

                            if (prob < 10) {
                                color = "#FF6666"; // rouge
                            } else if (prob < 40) {
                                color = "#FFA500"; // orange
                            }

                            statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + color + "; -fx-font-weight: bold;");
                            statusLabel.getStyleClass().add("exploration-body-species-prob");
                        }
                        // S'assurer que le pourcentage ne soit jamais coupé - pas de contrainte de largeur
                        statusLabel.setMinWidth(0);
                        statusLabel.setPrefWidth(-1);
                        statusLabel.setMaxWidth(Double.MAX_VALUE);

                        // Calculer et afficher le prix
                        // Utiliser confirmedSpecies si disponible, sinon bioSpecies
                        BioSpecies speciesForPrice = (confirmedSpecies != null) ? confirmedSpecies : bioSpecies;
                        long price;
                        if (!planet.isWasFootfalled()) {
                            // Si wasFootfalled est false, prendre bonusValue
                            price = speciesForPrice.getBonusValue() + speciesForPrice.getBaseValue();
                        } else {
                            // Sinon, prendre baseValue
                            price = speciesForPrice.getBaseValue();
                        }

                        Label priceLabel = new Label(String.format("%,d Cr", price));
                        priceLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #FFD700; -fx-font-weight: bold;");
                        // S'assurer que le prix ne soit jamais coupé - pas de contrainte de largeur
                        priceLabel.setMinWidth(0);
                        priceLabel.setPrefWidth(-1);
                        priceLabel.setMaxWidth(Double.MAX_VALUE);
                        statusLabel.setMinWidth(Region.USE_PREF_SIZE);
                        priceLabel.setMinWidth(Region.USE_PREF_SIZE);
                        speciesRow.getChildren().addAll(speciesNameLabel, statusLabel, priceLabel);

                        // Vérifier si cette espèce est en cours d'analyse
                        boolean isAnalyzing = false;
                        ExplorationService explorationService = ExplorationService.getInstance();
                        if (explorationService.isBiologicalAnalysisInProgress() &&
                                explorationService.getCurrentAnalysisPlanet() != null &&
                                explorationService.getCurrentAnalysisPlanet().getBodyID() == planet.getBodyID() &&
                                explorationService.getCurrentAnalysisSpecies() != null) {

                            BioSpecies currentSpecies = explorationService.getCurrentAnalysisSpecies();
                            BioSpecies speciesToCheck = (confirmedSpecies != null) ? confirmedSpecies : bioSpecies;

                            // Comparer les espèces par nom
                            if (speciesToCheck != null && currentSpecies != null &&
                                    speciesToCheck.getName() != null && currentSpecies.getName() != null &&
                                    speciesToCheck.getName().equals(currentSpecies.getName())) {
                                isAnalyzing = true;
                            }
                        }

                        // Ajouter le speciesRow à la liste d'abord
                        speciesList.getChildren().add(speciesRow);

                        // Ensuite, si c'est en cours d'analyse, ajouter la bordure animée
                        if (isAnalyzing) {
                            Platform.runLater(() -> {
                                addBorder(speciesRow);
                            });
                        }
                    }
                }

                // Ajouter les espèces confirmées qui n'ont pas été prédites (pas dans le scan)
                // Ce code s'exécute même s'il n'y a pas de scan (latestScan == null)
                if (planet.getConfirmedSpecies() != null && !planet.getConfirmedSpecies().isEmpty()) {
                    for (BioSpecies confirmedSpecies : planet.getConfirmedSpecies()) {
                        String confirmedName = confirmedSpecies.getName();

                        // Si cette espèce confirmée n'a pas été traitée (pas dans processedConfirmedNames),
                        // c'est qu'elle n'était pas dans les prédictions, il faut l'ajouter
                        if (confirmedName != null && !processedConfirmedNames.contains(confirmedName)) {
                            HBox speciesRow = new HBox(8);
                            speciesRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                            speciesRow.setPadding(new javafx.geometry.Insets(4, 6, 4, 6));
                            speciesRow.getStyleClass().add("exploration-body-species-row");

                            // Afficher le nom de l'espèce confirmée
                            String speciesName = confirmedSpecies.getFullName();
                            Label speciesNameLabel = new Label(speciesName);
                            speciesNameLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: -fx-elite-text;");
                            speciesNameLabel.setMaxWidth(Double.MAX_VALUE);
                            speciesNameLabel.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
                            HBox.setHgrow(speciesNameLabel, javafx.scene.layout.Priority.ALWAYS);

                            // Afficher ✓ ou ✗ pour l'espèce confirmée
                            Label statusLabel = new Label();
                            if (confirmedSpecies.isCollected()) {
                                statusLabel.setText("✓");
                                statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #00FF00; -fx-font-weight: bold;");
                            } else if (confirmedSpecies.getSampleNumber() != 0) {
                                statusLabel.setText(confirmedSpecies.getSampleNumber() + "/3");
                                statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: -fx-elite-orange; -fx-font-weight: bold;");
                            } else {
                                statusLabel.setText("✗");
                                statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #FF0000; -fx-font-weight: bold;");
                            }

                            statusLabel.setMinWidth(Region.USE_PREF_SIZE);

                            // Calculer et afficher le prix
                            long price;
                            if (!planet.isWasFootfalled()) {
                                price = confirmedSpecies.getBonusValue() + confirmedSpecies.getBaseValue();
                            } else {
                                price = confirmedSpecies.getBaseValue();
                            }

                            Label priceLabel = new Label(String.format("%,d Cr", price));
                            priceLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #FFD700; -fx-font-weight: bold;");
                            priceLabel.setMinWidth(0);
                            priceLabel.setPrefWidth(-1);
                            priceLabel.setMaxWidth(Double.MAX_VALUE);
                            priceLabel.setMinWidth(Region.USE_PREF_SIZE);

                            speciesRow.getChildren().addAll(speciesNameLabel, statusLabel, priceLabel);

                            // Vérifier si cette espèce est en cours d'analyse
                            boolean isAnalyzing = false;
                            ExplorationService explorationService = ExplorationService.getInstance();
                            if (explorationService.isBiologicalAnalysisInProgress() &&
                                    explorationService.getCurrentAnalysisPlanet() != null &&
                                    explorationService.getCurrentAnalysisPlanet().getBodyID() == planet.getBodyID() &&
                                    explorationService.getCurrentAnalysisSpecies() != null) {

                                BioSpecies currentSpecies = explorationService.getCurrentAnalysisSpecies();

                                // Comparer les espèces par nom
                                if (confirmedSpecies != null && currentSpecies != null &&
                                        confirmedSpecies.getName() != null && currentSpecies.getName() != null &&
                                        confirmedSpecies.getName().equals(currentSpecies.getName())) {
                                    isAnalyzing = true;
                                }
                            }

                            // Ajouter le speciesRow à la liste
                            speciesList.getChildren().add(speciesRow);

                            // Si c'est en cours d'analyse, ajouter la bordure animée
                            if (isAnalyzing) {
                                Platform.runLater(() -> {
                                    addBorder(speciesRow);
                                });
                            }
                        }
                    }
                }

                if (!speciesList.getChildren().isEmpty()) {
                    cardContent.getChildren().add(speciesList);
                }
            }
        }

        // Assembler le tout
        if (depth > 0) {
            mainContainer.getChildren().add(hierarchyLines);
        }
        mainContainer.getChildren().add(cardContent);
        card.getChildren().add(mainContainer);

        return card;
    }

    /**
     * Classe pour stocker la position d'un corps
     */
    private static class BodyPosition {
        double x, y;
        double size; // Taille du corps céleste
        ACelesteBody body;

        BodyPosition(double x, double y, ACelesteBody body, double size) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.body = body;
        }
    }

    /**
     * Vérifie si un corps est "high value" (contient exobiologie ou est mappable)
     */
    private boolean isHighValueBody(ACelesteBody body) {
        // Vérifier l'exobiologie
        if (body instanceof PlaneteDetail planet) {
            boolean hasExobio = (planet.getBioSpecies() != null && !planet.getBioSpecies().isEmpty()) ||
                    (planet.getConfirmedSpecies() != null && !planet.getConfirmedSpecies().isEmpty());

            if (hasExobio) {
                return true;
            }

            // Vérifier si mappable (terraformable ou baseK > 50000)
            if (planet.getPlanetClass() != null) {
                int baseK = planet.getPlanetClass().getBaseK();
                return planet.isTerraformable() || baseK > 50000;
            }
        }

        return false;
    }

    public void setBodiesListPanelVisible(boolean visible) {
        if (bodiesListPanel == null) {
            return;
        }
        bodiesListPanel.setVisible(visible);
        bodiesListPanel.setManaged(visible);
        if (!visible && showOnlyHighValueBodiesCheckBox != null) {
            showOnlyHighValueBodiesCheckBox.setSelected(false);
        }
    }

    /**
     * Gère le changement de la checkbox de filtre
     */
    @FXML
    private void onFilterChanged() {
        if (currentSystem != null) {
            updateBodiesList(currentSystem);

            // Mettre à jour l'overlay si il est ouvert
            if (bodiesOverlayComponent != null && bodiesOverlayComponent.isShowing()) {
                boolean showOnlyHighValue = isHighValueBodyListFilterActive();
                bodiesOverlayComponent.updateContent(currentSystem, showOnlyHighValue);
            }
        }
    }

    /**
     * Affiche ou ferme l'overlay des corps d'exploration
     */
    @FXML
    private void showBodiesOverlay() {
        if (bodiesOverlayComponent != null) {
            boolean showOnlyHighValue = isHighValueBodyListFilterActive();

            CommanderStatus commanderStatus = CommanderStatus.getInstance();
            boolean isOnFoot = commanderStatus.isOnFoot();

            if (isOnFoot) {
                // Si on est à pied, utiliser le popup
                // Si l'overlay est ouvert, le fermer
                if (bodiesOverlayComponent.isOverlayShowing()) {
                    bodiesOverlayComponent.closeOverlay();
                }
                // Si le popup est déjà ouvert, le fermer (toggle)
                if (bodiesOverlayComponent.isPopupShowing()) {
                    bodiesOverlayComponent.closePopup();
                } else {
                    // Récupérer la largeur du panneau de gauche
                    double leftPanelWidth = 450.0; // Largeur par défaut
                    if (bodiesListPanel != null) {
                        leftPanelWidth = bodiesListPanel.getWidth();
                        // Si la largeur n'est pas encore calculée, utiliser la largeur préférée ou minimale
                        if (leftPanelWidth <= 0) {
                            leftPanelWidth = Math.max(bodiesListPanel.getPrefWidth(), bodiesListPanel.getMinWidth());
                        }
                    }
                    bodiesOverlayComponent.showPopup(currentSystem, showOnlyHighValue, leftPanelWidth);
                }
            } else {
                // Si on n'est pas à pied, utiliser l'overlay
                // Si le popup est ouvert, le fermer
                if (bodiesOverlayComponent.isPopupShowing()) {
                    bodiesOverlayComponent.closePopup();
                }
                // Si l'overlay est déjà ouvert, le fermer (toggle)
                if (bodiesOverlayComponent.isShowing()) {
                    bodiesOverlayComponent.closeOverlay();
                } else {
                    bodiesOverlayComponent.showOverlay(currentSystem, showOnlyHighValue, true);
                }
            }

            updateBodiesOverlayButtonText();
        }
    }

    /**
     * Gère le changement d'état "à pied" pour faire la transition automatique overlay/popup
     */
    private void handleOnFootStateChanged(boolean isOnFoot) {
        Platform.runLater(() -> {
            if (bodiesOverlayComponent == null || currentSystem == null) {
                return;
            }

            boolean showOnlyHighValue = isHighValueBodyListFilterActive();

            if (isOnFoot) {
                // Si on est à pied, fermer l'overlay et ouvrir le popup
                if (bodiesOverlayComponent.isOverlayShowing()) {
                    bodiesOverlayComponent.closeOverlay();
                    // Récupérer la largeur du panneau de gauche
                    double leftPanelWidth = 450.0; // Largeur par défaut
                    if (bodiesListPanel != null) {
                        leftPanelWidth = bodiesListPanel.getWidth();
                        if (leftPanelWidth <= 0) {
                            leftPanelWidth = Math.max(bodiesListPanel.getPrefWidth(), bodiesListPanel.getMinWidth());
                        }
                    }
                    bodiesOverlayComponent.showPopup(currentSystem, showOnlyHighValue, leftPanelWidth);
                }
            } else {
                // Si on n'est plus à pied, fermer le popup et ouvrir l'overlay
                if (bodiesOverlayComponent.isPopupShowing()) {
                    bodiesOverlayComponent.closePopup();
                    bodiesOverlayComponent.showOverlay(currentSystem, showOnlyHighValue, false);
                }
            }

            updateBodiesOverlayButtonText();
        });
    }

    /**
     * Met à jour le texte du bouton overlay selon l'état de la fenêtre
     */
    private void updateBodiesOverlayButtonText() {
        if (bodiesOverlayButton != null && bodiesOverlayComponent != null) {
            String text;
            String icon;

            if (bodiesOverlayComponent.isShowing() || bodiesOverlayComponent.isPopupShowing()) {
                text = localizationService.getString("exploration.close");
                icon = "✖"; // Croix pour fermer
            } else {
                text = localizationService.getString("exploration.overlay");
                icon = "🗔"; // Icône de fenêtre pour ouvrir
            }

            // Combiner l'icône et le texte
            bodiesOverlayButton.setText(icon + " " + text);
        }
    }

    /**
     * Crée la liste des corps pour l'overlay (similaire à updateBodiesList mais retourne un VBox)
     */
    private VBox createBodiesListForOverlay(SystemVisited system) {
        VBox container = new VBox(5);
        container.setSpacing(5);
        container.setPadding(new javafx.geometry.Insets(5));

        // Ajouter le radar s'il est visible dans le panel de gauche
        // Toujours vérifier l'état actuel du radar principal pour s'assurer qu'on crée le bon radar
        if (radarComponent != null && radarComponent.getRadarPane() != null) {
            Pane radarPane = radarComponent.getRadarPane();
            if (radarPane.isVisible()) {
                // Vérifier si le radar de l'overlay existe et est valide
                // Si le radarPane de l'overlay est null ou n'a pas de parent valide, le recréer
                boolean needNewRadar = false;
                if (overlayRadarComponent == null) {
                    needNewRadar = true;
                } else {
                    Pane existingPane = overlayRadarComponent.getRadarPane();
                    if (existingPane == null || existingPane.getParent() == null) {
                        needNewRadar = true;
                    }
                }

                // Recréer le radar si nécessaire (après une transition on foot par exemple)
                if (needNewRadar) {
                    overlayRadarComponent = new RadarComponent();
                }

                // Toujours s'assurer que le radar de l'overlay est visible et initialisé
                overlayRadarComponent.showRadar();

                Pane overlayRadarPane = overlayRadarComponent.getRadarPane();

                // Détacher le radar de son ancien parent s'il en a un
                Parent oldParent = overlayRadarPane.getParent();
                if (oldParent instanceof Pane) {
                    ((Pane) oldParent).getChildren().remove(overlayRadarPane);
                }

                // Retirer le cadre noir (fond et bordure) dans l'overlay/popup
                overlayRadarPane.setStyle("-fx-background-color: transparent;");

                // Débinder l'ancien binding s'il existe
                overlayRadarPane.prefWidthProperty().unbind();

                // Faire en sorte que le radar prenne la largeur du container
                // Utiliser un binding au lieu d'un listener pour éviter les fuites mémoire
                // Le binding sera automatiquement nettoyé quand le container sera détaché
                overlayRadarPane.prefWidthProperty().bind(
                        container.widthProperty().subtract(10) // -10 pour le padding
                );

                // S'assurer que le radar est visible avant de l'ajouter
                overlayRadarPane.setVisible(true);
                overlayRadarPane.setManaged(true);

                // S'assurer que le radar a une hauteur minimale pour être visible
                overlayRadarPane.setPrefHeight(200);
                overlayRadarPane.setMinHeight(200);
                overlayRadarPane.setMaxHeight(200);

                container.getChildren().add(overlayRadarPane);

                // Forcer la mise à jour du radar après l'ajout pour s'assurer qu'il est dessiné
                Platform.runLater(() -> {
                    overlayRadarComponent.forceUpdate();
                });
            }
        }

        if (system == null || system.getCelesteBodies() == null || system.getCelesteBodies().isEmpty()) {
            return container;
        }

        // Créer une map pour lookup rapide
        Map<Integer, ACelesteBody> bodiesMap = system.getCelesteBodies().stream()
                .collect(Collectors.toMap(ACelesteBody::getBodyID, body -> body));

        // Trier les corps hiérarchiquement
        List<ACelesteBody> sortedBodies = sortBodiesHierarchically(system.getCelesteBodies());

        // Filtrer les corps si nécessaire (seulement les high value)
        List<ACelesteBody> filteredBodies = sortedBodies;
        if (isHighValueBodyListFilterActive()) {
            filteredBodies = sortedBodies.stream()
                    .filter(this::isHighValueBody)
                    .collect(Collectors.toList());
        }

        // Appliquer le filtre de bodyID si actif (pour n'afficher que le corps approché avec exobio non collecté)
        if (filteredBodyID != null && CommanderStatus.getInstance().getCurrentStarSystem().equals(system.getSystemName())) {
            filteredBodies = filteredBodies.stream()
                    .filter(body -> body.getBodyID() == filteredBodyID)
                    .collect(Collectors.toList());
        }

        // Créer les cartes pour chaque corps
        for (ACelesteBody body : filteredBodies) {
            int depth = calculateBodyDepth(body, bodiesMap);
            Map<Integer, Boolean> levelHasNext = new HashMap<>();
            // Pour simplifier, on ne calcule pas les lignes hiérarchiques dans l'overlay
            for (int d = 0; d <= depth; d++) {
                levelHasNext.put(d, false);
            }

            VBox card = createBodyCard(body, depth, levelHasNext, bodiesMap);
            if (card != null) {
                // Ajouter le style overlay
                card.getStyleClass().add("mirror-overlay");
                container.getChildren().add(card);
            }
        }

        return container;
    }

    /**
     * Ajoute une bordure verte autour d'un HBox pour indiquer qu'une espèce est en cours d'analyse
     */
    private void addBorder(HBox speciesRow) {
        // Ajouter une classe CSS pour la bordure verte
        speciesRow.getStyleClass().add("exobio-species-analyzing");
    }

    /**
     * Efface l'affichage
     */
    private void clearDisplay() {
        Platform.runLater(() -> {
            hideSpanshLoadingLayer();
            bodiesPane.getChildren().clear();
            if (bodiesListContainer != null) {
                bodiesListContainer.getChildren().clear();
            }
            currentSystem = null;
        });
    }

    /**
     * Implémentation de BodyFilterListener
     * Filtre la liste des corps pour n'afficher que le corps approché avec exobio non collecté
     */
    @Override
    public void onBodyFilter(Integer bodyID) {
        Platform.runLater(() -> {
            filteredBodyID = bodyID;
            // Rafraîchir la liste des corps avec le nouveau filtre
            if (currentSystem != null) {
                updateBodiesList(currentSystem);
            }
        });
    }

    /**
     * Met à jour toutes les traductions de l'interface
     */
    private void updateTranslations() {
        if (systemBodiesLabel != null) {
            systemBodiesLabel.setText(localizationService.getString("exploration.system_bodies"));
        }
        if (showOnlyHighValueBodiesCheckBox != null) {
            showOnlyHighValueBodiesCheckBox.setText(localizationService.getString("exploration.show_only_high_value"));
        }
        if (systemTitleLabel != null && currentSystem == null) {
            systemTitleLabel.setText(localizationService.getString("exploration.system_visual_view"));
        }
        if (spacingTitleLabel != null) {
            spacingTitleLabel.setText(localizationService.getString("exploration.spacing.title"));
        }
        if (spacingHorizontalLabel != null) {
            spacingHorizontalLabel.setText(localizationService.getString("exploration.spacing.horizontal"));
        }
        if (spacingVerticalLabel != null) {
            spacingVerticalLabel.setText(localizationService.getString("exploration.spacing.vertical"));
        }
    }

    /**
     * Extrait le numéro du corps céleste à partir de son nom.
     * Par exemple : "System AB 1" -> 1, "System A 2" -> 2, "System 3" -> 3
     * Si aucun numéro n'est trouvé, retourne Integer.MAX_VALUE pour placer ces corps à la fin.
     *
     * @param body Le corps céleste dont on veut extraire le numéro
     * @return Le numéro extrait, ou Integer.MAX_VALUE si aucun numéro n'est trouvé
     */
    private int extractBodyNumber(ACelesteBody body) {
        String bodyName = getBodyNameWithoutSystem(body);
        if (bodyName == null || bodyName.isEmpty()) {
            return Integer.MAX_VALUE;
        }

        // Extraire le dernier nombre du nom (généralement après le dernier espace)
        String[] parts = bodyName.split("\\s+");
        if (parts.length > 0) {
            String lastPart = parts[parts.length - 1];
            try {
                return Integer.parseInt(lastPart);
            } catch (NumberFormatException e) {
                // Si la dernière partie n'est pas un nombre, essayer de trouver un nombre ailleurs
                for (int i = parts.length - 1; i >= 0; i--) {
                    try {
                        return Integer.parseInt(parts[i]);
                    } catch (NumberFormatException ignored) {
                        // Continuer à chercher
                    }
                }
            }
        }

        return Integer.MAX_VALUE;
    }

    /**
     * Extrait le nom de l'étoile à partir du nom de la planète.
     * Si la planète s'appelle "nom_systeme AB 1", retourne "nom_systeme AB".
     *
     * @param planet Le corps céleste (planète) dont on veut extraire le nom de l'étoile
     * @return Le nom de l'étoile extrait (nom_systeme + nom_étoile), ou "null" si l'extraction échoue
     */
    private String extractStarNameFromPlanetName(ACelesteBody planet) {
        String bodyName = planet.getBodyName();
        String systemName = planet.getStarSystem();

        if (bodyName == null || systemName == null) {
            return systemName != null ? systemName + " null" : "null";
        }

        // Retirer le nom du système du début du bodyName
        String nameWithoutSystem = bodyName;
        if (bodyName.startsWith(systemName)) {
            nameWithoutSystem = bodyName.substring(systemName.length()).trim();
            // Si le nom commence par un espace, le retirer
            if (nameWithoutSystem.startsWith(" ")) {
                nameWithoutSystem = nameWithoutSystem.substring(1);
            }
        }

        if (nameWithoutSystem.isEmpty()) {
            return systemName + " null";
        }

        // Extraire la partie avant le dernier espace et le numéro
        // Par exemple : "AB 1" -> "AB", "A 2" -> "A", "ABC 3" -> "ABC"
        String[] parts = nameWithoutSystem.split("\\s+");
        String starNamePart;
        if (parts.length >= 2) {
            // Vérifier si la dernière partie est un nombre
            try {
                Integer.parseInt(parts[parts.length - 1]);
                // Si c'est un nombre, retourner tout sauf la dernière partie
                starNamePart = String.join(" ", Arrays.copyOf(parts, parts.length - 1));
            } catch (NumberFormatException e) {
                // Si ce n'est pas un nombre, retourner le nom complet
                starNamePart = nameWithoutSystem;
            }
        } else {
            starNamePart = nameWithoutSystem;
        }

        // Retourner le nom du système + le nom de l'étoile
        return systemName + " " + starNamePart;
    }
}

