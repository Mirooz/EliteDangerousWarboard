package be.mirooz.elitedangerous.dashboard.view.exploration;

import be.mirooz.elitedangerous.analytics.SpanshGuidExpiredException;
import be.mirooz.elitedangerous.analytics.dto.spansh.SpanshSearchRequestDTO;
import be.mirooz.elitedangerous.analytics.dto.spansh.SpanshRouteRequestDTO;
import be.mirooz.elitedangerous.analytics.dto.spansh.SpanshSearchResponse;
import be.mirooz.elitedangerous.analytics.dto.spansh.SpanshSearchResponseDTO;
import be.mirooz.elitedangerous.analytics.dto.spansh.SpanshRouteResponseDTO;
import be.mirooz.elitedangerous.analytics.dto.spansh.SpanshRouteResultsResponseDTO;
import be.mirooz.elitedangerous.dashboard.model.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.model.exploration.ExplorationMode;
import be.mirooz.elitedangerous.dashboard.model.navigation.NavRoute;
import be.mirooz.elitedangerous.dashboard.model.navigation.RouteSystem;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.ExplorationModeRegistry;
import be.mirooz.elitedangerous.dashboard.model.registries.navigation.NavRouteRegistry;
import be.mirooz.elitedangerous.dashboard.model.registries.navigation.NavRouteTargetRegistry;
import be.mirooz.elitedangerous.dashboard.service.webservice.AnalyticsService;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.service.listeners.NavRouteNotificationService;
import be.mirooz.elitedangerous.dashboard.service.listeners.ExplorationRefreshNotificationService;
import be.mirooz.elitedangerous.dashboard.view.common.TooltipComponent;
import be.mirooz.elitedangerous.dashboard.view.common.context.DashboardContext;
import be.mirooz.elitedangerous.dashboard.view.common.managers.CopyClipboardManager;
import be.mirooz.elitedangerous.dashboard.view.common.managers.PopupManager;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Composant pour afficher la route de navigation dans le panel d'exploration
 * Représentation graphique horizontale avec des boules (cercles) et des lignes
 */
public class NavRouteComponent implements Initializable {

    private static final double CIRCLE_RADIUS_BASE = 12.0; // Taille de base
    private static final double CIRCLE_CURRENT_RADIUS_BASE = 16.0; // Taille de base pour le système actuel
    private static final double CIRCLE_RADIUS_MIN = 6.0; // Taille minimum
    private static final double CIRCLE_CURRENT_RADIUS_MIN = 8.0; // Taille minimum pour le système actuel
    private static final double LINE_HEIGHT = 70.0; // Hauteur fixe du panel (augmentée pour l'indicateur scoopable)
    private static final double MIN_SPACING = 40.0; // Espacement minimum entre les cercles
    private static final double MAX_SPACING = 120.0; // Espacement maximum entre les cercles
    private static final double PADDING_X = 20.0; // Padding horizontal
    private static final int SYSTEM_COUNT_THRESHOLD = 10; // Seuil à partir duquel on commence à réduire

    // Types d'étoiles scoopables (KGBFOAM)
    private static final Set<String> SCOOPABLE_STAR_TYPES = Set.of(
        "K", "G", "B", "F", "O", "A", "M"
    );

    // Types d'étoiles qui donnent un boost (Neutron Star, White Dwarf)
    // N = Neutron Star, D = White Dwarf (DA, DB, DC, etc.)
    private static final Set<String> BOOST_STAR_TYPES = Set.of(
        "N", "D"
    );

    @FXML
    private VBox navRouteContainer;

    @FXML
    private Label routeTitleLabel;

    @FXML
    private Pane routeSystemsPane;

    @FXML
    private HBox modeSelectorContainer;

    @FXML
    private Label modeDescriptionLabel;

    @FXML
    private VBox modeDescriptionContainer;

    @FXML
    private ProgressIndicator loadingIndicator;

    @FXML
    private javafx.scene.control.Button reloadButton;

    @FXML
    private javafx.scene.control.Button stratumInfoButton;

    @FXML
    private Label remainingJumpsLabel;

    @FXML
    private HBox routeParamsContainer;

    @FXML
    private TextField destinationSystemField;

    @FXML
    private TextField maxSystemsField;

    @FXML
    private javafx.scene.control.Button overlayButton;

    private ComboBox<ExplorationMode> modeComboBox; // Référence au ComboBox pour les mises à jour de traduction
    private NavRouteOverlayComponent navRouteOverlayComponent;

    private final NavRouteRegistry navRouteRegistry = NavRouteRegistry.getInstance();
    private final NavRouteTargetRegistry navRouteTargetRegistry = NavRouteTargetRegistry.getInstance();
    private final ExplorationModeRegistry explorationModeRegistry = ExplorationModeRegistry.getInstance();
    private ExplorationMode currentMode = ExplorationMode.FREE_EXPLORATION;
    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();
    private final AnalyticsService analyticsService = AnalyticsService.getInstance();
    private final CopyClipboardManager copyClipboardManager = CopyClipboardManager.getInstance();
    private final PopupManager popupManager = PopupManager.getInstance();
    private final LocalizationService localizationService = LocalizationService.getInstance();
    private final be.mirooz.elitedangerous.dashboard.service.PreferencesService preferencesService =
        be.mirooz.elitedangerous.dashboard.service.PreferencesService.getInstance();
    private ChangeListener<String> currentSystemListener;
    private ChangeListener<Number> widthListener;
    private final Set<String> visitedSystems = new HashSet<>(); // Systèmes visités dans la route actuelle
    private String lastCopiedSystemName = null; // Nom du dernier système copié dans le clipboard

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialiser le registre avec le mode par défaut
        explorationModeRegistry.setCurrentMode(ExplorationMode.FREE_EXPLORATION);

        // Charger les systèmes visités depuis les préférences
        loadVisitedSystemsFromPreferences();

        // Initialiser le sélecteur de mode
        initializeModeSelector();
        
        // Initialiser les champs de paramètres de route
        initializeRouteParams();
        
        // Initialiser la visibilité des champs de paramètres selon le mode actuel
        updateRouteParamsVisibility();

        // Initialiser l'overlay component
        navRouteOverlayComponent = new NavRouteOverlayComponent();
        navRouteOverlayComponent.setNavRouteComponent(this); // Passer la référence pour dessiner les boules
        navRouteOverlayComponent.setOnOverlayStateChanged(this::updateOverlayButtonText);
        
        // Mettre à jour le texte du bouton overlay
        updateOverlayButtonText();

        // Écouter les changements de route
        navRouteRegistry.getCurrentRouteProperty().addListener((obs, oldRoute, newRoute) -> {
            Platform.runLater(() -> {
                // Ne pas réinitialiser les systèmes visités - on les maintient même lors du changement de route
                // Les systèmes visités sont sauvegardés dans les préférences et chargés au démarrage
                // Vérifier que la route correspond au mode actuel avant de l'afficher
                if (newRoute != null) {
                    updateRouteDisplay(newRoute);
                }
            });
        });

        // Écouter les changements du système actuel via CommanderStatusComponent
        be.mirooz.elitedangerous.dashboard.view.common.CommanderStatusComponent statusComponent =
            be.mirooz.elitedangerous.dashboard.view.common.CommanderStatusComponent.getInstance();
        currentSystemListener = (obs, oldSystem, newSystem) -> {
            Platform.runLater(() -> {
                // Marquer le système précédent (oldSystem) comme visité s'il était dans la route
                // (basé sur FSDJumpHandler qui met à jour CommanderStatus quand on arrive dans un nouveau système)
                if (oldSystem != null && !oldSystem.isEmpty() && newSystem != null && !newSystem.isEmpty()) {
                    NavRoute route = navRouteRegistry.getCurrentRoute();
                    if (route != null && route.getRoute() != null) {
                        for (RouteSystem routeSystem : route.getRoute()) {
                            if (routeSystem.getSystemName().equals(oldSystem)) {
                                visitedSystems.add(oldSystem);
                                System.out.println("✅ Système marqué comme visité: " + oldSystem);
                                // Sauvegarder dans les préférences
                                saveVisitedSystemsToPreferences();
                                break;
                            }
                        }
                    }
                }

                // En mode nécessitant l'API Spansh, copier automatiquement le système suivant dans le clipboard
                if (currentMode != null && currentMode.requiresSpanshApi() && newSystem != null && !newSystem.isEmpty()) {
                    copyNextSystemAndShowPopup(newSystem);
                }

                // Mettre à jour l'affichage
                NavRoute route = navRouteRegistry.getCurrentRoute();
                if (route != null) {
                    updateRouteDisplay(route);
                }
            });
        };
        statusComponent.getCurrentStarSystem().addListener(currentSystemListener);
        
        // Écouter les changements de RemainingJumpsInRoute
        navRouteTargetRegistry.getRemainingJumpsInRouteProperty().addListener((obs, oldValue, newValue) -> {
            Platform.runLater(() -> {
                updateRemainingJumpsLabel(newValue.intValue());
            });
        });
        
        // Écouter les changements de largeur du conteneur pour recalculer l'espacement
        if (navRouteContainer != null) {
            widthListener = (obs, oldWidth, newWidth) -> {
                Platform.runLater(() -> {
                    NavRoute route = navRouteRegistry.getCurrentRoute();
                    if (route != null) {
                        updateRouteDisplay(route);
                    }
                });
            };
            navRouteContainer.widthProperty().addListener(widthListener);
        }

        // Initialiser le label avec la valeur actuelle
        // Utiliser Platform.runLater pour s'assurer que le label est bien injecté
        Platform.runLater(() -> {
            updateRemainingJumpsLabel(navRouteTargetRegistry.getRemainingJumpsInRoute());
        });

        // S'abonner au service de notification pour le refresh de la route
        NavRouteNotificationService.getInstance().addListener(this::refreshRouteDisplay);

        // Écouter les changements de langue
        localizationService.addLanguageChangeListener(locale -> {
            Platform.runLater(() -> {
                updateTranslations();
            });
        });

        // Initialiser les traductions
        updateTranslations();

        // Afficher la route actuelle si elle existe
        updateRouteDisplay(navRouteRegistry.getCurrentRoute());
        
