package be.mirooz.elitedangerous.dashboard.view.combat;

import be.mirooz.elitedangerous.dashboard.model.targetpanel.CibleStats;
import be.mirooz.elitedangerous.dashboard.model.targetpanel.SourceFactionStats;
import be.mirooz.elitedangerous.dashboard.model.targetpanel.TargetFactionStats;
import be.mirooz.elitedangerous.dashboard.model.enums.TargetType;
import be.mirooz.elitedangerous.dashboard.model.commander.Mission;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.view.common.TooltipComponent;
import be.mirooz.elitedangerous.dashboard.view.common.overlay.OverlayUi;
import be.mirooz.elitedangerous.dashboard.view.common.managers.PopupManager;
import be.mirooz.elitedangerous.dashboard.view.common.managers.CopyClipboardManager;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.Map;

/**
 * Composant pour afficher les statistiques des cibles (pirates et conflits)
 * de manière structurée et alignée grâce à GridPane
 */
public class TargetPanelComponent extends VBox {

    private final GridPane pirateGrid;
    private final GridPane conflictGrid;
    private final Label pirateTitle;
    private final Label conflictTitle;
    private final Separator separator;
    private final LocalizationService localizationService;
    private final Label mainTitle;
    private final PopupManager popupManager;
    private final CopyClipboardManager copyClipboardManager;
    private final Button overlayButton;
    private final ToggleButton overlayPassThroughLockButton;
    private Runnable onOverlayButtonClick;

    public TargetPanelComponent() {
        super();
        this.getStyleClass().add("target-panel");
        this.localizationService = LocalizationService.getInstance();
        this.popupManager = PopupManager.getInstance();
        this.copyClipboardManager = CopyClipboardManager.getInstance();

        // Créer le bouton overlay
        overlayButton = new Button();
        overlayButton.getStyleClass().add("elite-nav-button");
        overlayButton.setOnAction(e -> {
            if (onOverlayButtonClick != null) {
                onOverlayButtonClick.run();
            }
        });

        overlayPassThroughLockButton = new ToggleButton();
        OverlayUi.applyOverlayLockToggleStyle(overlayPassThroughLockButton);
        overlayPassThroughLockButton.setSelected(false);
        Tooltip lockTip = new Tooltip();
        lockTip.setWrapText(true);
        lockTip.setMaxWidth(340);
        lockTip.setShowDelay(Duration.millis(200));
        overlayPassThroughLockButton.setTooltip(lockTip);
        OverlayUi.updateLockToggleGlyph(overlayPassThroughLockButton);
        OverlayUi.refreshLockTooltip(overlayPassThroughLockButton, localizationService);

        // Créer le titre
        mainTitle = new Label();
        mainTitle.getStyleClass().add("target-panel-title");
        mainTitle.setWrapText(false);
        mainTitle.setMaxWidth(Double.MAX_VALUE);

        /* Deux lignes : évite l’écrasement titre + boutons quand la colonne est étroite
         * (et ne pas appeler setMinWidth(USE_PREF_SIZE) avant que le texte du titre soit défini). */
        VBox titleBlock = new VBox(6);
        titleBlock.setFillWidth(true);
        titleBlock.getStyleClass().add("target-panel-header");
        HBox overlayActionsRow = new HBox(8);
        overlayActionsRow.setAlignment(Pos.CENTER_RIGHT);
        Region overlayRowSpacer = new Region();
        HBox.setHgrow(overlayRowSpacer, Priority.ALWAYS);
        overlayActionsRow.getChildren().addAll(overlayRowSpacer, overlayButton, overlayPassThroughLockButton);
        titleBlock.getChildren().addAll(mainTitle, overlayActionsRow);

        // Section Pirates
        pirateTitle = new Label();
        pirateTitle.getStyleClass().addAll("target-section-title", "pirate-title");
        HBox pirateTitleContainer = new HBox();
        pirateTitleContainer.setAlignment(Pos.CENTER);
        pirateTitleContainer.getChildren().add(pirateTitle);
        pirateGrid = createStatsGrid();

        // Séparateur
        separator = new Separator();
        separator.getStyleClass().add("target-separator");

        // Section Conflits
        conflictTitle = new Label();
        conflictTitle.getStyleClass().addAll("target-section-title", "conflict-title");

        HBox conflictTitleContainer = new HBox();
        conflictTitleContainer.setAlignment(Pos.CENTER);
        conflictTitleContainer.getChildren().add(conflictTitle);
        conflictGrid = createStatsGrid();

        // Layout global
        this.getChildren().addAll(
                titleBlock,
                pirateTitleContainer,
                pirateGrid,
                separator,
                conflictTitleContainer,
                conflictGrid
        );
        this.setSpacing(10);
        this.setPadding(new Insets(10));

        // Masquer par défaut
        setSectionVisible(pirateTitle, pirateGrid, false);
        separator.setVisible(false);
        separator.setManaged(false);
        setSectionVisible(conflictTitle, conflictGrid, false);

        // Traductions
        updateTranslations();
        localizationService.addLanguageChangeListener(locale -> {
            updateTranslations();
            updateOverlayButtonText(false);
        });
        
        // Mettre à jour le texte du bouton overlay initialement
        updateOverlayButtonText(false);
    }

