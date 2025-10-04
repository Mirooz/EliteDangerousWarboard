package be.mirooz.elitedangerous.dashboard.controller.ui.component;

import be.mirooz.elitedangerous.dashboard.model.targetpanel.CibleStats;
import be.mirooz.elitedangerous.dashboard.model.targetpanel.SourceFactionStats;
import be.mirooz.elitedangerous.dashboard.model.targetpanel.TargetFactionStats;
import be.mirooz.elitedangerous.dashboard.model.enums.TargetType;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Map;

public class FactionStatsComponent extends VBox {

    private static final double COL_WIDTH_TARGET = 150;
    private static final double COL_WIDTH_TARGETFACTION = 150;
    private static final double COL_WIDTH_SOURCE = 180;
    private static final double COL_WIDTH_KILLS = 80;
    public FactionStatsComponent(){
        super();
    }

    public void displayStats(Map<TargetType, CibleStats> stats) {
        String lastCible = null;
        String lastTargetFaction = null;

        for (Map.Entry<TargetType, CibleStats> entry : stats.entrySet()) {
            TargetType targetType = entry.getKey();
            CibleStats cibleStats = entry.getValue();
            // ⚡ Si on change de cible → ajouter un espace
            if (lastCible != null && !lastCible.equals(targetType.getDisplayName())) {
                HBox spacer = new HBox();
                spacer.setMinHeight(15); // hauteur de l’espace
                this.getChildren().add(spacer);
            }
            for (TargetFactionStats stat : cibleStats.getFactions().values()) {
                // Trouver le plus haut score pour cette faction cible
                int maxKills = stat.getSources().values().stream()
                        .mapToInt(SourceFactionStats::getKills)
                        .max()
                        .orElse(0);

                for (SourceFactionStats src : stat.getSources().values()) {
                    HBox row = new HBox(0);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.getStyleClass().add("faction-row");

                    // Label de la cible (une seule fois par bloc cible/faction)
                    Label cibleLabel = new Label(
                            lastCible != null && lastCible.equals(stat.getCible().getDisplayName())
                                    ? "" : targetType.getDisplayName()
                    );
                    cibleLabel.setPrefWidth(COL_WIDTH_TARGET);
                    cibleLabel.getStyleClass().add("faction-col");
                    cibleLabel.setTranslateX(10);

                    // Label de la faction cible
                    Label targetLabel = new Label(
                            lastTargetFaction != null && lastTargetFaction.equals(stat.getTargetFaction())
                                    ? "" : stat.getTargetFaction()
                    );
                    targetLabel.setPrefWidth(COL_WIDTH_TARGETFACTION);
                    targetLabel.getStyleClass().add("faction-col");

                    // Label de la faction source
                    Label sourceLabel = new Label(src.getSourceFaction());
                    sourceLabel.setPrefWidth(COL_WIDTH_SOURCE);
                    sourceLabel.getStyleClass().add("faction-col");
                    sourceLabel.setTranslateX(30);

                    // Label des kills
                    Label killsLabel;
                    if (src.getKills() == maxKills) {
                        // Plus haut score : en gras et orange
                        killsLabel = new Label(String.valueOf(src.getKills()));
                        killsLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #FF6B00;");
                    } else {
                        // Autres scores : avec écart en vert
                        int difference = maxKills - src.getKills();
                        killsLabel = new Label(src.getKills() + " (-" + difference + ")");
                        killsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #00FF00;");
                    }

                    killsLabel.setPrefWidth(COL_WIDTH_KILLS);
                    killsLabel.setAlignment(Pos.CENTER_RIGHT);
                    killsLabel.getStyleClass().addAll("faction-col", "kills");
                    killsLabel.setTranslateX(30);

                    // Construction de la ligne
                    row.getChildren().addAll(cibleLabel, targetLabel, sourceLabel, killsLabel);
                    this.getChildren().add(row);
                    lastCible = stat.getCible().getDisplayName();
                    lastTargetFaction = stat.getTargetFaction();
                }

                lastTargetFaction = null;
            }
        }
    }
}
