package be.mirooz.elitedangerous.dashboard.view.exploration;

import be.mirooz.elitedangerous.analytics.dto.spansh.SpanshSearchRequestDTO;
import be.mirooz.elitedangerous.analytics.dto.spansh.SpanshSearchResponse;
import be.mirooz.elitedangerous.analytics.dto.spansh.SpanshSearchResponseDTO;
import be.mirooz.elitedangerous.dashboard.model.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.model.exploration.ExplorationMode;
import be.mirooz.elitedangerous.dashboard.model.navigation.NavRoute;
import be.mirooz.elitedangerous.dashboard.model.navigation.RouteSystem;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.ExplorationModeRegistry;
import be.mirooz.elitedangerous.dashboard.model.registries.navigation.NavRouteRegistry;
import be.mirooz.elitedangerous.dashboard.model.registries.navigation.NavRouteTargetRegistry;
import be.mirooz.elitedangerous.dashboard.service.AnalyticsService;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.service.listeners.NavRouteNotificationService;
import be.mirooz.elitedangerous.dashboard.view.common.TooltipComponent;
import be.mirooz.elitedangerous.dashboard.view.common.managers.CopyClipboardManager;
import be.mirooz.elitedangerous.dashboard.view.common.managers.PopupManager;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
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
    private javafx.scene.control.ScrollPane routeScrollPane;
    
    @FXML
    private HBox modeSelectorContainer;
    
    @FXML
    private Label modeDescriptionLabel;
    
    @FXML
    private ProgressIndicator loadingIndicator;
    
    @FXML
    private javafx.scene.control.Button reloadButton;
    
    @FXML
    private Label remainingJumpsLabel;

    private final NavRouteRegistry navRouteRegistry = NavRouteRegistry.getInstance();
    private final NavRouteTargetRegistry navRouteTargetRegistry = NavRouteTargetRegistry.getInstance();
    private final ExplorationModeRegistry explorationModeRegistry = ExplorationModeRegistry.getInstance();
    private ExplorationMode currentMode = ExplorationMode.FREE_EXPLORATION;
    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();
    private final AnalyticsService analyticsService = AnalyticsService.getInstance();
    private NavRoute savedNormalRoute = null; // Route normale sauvegardée quand on passe en mode STRATUM
    private final CopyClipboardManager copyClipboardManager = CopyClipboardManager.getInstance();
    private final PopupManager popupManager = PopupManager.getInstance();
    private final LocalizationService localizationService = LocalizationService.getInstance();
    private final be.mirooz.elitedangerous.dashboard.service.PreferencesService preferencesService = 
        be.mirooz.elitedangerous.dashboard.service.PreferencesService.getInstance();
    private ChangeListener<String> currentSystemListener;
    private ChangeListener<Number> widthListener;
    private final Set<String> visitedSystems = new HashSet<>(); // Systèmes visités dans la route actuelle

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialiser le registre avec le mode par défaut
        explorationModeRegistry.setCurrentMode(ExplorationMode.FREE_EXPLORATION);
        
        // Charger les systèmes visités depuis les préférences
        loadVisitedSystemsFromPreferences();
        
        // Initialiser le sélecteur de mode
        initializeModeSelector();
        
        // Écouter les changements de route
        navRouteRegistry.getCurrentRouteProperty().addListener((obs, oldRoute, newRoute) -> {
            Platform.runLater(() -> {
                // Ne pas réinitialiser les systèmes visités - on les maintient même lors du changement de route
                // Les systèmes visités sont sauvegardés dans les préférences et chargés au démarrage
                updateRouteDisplay(newRoute);
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
                
                // Mettre à jour l'affichage
                NavRoute route = navRouteRegistry.getCurrentRoute();
                if (route != null) {
                    updateRouteDisplay(route);
                }
            });
        };
        statusComponent.getCurrentStarSystem().addListener(currentSystemListener);
        
        // Écouter les changements de largeur du ScrollPane pour recalculer l'espacement
        if (routeScrollPane != null) {
            widthListener = (obs, oldWidth, newWidth) -> {
                Platform.runLater(() -> {
                    NavRoute route = navRouteRegistry.getCurrentRoute();
                    if (route != null) {
                        updateRouteDisplay(route);
                    }
                });
            };
            routeScrollPane.widthProperty().addListener(widthListener);
        }
        
        // Écouter les changements de RemainingJumpsInRoute
        navRouteTargetRegistry.getRemainingJumpsInRouteProperty().addListener((obs, oldValue, newValue) -> {
            Platform.runLater(() -> {
                updateRemainingJumpsLabel(newValue.intValue());
            });
        });
        
        // Initialiser le label avec la valeur actuelle
        updateRemainingJumpsLabel(navRouteTargetRegistry.getRemainingJumpsInRoute());
        
        // S'abonner au service de notification pour le refresh de la route
        NavRouteNotificationService.getInstance().addListener(this::refreshRouteDisplay);
        
        // Afficher la route actuelle si elle existe
        updateRouteDisplay(navRouteRegistry.getCurrentRoute());
    }
    
    /**
     * Méthode appelée par le service de notification pour rafraîchir l'affichage de la route
     */
    private void refreshRouteDisplay() {
        Platform.runLater(() -> {
            // En mode Stratum, forcer le rafraîchissement en créant une nouvelle instance de la route
            if (currentMode == ExplorationMode.STRATUM_UNDISCOVERED) {
                NavRoute currentRoute = navRouteRegistry.getCurrentRoute();
                if (currentRoute != null) {
                    // Créer une nouvelle instance pour forcer le listener à se déclencher
                    NavRoute refreshedRoute = new NavRoute();
                    refreshedRoute.setTimestamp(currentRoute.getTimestamp());
                    refreshedRoute.setRoute(new java.util.ArrayList<>(currentRoute.getRoute()));
                    navRouteRegistry.setCurrentRoute(refreshedRoute);
                }
            } else {
                // En mode Free Exploration, simplement mettre à jour l'affichage
                NavRoute route = navRouteRegistry.getCurrentRoute();
                if (route != null) {
                    updateRouteDisplay(route);
                }
            }
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
        ComboBox<ExplorationMode> modeComboBox = new ComboBox<>();
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
                    setText(mode.getDisplayName());
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
                    setText(mode.getDisplayName());
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
        Label modeLabel = new Label("Mode:");
        modeLabel.getStyleClass().add("filter-label");
        
        modeSelectorContainer.getChildren().addAll(modeLabel, modeComboBox);
        
        // Initialiser la description du mode par défaut
        if (modeDescriptionLabel != null) {
            updateModeDescription(ExplorationMode.FREE_EXPLORATION);
        }
        
        // Initialiser le bouton de rechargement
        initializeReloadButton();
    }
    
    /**
     * Initialise le bouton de rechargement
     */
    private void initializeReloadButton() {
        if (reloadButton != null) {
            // Tooltip
            Tooltip tooltip = new Tooltip("Recharge les données");
            reloadButton.setTooltip(tooltip);
            
            // Visibilité selon le mode
            updateReloadButtonVisibility();
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
     * Action du bouton de rechargement
     */
    @FXML
    public void onReloadButtonClicked() {
        if (currentMode == ExplorationMode.STRATUM_UNDISCOVERED) {
            // Effacer le GUID sauvegardé pour forcer une nouvelle recherche avec le système actuel
            String modeKey = "spansh.guid." + currentMode.name();
            preferencesService.setPreference(modeKey, "");
            
            // Recharger la route avec le système actuel
            loadStratumRoute();
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
            modeDescriptionLabel.setText(mode.getDescription());
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
        
        // Mettre à jour la visibilité du bouton de rechargement
        updateReloadButtonVisibility();
        
        if (newMode == ExplorationMode.STRATUM_UNDISCOVERED) {
            // Sauvegarder la route normale si elle existe
            NavRoute currentRoute = navRouteRegistry.getCurrentRoute();
            if (currentRoute != null && oldMode == ExplorationMode.FREE_EXPLORATION) {
                savedNormalRoute = currentRoute;
            }
            
            // Ne PAS réinitialiser les systèmes visités - on les maintient même lors du rechargement
            // visitedSystems.clear(); // Commenté pour maintenir les systèmes visités
            
            // Appeler le backend pour obtenir la route Stratum
            loadStratumRoute();
        } else if (newMode == ExplorationMode.FREE_EXPLORATION) {
            // Réinitialiser les systèmes visités pour la nouvelle route
            visitedSystems.clear();
            
            // Toujours recharger le fichier NavRoute.json pour avoir les données les plus récentes
            // (même si on a une route sauvegardée, car le fichier peut avoir été mis à jour
            // pendant qu'on était en mode Stratum et que les événements NavRoute ont été ignorés)
            be.mirooz.elitedangerous.dashboard.service.NavRouteService.getInstance().loadAndStoreNavRoute();
            
            // Nettoyer la route sauvegardée puisqu'on recharge toujours depuis le fichier
            savedNormalRoute = null;
            
            // Forcer le rafraîchissement de l'UI
            Platform.runLater(() -> {
                NavRoute route = navRouteRegistry.getCurrentRoute();
                if (route != null) {
                    updateRouteDisplay(route);
                }
            });
        }
    }
    
    /**
     * Charge la route depuis Spansh pour le mode Stratum Undiscovered
     */
    private void loadStratumRoute() {
        // Afficher l'indicateur de chargement
        setLoadingVisible(true);
        
        // Exécuter dans un thread séparé pour ne pas bloquer l'UI
        new Thread(() -> {
            try {
                // Obtenir le système actuel comme référence
                String currentSystem = commanderStatus.getCurrentStarSystem();
                if (currentSystem == null || currentSystem.isEmpty()) {
                    Platform.runLater(() -> {
                        System.err.println("⚠️ Impossible de charger la route Stratum : système actuel inconnu");
                        setLoadingVisible(false);
                    });
                    return;
                }
                
                // Charger le GUID depuis les préférences pour ce mode
                String modeKey = "spansh.guid." + currentMode.name();
                String savedGuid = preferencesService.getPreference(modeKey, null);
                
                SpanshSearchResponseDTO responseDTO;
                
                boolean isNewCall = false;
                if (savedGuid != null && !savedGuid.isEmpty()) {
                    // Si on a un GUID, faire un GET (reprise en cours de route)
                    System.out.println("📋 Utilisation du GUID sauvegardé pour le mode " + currentMode.name() + ": " + savedGuid);
                    responseDTO = analyticsService.getSpanshSearchByGuid(savedGuid);
                    isNewCall = false;
                } else {
                    // Sinon, faire un POST normal (nouveau call)
                    System.out.println("🆕 Création d'une nouvelle recherche Spansh pour le mode " + currentMode.name());
                    SpanshSearchRequestDTO requestDTO = new SpanshSearchRequestDTO(currentSystem);
                    responseDTO = analyticsService.searchSpansh(requestDTO);
                    isNewCall = true;
                    
                    // Sauvegarder le GUID reçu
                    if (responseDTO != null && responseDTO.getSearchReference() != null && !responseDTO.getSearchReference().isEmpty()) {
                        preferencesService.setPreference(modeKey, responseDTO.getSearchReference());
                        System.out.println("💾 GUID sauvegardé pour le mode " + currentMode.name() + ": " + responseDTO.getSearchReference());
                    }
                }
                
                // Sauvegarder les systèmes visités avant de reconstruire la route
                Set<String> previousVisitedSystems = new HashSet<>(visitedSystems);
                
                // Construire la route à partir de la réponse
                NavRoute stratumRoute = buildRouteFromSpanshResponse(responseDTO, currentSystem, isNewCall);
                
                // Restaurer les systèmes visités après reconstruction
                visitedSystems.clear();
                visitedSystems.addAll(previousVisitedSystems);
                
                // Mettre à jour le registre sur le thread JavaFX
                Platform.runLater(() -> {
                    setLoadingVisible(false);
                    if (stratumRoute != null) {
                        navRouteRegistry.setCurrentRoute(stratumRoute);
                        System.out.println("✅ Route Stratum chargée : " + stratumRoute.getRoute().size() + " systèmes");
                        // Forcer la mise à jour de l'affichage pour prendre en compte les systèmes visités
                        updateRouteDisplay(stratumRoute);
                    } else {
                        System.err.println("⚠️ Aucune route Stratum trouvée");
                    }
                });
                
            } catch (Exception e) {
                System.err.println("❌ Erreur lors du chargement de la route Stratum: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> {
                    setLoadingVisible(false);
                    // En cas d'erreur, restaurer la route normale si elle existe
                    if (savedNormalRoute != null) {
                        navRouteRegistry.setCurrentRoute(savedNormalRoute);
                    }
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
            if (routeScrollPane != null) {
                routeScrollPane.setVisible(!visible);
                routeScrollPane.setManaged(!visible);
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
                routeTitleLabel.setText("ROUTE DE NAVIGATION (Aucune route active)");
            }
            if (routeSystemsPane != null) {
                routeSystemsPane.getChildren().clear();
            }
            return;
        }

        // Mettre à jour le titre
        if (routeTitleLabel != null) {
            routeTitleLabel.setText("ROUTE DE NAVIGATION (" + route.getRoute().size() + " systèmes)");
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
            // Utiliser la largeur du ScrollPane ou du conteneur
            double availableWidth = 800; // Largeur par défaut
            if (routeScrollPane != null && routeScrollPane.getWidth() > 0) {
                availableWidth = routeScrollPane.getWidth() - PADDING_X * 2 - 30; // -30 pour padding/marges
            } else if (navRouteContainer != null && navRouteContainer.getWidth() > 0) {
                availableWidth = navRouteContainer.getWidth() - PADDING_X * 2 - 30;
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
                    currentX = nextX;
                }

                // Dessiner le cercle pour le système
                double radius = isCurrent ? currentCircleRadius : circleRadius;
                Circle circle = createCircle(x, centerY, system, isCurrent, isVisited, radius);
                routeSystemsPane.getChildren().add(circle);
                
                // Ajouter l'indicateur scoopable si applicable
                if (isScoopable(system.getStarClass())) {
                    Text scoopIndicator = createScoopIndicator(x, centerY, radius);
                    routeSystemsPane.getChildren().add(scoopIndicator);
                }
            }
        }
    }
    
    /**
     * Calcule la taille du cercle en fonction du nombre de systèmes
     */
    private double calculateCircleRadius(int systemCount, boolean isCurrent) {
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
    private boolean isScoopable(String starClass) {
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
    private boolean isBoostStar(String starClass) {
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
    private Text createScoopIndicator(double x, double y, double circleRadius) {
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
        
        Tooltip tooltip = new TooltipComponent("Système scoopable");
        Tooltip.install(indicator, tooltip);
        
        return indicator;
    }

    /**
     * Crée une ligne entre deux systèmes
     */
    private Line createLine(double startX, double startY, double endX, double endY, RouteSystem system, boolean isVisited, boolean hasBoost) {
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
     * Crée un cercle pour représenter un système
     */
    private Circle createCircle(double x, double y, RouteSystem system, boolean isCurrent, boolean isVisited, double radius) {
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
        if (system.getDistanceFromPrevious() > 0) {
            tooltipText += " (" + String.format("%.2f AL", system.getDistanceFromPrevious()) + ")";
        }
        if (isScoopable(system.getStarClass())) {
            tooltipText += " - Scoopable";
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
        
        // Afficher un popup de confirmation
        Stage stage = (Stage) routeSystemsPane.getScene().getWindow();
        popupManager.showPopup(localizationService.getString("system.copied"), event.getSceneX(), event.getSceneY(), stage);
    }
    
    /**
     * Met à jour le label affichant le nombre de sauts restants
     */
    private void updateRemainingJumpsLabel(int remainingJumps) {
        if (remainingJumpsLabel != null) {
            if (remainingJumps >= 0) {
                remainingJumpsLabel.setText(remainingJumps + " saut(s) restant(s)");
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