    private GridPane createStatsGrid() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("faction-grid");
        grid.setHgap(0);
        grid.setVgap(5);
        grid.setAlignment(Pos.TOP_LEFT);

        // Colonnes : Source (noms longs) | Kills (ex. "42 (-12)" en gras)
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(200);
        col1.setPrefWidth(320);
        col1.setHgrow(Priority.ALWAYS);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setMinWidth(100);
        col2.setPrefWidth(120);
        col2.setHalignment(HPos.RIGHT);
        col2.setHgrow(Priority.NEVER);

        grid.getColumnConstraints().addAll(col1, col2);

        return grid;
    }

    public void displayStats(Map<TargetType, CibleStats> stats, Map<String, Mission> missions) {
        pirateGrid.getChildren().clear();
        conflictGrid.getChildren().clear();

        boolean hasPirates = false;
        boolean hasConflicts = false;

        for (Map.Entry<TargetType, CibleStats> entry : stats.entrySet()) {
            TargetType targetType = entry.getKey();
            CibleStats cibleStats = entry.getValue();

            if (targetType == TargetType.PIRATE) {
                displayStats(cibleStats, pirateGrid, missions);
                hasPirates = true;
            } else {
                displayStats(cibleStats, conflictGrid, missions);
                hasConflicts = true;
            }
        }

        // Visibilité des sections (sans headers)
        setSectionVisible(pirateTitle, pirateGrid, hasPirates);
        setSectionVisible(conflictTitle, conflictGrid, hasConflicts);

        separator.setVisible(hasPirates && hasConflicts);
        separator.setManaged(hasPirates && hasConflicts);
    }

    private void displayStats(CibleStats cibleStats, GridPane grid, Map<String, Mission> missions) {
        int rowIndex = 0; // Pas d'en-têtes

        for (TargetFactionStats targetFaction : cibleStats.getFactions().values()) {
            int maxKills = targetFaction.getSources().values().stream()
                    .mapToInt(SourceFactionStats::getKills)
                    .max()
                    .orElse(0);

            // Ajouter la faction cible comme titre de groupe
            Label targetFactionLabel = new Label(targetFaction.getTargetFaction());
            targetFactionLabel.getStyleClass().add("target-faction-header");
            targetFactionLabel.setWrapText(true);
            targetFactionLabel.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(targetFactionLabel, Priority.ALWAYS);
            
            // Ajouter tooltip et clic pour copier le système de destination
            String destinationSystem = findDestinationSystemForFaction(targetFaction.getTargetFaction(), missions);
            if (destinationSystem != null && !destinationSystem.isEmpty()) {
                String tooltipText = localizationService.getString("tooltip.destination_system") + ": " + destinationSystem;
                targetFactionLabel.setTooltip(new TooltipComponent(tooltipText));
                targetFactionLabel.getStyleClass().add("clickable-system-target");
                targetFactionLabel.setOnMouseClicked(e -> onClickSystem(destinationSystem, e));
            } else {
                targetFactionLabel.setTooltip(new TooltipComponent(localizationService.getString("tooltip.destination_system_undefined")));
            }
            
            // Span sur toutes les colonnes pour le titre de faction cible
            grid.add(targetFactionLabel, 0, rowIndex, 2, 1);
            rowIndex++;

            // Ajouter les sources liées à cette faction cible
            for (SourceFactionStats src : targetFaction.getSources().values()) {
                Label sourceLabel = new Label("  " + src.getSourceFaction()); // Indentation pour montrer la hiérarchie
                sourceLabel.getStyleClass().add("source-faction-label");
                sourceLabel.setWrapText(true);
                sourceLabel.setMaxWidth(Double.MAX_VALUE);
                GridPane.setHgrow(sourceLabel, Priority.ALWAYS);
                
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
                    sourceLabel.setOnMouseClicked(e -> onClickSystem(sourceSystem, e));
                } else {
                    String tooltipText = localizationService.getString("tooltip.origin_system_undefined");
                    sourceLabel.setTooltip(new TooltipComponent(tooltipText));
                }

                Label killsLabel;
                if (src.getKills() == maxKills) {
                    killsLabel = new Label(String.valueOf(src.getKills()));
                    killsLabel.setStyle("-fx-font-size: 19px; -fx-font-weight: bold; -fx-text-fill: #FF6B00;");
                } else {
                    int difference = maxKills - src.getKills();
                    killsLabel = new Label(src.getKills() + " (-" + difference + ")");
                    killsLabel.setStyle("-fx-font-size: 19px; -fx-text-fill: #00FF00; ");
                }
                killsLabel.getStyleClass().addAll("faction-col", "kills");
                killsLabel.setWrapText(false);
                killsLabel.setMinWidth(Region.USE_PREF_SIZE);
                GridPane.setHgrow(killsLabel, Priority.NEVER);

                // Source dans la colonne 0, kills dans la colonne 1
                grid.add(sourceLabel, 0, rowIndex);
                grid.add(killsLabel, 1, rowIndex);

                rowIndex++;
            }
            
            // Ajouter un espace entre les groupes de factions cibles
            rowIndex++;
        }
    }
    
    // Méthode de compatibilité pour l'ancienne signature
    private void displayStats(CibleStats cibleStats, GridPane grid) {
        displayStats(cibleStats, grid, null);
    }
    
    private String findDestinationSystemForFaction(String targetFaction, Map<String, Mission> missions) {
        if (missions == null) return null;
        
        return missions.values().stream()
                .filter(mission -> mission.getTargetFaction() != null && mission.getTargetFaction().equals(targetFaction))
                .filter(mission -> mission.getDestinationSystem() != null && !mission.getDestinationSystem().isEmpty())
                .map(Mission::getDestinationSystem)
                .findFirst()
                .orElse(null);
    }
    
    private String[] findOriginSystemAndStationForFaction(String sourceFaction, Map<String, Mission> missions) {
        if (missions == null) return new String[]{null, null};
        
        return missions.values().stream()
                .filter(mission -> mission.getFaction() != null && mission.getFaction().equals(sourceFaction))
                .filter(mission -> mission.getOriginSystem() != null && !mission.getOriginSystem().isEmpty())
                .map(mission -> new String[]{mission.getOriginSystem(), mission.getOriginStation()})
                .findFirst()
                .orElse(new String[]{null, null});
    }
    
    private void onClickSystem(String systemName, javafx.scene.input.MouseEvent event) {
        copyClipboardManager.copyToClipboard(systemName);
        Stage stage = (Stage) getScene().getWindow();
        popupManager.showPopup(localizationService.getString("system.copied"), event.getSceneX(), event.getSceneY(), stage);
    }

    public void updateTranslations() {
        mainTitle.setText(localizationService.getString("targets.title"));
        pirateTitle.setText(localizationService.getString("targets.pirates"));
        conflictTitle.setText(localizationService.getString("targets.conflict"));
        refreshOverlayLockUi();
    }

    public ToggleButton getOverlayPassThroughLockButton() {
        return overlayPassThroughLockButton;
    }

    private void refreshOverlayLockUi() {
        if (overlayPassThroughLockButton != null) {
            OverlayUi.updateLockToggleGlyph(overlayPassThroughLockButton);
            OverlayUi.refreshLockTooltip(overlayPassThroughLockButton, localizationService);
        }
    }

    private void setSectionVisible(Label title, GridPane grid, boolean visible) {
        title.setVisible(visible);
        title.setManaged(visible);
        grid.setVisible(visible);
        grid.setManaged(visible);
    }
    
    /**
     * Définit l'action à exécuter lors du clic sur le bouton overlay
     */
    public void setOnOverlayButtonClick(Runnable action) {
        this.onOverlayButtonClick = action;
    }
    
    /**
     * Met à jour le texte du bouton overlay
     */
    public void updateOverlayButtonText(boolean isOverlayShowing) {
        if (overlayButton != null) {
            String text;

            if (isOverlayShowing) {
                text = localizationService.getString("overlay.action.close");
                if (text == null || text.startsWith("overlay.")) {
                    text = localizationService.getString("targets.overlay_close");
                }
                if (text == null || text.startsWith("targets.")) {
                    text = "Close";
                }
                overlayButton.setText("✖ " + text);
            } else {
                text = localizationService.getString("overlay.action.open");
                if (text == null || text.startsWith("overlay.")) {
                    text = localizationService.getString("targets.overlay_open");
                }
                if (text == null || text.startsWith("targets.")) {
                    text = "Overlay";
                }
                overlayButton.setText(OverlayUi.overlayActionLabel(text));
            }
        }
        refreshOverlayLockUi();
    }
}
