package be.mirooz.elitedangerous.dashboard.view.combat;

import be.mirooz.elitedangerous.dashboard.model.enums.TargetType;
import be.mirooz.elitedangerous.dashboard.model.targetpanel.CibleStats;
import be.mirooz.elitedangerous.dashboard.model.targetpanel.SourceFactionStats;
import be.mirooz.elitedangerous.dashboard.model.targetpanel.TargetFactionStats;
import be.mirooz.elitedangerous.dashboard.model.commander.Mission;
import be.mirooz.elitedangerous.dashboard.service.PreferencesService;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.view.common.TooltipComponent;
import be.mirooz.elitedangerous.dashboard.view.common.managers.PopupManager;
import be.mirooz.elitedangerous.dashboard.view.common.managers.CopyClipboardManager;
import be.mirooz.elitedangerous.dashboard.model.registries.combat.DestroyedShipsRegistery;
import be.mirooz.elitedangerous.dashboard.model.registries.combat.MissionsRegistry;
import static be.mirooz.elitedangerous.dashboard.util.NumberUtil.getFormattedNumber;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Composant pour gérer l'overlay de cibles
 * <p>
 * Ce composant gère :
 * - La création et gestion de la fenêtre overlay
 * - Le redimensionnement et déplacement de l'overlay
 * - Le curseur de transparence du background
 * - La sauvegarde/restauration des préférences
 */
public class TargetOverlayComponent {

    public static final double MIN_OPPACITY = 0.01;
    public static final int MIN_WIDTH_OVERLAY = 250;
    public static final int MIN_HEIGHT_OVERLAY = 200;
    private final PreferencesService preferencesService = PreferencesService.getInstance();

    // Clés pour les préférences de l'overlay
    private static final String TARGET_OVERLAY_WIDTH_KEY = "target_overlay.width";
    private static final String TARGET_OVERLAY_HEIGHT_KEY = "target_overlay.height";
    private static final String TARGET_OVERLAY_OPACITY_KEY = "target_overlay.opacity";
    private static final String TARGET_OVERLAY_X_KEY = "target_overlay.x";
    private static final String TARGET_OVERLAY_Y_KEY = "target_overlay.y";
    private static final String TARGET_OVERLAY_TEXT_SCALE_KEY = "target_overlay.text_scale";

    private Stage overlayStage;
    private double overlayOpacity = 0.92; // Valeur par défaut
    private Slider opacitySlider;
    private Slider textScaleSlider;
    private double textScale = 1.0;
    private StackPane stackPane;
    private VBox targetPanelComponent;

