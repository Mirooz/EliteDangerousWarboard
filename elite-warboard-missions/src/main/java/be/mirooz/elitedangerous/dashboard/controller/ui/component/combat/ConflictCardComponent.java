package be.mirooz.elitedangerous.dashboard.controller.ui.component.combat;

import be.mirooz.elitedangerous.dashboard.controller.ui.component.TooltipComponent;
import be.mirooz.elitedangerous.dashboard.controller.ui.manager.CopyClipboardManager;
import be.mirooz.elitedangerous.dashboard.controller.ui.manager.PopupManager;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.model.ConflictSystem;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/**
 * Composant pour afficher une carte de conflit
 */
public class ConflictCardComponent extends HBox {
    private final ConflictSystem conflict;
    private final PopupManager popupManager;
    private final LocalizationService localizationService = LocalizationService.getInstance();

    private final CopyClipboardManager copyClipboardManager = CopyClipboardManager.getInstance();
    public ConflictCardComponent(ConflictSystem conflict) {
        this.conflict = conflict;
        this.popupManager = PopupManager.getInstance();
        initializeComponent();
    }

    private void initializeComponent() {
        getStyleClass().add("massacre-card");

        // Système
        Label systemLabel = new Label(conflict.getSystemName());
        systemLabel.getStyleClass().addAll("conflict-card-title", "clickable-system-source");
        systemLabel.setTooltip(new TooltipComponent(conflict.getSystemName()));
        systemLabel.setOnMouseClicked(e -> onClickSystem(conflict.getSystemName(), e));

        // Distance
        Label distanceLabel = new Label(conflict.getDistanceLy()
                + " "
                + localizationService.getString("search.distance.unit"));
        distanceLabel.getStyleClass().add("conflict-card-distance");

        // Gouvernement (Faction 2)
        Label faction = new Label(conflict.getFaction());
        faction.getStyleClass().add("conflict-card-government");

        // Allégeance (Faction 1)
        Label opponent = new Label(conflict.getOpponentFaction());
        opponent.getStyleClass().add("conflict-card-allegiance");

        Label separator = new Label("|   ");
        // Updated (utilise le statut pour l'instant)
        Label updatedLabel = new Label(String.valueOf(conflict.getSurfaceConflicts()));
        updatedLabel.getStyleClass().add("conflict-card-updated");

        getChildren().addAll(systemLabel, distanceLabel, faction, separator,opponent, updatedLabel);
    }

    private void onClickSystem(String systemName, javafx.scene.input.MouseEvent event) {
        copyClipboardManager.copyToClipboard(systemName);
        Stage stage = (Stage) getScene().getWindow();
        popupManager.showPopup(localizationService.getString("system.copied"), event.getSceneX(), event.getSceneY(),stage);
    }
}
