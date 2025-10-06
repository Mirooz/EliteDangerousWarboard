package be.mirooz.elitedangerous.dashboard.controller.ui.component;

import be.mirooz.elitedangerous.dashboard.controller.MassacreSearchDialogController;
import be.mirooz.elitedangerous.dashboard.controller.ui.manager.CopyClipboardManager;
import be.mirooz.elitedangerous.dashboard.controller.ui.manager.PopupManager;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.lib.edtools.model.MassacreSystem;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class SystemCardComponent extends VBox{

    private final PopupManager popupManager = PopupManager.getInstance();
    private final CopyClipboardManager copyClipboardManager = CopyClipboardManager.getInstance();
    private final LocalizationService localizationService = LocalizationService.getInstance();
    private final MassacreSystem system;
    private final MassacreSearchDialogController controller;
    public SystemCardComponent(MassacreSystem system, MassacreSearchDialogController controller) {
        this.system = system;
        this.controller = controller;
        buildUI();
    }
    private void buildUI(){
        this.getStyleClass().add("massacre-card");

        // Layout horizontal compact
        HBox mainContent = new HBox();
        mainContent.setAlignment(Pos.CENTER_LEFT);
        mainContent.setSpacing(15);

        // 🔸 Ligne système : source → target / [x]
        HBox systemInfo = new HBox();
        systemInfo.setSpacing(5);
        systemInfo.setAlignment(Pos.CENTER_LEFT);

        Label sourceSystem = new Label(system.getSourceSystem());
        sourceSystem.getStyleClass().addAll("massacre-card-title", "clickable-system-source");
        sourceSystem.setTooltip(new TooltipComponent(system.getSourceSystem()));
        sourceSystem.setOnMouseClicked(e -> onClickSystem(system.getSourceSystem(), e));


        Label targetSystem = new Label("→ " +system.getTargetSystem());
        targetSystem.getStyleClass().addAll("massacre-card-subtitle", "clickable-system-target");
        targetSystem.setTooltip(new TooltipComponent(system.getTargetSystem()));
        targetSystem.setOnMouseClicked(e -> onClickSystem(system.getTargetSystem(), e));

        Label targetCount = new Label("[ " + system.getTargetCount() + " ]");
        targetCount.setOnMouseClicked(e -> onClickTargetCount(system.getTargetSystem()));
        targetCount.setTooltip(new TooltipComponent("Voir systèmes source"));
        targetCount.getStyleClass().addAll("massacre-card-count", "clickable-system-target");

        systemInfo.getChildren().addAll(sourceSystem, targetSystem,targetCount);

        // Distance
        Label distance = new Label(system.getDistanceLy() + " AL");
        distance.getStyleClass().add("massacre-card-distance");

        // Pads
        HBox padsContainer = new HBox();
        padsContainer.setSpacing(5);
        Label LPad = addShipPad("L");
        if (system.getLargePads().isEmpty()) {
            LPad.setOpacity(0);
        }
        padsContainer.getChildren().addAll(LPad, addShipPad("M"), addShipPad("S"));

        // Factions
        HBox factionsContainer = new HBox();
        factionsContainer.setSpacing(5);
        factionsContainer.setAlignment(Pos.CENTER);

        Label fedValue = getSuperFaction(system.getFed());
        fedValue.setTranslateX(30);

        Label impValue = getSuperFaction(system.getImp());
        impValue.setTranslateX(20);

        Label allValue = getSuperFaction(system.getAll());
        allValue.setTranslateX(10);

        Label indValue = getSuperFaction(system.getInd());
        indValue.setTranslateX(-5);

        factionsContainer.getChildren().addAll(fedValue, impValue, allValue, indValue);

        // RES
        Label resValue = new Label(system.getResRings());
        resValue.getStyleClass().add("massacre-card-value");
        resValue.setTranslateX(-30);

        // Assemblage final
        mainContent.getChildren().addAll(systemInfo, distance, padsContainer, factionsContainer, resValue);
        this.getChildren().add(mainContent);
    }

    private Label addShipPad(String pad) {

        Label mediumPad = new Label(pad);
        mediumPad.getStyleClass().add("massacre-card-pad");
        return mediumPad;
    }


    private void onClickSystem(String systemName, javafx.scene.input.MouseEvent event) {
        copyClipboardManager.copyToClipboard(systemName);
        Stage stage = (Stage) getScene().getWindow();
        popupManager.showPopup(localizationService.getString("system.copied"), event.getSceneX(), event.getSceneY(),stage);
    }

    private void onClickTargetCount(String systemName) {
        controller.searchFromTarget(systemName);
    }


    private static Label getSuperFaction(String factionNbre) {
        Label fedValue = new Label(factionNbre);
        fedValue.getStyleClass().add("massacre-card-faction-number");
        return fedValue;
    }
}
