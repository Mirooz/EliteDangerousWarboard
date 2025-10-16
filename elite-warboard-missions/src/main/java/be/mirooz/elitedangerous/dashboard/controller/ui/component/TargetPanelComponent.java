package be.mirooz.elitedangerous.dashboard.controller.ui.component;

import be.mirooz.elitedangerous.dashboard.model.targetpanel.CibleStats;
import be.mirooz.elitedangerous.dashboard.model.targetpanel.SourceFactionStats;
import be.mirooz.elitedangerous.dashboard.model.targetpanel.TargetFactionStats;
import be.mirooz.elitedangerous.dashboard.model.enums.TargetType;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;

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

    public TargetPanelComponent() {
        super();
        this.getStyleClass().add("target-panel");
        this.localizationService = LocalizationService.getInstance();

        // Titre principal
        mainTitle = new Label();
        mainTitle.getStyleClass().add("target-panel-title");

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
                mainTitle,
                pirateTitleContainer,
                pirateGrid,
                separator,
                conflictTitleContainer,
                conflictGrid
        );
        this.setSpacing(10);
        this.setPadding(new Insets(15));

        // Masquer par défaut
        setSectionVisible(pirateTitle, pirateGrid, false);
        separator.setVisible(false);
        separator.setManaged(false);
        setSectionVisible(conflictTitle, conflictGrid, false);

        // Traductions
        updateTranslations();
        localizationService.addLanguageChangeListener(locale -> updateTranslations());
    }

    private GridPane createStatsGrid() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("faction-grid");
        grid.setHgap(0);
        grid.setVgap(5);
        grid.setAlignment(Pos.TOP_LEFT);

        // Colonnes : Cible | Source | Kills
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(80);
        col1.setPrefWidth(150);
        col1.setHgrow(Priority.ALWAYS);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setMinWidth(100);
        col2.setPrefWidth(180);
        col2.setHgrow(Priority.ALWAYS);

        ColumnConstraints col3 = new ColumnConstraints();
        col3.setMinWidth(80);
        col3.setPrefWidth(80);
        col3.setHgrow(Priority.NEVER); // kills reste fixe

        grid.getColumnConstraints().addAll(col1, col2, col3);

        return grid;
    }

    public void displayStats(Map<TargetType, CibleStats> stats) {
        pirateGrid.getChildren().clear();
        conflictGrid.getChildren().clear();

        boolean hasPirates = false;
        boolean hasConflicts = false;

        for (Map.Entry<TargetType, CibleStats> entry : stats.entrySet()) {
            TargetType targetType = entry.getKey();
            CibleStats cibleStats = entry.getValue();

            if (targetType == TargetType.PIRATE) {
                displayStats(cibleStats, pirateGrid);
                hasPirates = true;
            } else {
                displayStats(cibleStats, conflictGrid);
                hasConflicts = true;
            }
        }

        // Header + Visibilité
        if (hasPirates) {
            addHeaderRow(pirateGrid);
        }
        setSectionVisible(pirateTitle, pirateGrid, hasPirates);

        if (hasConflicts) {
            addHeaderRow(conflictGrid);
        }
        setSectionVisible(conflictTitle, conflictGrid, hasConflicts);

        separator.setVisible(hasPirates && hasConflicts);
        separator.setManaged(hasPirates && hasConflicts);
    }

    private void addHeaderRow(GridPane grid) {
        Label targetHeader = new Label(localizationService.getString("targets.target_faction"));
        targetHeader.getStyleClass().add("faction-col-header");

        Label sourceHeader = new Label(localizationService.getString("targets.source_faction"));
        sourceHeader.getStyleClass().add("faction-col-header");

        Label killsHeader = new Label(localizationService.getString("targets.kills"));
        killsHeader.getStyleClass().add("faction-col-header");

        grid.add(targetHeader, 0, 0);
        grid.add(sourceHeader, 1, 0);
        grid.add(killsHeader, 2, 0);
    }

    private void displayStats(CibleStats cibleStats, GridPane grid) {
        int rowIndex = 1; // 0 = header

        String lastTargetFaction = null;
        for (TargetFactionStats targetFaction : cibleStats.getFactions().values()) {
            int maxKills = targetFaction.getSources().values().stream()
                    .mapToInt(SourceFactionStats::getKills)
                    .max()
                    .orElse(0);

            for (SourceFactionStats src : targetFaction.getSources().values()) {

                Label targetLabel = new Label(
                        lastTargetFaction != null && lastTargetFaction.equals(targetFaction.getTargetFaction())
                                ? "" : targetFaction.getTargetFaction()
                );
                targetLabel.getStyleClass().add("faction-col");

                Label sourceLabel = new Label(src.getSourceFaction());
                sourceLabel.getStyleClass().add("faction-col");

                Label killsLabel;
                if (src.getKills() == maxKills) {
                    killsLabel = new Label(String.valueOf(src.getKills()));
                    killsLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #FF6B00;");
                } else {
                    int difference = maxKills - src.getKills();
                    killsLabel = new Label(src.getKills() + " (-" + difference + ")");
                    killsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #00FF00;");
                }
                killsLabel.getStyleClass().addAll("faction-col", "kills");

                grid.add(targetLabel, 0, rowIndex);
                grid.add(sourceLabel, 1, rowIndex);
                grid.add(killsLabel, 2, rowIndex);

                lastTargetFaction = targetFaction.getTargetFaction();
                rowIndex++;
            }
            lastTargetFaction = null;
        }
    }

    public void updateTranslations() {
        mainTitle.setText(localizationService.getString("targets.title"));
        pirateTitle.setText(localizationService.getString("targets.pirates"));
        conflictTitle.setText(localizationService.getString("targets.conflict"));
    }

    private void setSectionVisible(Label title, GridPane grid, boolean visible) {
        title.setVisible(visible);
        title.setManaged(visible);
        grid.setVisible(visible);
        grid.setManaged(visible);
    }
}