        // Écouter les changements d'état "à pied"
        ExplorationRefreshNotificationService.getInstance().addOnFootStateListener(this::handleOnFootStateChanged);
    }

    
    /**
     * Gère le changement d'état "à pied" pour fermer/rouvrir l'overlay
     */
    private void handleOnFootStateChanged(boolean isOnFoot) {
        Platform.runLater(() -> {
            if (navRouteOverlayComponent == null) {
                return;
            }
            
            if (isOnFoot) {
                // Si on est à pied, fermer l'overlay s'il est ouvert
                if (navRouteOverlayComponent.isShowing()) {
                    navRouteOverlayComponent.setWasOpenBeforeOnFoot(true);
                    navRouteOverlayComponent.closeOverlay();
                } else {
                    navRouteOverlayComponent.setWasOpenBeforeOnFoot(false);
                }
            } else {
                // Si on n'est plus à pied, rouvrir l'overlay s'il était ouvert avant
                if (navRouteOverlayComponent.wasOpenBeforeOnFoot()) {
                    navRouteOverlayComponent.showOverlayWithoutBordered();
                    navRouteOverlayComponent.setWasOpenBeforeOnFoot(false);
                }
            }
            
            updateOverlayButtonText();
        });
    }

    /**
     * Méthode appelée par le service de notification pour rafraîchir l'affichage de la route
     */
    private void refreshRouteDisplay() {
        Platform.runLater(() -> {
            // Toujours rafraîchir l'affichage, même en mode Stratum
            // car on affiche des informations basées sur la route Free Exploration (nombre de sauts)
            NavRoute route = navRouteRegistry.getCurrentRoute();
            if (route != null) {
                // En mode nécessitant l'API Spansh, forcer le rafraîchissement en créant une nouvelle instance de la route
                if (currentMode != null && currentMode.requiresSpanshApi()) {
                    // Créer une nouvelle instance pour forcer le listener à se déclencher
                    NavRoute refreshedRoute = new NavRoute();
                    refreshedRoute.setTimestamp(route.getTimestamp());
                    refreshedRoute.setRoute(new java.util.ArrayList<>(route.getRoute()));
                    navRouteRegistry.setCurrentRoute(refreshedRoute);
                    // Forcer aussi un appel direct à updateRouteDisplay pour s'assurer que l'UI se met à jour
                    // (car le nombre de sauts dépend de la route Free Exploration qui vient de changer)
                    updateRouteDisplay(refreshedRoute);
                } else {
                    // En mode Free Exploration, simplement mettre à jour l'affichage
                    updateRouteDisplay(route);
                }
            } else {
                // Même si pas de route, mettre à jour l'affichage pour rafraîchir les infos Free Exploration
                updateRouteDisplay(null);
            }
            // Toujours mettre à jour le label des remaining jumps lors du rafraîchissement
            updateRemainingJumpsLabel(navRouteTargetRegistry.getRemainingJumpsInRoute());
        });
    }

    /**
     * Initialise le sélecteur de mode d'exploration
     */
    private void initializeModeSelector() {
        if (modeSelectorContainer == null) {
            return;
        }

        // Créer un ComboBox compact pour les modes avec le même style que les combobox de mission
        modeComboBox = new ComboBox<>();
        modeComboBox.getItems().addAll(ExplorationMode.values());
        modeComboBox.setValue(ExplorationMode.FREE_EXPLORATION);
        modeComboBox.getStyleClass().add("elite-combobox");

        // Afficher le nom du mode
        modeComboBox.setCellFactory(param -> new javafx.scene.control.ListCell<ExplorationMode>() {
            @Override
            protected void updateItem(ExplorationMode mode, boolean empty) {
                super.updateItem(mode, empty);
                if (empty || mode == null) {
                    setText(null);
                } else {
                    String displayNameKey = "exploration.mode." + mode.name().toLowerCase();
                    setText(localizationService.getString(displayNameKey));
                }
            }
        });

        // Afficher le nom du mode dans le bouton
        modeComboBox.setButtonCell(new javafx.scene.control.ListCell<ExplorationMode>() {
            @Override
            protected void updateItem(ExplorationMode mode, boolean empty) {
                super.updateItem(mode, empty);
                if (empty || mode == null) {
                    setText(null);
                } else {
                    String displayNameKey = "exploration.mode." + mode.name().toLowerCase();
                    setText(localizationService.getString(displayNameKey));
                }
            }
        });

        // Gérer le changement de mode
        modeComboBox.setOnAction(e -> {
            ExplorationMode selectedMode = modeComboBox.getSelectionModel().getSelectedItem();
            if (selectedMode != null && selectedMode != currentMode) {
                handleModeChange(selectedMode);
            }
        });

        // Ajouter un label "Mode:" avant le ComboBox
        Label modeLabel = new Label(localizationService.getString("nav.route.mode.label"));
        modeLabel.getStyleClass().add("filter-label");

        modeSelectorContainer.getChildren().addAll(modeLabel, modeComboBox);

        // Initialiser la description du mode par défaut
        if (modeDescriptionLabel != null) {
            updateModeDescription(ExplorationMode.FREE_EXPLORATION);
        }

        // Initialiser le bouton de rechargement
        initializeReloadButton();

        // Initialiser le bouton d'information Stratum
        initializeStratumInfoButton();
    }

    /**
     * Initialise le bouton de rechargement
     */
    private void initializeReloadButton() {
        if (reloadButton != null) {
            // Tooltip
            updateReloadButtonTooltip();

            // Visibilité selon le mode
            updateReloadButtonVisibility();
        }
    }


    /**
     * Met à jour le tooltip du bouton de rechargement
     */
    private void updateReloadButtonTooltip() {
        if (reloadButton != null) {
            TooltipComponent tooltip = new TooltipComponent(localizationService.getString("nav.route.reload.tooltip"));
            reloadButton.setTooltip(tooltip);
        }
    }

    /**
     * Met à jour la visibilité du bouton de rechargement selon le mode
     */
    private void updateReloadButtonVisibility() {
        if (reloadButton != null) {
            boolean visible = currentMode != ExplorationMode.FREE_EXPLORATION;
            reloadButton.setVisible(visible);
            reloadButton.setManaged(visible);
        }
    }

    /**
     * Initialise le bouton d'information Stratum
     */
    private void initializeStratumInfoButton() {
        if (stratumInfoButton != null) {
            updateStratumInfoButtonTooltip();

            // Visibilité selon le mode
            updateStratumInfoButtonVisibility();
        }
    }


    /**
     * Met à jour le tooltip du bouton d'information selon le mode
     */
    private void updateStratumInfoButtonTooltip() {
        if (stratumInfoButton != null) {
            String tooltipText;
            String tooltipKey;
            
            // Adapter le texte selon le mode
            if (currentMode == ExplorationMode.STRATUM_UNDISCOVERED) {
                tooltipKey = "nav.route.stratum.info";
            } else if (currentMode == ExplorationMode.EXPRESSWAY_TO_EXOMASTERY) {
                tooltipKey = "nav.route.expressway.info";
            } else if (currentMode == ExplorationMode.ROAD_TO_RICHES) {
                tooltipKey = "nav.route.road_to_riches.info";
            } else {
                tooltipKey = "nav.route.stratum.info"; // Fallback
            }
            
            tooltipText = localizationService.getString(tooltipKey);
            TooltipComponent tooltip = new TooltipComponent(tooltipText);
            tooltip.setWrapText(true);
            tooltip.setMaxWidth(400);
            // Faire en sorte que le tooltip reste affiché tant que la souris est dessus
            tooltip.setShowDuration(javafx.util.Duration.INDEFINITE);
            stratumInfoButton.setTooltip(tooltip);
        }
    }

    /**
     * Met à jour la visibilité du bouton d'information selon le mode
     */
    private void updateStratumInfoButtonVisibility() {
        if (stratumInfoButton != null) {
            boolean visible = currentMode != null && currentMode.requiresSpanshApi();
            stratumInfoButton.setVisible(visible);
            stratumInfoButton.setManaged(visible);
        }
    }

    /**
     * Initialise les champs de paramètres de route
     */
    private void initializeRouteParams() {
        if (maxSystemsField != null) {
            maxSystemsField.setText("10"); // Valeur par défaut
            // Valider que seule une valeur numérique peut être saisie
            maxSystemsField.textProperty().addListener((obs, oldValue, newValue) -> {
                if (newValue != null && !newValue.isEmpty() && !newValue.matches("\\d*")) {
                    maxSystemsField.setText(oldValue);
                }
            });
        }
    }

    /**
     * Met à jour la visibilité des champs de paramètres de route selon le mode
     */
    private void updateRouteParamsVisibility() {
        if (routeParamsContainer != null) {
            boolean visible = currentMode == ExplorationMode.EXPRESSWAY_TO_EXOMASTERY 
                || currentMode == ExplorationMode.ROAD_TO_RICHES;
            routeParamsContainer.setVisible(visible);
            routeParamsContainer.setManaged(visible);
        }
    }

    /**
     * Affiche ou ferme l'overlay de la route de navigation
     */
    @FXML
    public void showNavRouteOverlay() {
        if (navRouteOverlayComponent != null) {
            navRouteOverlayComponent.showOverlay();
            updateOverlayButtonText();
        }
    }

    /**
     * Met à jour le texte du bouton overlay selon l'état de la fenêtre
     */
    private void updateOverlayButtonText() {
        if (overlayButton != null && navRouteOverlayComponent != null) {
            boolean isOverlayShowing = navRouteOverlayComponent.isShowing();
            String text;
            String icon;

            if (isOverlayShowing) {
                text = localizationService.getString("nav.route.overlay_close");
                if (text == null || text.startsWith("nav.route.")) {
                    text = "Fermer";
                }
                icon = "✖";
            } else {
                text = localizationService.getString("nav.route.overlay_open");
                if (text == null || text.startsWith("nav.route.")) {
                    text = "Overlay";
                }
                icon = "🗔";
            }

            overlayButton.setText(icon + " " + text);
        }
    }

    /**
     * Action du bouton de rechargement
     */
    @FXML
    public void onReloadButtonClicked() {
        if (currentMode != null && currentMode.requiresSpanshApi()) {
            // Désactiver le bouton pendant la requête
            if (reloadButton != null) {
                reloadButton.setDisable(true);
            }

            // Effacer le GUID sauvegardé pour forcer une nouvelle recherche avec le système actuel
            String modeKey = "spansh.guid." + currentMode.name();
            preferencesService.setPreference(modeKey, "");

            // Recharger la route avec le système actuel
            loadSpanshRoute(false);
        }
    }

    /**
     * Charge les systèmes visités depuis les préférences
     */
    private void loadVisitedSystemsFromPreferences() {
        String modeKey = "navroute.visited." + currentMode.name();
        String visitedSystemsStr = preferencesService.getPreference(modeKey, "");
        if (visitedSystemsStr != null && !visitedSystemsStr.isEmpty()) {
            String[] systems = visitedSystemsStr.split(",");
            visitedSystems.clear();
            for (String system : systems) {
                if (system != null && !system.trim().isEmpty()) {
                    visitedSystems.add(system.trim());
                }
            }
            System.out.println("📋 Systèmes visités chargés pour le mode " + currentMode.name() + ": " + visitedSystems.size());
        }
    }

    /**
     * Sauvegarde les systèmes visités dans les préférences
     */
    private void saveVisitedSystemsToPreferences() {
        String modeKey = "navroute.visited." + currentMode.name();
        String visitedSystemsStr = String.join(",", visitedSystems);
        preferencesService.setPreference(modeKey, visitedSystemsStr);
    }

    /**
     * Met à jour la description du mode sélectionné
     */
    private void updateModeDescription(ExplorationMode mode) {
        if (modeDescriptionLabel != null && mode != null) {
            String descriptionKey = "exploration.mode." + mode.name().toLowerCase() + ".description";
            modeDescriptionLabel.setText(localizationService.getString(descriptionKey));
        }
    }

    /**
     * Récupère le mode d'exploration actuellement sélectionné
     */
    public ExplorationMode getCurrentMode() {
        return currentMode;
    }

    /**
     * Gère le changement de mode d'exploration
     */
    private void handleModeChange(ExplorationMode newMode) {
        ExplorationMode oldMode = currentMode;

        // Sauvegarder les systèmes visités de l'ancien mode
        if (oldMode != null) {
            saveVisitedSystemsToPreferences();
        }

        currentMode = newMode;
        explorationModeRegistry.setCurrentMode(newMode); // Mettre à jour le registre
        updateModeDescription(newMode);

        // Charger les systèmes visités du nouveau mode
        loadVisitedSystemsFromPreferences();

        // Mettre à jour la visibilité des boutons
        updateReloadButtonVisibility();
        updateStratumInfoButtonVisibility();
        updateRouteParamsVisibility();
        
        // Mettre à jour le tooltip du bouton d'information selon le nouveau mode
        updateStratumInfoButtonTooltip();

        if (newMode != null && newMode.requiresSpanshApi()) {
            // Ne PAS réinitialiser les systèmes visités - on les maintient même lors du rechargement
            // visitedSystems.clear(); // Commenté pour maintenir les systèmes visités
            
            // Charger la route depuis le registre (si elle existe déjà)
            NavRoute spanshRoute = navRouteRegistry.getRouteForMode(newMode);
            
            // Vérifier si un GUID est sauvegardé pour ce mode
            String modeKey = "spansh.guid." + newMode.name();
            String savedGuid = preferencesService.getPreference(modeKey, null);
            boolean hasSavedGuid = (savedGuid != null && !savedGuid.isEmpty());
            
            if (spanshRoute != null && hasSavedGuid) {
                // Afficher la route existante (rechargée depuis un GUID sauvegardé)
                Platform.runLater(() -> {
                    updateRouteDisplay(spanshRoute);
                    ShowLoadPopup();
                    // Copier automatiquement le système suivant dans le clipboard
                    String currentSystem = commanderStatus.getCurrentStarSystem();
                    if (currentSystem != null && !currentSystem.isEmpty()) {
                        copyNextSystemAndShowPopup(currentSystem);
                    }
                });
            } else {
                // Appeler le backend pour obtenir la route (nouvelle route ou route sans GUID sauvegardé)
                loadSpanshRoute(true);
            }
        } else if (newMode == ExplorationMode.FREE_EXPLORATION) {
            // Réinitialiser les systèmes visités pour la nouvelle route
            visitedSystems.clear();
            
            // Charger la route Free Exploration depuis le registre (si elle existe déjà)
            NavRoute freeRoute = navRouteRegistry.getRouteForMode(ExplorationMode.FREE_EXPLORATION);
            if (freeRoute != null) {
                // Afficher la route Free Exploration existante
                Platform.runLater(() -> {
                    updateRouteDisplay(freeRoute);
                });
            } else {
                // Toujours recharger le fichier NavRoute.json pour avoir les données les plus récentes
                be.mirooz.elitedangerous.dashboard.service.NavRouteService.getInstance().loadAndStoreNavRoute();
            }
        }
    }

    private void ShowLoadPopup() {
        Platform.runLater(() -> {
            if (modeComboBox != null && modeComboBox.getScene() != null) {
                Stage stage = (Stage) modeComboBox.getScene().getWindow();
                // Obtenir les coordonnées de la combobox dans la scène
                javafx.geometry.Bounds bounds = modeComboBox.localToScene(modeComboBox.getBoundsInLocal());
                double popupX = bounds.getMinX(); // Aligné à gauche de la combobox
                double popupY = bounds.getMaxY() + 5; // Juste en dessous de la combobox avec un petit espacement
                popupManager.showPopup(
                        localizationService.getString("nav.route.previous_route_reloaded"),
                        popupX,
                        popupY,
                        stage,
                        -1, // Largeur automatique basée sur le texte
                        3000 // Durée de 3 secondes
                );
            }
        });
    }

    /**
     * Copie automatiquement le système suivant dans le clipboard, met à jour l'affichage et affiche le popup
     * @param currentSystem Le système actuel
     */
    private void copyNextSystemAndShowPopup(String currentSystem) {
        if (currentSystem == null || currentSystem.isEmpty()) {
            return;
        }
        
        NavRoute route = navRouteRegistry.getCurrentRoute();
        if (route == null || route.getRoute() == null) {
            return;
        }
        
        // Trouver l'index du système actuel dans la route
        int currentSystemIndex = -1;
        for (int i = 0; i < route.getRoute().size(); i++) {
            if (route.getRoute().get(i).getSystemName().equals(currentSystem)) {
                currentSystemIndex = i;
                break;
            }
        }
        
        // Si le système actuel est dans la route et qu'il y a un système suivant
        if (currentSystemIndex >= 0 && currentSystemIndex < route.getRoute().size() - 1) {
            RouteSystem nextSystem = route.getRoute().get(currentSystemIndex + 1);
            if (nextSystem != null && nextSystem.getSystemName() != null && !nextSystem.getSystemName().isEmpty()) {
                String copiedSystemName = nextSystem.getSystemName();
                // Copier le système suivant dans le clipboard
                copyClipboardManager.copyToClipboard(copiedSystemName);
                lastCopiedSystemName = copiedSystemName;
                System.out.println("📋 Système suivant copié automatiquement: " + copiedSystemName);
                
                // Mettre à jour l'affichage pour afficher l'indicateur du système copié
                updateRouteDisplay(route);
                
                // Afficher le popup "System copied" sur la boule correspondante
                if (routeSystemsPane != null && routeSystemsPane.getScene() != null) {
                    Stage stage = (Stage) routeSystemsPane.getScene().getWindow();
                    showSystemCopiedPopup(copiedSystemName, -1, -1, stage);
                }
            }
        }
    }

    /**
     * Calcule la position X d'un système dans la route pour l'affichage
     * Retourne la position X, ou -1 si le système n'est pas trouvé
     */
    private double calculateSystemXPosition(NavRoute route, String systemName) {
        if (route == null || route.getRoute() == null || systemName == null || systemName.isEmpty()) {
            return -1;
        }

        int systemCount = route.getRoute().size();
        if (systemCount == 0) {
            return -1;
        }

        // Trouver l'index du système
        int systemIndex = -1;
        for (int i = 0; i < systemCount; i++) {
            if (route.getRoute().get(i).getSystemName().equals(systemName)) {
                systemIndex = i;
                break;
            }
        }
        if (systemIndex < 0) {
            return -1;
        }

        // Calculer la largeur disponible (même logique que dans updateRouteDisplay)
        double availableWidth = 800;
        if (navRouteContainer != null && navRouteContainer.getWidth() > 0) {
            availableWidth = navRouteContainer.getWidth() - PADDING_X * 2 - 30;
        } else if (routeSystemsPane != null && routeSystemsPane.getWidth() > 0) {
            availableWidth = routeSystemsPane.getWidth() - PADDING_X * 2 - 30;
        }

        // Calculer la distance totale
        double totalDistance = 0.0;
        for (int i = 1; i < systemCount; i++) {
            totalDistance += route.getRoute().get(i).getDistanceFromPrevious();
        }

        // Calculer les espacements (même logique que dans updateRouteDisplay)
        double[] spacings = new double[systemCount - 1];
        if (totalDistance <= 0 || systemCount <= 1) {
            double uniformSpacing = systemCount <= 1 ? MAX_SPACING :
                Math.max(MIN_SPACING, Math.min(MAX_SPACING, (availableWidth - PADDING_X * 2) / (systemCount - 1)));
            for (int i = 0; i < systemCount - 1; i++) {
                spacings[i] = uniformSpacing;
            }
        } else {
            double scaleFactor = (availableWidth - PADDING_X * 2) / totalDistance;
            for (int i = 0; i < systemCount - 1; i++) {
                double distance = route.getRoute().get(i + 1).getDistanceFromPrevious();
                double proportionalSpacing = distance * scaleFactor;
                spacings[i] = Math.max(MIN_SPACING, Math.min(MAX_SPACING, proportionalSpacing));
            }

            // Ajuster si nécessaire
            double totalSpacing = 0;
            for (double spacing : spacings) {
                totalSpacing += spacing;
            }
            if (totalSpacing > availableWidth - PADDING_X * 2) {
                double adjustmentFactor = (availableWidth - PADDING_X * 2) / totalSpacing;
                for (int i = 0; i < spacings.length; i++) {
                    spacings[i] *= adjustmentFactor;
                    if (spacings[i] < MIN_SPACING) {
                        spacings[i] = MIN_SPACING;
                    }
                }
            }
        }

        // Calculer la position X du système
        double x = PADDING_X;
        for (int i = 0; i < systemIndex; i++) {
            x += spacings[i];
        }

        return x;
    }

    /**
     * Charge la route depuis Spansh pour le mode actuel (générique pour tous les modes nécessitant l'API Spansh)
     */
    private void loadSpanshRoute(boolean loadPopup) {
        // Afficher l'indicateur de chargement
        setLoadingVisible(true);

        // Exécuter dans un thread séparé pour ne pas bloquer l'UI
        new Thread(() -> {
            try {
                // Vérifier que le mode actuel nécessite l'API Spansh
                if (currentMode == null || !currentMode.requiresSpanshApi() || currentMode.getSpanshEndpoint() == null) {
                    Platform.runLater(() -> {
                        System.err.println("⚠️ Le mode actuel ne nécessite pas l'API Spansh");
                        setLoadingVisible(false);
                        if (reloadButton != null) {
                            reloadButton.setDisable(false);
                        }
                    });
                    return;
                }

                // Obtenir le système actuel comme référence
                String currentSystem = commanderStatus.getCurrentStarSystem();
                if (currentSystem == null || currentSystem.isEmpty()) {
                    Platform.runLater(() -> {
                        System.err.println("⚠️ Impossible de charger la route " + currentMode.name() + " : système actuel inconnu");
                        setLoadingVisible(false);
                        if (reloadButton != null) {
                            reloadButton.setDisable(false);
                        }
                    });
                    return;
                }

                // Charger le GUID depuis les préférences pour ce mode
                String modeKey = "spansh.guid." + currentMode.name();
                String savedGuid = null;
                // Toujours utiliser le GUID sauvegardé s'il existe (sauvegarde par défaut)
                savedGuid = preferencesService.getPreference(modeKey, null);
                boolean useSavedGuid = (savedGuid != null && !savedGuid.isEmpty());
                
                // Variable pour savoir si on doit afficher le popup (uniquement si on recharge une route existante)
                final boolean shouldShowPopup = useSavedGuid;

                String endpoint = currentMode.getSpanshEndpoint();
                boolean requiresMaxJumpRange = currentMode == ExplorationMode.EXPRESSWAY_TO_EXOMASTERY 
                    || currentMode == ExplorationMode.ROAD_TO_RICHES;

                final NavRoute[] spanshRouteRef = new NavRoute[1]; // Utiliser un tableau pour rendre la variable effectively final
                String savedJobGuid = null;

                if (useSavedGuid) {
                    // Si on a un GUID et que la checkbox est cochée, faire un GET (reprise en cours de route)
                    System.out.println("📋 Utilisation du GUID sauvegardé pour le mode " + currentMode.name() + ": " + savedGuid);
                    
                    try {
                        if (requiresMaxJumpRange) {
                            // Pour les routes (expressway-to-exomastery et road-to-riches), utiliser /api/spansh/search/{guid}
                            SpanshRouteResultsResponseDTO routeResults = analyticsService.getSpanshRouteResultsByGuid(savedGuid);
                            spanshRouteRef[0] = buildRouteFromSpanshRouteResults(routeResults, currentSystem);
                            savedJobGuid = savedGuid;
                        } else {
                            // Pour les recherches, utiliser getSpanshSearchByGuidAndEndpoint
                            SpanshSearchResponseDTO responseDTO = analyticsService.getSpanshSearchByGuidAndEndpoint(endpoint, savedGuid);
                            spanshRouteRef[0] = buildRouteFromSpanshResponse(responseDTO, currentSystem, false);
                        }
                    } catch (SpanshGuidExpiredException e) {
                        // Le GUID a expiré, réinitialiser et faire une nouvelle demande
                        System.out.println("⚠️ GUID expiré, réinitialisation et nouvelle demande pour le mode " + currentMode.name());
                        preferencesService.removePreference(modeKey); // Effacer le GUID invalide
                        useSavedGuid = false; // Forcer une nouvelle demande
                        savedGuid = null; // Réinitialiser le GUID
                        // Le code continuera dans le bloc if (!useSavedGuid) ci-dessous pour faire une nouvelle demande POST
                    }
                }
                
                // Si useSavedGuid est false (soit initialement, soit après expiration du GUID), faire une nouvelle demande POST
                if (!useSavedGuid) {
                    // Faire un POST normal (nouveau call)
                    System.out.println("🆕 Création d'une nouvelle recherche Spansh pour le mode " + currentMode.name() + " (endpoint: " + endpoint + ")");
                    
                    if (requiresMaxJumpRange) {
                        // Récupérer la portée maximale de saut depuis le vaisseau
                        Double maxJumpRange = null;
                        if (commanderStatus.getShip() != null) {
                            maxJumpRange = commanderStatus.getShip().getMaxRange();
                        }
                        
                        if (maxJumpRange == null || maxJumpRange <= 0) {
                            Platform.runLater(() -> {
                                System.err.println("⚠️ Impossible de charger la route " + currentMode.name() + " : portée de saut inconnue");
                                setLoadingVisible(false);
                                if (reloadButton != null) {
                                    reloadButton.setDisable(false);
                                }
                            });
                            return;
                        }
                        
                        // Pour les routes : POST retourne un wrapper avec searchReference et spanshResponse
                        // Récupérer les paramètres optionnels depuis l'UI
                        String destinationSystem = null;
                        Integer maxSystems = 10; // Valeur par défaut
                        
                        if (destinationSystemField != null && destinationSystemField.getText() != null 
                            && !destinationSystemField.getText().trim().isEmpty()) {
                            destinationSystem = destinationSystemField.getText().trim();
                        }
                        
                        if (maxSystemsField != null && maxSystemsField.getText() != null 
                            && !maxSystemsField.getText().trim().isEmpty()) {
                            try {
                                maxSystems = Integer.parseInt(maxSystemsField.getText().trim());
                                if (maxSystems <= 0) {
                                    maxSystems = 10; // Valeur par défaut si invalide
                                }
                            } catch (NumberFormatException e) {
                                maxSystems = 10; // Valeur par défaut si parsing échoue
                            }
                        }
                        
                        SpanshRouteRequestDTO routeRequestDTO = new SpanshRouteRequestDTO(
                            maxJumpRange, 
                            currentSystem, 
                            destinationSystem, 
                            maxSystems,
                            commanderStatus.getCommanderName()
                        );
                        System.out.println("📤 Envoi de la requête route : maxJumpRange=" + maxJumpRange 
                            + ", systemName=" + currentSystem 
                            + ", destinationSystem=" + (destinationSystem != null ? destinationSystem : "null")
                            + ", maxSystems=" + maxSystems);
                        
                        SpanshRouteResponseDTO routeResponse = analyticsService.searchSpanshRouteByEndpoint(endpoint, routeRequestDTO);
                        
                        if (routeResponse != null && routeResponse.getSpanshResponse() != null) {
                            SpanshRouteResultsResponseDTO routeResults = routeResponse.getSpanshResponse();
                            
                            System.out.println("📊 Résultats de route reçus : " + (routeResults.result != null ? routeResults.result.size() : 0) + " systèmes");
                            System.out.println("📊 État de la route : " + routeResults.state + ", Statut : " + routeResults.status);
                            
                            // Sauvegarder le searchReference ou le job
                            if (routeResponse.getSearchReference() != null && !routeResponse.getSearchReference().isEmpty()) {
                                savedJobGuid = routeResponse.getSearchReference();
                                System.out.println("💾 SearchReference reçu : " + savedJobGuid);
                            } else if (routeResults.job != null && !routeResults.job.isEmpty()) {
                                savedJobGuid = routeResults.job;
                                System.out.println("💾 Job reçu : " + savedJobGuid);
                            }
                            
                            if (routeResults.result != null && !routeResults.result.isEmpty()) {
                                System.out.println("📋 Premier système : " + routeResults.result.get(0).name);
                            }
                            
                            spanshRouteRef[0] = buildRouteFromSpanshRouteResults(routeResults, currentSystem);
                            
                            if (spanshRouteRef[0] != null) {
                                System.out.println("✅ Route construite avec " + spanshRouteRef[0].getRoute().size() + " systèmes");
                            } else {
                                System.err.println("❌ Échec de construction de la route");
                            }
                            
                            // Sauvegarder le GUID (searchReference ou job) si disponible
                            if (savedJobGuid != null && !savedJobGuid.isEmpty()) {
                                preferencesService.setPreference(modeKey, savedJobGuid);
                                System.out.println("💾 GUID sauvegardé pour le mode " + currentMode.name() + ": " + savedJobGuid);
                            }
                        } else {
                            System.err.println("❌ Réponse de route est null ou spanshResponse est null");
                        }
                    } else {
                        // Pour stratum-undiscovered, utiliser le DTO simple
                        SpanshSearchRequestDTO requestDTO = new SpanshSearchRequestDTO(
                            currentSystem,
                            commanderStatus.getCommanderName()
                        );
                        SpanshSearchResponseDTO responseDTO = analyticsService.searchSpanshByEndpoint(endpoint, requestDTO);
                        spanshRouteRef[0] = buildRouteFromSpanshResponse(responseDTO, currentSystem, true);
                        
                        // Sauvegarder le GUID reçu
                        if (responseDTO != null && responseDTO.getSearchReference() != null && !responseDTO.getSearchReference().isEmpty()) {
                            preferencesService.setPreference(modeKey, responseDTO.getSearchReference());
                            System.out.println("💾 GUID sauvegardé pour le mode " + currentMode.name() + ": " + responseDTO.getSearchReference());
                        }
                    }
                }

                // Sauvegarder les systèmes visités avant de reconstruire la route
                Set<String> previousVisitedSystems = new HashSet<>(visitedSystems);
                
                final NavRoute spanshRoute = spanshRouteRef[0]; // Variable final pour utilisation dans lambda

                // Restaurer les systèmes visités après reconstruction
                visitedSystems.clear();
                visitedSystems.addAll(previousVisitedSystems);

                // Mettre à jour le registre sur le thread JavaFX
                Platform.runLater(() -> {
                    setLoadingVisible(false);
                    // Réactiver le bouton
                    if (reloadButton != null) {
                        reloadButton.setDisable(false);
                    }
                    if (spanshRoute != null && spanshRoute.getRoute() != null && !spanshRoute.getRoute().isEmpty()) {
                        // Sauvegarder la route dans le registre pour le mode actuel
                        navRouteRegistry.setRouteForMode(spanshRoute, currentMode);
                        System.out.println("✅ Route " + currentMode.name() + " chargée : " + spanshRoute.getRoute().size() + " systèmes");
                        // Forcer la mise à jour de l'affichage pour prendre en compte les systèmes visités
                        updateRouteDisplay(spanshRoute);
                        // Copier automatiquement le système suivant dans le clipboard
                        String currentSystemForCopy = commanderStatus.getCurrentStarSystem();
                        if (currentSystemForCopy != null && !currentSystemForCopy.isEmpty()) {
                            copyNextSystemAndShowPopup(currentSystemForCopy);
                        }
                    } else {
                        if (spanshRoute == null) {
                            System.err.println("⚠️ Aucune route " + currentMode.name() + " trouvée (route est null)");
                        } else if (spanshRoute.getRoute() == null) {
                            System.err.println("⚠️ Aucune route " + currentMode.name() + " trouvée (liste de route est null)");
                        } else {
                            System.err.println("⚠️ Aucune route " + currentMode.name() + " trouvée (liste de route est vide, " + spanshRoute.getRoute().size() + " systèmes)");
                        }
                    }
                });
                // Afficher le popup uniquement si on a rechargé une route existante (GUID sauvegardé)
                if (loadPopup && shouldShowPopup) {
                    ShowLoadPopup();
                }

            } catch (Exception e) {
                System.err.println("❌ Erreur lors du chargement de la route " + currentMode.name() + ": " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> {
                    setLoadingVisible(false);
                    // Réactiver le bouton en cas d'erreur
                    if (reloadButton != null) {
                        reloadButton.setDisable(false);
                    }
                    updateRouteDisplay(null);
                    // En cas d'erreur, la route reste dans le registre (si elle existait)
                    // On ne fait rien, l'utilisateur peut recharger manuellement
                });
            }
        }).start();
    }

    /**
     * Gère la visibilité de l'indicateur de chargement
     */
    private void setLoadingVisible(boolean visible) {
        Platform.runLater(() -> {
            if (loadingIndicator != null) {
                loadingIndicator.setVisible(visible);
                loadingIndicator.setManaged(visible);
            }

            // Cacher le contenu du panel pendant le chargement
            if (routeSystemsPane != null) {
                routeSystemsPane.setVisible(!visible);
                routeSystemsPane.setManaged(!visible);
            }
        });
    }

    /**
     * Construit une NavRoute à partir de la réponse Spansh
     * @param responseDTO La réponse Spansh
     * @param currentSystemName Le système de référence (système actuel lors du call)
     * @param isNewCall true si c'est un nouveau call (POST), false si c'est un rechargement (GET avec GUID)
     */
    private NavRoute buildRouteFromSpanshResponse(SpanshSearchResponseDTO responseDTO, String currentSystemName, boolean isNewCall) {
        if (responseDTO == null || responseDTO.getSpanshResponse() == null) {
            return null;
        }

        SpanshSearchResponse spanshResponse = responseDTO.getSpanshResponse();
        if (spanshResponse.results == null || spanshResponse.results.isEmpty()) {
            return null;
        }

        NavRoute route = new NavRoute();
        route.setTimestamp(java.time.Instant.now().toString());

        List<RouteSystem> routeSystems = new ArrayList<>();

        // Grouper les résultats par système pour éviter les doublons et collecter toutes les infos
        Map<String, List<SpanshSearchResponse.BodyResult>> systemsMap = spanshResponse.results.stream()
            .collect(Collectors.groupingBy(result -> result.system_name));

        // Créer une map pour stocker la classe d'étoile principale de chaque système
        Map<String, String> systemStarClassMap = new HashMap<>();

        // Pour chaque système, trouver l'étoile principale
        for (Map.Entry<String, List<SpanshSearchResponse.BodyResult>> entry : systemsMap.entrySet()) {
            String systemName = entry.getKey();
            List<SpanshSearchResponse.BodyResult> systemResults = entry.getValue();

            // Chercher l'étoile principale dans les résultats du système
            String starClass = findMainStarClass(systemResults);
            if (starClass != null && !starClass.isEmpty()) {
                systemStarClassMap.put(systemName, starClass);
            }
        }

        // Trier les systèmes par distance (prendre le premier résultat de chaque système pour la distance)
        List<SpanshSearchResponse.BodyResult> sortedResults = systemsMap.values().stream()
            .map(results -> results.get(0)) // Prendre le premier résultat de chaque système
            .sorted(Comparator.comparingDouble(result -> result.distance))
            .collect(Collectors.toList());

        // Déterminer le système de référence à utiliser
        String referenceSystemName = currentSystemName;
        double[] referencePosition = null;
        long referenceId64 = 0;

        if (!isNewCall && spanshResponse.reference != null && spanshResponse.reference.name != null) {
            // Lors d'un rechargement avec GUID, utiliser le système de référence depuis la réponse
            referenceSystemName = spanshResponse.reference.name;
            referenceId64 = spanshResponse.reference.id64;
            referencePosition = new double[]{
                spanshResponse.reference.x,
                spanshResponse.reference.y,
                spanshResponse.reference.z
            };
        }

        // Ajouter le système de référence en premier (pour nouveau call ET rechargement avec GUID)
        double[] previousPosition = null;
        RouteSystem referenceSystem = new RouteSystem();
        referenceSystem.setSystemName(referenceSystemName);
        referenceSystem.setSystemAddress(referenceId64);
        referenceSystem.setStarClass(""); // On n'a pas la classe d'étoile du système de référence
        if (referencePosition != null) {
            referenceSystem.setStarPos(referencePosition);
            previousPosition = referencePosition;
        } else {
            referenceSystem.setStarPos(new double[]{0, 0, 0}); // Position par défaut
        }
        referenceSystem.setDistanceFromPrevious(0.0);
        routeSystems.add(referenceSystem);

        // Ajouter les systèmes de la réponse Spansh (en excluant le système de référence s'il est présent)
        for (SpanshSearchResponse.BodyResult result : sortedResults) {
            // Ne pas ajouter le système de référence s'il est déjà dans la route
            if (result.system_name.equals(referenceSystemName)) {
                continue;
            }

            RouteSystem routeSystem = new RouteSystem();
            routeSystem.setSystemName(result.system_name);
            routeSystem.setSystemAddress(result.system_id64);

            // Récupérer la classe d'étoile depuis la map
            String starClass = systemStarClassMap.get(result.system_name);
            routeSystem.setStarClass(starClass != null ? starClass : "");

            // Position du système
            double[] starPos = new double[]{
                result.system_x,
                result.system_y,
                result.system_z
            };
            routeSystem.setStarPos(starPos);

            // Calculer la distance depuis le système précédent
            double distance = 0.0;
            if (previousPosition != null) {
                // Calculer la distance depuis le système précédent (système de référence ou système précédent)
                distance = calculateDistance(previousPosition, starPos);
            } else {
                // Si pas de position précédente, utiliser la distance depuis Spansh
                distance = result.distance;
            }
            routeSystem.setDistanceFromPrevious(distance);

            // Mettre à jour la position précédente pour le prochain système
            previousPosition = starPos;

            routeSystems.add(routeSystem);
            previousPosition = starPos;
        }

        route.setRoute(routeSystems);
        return route;
    }

    /**
     * Construit une NavRoute à partir de la réponse de résultats de route Spansh (pour expressway-to-exomastery et road-to-riches)
     * Le premier système dans result est le système de référence (source), il doit être inclus comme premier système de la route
     * @param routeResults La réponse de résultats de route Spansh
     * @param currentSystemName Le système de référence (système actuel lors du call)
     */
    private NavRoute buildRouteFromSpanshRouteResults(SpanshRouteResultsResponseDTO routeResults, String currentSystemName) {
        if (routeResults == null) {
            System.err.println("❌ buildRouteFromSpanshRouteResults: routeResults est null");
            return null;
        }
        
        if (routeResults.result == null) {
            System.err.println("❌ buildRouteFromSpanshRouteResults: result est null");
            return null;
        }
        
        if (routeResults.result.isEmpty()) {
            System.err.println("❌ buildRouteFromSpanshRouteResults: result est vide");
            return null;
        }

        System.out.println("🔧 Construction de la route depuis " + routeResults.result.size() + " systèmes (système de référence: " + currentSystemName + ")");

        NavRoute route = new NavRoute();
        route.setTimestamp(java.time.Instant.now().toString());

        List<RouteSystem> routeSystems = new ArrayList<>();
        double[] previousPosition = null;

        // Conserver l'ordre des systèmes tel que reçu dans le JSON (pas de tri)
        // Ajouter tous les systèmes de la route dans l'ordre du JSON
        for (SpanshRouteResultsResponseDTO.SystemResult systemResult : routeResults.result) {
            if (systemResult.name == null || systemResult.name.isEmpty()) {
                System.out.println("⏭️ Système ignoré (nom vide)");
                continue;
            }

            RouteSystem routeSystem = new RouteSystem();
            routeSystem.setSystemName(systemResult.name);
            
            // Convertir id64 en long si possible
            try {
                long systemId64 = Long.parseLong(systemResult.id64);
                routeSystem.setSystemAddress(systemId64);
            } catch (NumberFormatException e) {
                // Si la conversion échoue, utiliser 0
                routeSystem.setSystemAddress(0L);
            }

            // Pour les routes, on n'a pas d'information sur la classe d'étoile
            routeSystem.setStarClass("");

            // Position du système
            double[] starPos = new double[]{
                systemResult.x,
                systemResult.y,
                systemResult.z
            };
            routeSystem.setStarPos(starPos);

            // Calculer la distance depuis le système précédent
            double distance = 0.0;
            if (previousPosition != null) {
                distance = calculateDistance(previousPosition, starPos);
            }
            routeSystem.setDistanceFromPrevious(distance);

            // Mettre à jour la position précédente pour le prochain système
            previousPosition = starPos;

            routeSystems.add(routeSystem);
        }

        System.out.println("✅ Route construite : " + routeSystems.size() + " systèmes ajoutés (premier système: " + (routeSystems.isEmpty() ? "aucun" : routeSystems.get(0).getSystemName()) + ")");

        route.setRoute(routeSystems);
        return route;
    }

    /**
     * Trouve la classe de l'étoile principale d'un système à partir des résultats Spansh
     * Cherche dans les parents des bodies pour trouver l'étoile principale
     */
    private String findMainStarClass(List<SpanshSearchResponse.BodyResult> systemResults) {
        if (systemResults == null || systemResults.isEmpty()) {
            return null;
        }

        // Chercher d'abord si un body est une étoile principale (is_main_star = true)
        for (SpanshSearchResponse.BodyResult result : systemResults) {
            if (result.is_main_star != null && result.is_main_star && "Star".equals(result.type)) {
                // Extraire la première lettre du subtype (ex: "K (Yellow-Orange) Star" -> "K")
                return extractStarClassFromSubtype(result.subtype);
            }
        }

        // Sinon, chercher dans les parents pour trouver une étoile principale
        for (SpanshSearchResponse.BodyResult result : systemResults) {
            if (result.parents != null && !result.parents.isEmpty()) {
                for (SpanshSearchResponse.Parent parent : result.parents) {
                    if ("Star".equals(parent.type)) {
                        // Extraire la première lettre du subtype
                        return extractStarClassFromSubtype(parent.subtype);
                    }
                }
            }
        }

        return null;
    }

    /**
     * Extrait la classe d'étoile depuis le subtype Spansh
     * Ex: "K (Yellow-Orange) Star" -> "K"
     * Ex: "Neutron Star" -> "N"
     * Ex: "DA White Dwarf" -> "D"
     */
    private String extractStarClassFromSubtype(String subtype) {
        if (subtype == null || subtype.isEmpty()) {
            return "";
        }

        // Pour les naines blanches, chercher "White Dwarf" ou "Dwarf"
        if (subtype.contains("White Dwarf") || subtype.contains("Dwarf")) {
            return "D";
        }

        // Pour les étoiles à neutrons
        if (subtype.contains("Neutron")) {
            return "N";
        }

        // Pour les autres étoiles, prendre la première lettre
        // Ex: "K (Yellow-Orange) Star" -> "K"
        String trimmed = subtype.trim();
        if (!trimmed.isEmpty()) {
            String firstChar = trimmed.substring(0, 1).toUpperCase();
            // Vérifier que c'est une lettre valide (K, G, B, F, O, A, M, etc.)
            if (firstChar.matches("[A-Z]")) {
                return firstChar;
            }
        }

        return "";
    }

    /**
     * Calcule la distance en années-lumière entre deux positions 3D
     */
    private double calculateDistance(double[] pos1, double[] pos2) {
        if (pos1 == null || pos2 == null || pos1.length != 3 || pos2.length != 3) {
            return 0.0;
        }

        double dx = pos2[0] - pos1[0];
        double dy = pos2[1] - pos1[1];
        double dz = pos2[2] - pos1[2];

        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Dessine les boules de la route dans un Pane donné (méthode publique pour l'overlay)
     * @param targetPane Le Pane où dessiner les boules
     * @param route La route à afficher
     * @param availableWidth La largeur disponible pour le dessin
     */
    public void drawRouteSystems(javafx.scene.layout.Pane targetPane, NavRoute route, double availableWidth) {
        if (targetPane == null) {
            return;
        }
        
        // Vider le conteneur
        targetPane.getChildren().clear();

        if (route == null || route.getRoute() == null || route.getRoute().isEmpty()) {
            return;
        }

        int systemCount = route.getRoute().size();
        if (systemCount == 0) {
            return;
        }

        // Obtenir le système actuel
        String currentSystemName = commanderStatus.getCurrentStarSystem();

        // Trouver l'index du système actuel dans la route
        int currentSystemIndex = -1;
        for (int i = 0; i < systemCount; i++) {
            if (route.getRoute().get(i).getSystemName().equals(currentSystemName)) {
                currentSystemIndex = i;
                break;
            }
        }

        // Calculer la distance totale de la route
        double totalDistance = 0.0;
        for (int i = 1; i < systemCount; i++) {
            totalDistance += route.getRoute().get(i).getDistanceFromPrevious();
        }

        // Si pas de distance ou distance nulle, utiliser un espacement uniforme
        double[] spacings = new double[systemCount - 1];
        if (totalDistance <= 0 || systemCount <= 1) {
            // Espacement uniforme
            double uniformSpacing = systemCount <= 1 ? MAX_SPACING :
                Math.max(MIN_SPACING, Math.min(MAX_SPACING, (availableWidth - PADDING_X * 2) / (systemCount - 1)));
            for (int i = 0; i < systemCount - 1; i++) {
                spacings[i] = uniformSpacing;
            }
        } else {
            // Calculer les espacements proportionnels aux distances
            double scaleFactor = (availableWidth - PADDING_X * 2) / totalDistance;

            // Appliquer des limites min/max pour chaque espacement
            for (int i = 0; i < systemCount - 1; i++) {
                double distance = route.getRoute().get(i + 1).getDistanceFromPrevious();
                double proportionalSpacing = distance * scaleFactor;
                spacings[i] = Math.max(MIN_SPACING, Math.min(MAX_SPACING, proportionalSpacing));
            }

            // Ajuster si la somme dépasse la largeur disponible
            double totalSpacing = 0;
            for (double spacing : spacings) {
                totalSpacing += spacing;
            }

            if (totalSpacing > availableWidth - PADDING_X * 2) {
                double adjustmentFactor = (availableWidth - PADDING_X * 2) / totalSpacing;
                for (int i = 0; i < spacings.length; i++) {
                    spacings[i] *= adjustmentFactor;
                    if (spacings[i] < MIN_SPACING) {
                        spacings[i] = MIN_SPACING;
                    }
                }
            }
        }

        // Calculer la largeur totale nécessaire
        double totalWidth = PADDING_X * 2;
        for (double spacing : spacings) {
            totalWidth += spacing;
        }
        targetPane.setPrefWidth(totalWidth);
        targetPane.setMinHeight(LINE_HEIGHT);
        targetPane.setPrefHeight(LINE_HEIGHT);

        double centerY = LINE_HEIGHT / 2;

        // Calculer la taille des cercles en fonction du nombre de systèmes
        double circleRadius = calculateCircleRadius(systemCount, false);
        double currentCircleRadius = calculateCircleRadius(systemCount, true);

        // Dessiner les systèmes et les lignes
        double currentX = PADDING_X;
        for (int i = 0; i < systemCount; i++) {
            RouteSystem system = route.getRoute().get(i);
            double x = currentX;

            boolean isCurrent = (i == currentSystemIndex);
            boolean isVisited = visitedSystems.contains(system.getSystemName()) ||
                               (currentSystemIndex >= 0 && i < currentSystemIndex);

            // Dessiner la ligne vers le système suivant (sauf pour le dernier)
            if (i < systemCount - 1) {
                RouteSystem nextSystem = route.getRoute().get(i + 1);
                double nextX = currentX + spacings[i];
                boolean hasBoost = isBoostStar(system.getStarClass());
                Line line = createLine(x, centerY, nextX, centerY, nextSystem, isVisited, hasBoost);
                targetPane.getChildren().add(line);
                currentX = nextX;
            }

            // Dessiner le cercle pour le système
            double radius = isCurrent ? currentCircleRadius : circleRadius;
            boolean isLastCopied = system.getSystemName().equals(lastCopiedSystemName);
            
            if (isLastCopied) {
                Circle copiedIndicator = createCopiedIndicator(x, centerY, radius);
                targetPane.getChildren().add(copiedIndicator);
            }
            
            Circle circle = createCircle(x, centerY, system, isCurrent, isVisited, radius, isLastCopied);
            targetPane.getChildren().add(circle);

            // Ajouter l'indicateur scoopable si applicable
            if (isScoopable(system.getStarClass())) {
                Text scoopIndicator = createScoopIndicator(x, centerY, radius);
                targetPane.getChildren().add(scoopIndicator);
            }
        }
    }

    /**
     * Met à jour l'affichage de la route
     */
    private void updateRouteDisplay(NavRoute route) {
        // Toujours afficher le composant
        if (navRouteContainer != null) {
            navRouteContainer.setVisible(true);
            navRouteContainer.setManaged(true);
        }

        if (route == null || route.getRoute() == null || route.getRoute().isEmpty()) {
            // Afficher un message si pas de route
            if (routeTitleLabel != null) {
                routeTitleLabel.setText(localizationService.getString("nav.route.title.no_route"));
            }
            if (routeSystemsPane != null) {
                routeSystemsPane.getChildren().clear();
            }
            return;
        }

        // Mettre à jour le titre
        if (routeTitleLabel != null) {
            routeTitleLabel.setText(localizationService.getString("nav.route.title.with_systems", route.getRoute().size()));
        }

        // Vider le conteneur des systèmes
        if (routeSystemsPane != null) {
            routeSystemsPane.getChildren().clear();

            int systemCount = route.getRoute().size();
            if (systemCount == 0) {
                return;
            }

            // Obtenir le système actuel
            String currentSystemName = commanderStatus.getCurrentStarSystem();

            // Trouver l'index du système actuel dans la route
            int currentSystemIndex = -1;
            for (int i = 0; i < systemCount; i++) {
                if (route.getRoute().get(i).getSystemName().equals(currentSystemName)) {
                    currentSystemIndex = i;
                    break;
                }
            }

            // Si le système actuel est dans la route, marquer tous les systèmes précédents comme visités
            if (currentSystemIndex >= 0) {
                for (int i = 0; i < currentSystemIndex; i++) {
                    String systemName = route.getRoute().get(i).getSystemName();
                    visitedSystems.add(systemName);
                    System.out.println("✅ Système marqué comme visité (avant système actuel): " + systemName);
                }
                // Sauvegarder les systèmes visités
                saveVisitedSystemsToPreferences();
            } else {
                System.out.println("⚠️ Système actuel '" + currentSystemName + "' non trouvé dans la route");
            }

            // Calculer l'espacement proportionnel aux distances en AL
            // Utiliser la largeur du conteneur
            double availableWidth = 800; // Largeur par défaut
            if (navRouteContainer != null && navRouteContainer.getWidth() > 0) {
                availableWidth = navRouteContainer.getWidth() - PADDING_X * 2 - 30; // -30 pour padding/marges
            } else if (routeSystemsPane != null && routeSystemsPane.getWidth() > 0) {
                availableWidth = routeSystemsPane.getWidth() - PADDING_X * 2 - 30;
            }

            // Calculer la distance totale de la route
            double totalDistance = 0.0;
            for (int i = 1; i < systemCount; i++) {
                totalDistance += route.getRoute().get(i).getDistanceFromPrevious();
            }

            // Si pas de distance ou distance nulle, utiliser un espacement uniforme
            double[] spacings = new double[systemCount - 1];
            if (totalDistance <= 0 || systemCount <= 1) {
                // Espacement uniforme
                double uniformSpacing = systemCount <= 1 ? MAX_SPACING :
                    Math.max(MIN_SPACING, Math.min(MAX_SPACING, (availableWidth - PADDING_X * 2) / (systemCount - 1)));
                for (int i = 0; i < systemCount - 1; i++) {
                    spacings[i] = uniformSpacing;
                }
            } else {
                // Calculer les espacements proportionnels aux distances
                double scaleFactor = (availableWidth - PADDING_X * 2) / totalDistance;

                // Appliquer des limites min/max pour chaque espacement
                for (int i = 0; i < systemCount - 1; i++) {
                    double distance = route.getRoute().get(i + 1).getDistanceFromPrevious();
                    double proportionalSpacing = distance * scaleFactor;
                    spacings[i] = Math.max(MIN_SPACING, Math.min(MAX_SPACING, proportionalSpacing));
                }

                // Ajuster si la somme dépasse la largeur disponible (réduire proportionnellement)
                double totalSpacing = 0;
                for (double spacing : spacings) {
                    totalSpacing += spacing;
                }

                if (totalSpacing > availableWidth - PADDING_X * 2) {
                    double adjustmentFactor = (availableWidth - PADDING_X * 2) / totalSpacing;
                    for (int i = 0; i < spacings.length; i++) {
                        spacings[i] *= adjustmentFactor;
                        // S'assurer que chaque espacement respecte toujours le minimum
                        if (spacings[i] < MIN_SPACING) {
                            spacings[i] = MIN_SPACING;
                        }
                    }
                }
            }

            // Calculer la largeur totale nécessaire
            double totalWidth = PADDING_X * 2;
            for (double spacing : spacings) {
                totalWidth += spacing;
            }
            routeSystemsPane.setPrefWidth(totalWidth);
            routeSystemsPane.setMinHeight(LINE_HEIGHT);
            routeSystemsPane.setPrefHeight(LINE_HEIGHT);

            double centerY = LINE_HEIGHT / 2;

            // Calculer la taille des cercles en fonction du nombre de systèmes
            double circleRadius = calculateCircleRadius(systemCount, false);
            double currentCircleRadius = calculateCircleRadius(systemCount, true);

            // Dessiner les systèmes et les lignes
            double currentX = PADDING_X;
            for (int i = 0; i < systemCount; i++) {
                RouteSystem system = route.getRoute().get(i);
                double x = currentX;

                boolean isCurrent = (i == currentSystemIndex);
                // Un système est visité s'il est dans le Set des systèmes visités
                // ou s'il est avant le système actuel dans la route
                boolean isVisited = visitedSystems.contains(system.getSystemName()) ||
                                   (currentSystemIndex >= 0 && i < currentSystemIndex);

                // Dessiner la ligne vers le système suivant (sauf pour le dernier)
                if (i < systemCount - 1) {
                    RouteSystem nextSystem = route.getRoute().get(i + 1);
                    double nextX = currentX + spacings[i];
                    // Vérifier si le système actuel (celui d'où part la ligne) a une étoile à boost
                    boolean hasBoost = isBoostStar(system.getStarClass());
                    Line line = createLine(x, centerY, nextX, centerY, nextSystem, isVisited, hasBoost);
                    routeSystemsPane.getChildren().add(line);
                    
                    // En mode Stratum, vérifier et afficher le nombre de sauts restants pour toutes les lignes
                    if (currentMode != null && currentMode.requiresSpanshApi()) {
                        NavRoute freeExplorationRoute = navRouteRegistry.getRouteForMode(ExplorationMode.FREE_EXPLORATION);
                        if (freeExplorationRoute != null && freeExplorationRoute.getRoute() != null && !freeExplorationRoute.getRoute().isEmpty()) {
                            RouteSystem lastFreeSystem = freeExplorationRoute.getRoute().get(freeExplorationRoute.getRoute().size() - 1);
                            // Vérifier si le dernier système de Free Exploration correspond à la prochaine boule
                            if (lastFreeSystem.getSystemName().equals(nextSystem.getSystemName())) {
                                // Utiliser le nombre de sauts restants du registre (celui du label "x jump remaining")
                                int remainingJumps = navRouteTargetRegistry.getRemainingJumpsInRoute();
                                if (remainingJumps > 0) {
                                    // Afficher le nombre de sauts au-dessus de la ligne, au milieu
                                    double midX = (x + nextX) / 2;
                                    Label jumpsLabel = createJumpsRemainingLabel(midX, centerY - 15, remainingJumps);
                                    routeSystemsPane.getChildren().add(jumpsLabel);
                                }
                            }
                        }
                    }
                    
                    currentX = nextX;
                }

                // Dessiner le cercle pour le système
                double radius = isCurrent ? currentCircleRadius : circleRadius;
                boolean isLastCopied = system.getSystemName().equals(lastCopiedSystemName);
                
                // Ajouter l'indicateur visuel pour le dernier système copié (avant le cercle pour qu'il soit en arrière-plan)
                if (isLastCopied) {
                    Circle copiedIndicator = createCopiedIndicator(x, centerY, radius);
                    routeSystemsPane.getChildren().add(copiedIndicator);
                }
                
                Circle circle = createCircle(x, centerY, system, isCurrent, isVisited, radius, isLastCopied);
                routeSystemsPane.getChildren().add(circle);

                // Ajouter l'indicateur scoopable si applicable
                if (isScoopable(system.getStarClass())) {
                    Text scoopIndicator = createScoopIndicator(x, centerY, radius);
                    routeSystemsPane.getChildren().add(scoopIndicator);
                }
            }
        }
        
        // Mettre à jour l'overlay si il est ouvert
        if (navRouteOverlayComponent != null && navRouteOverlayComponent.isShowing()) {
            navRouteOverlayComponent.updateOverlayContent();
        }
    }

    /**
     * Calcule la taille du cercle en fonction du nombre de systèmes
     */
    public double calculateCircleRadius(int systemCount, boolean isCurrent) {
        if (systemCount <= SYSTEM_COUNT_THRESHOLD) {
            // Nombre de systèmes normal, utiliser la taille de base
            return isCurrent ? CIRCLE_CURRENT_RADIUS_BASE : CIRCLE_RADIUS_BASE;
        }

        // Réduire progressivement la taille quand il y a beaucoup de systèmes
        // Réduction linéaire entre le seuil et 30 systèmes
        double reductionFactor = Math.max(0.5, 1.0 - ((systemCount - SYSTEM_COUNT_THRESHOLD) / 20.0));

        double baseRadius = isCurrent ? CIRCLE_CURRENT_RADIUS_BASE : CIRCLE_RADIUS_BASE;
        double minRadius = isCurrent ? CIRCLE_CURRENT_RADIUS_MIN : CIRCLE_RADIUS_MIN;

        double calculatedRadius = baseRadius * reductionFactor;
        return Math.max(minRadius, calculatedRadius);
    }

    /**
     * Vérifie si un système est scoopable (KGBFOAM)
     */
    public boolean isScoopable(String starClass) {
        if (starClass == null || starClass.isEmpty()) {
            return false;
        }
        // Prendre la première lettre (ex: "M" pour "M (Red dwarf) Star")
        String firstChar = starClass.substring(0, 1).toUpperCase();
        return SCOOPABLE_STAR_TYPES.contains(firstChar);
    }

    /**
     * Vérifie si un système a une étoile à neutrons ou naine blanche (boost)
     */
    public boolean isBoostStar(String starClass) {
        if (starClass == null || starClass.isEmpty()) {
            return false;
        }
        // Prendre la première lettre (ex: "N" pour "Neutron Star", "D" pour "White Dwarf")
        String firstChar = starClass.substring(0, 1).toUpperCase();
        return BOOST_STAR_TYPES.contains(firstChar);
    }

    /**
     * Crée un indicateur visuel pour les systèmes scoopables
     */
    public Text createScoopIndicator(double x, double y, double circleRadius) {
        Text indicator = new Text("⛽");
        // Ajuster la taille de la police en fonction de la taille du cercle
        double fontSize = Math.max(8, circleRadius * 0.8);
        indicator.setFont(Font.font(fontSize));
        indicator.setFill(Color.rgb(255, 255, 0, 0.9)); // Jaune pour le fuel
        // Centrer l'indicateur au-dessus du cercle
        double textWidth = indicator.getLayoutBounds().getWidth();
        indicator.setX(x - textWidth / 2); // Centrer horizontalement
        indicator.setY(y - circleRadius - 6); // Positionner au-dessus du cercle
        indicator.getStyleClass().add("nav-route-scoop-indicator");

        Tooltip tooltip = new TooltipComponent(localizationService.getString("nav.route.scoopable"));
        Tooltip.install(indicator, tooltip);

        return indicator;
    }

    /**
     * Crée une ligne entre deux systèmes
     */
    public Line createLine(double startX, double startY, double endX, double endY, RouteSystem system, boolean isVisited, boolean hasBoost) {
        Line line = new Line(startX, startY, endX, endY);

        if (hasBoost) {
            // Ligne avec boost (étoile à neutrons ou naine blanche) : violet/magenta Elite Dangerous
            if (isVisited) {
                line.setStroke(Color.rgb(200, 100, 255, 0.6)); // Violet avec transparence pour visité
            } else {
                line.setStroke(Color.rgb(200, 100, 255, 0.9)); // Violet vif pour non visité
            }
            line.setStrokeWidth(2.5); // Légèrement plus épais pour le boost
        } else if (isVisited) {
            // Ligne visitée : gris Elite Dangerous
            line.setStroke(Color.rgb(128, 128, 128, 0.5));
            line.setStrokeWidth(2.0);
        } else {
            // Ligne non visitée : cyan
            line.setStroke(Color.rgb(0, 191, 255, 0.6));
            line.setStrokeWidth(2.0);
        }

        line.getStyleClass().add("nav-route-line");
        // Les lignes ne sont pas interactives (pas de hover, pas de clic)
        line.setMouseTransparent(true);

        return line;
    }

    /**
     * Crée un label pour afficher le nombre de sauts restants
     */
    private Label createJumpsRemainingLabel(double x, double y, int remainingJumps) {
        Label label = new Label(String.valueOf(remainingJumps));
        label.getStyleClass().add("nav-route-jumps-remaining-label");
        label.setTextFill(Color.rgb(255, 255, 0, 0.9)); // Jaune
        label.setFont(Font.font(12));
        
        // Centrer le label horizontalement en utilisant translateX
        label.layoutBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            if (newBounds != null && newBounds.getWidth() > 0) {
                label.setTranslateX(-newBounds.getWidth() / 2);
            }
        });
        
        // Position initiale
        label.setLayoutX(x);
        label.setLayoutY(y);
        
        return label;
    }

    /**
     * Crée un indicateur visuel autour du dernier système copié
     */
    public Circle createCopiedIndicator(double x, double y, double circleRadius) {
        // Créer un cercle plus grand autour du cercle du système
        double indicatorRadius = circleRadius + 6; // 6 pixels de plus que le cercle
        Circle indicator = new Circle(x, y, indicatorRadius);
        
        // Cercle avec contour animé (vert pour indiquer qu'il est copié)
        indicator.setFill(Color.TRANSPARENT);
        indicator.setStroke(Color.rgb(0, 255, 0, 0.8)); // Vert avec transparence
        indicator.setStrokeWidth(2.5);
        indicator.getStyleClass().add("nav-route-copied-indicator");
        
        // Le rendre non interactif pour ne pas bloquer les clics sur le cercle du système
        indicator.setMouseTransparent(true);
        
        return indicator;
    }

    /**
     * Crée un cercle pour représenter un système
     */
    public Circle createCircle(double x, double y, RouteSystem system, boolean isCurrent, boolean isVisited, double radius, boolean isLastCopied) {
        Circle circle = new Circle(x, y, radius);

        if (isCurrent) {
            // Système actuel : orange
            circle.setFill(Color.rgb(255, 107, 0, 0.8)); // Orange Elite Dangerous
            circle.setStroke(Color.rgb(255, 107, 0, 1.0));
            circle.getStyleClass().add("nav-route-circle-current");
        } else if (isVisited) {
            // Système visité : gris Elite Dangerous
            circle.setFill(Color.rgb(128, 128, 128, 0.6));
            circle.setStroke(Color.rgb(128, 128, 128, 0.8));
            circle.getStyleClass().add("nav-route-circle-visited");
        } else {
            // Système futur : cyan
            circle.setFill(Color.rgb(0, 191, 255, 0.6)); // Cyan
            circle.setStroke(Color.rgb(0, 191, 255, 0.8));
            circle.getStyleClass().add("nav-route-circle");
        }

        circle.setStrokeWidth(2.0);

        // Tooltip au survol pour afficher le nom du système
        String tooltipText = system.getSystemName();
        if (isVisited) {
            tooltipText += " (" + localizationService.getString("nav.route.visited") + ")";
        }
        if (system.getDistanceFromPrevious() > 0) {
            tooltipText += " (" + String.format(localizationService.getString("nav.route.distance.format"), system.getDistanceFromPrevious()) + ")";
        }
        if (isScoopable(system.getStarClass())) {
            tooltipText += localizationService.getString("nav.route.scoopable.suffix");
        }
        Tooltip tooltip = new TooltipComponent(tooltipText);
        Tooltip.install(circle, tooltip);

        // Effet hover
        final double originalRadius = radius;
        circle.setOnMouseEntered(e -> {
            if (isCurrent) {
                circle.setFill(Color.rgb(255, 107, 0, 1.0));
            } else if (isVisited) {
                circle.setFill(Color.rgb(128, 128, 128, 0.8));
            } else {
                circle.setFill(Color.rgb(0, 191, 255, 0.9));
            }
            circle.setRadius(originalRadius + 2);
        });
        circle.setOnMouseExited(e -> {
            if (isCurrent) {
                circle.setFill(Color.rgb(255, 107, 0, 0.8));
            } else if (isVisited) {
                circle.setFill(Color.rgb(128, 128, 128, 0.6));
            } else {
                circle.setFill(Color.rgb(0, 191, 255, 0.6));
            }
            circle.setRadius(originalRadius);
        });

        // Gestion du clic pour copier le nom du système
        circle.setOnMouseClicked(e -> onSystemCircleClicked(e, system));

        return circle;
    }

    /**
     * Gère le clic sur un cercle de système pour copier le nom dans le presse-papier
     */
    private void onSystemCircleClicked(MouseEvent event, RouteSystem system) {
        if (system == null || system.getSystemName() == null || system.getSystemName().isEmpty()) {
            return;
        }

        String systemName = system.getSystemName();
        copyClipboardManager.copyToClipboard(systemName);

        // Mettre à jour le dernier système copié et rafraîchir l'affichage
        lastCopiedSystemName = systemName;
        NavRoute route = navRouteRegistry.getCurrentRoute();
        if (route != null) {
            updateRouteDisplay(route);
        }
        
        // Mettre à jour l'overlay si il est ouvert
        if (navRouteOverlayComponent != null && navRouteOverlayComponent.isShowing()) {
            navRouteOverlayComponent.updateOverlayContent();
        }

        // Afficher un popup de confirmation à la position de la souris
        Stage stage = (Stage) routeSystemsPane.getScene().getWindow();
        showSystemCopiedPopup(systemName, event.getSceneX(), event.getSceneY(), stage);
    }

    /**
     * Affiche le popup "System copied" pour un système donné
     * @param systemName Le nom du système copié
     * @param x La position X (en coordonnées de scène) - si < 0, calcule automatiquement depuis la route
     * @param y La position Y (en coordonnées de scène) - si < 0, calcule automatiquement depuis la route
     * @param stage La fenêtre où afficher le popup
     */
    private void showSystemCopiedPopup(String systemName, double x, double y, Stage stage) {
        if (systemName == null || systemName.isEmpty() || stage == null) {
            return;
        }

        double popupX = x;
        double popupY = y;

        // Si les coordonnées ne sont pas fournies, calculer depuis la route
        if (x < 0 || y < 0) {
            NavRoute route = navRouteRegistry.getCurrentRoute();
            if (route != null && routeSystemsPane != null && routeSystemsPane.getScene() != null) {
                double systemX = calculateSystemXPosition(route, systemName);
                if (systemX >= 0) {
                    // Convertir les coordonnées locales en coordonnées de scène
                    javafx.geometry.Bounds bounds = routeSystemsPane.localToScene(routeSystemsPane.getBoundsInLocal());
                    popupX = bounds.getMinX() + systemX;
                    popupY = bounds.getMinY() + LINE_HEIGHT / 2;
                } else {
                    // Si on ne peut pas calculer la position, utiliser le centre
                    popupX = routeSystemsPane.getWidth() / 2;
                    popupY = routeSystemsPane.getHeight() / 2;
                }
            } else {
                return; // Pas de route ou pas de pane, on ne peut pas afficher le popup
            }
        }

        // Vérifier si la fenêtre est enregistrée dans le PopupManager avant d'afficher le popup
        try {
            popupManager.showPopup(localizationService.getString("system.copied"), popupX, popupY, stage);
        } catch (IllegalStateException e) {
            // La fenêtre n'est pas enregistrée, on ignore simplement l'affichage du popup
            System.out.println("⚠️ Impossible d'afficher le popup 'System copied' : fenêtre non enregistrée dans PopupManager");
        }
    }

    /**
     * Met à jour le label affichant le nombre de sauts restants
     */
    private void updateRemainingJumpsLabel(int remainingJumps) {
        if (remainingJumpsLabel != null && !DashboardContext.getInstance().isBatchLoading()) {
            if (remainingJumps >= 0) {
                remainingJumpsLabel.setText(localizationService.getString("nav.route.remaining_jumps", remainingJumps));
                remainingJumpsLabel.setVisible(true);
                remainingJumpsLabel.setManaged(true);
               // System.out.println("✅ Remaining jumps label mis à jour: " + remainingJumps + " (visible: " + remainingJumpsLabel.isVisible() + ", managed: " + remainingJumpsLabel.isManaged() + ")");
            } else {
                remainingJumpsLabel.setText("");
                remainingJumpsLabel.setVisible(false);
                remainingJumpsLabel.setManaged(false);
                System.out.println("❌ Remaining jumps label masqué (remainingJumps: " + remainingJumps + ")");
            }
        } else {
            System.out.println("⚠️ remainingJumpsLabel est null!");
        }
    }

    /**
     * Met à jour toutes les traductions du panel
     */
    private void updateTranslations() {
        // Mettre à jour le titre de base
        if (routeTitleLabel != null) {
            NavRoute currentRoute = navRouteRegistry.getCurrentRoute();
            if (currentRoute == null || currentRoute.getRoute() == null || currentRoute.getRoute().isEmpty()) {
                routeTitleLabel.setText(localizationService.getString("nav.route.title.no_route"));
            } else {
                routeTitleLabel.setText(localizationService.getString("nav.route.title.with_systems", currentRoute.getRoute().size()));
            }
        }

        // Mettre à jour le label "Mode:" dans le modeSelectorContainer
        if (modeSelectorContainer != null && modeSelectorContainer.getChildren().size() > 0) {
            javafx.scene.Node firstChild = modeSelectorContainer.getChildren().get(0);
            if (firstChild instanceof Label) {
                ((Label) firstChild).setText(localizationService.getString("nav.route.mode.label"));
            }
        }

        // Mettre à jour le ComboBox (forcer la mise à jour en réinitialisant la valeur)
        if (modeComboBox != null) {
            ExplorationMode currentValue = modeComboBox.getValue();
            modeComboBox.setValue(null);
            modeComboBox.setValue(currentValue);
        }

        // Mettre à jour le label des remaining jumps avec la nouvelle langue
        if (remainingJumpsLabel != null) {
            int remainingJumps = navRouteTargetRegistry.getRemainingJumpsInRoute();
            updateRemainingJumpsLabel(remainingJumps);
        }

        // Mettre à jour le bouton Reload
        if (reloadButton != null && !reloadButton.isDisable()) {
            reloadButton.setText(localizationService.getString("nav.route.reload.button"));
            updateReloadButtonTooltip();
        }

        // Mettre à jour le tooltip du bouton Stratum
        updateStratumInfoButtonTooltip();

        // Mettre à jour la description du mode
        if (modeDescriptionLabel != null) {
            updateModeDescription(currentMode);
        }
    }

}