    public TargetOverlayComponent() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (overlayStage != null && overlayStage.isShowing()) {
                saveOverlayPreferences();
            }
        }));
        
        // Écouter les changements de langue pour mettre à jour l'overlay
        LocalizationService.getInstance().addLanguageChangeListener(locale -> {
            if (overlayStage != null && overlayStage.isShowing()) {
                // Recréer le contenu avec la nouvelle langue
                // Note: nécessite de stocker les dernières stats et missions
                refreshOverlayContent();
            }
        });
    }
    
    // Variables pour stocker les dernières données
    private Map<TargetType, CibleStats> lastStats;
    private Map<String, Mission> lastMissions;
    /**
     * Affiche l'overlay pour les cibles données
     */
    public void showOverlay(Map<TargetType, CibleStats> stats, Map<String, Mission> missions) {
        // Sauvegarder les données pour la mise à jour de langue
        lastStats = stats;
        lastMissions = missions;
        
        // Si la fenêtre est déjà ouverte, on la ferme
        if (overlayStage != null && overlayStage.isShowing()) {
            saveOverlayPreferences();
            overlayStage.close();
            overlayStage = null;
            return;
        }

        createOverlayStage(stats, missions);
    }
    
    /**
     * Rafraîchit le contenu de l'overlay avec la nouvelle langue
     */
    private void refreshOverlayContent() {
        if (lastStats != null && lastMissions != null && overlayStage != null && overlayStage.isShowing()) {
            // Nettoyer l'ancien panneau avant de le remplacer
            if (targetPanelComponent != null && stackPane.getChildren().contains(targetPanelComponent)) {
                cleanupNode(targetPanelComponent);
                stackPane.getChildren().remove(targetPanelComponent);
            }
            
            // Recréer le panneau avec les nouvelles traductions
            VBox newPanel = createTargetPanel(lastStats, lastMissions);
            newPanel.getStyleClass().add("mirror-overlay");
            
            // Ajouter le nouveau panneau dans le stackPane
            stackPane.getChildren().add(0, newPanel);
            
            // Appliquer le scaling actuel
            applyTextScaleToNode(newPanel, textScale);
            targetPanelComponent = newPanel;
        }
    }

    /**
     * Ferme l'overlay s'il est ouvert
     */
    public void closeOverlay() {
        if (overlayStage != null && overlayStage.isShowing()) {
            saveOverlayPreferences();
            // Nettoyer le container pour les popups
            PopupManager popupManager = PopupManager.getInstance();
            popupManager.unregisterContainer(overlayStage);
            overlayStage.close();
            overlayStage = null;
        }
    }

    /**
     * Met à jour le contenu de l'overlay avec de nouvelles stats
     */
    public void updateContent(Map<TargetType, CibleStats> stats, Map<String, Mission> missions) {
        // Sauvegarder les données pour la mise à jour de langue
        lastStats = stats;
        lastMissions = missions;
        
        if (overlayStage != null && overlayStage.isShowing() && stackPane != null) {
            // Nettoyer l'ancien panneau avant de le remplacer
            if (targetPanelComponent != null && stackPane.getChildren().contains(targetPanelComponent)) {
                cleanupNode(targetPanelComponent);
                stackPane.getChildren().remove(targetPanelComponent);
            }
            
            // Recréer complètement le panneau avec les nouvelles stats
            VBox newPanel = createTargetPanel(stats, missions);
            newPanel.getStyleClass().add("mirror-overlay");
            
            // Ajouter le nouveau panneau dans le stackPane
            stackPane.getChildren().add(0, newPanel);
            
            // Appliquer le scaling actuel
            applyTextScaleToNode(newPanel, textScale);
            targetPanelComponent = newPanel;
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
    private void createOverlayStage(Map<TargetType, CibleStats> stats, Map<String, Mission> missions) {

        
        // Création de la fenêtre overlay
        overlayStage = new Stage();
        overlayStage.initStyle(StageStyle.TRANSPARENT);
        overlayStage.setAlwaysOnTop(true);
        overlayStage.setTitle("Targets Overlay");
        overlayStage.setResizable(true);
        overlayStage.setMinWidth(MIN_WIDTH_OVERLAY);
        overlayStage.setMinHeight(MIN_HEIGHT_OVERLAY);

        // Restaurer les préférences sauvegardées
        restoreOverlayPreferences();

        // Créer le contenu de l'overlay
        createOverlayContent(stats, missions);

        // Configurer la scène
        Scene scene = new Scene(stackPane);
        scene.setFill(Color.TRANSPARENT);
        overlayStage.setScene(scene);
        overlayStage.setOpacity(1.0);

        // Appliquer les styles CSS
        scene.getStylesheets().add(getClass().getResource("/css/elite-theme.css").toExternalForm());
        stackPane.getStyleClass().addAll("overlay-root", "overlay-root-bordered");
        stackPane.setOnMouseExited(event -> {
            stackPane.getStyleClass().remove("overlay-root-bordered");
        });
        
        // Enregistrer le StackPane comme container pour les popups
        PopupManager popupManager = PopupManager.getInstance();
        popupManager.registerContainer(overlayStage, stackPane);
        
        // Configurer les interactions (déplacement, redimensionnement)
        setupInteractions();

        // Afficher l'overlay
        overlayStage.show();
    }

    /**
     * Crée le contenu de l'overlay
     */
    private void createOverlayContent(Map<TargetType, CibleStats> stats, Map<String, Mission> missions) {
        // Créer le panneau de cibles
        targetPanelComponent = createTargetPanel(stats, missions);
        targetPanelComponent.getStyleClass().add("mirror-overlay");

        // Créer l'icône de redimensionnement
        Label resizeHandle = createResizeHandle();

        // Créer le curseur de transparence
        opacitySlider = createOpacitySlider();
        
        // Créer le curseur de scaling du texte
        textScaleSlider = createTextScaleSlider();

        // Créer le conteneur principal
        stackPane = new StackPane();
        stackPane.getChildren().addAll(targetPanelComponent, resizeHandle, opacitySlider, textScaleSlider);
        StackPane.setAlignment(resizeHandle, Pos.BOTTOM_RIGHT);
        StackPane.setAlignment(opacitySlider, Pos.BOTTOM_RIGHT);
        StackPane.setAlignment(textScaleSlider, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(opacitySlider, new Insets(0, 30, 0, 0));
        StackPane.setMargin(textScaleSlider, new Insets(0, 60, 20, 0));
        stackPane.setPickOnBounds(true);

        // Appliquer le style initial
        updatePaneStyle(overlayOpacity, stackPane);
        
        // Appliquer le scaling initial du texte
        applyTextScaleToNode(targetPanelComponent, textScale);

        // Configurer les listeners du curseur
        setupOpacitySliderListener();
        setupTextScaleSliderListener();
    }

    /**
     * Crée un panneau de cibles pour l'overlay
     */
    private VBox createTargetPanel(Map<TargetType, CibleStats> stats, Map<String, Mission> missions) {
        VBox panel = new VBox();
        panel.getStyleClass().add("target-panel");
        panel.setSpacing(10);
        panel.setPadding(new Insets(15));
        
        LocalizationService localizationService = LocalizationService.getInstance();

        // 🔒 SNAPSHOT pour éviter ConcurrentModificationException
        // La map peut être modifiée pendant l'itération (ObservableMap, etc.)
        List<Map.Entry<TargetType, CibleStats>> statsSnapshot = new ArrayList<>(stats.entrySet());
        
        // Sections
        for (Map.Entry<TargetType, CibleStats> entry : statsSnapshot) {
            TargetType targetType = entry.getKey();
            CibleStats cibleStats = entry.getValue();
            
            // Titre de section
            Label sectionTitle = new Label();
            if (targetType == TargetType.PIRATE) {
                sectionTitle.setText(localizationService.getString("targets.pirates"));
                sectionTitle.getStyleClass().addAll("target-section-title", "pirate-title");
            } else {
                sectionTitle.setText(localizationService.getString("targets.conflict"));
                sectionTitle.getStyleClass().addAll("target-section-title", "conflict-title");
            }
            
            HBox titleContainer = new HBox();
            titleContainer.setAlignment(Pos.CENTER);
            titleContainer.getChildren().add(sectionTitle);
            
            GridPane grid = createStatsGrid();
            createTargetGridContent(cibleStats, grid, missions);
            
            panel.getChildren().addAll(titleContainer, grid);
        }
        
        // Ajouter les infos du dernier ship destroyed en bas
        addDestroyedShipInfo(panel);
        
        return panel;
    }
    
    /**
     * Ajoute les totaux de bounty et combat bonds en bas du panel
     */
    private void addDestroyedShipInfo(VBox panel) {
        DestroyedShipsRegistery registry = DestroyedShipsRegistery.getInstance();
        LocalizationService localizationService = LocalizationService.getInstance();
        MissionsRegistry missionsRegistry = MissionsRegistry.getInstance();
        
        if (registry != null) {
            int totalBounty = registry.getTotalBountyEarned();
            int totalConflictBounty = registry.getTotalConflictBounty();
            
            // Calculer les pending credits
            // 🔒 SNAPSHOT pour éviter ConcurrentModificationException
            List<Mission> missionsSnapshot = new ArrayList<>(missionsRegistry.getGlobalMissionMap().values());
            long pendingCredits = missionsSnapshot.stream()
                    .filter(Mission::isPending)
                    .mapToLong(Mission::getReward)
                    .sum();
            
            // Afficher seulement si au moins un total est différent de 0 ou s'il y a des pending credits
            if (totalBounty > 0 || totalConflictBounty > 0 || pendingCredits > 0) {
                // Séparateur
                Separator separator = new Separator();
                separator.getStyleClass().add("target-separator");
                panel.getChildren().add(separator);
                
                // Conteneur pour les totaux
                VBox totalContainer = new VBox(5);
                totalContainer.setPadding(new Insets(10, 0, 0, 0));
                
                // Affichage des bounty/bonds
                if (totalBounty > 0 || totalConflictBounty > 0) {
                    StringBuilder totals = new StringBuilder();
                    if (totalBounty > 0) {
                        totals.append(localizationService.getString("targets.total_bounty")).append(": ")
                               .append(getFormattedNumber(totalBounty)).append(" Cr");
                    }
                    if (totalBounty > 0 && totalConflictBounty > 0) {
                        totals.append(" / ");
                    }
                    if (totalConflictBounty > 0) {
                        totals.append(localizationService.getString("targets.total_bonds")).append(": ")
                               .append(getFormattedNumber(totalConflictBounty)).append(" Cr");
                    }
                    
                    Label totalLabel = new Label(totals.toString());
                    totalLabel.setStyle("-fx-text-fill: -fx-elite-success; -fx-font-weight: bold; -fx-font-size: 0.9em;");
                    totalContainer.getChildren().add(totalLabel);
                }
                
                // Affichage des pending credits
                if (pendingCredits > 0) {
                    Label pendingLabel = new Label(localizationService.getString("targets.pending") + ": " + getFormattedNumber(pendingCredits) + " Cr");
                    pendingLabel.setStyle("-fx-text-fill: -fx-elite-success; -fx-font-weight: bold; -fx-font-size: 0.9em;");
                    totalContainer.getChildren().add(pendingLabel);
                }
                
                panel.getChildren().add(totalContainer);
            }
        }
    }
    
    /**
     * Crée une grille de statistiques
     */
    private GridPane createStatsGrid() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("faction-grid");
        grid.setHgap(0);
        grid.setVgap(5);
        grid.setAlignment(Pos.TOP_LEFT);
        
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(150);
        col1.setPrefWidth(200);
        col1.setHgrow(Priority.ALWAYS);
        col1.setHalignment(HPos.LEFT);
        
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setMinWidth(150); // Augmenté encore plus pour éviter la troncature
        col2.setPrefWidth(150);
        col2.setHgrow(Priority.NEVER);
        col2.setHalignment(HPos.RIGHT);
        
        grid.getColumnConstraints().addAll(col1, col2);
        return grid;
    }
    
    /**
     * Remplit la grille avec le contenu des stats
     */
    private void createTargetGridContent(CibleStats cibleStats, GridPane grid, Map<String, Mission> missions) {
        int rowIndex = 0;
        LocalizationService localizationService = LocalizationService.getInstance();
        PopupManager popupManager = PopupManager.getInstance();
        CopyClipboardManager copyClipboardManager = CopyClipboardManager.getInstance();
        
        // 🔒 SNAPSHOT pour éviter ConcurrentModificationException
        List<TargetFactionStats> factionsSnapshot = new ArrayList<>(cibleStats.getFactions().values());
        
        for (TargetFactionStats targetFaction : factionsSnapshot) {
            // 🔒 SNAPSHOT pour éviter ConcurrentModificationException
            List<SourceFactionStats> sourcesSnapshot = new ArrayList<>(targetFaction.getSources().values());
            int maxKills = sourcesSnapshot.stream()
                    .mapToInt(SourceFactionStats::getKills)
                    .max()
                    .orElse(0);
            
            // Ajouter la faction cible comme titre de groupe
            Label targetFactionLabel = new Label(targetFaction.getTargetFaction());
            targetFactionLabel.getStyleClass().add("target-faction-header");
            
            // Ajouter tooltip et clic pour copier le système de destination
            String destinationSystem = findDestinationSystemForFaction(targetFaction.getTargetFaction(), missions);
            if (destinationSystem != null && !destinationSystem.isEmpty()) {
                String tooltipText = localizationService.getString("tooltip.destination_system") + ": " + destinationSystem;
                targetFactionLabel.setTooltip(new TooltipComponent(tooltipText));
                targetFactionLabel.getStyleClass().add("clickable-system-target");
                targetFactionLabel.setOnMouseClicked(e -> {
                    copyClipboardManager.copyToClipboard(destinationSystem);
                    // Utiliser le Stage de l'overlay au lieu de la scène du grid
                    popupManager.showPopup(localizationService.getString("system.copied"), e.getSceneX(), e.getSceneY(), overlayStage);
                });
            } else {
                targetFactionLabel.setTooltip(new TooltipComponent(localizationService.getString("tooltip.destination_system_undefined")));
            }
            
            // Span sur toutes les colonnes pour le titre de faction cible
            grid.add(targetFactionLabel, 0, rowIndex, 2, 1);
            rowIndex++;
            
            // Ajouter les sources liées à cette faction cible
            // Utiliser le snapshot créé plus haut
            for (SourceFactionStats src : sourcesSnapshot) {
                Label sourceLabel = new Label("  " + src.getSourceFaction());
                sourceLabel.getStyleClass().add("source-faction-label");
                sourceLabel.setWrapText(false);
                sourceLabel.setPrefWidth(Region.USE_COMPUTED_SIZE);
                sourceLabel.setMinWidth(Region.USE_COMPUTED_SIZE);
                
                // Ajouter tooltip et clic pour copier le système d'origine de cette faction
                String[] sourceInfo = findOriginSystemAndStationForFaction(src.getSourceFaction(), missions);
                String sourceSystem = sourceInfo[0];
                String sourceStation = sourceInfo[1];
                if (sourceSystem != null && !sourceSystem.isEmpty()) {
                    String tooltipText = localizationService.getString("tooltip.origin_system") + ": " + sourceSystem;
                    if (sourceStation != null && !sourceStation.isEmpty()) {
                        tooltipText += " | " + sourceStation;
                    }
                    sourceLabel.setTooltip(new TooltipComponent(tooltipText));
                    sourceLabel.getStyleClass().add("clickable-system-source");
                    sourceLabel.setOnMouseClicked(e -> {
                        copyClipboardManager.copyToClipboard(sourceSystem);
                        popupManager.showPopup(localizationService.getString("system.copied"), e.getSceneX(), e.getSceneY(), overlayStage);
                    });
                } else {
                    String tooltipText = localizationService.getString("tooltip.origin_system_undefined");
                    sourceLabel.setTooltip(new TooltipComponent(tooltipText));
                }
                
                Label killsLabel;
                if (src.getKills() == maxKills) {
                    killsLabel = new Label(String.valueOf(src.getKills()));
                    killsLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #FF6B00;");
                } else {
                    int difference = maxKills - src.getKills();
                    killsLabel = new Label(src.getKills() + " (-" + difference + ")");
                    killsLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #00FF00;");
                }
                killsLabel.getStyleClass().addAll("faction-col", "kills");
                // S'assurer que le texte n'est pas tronqué
                killsLabel.setWrapText(false);
                killsLabel.setPrefWidth(Region.USE_COMPUTED_SIZE);
                killsLabel.setMinWidth(Region.USE_COMPUTED_SIZE);
                killsLabel.setMaxWidth(Double.MAX_VALUE);
                GridPane.setHgrow(killsLabel, Priority.NEVER);
                
                grid.add(sourceLabel, 0, rowIndex);
                grid.add(killsLabel, 1, rowIndex);
                
                rowIndex++;
            }
            
            // Ajouter un espace entre les groupes de factions cibles
            rowIndex++;
        }
    }
    
    /**
     * Trouve le système de destination pour une faction cible
     */
    private String findDestinationSystemForFaction(String targetFaction, Map<String, Mission> missions) {
        if (missions == null) return null;
        
        // 🔒 SNAPSHOT pour éviter ConcurrentModificationException
        List<Mission> missionsSnapshot = new ArrayList<>(missions.values());
        return missionsSnapshot.stream()
                .filter(mission -> mission.getTargetFaction() != null && mission.getTargetFaction().equals(targetFaction))
                .filter(mission -> mission.getDestinationSystem() != null && !mission.getDestinationSystem().isEmpty())
                .map(Mission::getDestinationSystem)
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Trouve le système et la station d'origine pour une faction source
     */
    private String[] findOriginSystemAndStationForFaction(String sourceFaction, Map<String, Mission> missions) {
        if (missions == null) return new String[]{null, null};
        
        // 🔒 SNAPSHOT pour éviter ConcurrentModificationException
        List<Mission> missionsSnapshot = new ArrayList<>(missions.values());
        return missionsSnapshot.stream()
                .filter(mission -> mission.getFaction() != null && mission.getFaction().equals(sourceFaction))
                .filter(mission -> mission.getOriginSystem() != null && !mission.getOriginSystem().isEmpty())
                .map(mission -> new String[]{mission.getOriginSystem(), mission.getOriginStation()})
                .findFirst()
                .orElse(new String[]{null, null});
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
        slider.setPrefHeight(120);
        slider.setPrefWidth(12);
        slider.setOpacity(0.0);
        slider.getStyleClass().add("opacity-slider");

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
        slider.setOpacity(0.0);
        slider.getStyleClass().add("text-scale-slider");
        
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
     * Met à jour le scaling du texte dans le panneau
     */
    private void updateTextScale(double scale) {
        if (targetPanelComponent != null) {
            applyTextScaleToNode(targetPanelComponent, scale);
        }
    }
    
    /**
     * Applique le scaling du texte récursivement à tous les nœuds de texte
     * Cette méthode préserve les styles existants et applique seulement le font-size
     */
    private void applyTextScaleToNode(javafx.scene.Node node, double scale) {
        if (node instanceof Label) {
            Label label = (Label) node;
            String existingStyle = label.getStyle();
            
            // Extraire les styles existants (couleurs, font-weight, etc.)
            String preservedStyles = "";
            if (existingStyle != null && !existingStyle.isEmpty()) {
                // Ne garder que les styles qui ne sont pas font-size
                preservedStyles = existingStyle.replaceAll("-fx-font-size:\\s*[^;]+;?", "");
                // Ajouter le font-size avec le scale
                String scaleStyle = String.format(Locale.ENGLISH, "-fx-font-size: %.1fem;", scale);
                label.setStyle(preservedStyles + " " + scaleStyle);
            } else {
                String scaleStyle = String.format(Locale.ENGLISH, "-fx-font-size: %.1fem;", scale);
                label.setStyle(scaleStyle);
            }
        } else if (node instanceof javafx.scene.layout.Pane) {
            javafx.scene.layout.Pane pane = (javafx.scene.layout.Pane) node;
            // 🔒 SNAPSHOT pour éviter ConcurrentModificationException
            // La liste des enfants peut être modifiée pendant l'itération
            List<javafx.scene.Node> childrenSnapshot = new ArrayList<>(pane.getChildren());
            for (javafx.scene.Node child : childrenSnapshot) {
                applyTextScaleToNode(child, scale);
            }
        }
    }

    /**
     * Nettoie récursivement un nœud et ses enfants pour éviter les fuites mémoire
     * Supprime les listeners, détache les bindings, et vide les enfants
     */
    private void cleanupNode(javafx.scene.Node node) {
        if (node == null) {
            return;
        }
        
        // Si c'est un Parent, nettoyer récursivement tous les enfants
        if (node instanceof Parent) {
            Parent parent = (Parent) node;
            // Créer une copie de la liste des enfants pour éviter ConcurrentModificationException
            List<javafx.scene.Node> children = new ArrayList<>(parent.getChildrenUnmodifiable());
            for (javafx.scene.Node child : children) {
                cleanupNode(child);
            }
            // Vider la liste des enfants
            if (parent instanceof javafx.scene.layout.Pane) {
                ((javafx.scene.layout.Pane) parent).getChildren().clear();
            }
        }
        
        // Détacher les bindings des propriétés courantes pour éviter les fuites mémoire
        if (node instanceof javafx.scene.layout.Region region) {
            // Débinder les propriétés de taille qui pourraient être liées
            // Note: widthProperty() et heightProperty() sont en lecture seule, on ne peut pas les délier
            region.prefWidthProperty().unbind();
            region.prefHeightProperty().unbind();
            region.minWidthProperty().unbind();
            region.minHeightProperty().unbind();
            region.maxWidthProperty().unbind();
            region.maxHeightProperty().unbind();
        }
        
        // Détacher le nœud de son parent si possible
        javafx.scene.Parent parent = node.getParent();
        if (parent instanceof javafx.scene.layout.Pane) {
            ((javafx.scene.layout.Pane) parent).getChildren().remove(node);
        }
        
        // Réinitialiser les propriétés qui pourraient avoir des listeners
        node.setStyle(null);
        node.setUserData(null);
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
                    ((Label) stackPane.getChildren().get(1)).setOpacity(1.0);
                    ((Slider) stackPane.getChildren().get(2)).setOpacity(0.8);
                    ((Slider) stackPane.getChildren().get(3)).setOpacity(0.8);
                }
            } else {
                scene.setCursor(javafx.scene.Cursor.DEFAULT);
                if (stackPane.getChildren().size() > 3) {
                    ((Label) stackPane.getChildren().get(1)).setOpacity(0.8);
                    ((Slider) stackPane.getChildren().get(2)).setOpacity(0.8);
                    ((Slider) stackPane.getChildren().get(3)).setOpacity(0.8);
                }
            }
        });

        // Masquer les contrôles quand la souris quitte la scène
        scene.setOnMouseExited(e -> {
            if (stackPane.getChildren().size() > 3) {
                ((Label) stackPane.getChildren().get(1)).setOpacity(0.0);
                ((Slider) stackPane.getChildren().get(2)).setOpacity(0.0);
                ((Slider) stackPane.getChildren().get(3)).setOpacity(0.0);
            }
        });

        // Afficher les contrôles quand la souris entre dans la scène
        scene.setOnMouseEntered(e -> {
            if (stackPane.getChildren().size() > 3) {
                ((Label) stackPane.getChildren().get(1)).setOpacity(0.8);
                ((Slider) stackPane.getChildren().get(2)).setOpacity(0.8);
                ((Slider) stackPane.getChildren().get(3)).setOpacity(0.8);
            }
        });

        // Listener pour la fermeture de la fenêtre
        overlayStage.setOnCloseRequest(event -> {
            saveOverlayPreferences();
            PopupManager popupManager = PopupManager.getInstance();
            popupManager.unregisterContainer(overlayStage);
            overlayStage = null;
        });
    }

    /**
     * Restaure les préférences de l'overlay
     */
    private void restoreOverlayPreferences() {
        String savedWidthStr = preferencesService.getPreference(TARGET_OVERLAY_WIDTH_KEY, "350");
        String savedHeightStr = preferencesService.getPreference(TARGET_OVERLAY_HEIGHT_KEY, "400");
        String savedOpacityStr = preferencesService.getPreference(TARGET_OVERLAY_OPACITY_KEY, "0.92");
        String savedXStr = preferencesService.getPreference(TARGET_OVERLAY_X_KEY, "100");
        String savedYStr = preferencesService.getPreference(TARGET_OVERLAY_Y_KEY, "100");
        String savedTextScaleStr = preferencesService.getPreference(TARGET_OVERLAY_TEXT_SCALE_KEY, "1.0");

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
            preferencesService.setPreference(TARGET_OVERLAY_WIDTH_KEY, String.valueOf((int) overlayStage.getWidth()));
            preferencesService.setPreference(TARGET_OVERLAY_HEIGHT_KEY, String.valueOf((int) overlayStage.getHeight()));
            preferencesService.setPreference(TARGET_OVERLAY_OPACITY_KEY, String.valueOf(overlayOpacity));
            preferencesService.setPreference(TARGET_OVERLAY_X_KEY, String.valueOf((int) overlayStage.getX()));
            preferencesService.setPreference(TARGET_OVERLAY_Y_KEY, String.valueOf((int) overlayStage.getY()));
            preferencesService.setPreference(TARGET_OVERLAY_TEXT_SCALE_KEY, String.valueOf(textScale));
            System.out.println("💾 Préférences target overlay sauvegardées: " +
                    (int) overlayStage.getWidth() + "x" + (int) overlayStage.getHeight() +
                    " (opacité: " + String.format("%.2f", overlayOpacity) + 
                    ", position: " + (int) overlayStage.getX() + "," + (int) overlayStage.getY() +
                    ", scaling: " + String.format("%.2f", textScale) + ")");
        }
    }
}
