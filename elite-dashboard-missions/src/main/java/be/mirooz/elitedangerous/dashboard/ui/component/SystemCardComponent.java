package be.mirooz.elitedangerous.dashboard.ui.component;

import be.mirooz.elitedangerous.dashboard.ui.CopyClipboardManager;
import be.mirooz.elitedangerous.dashboard.ui.PopupManager;
import be.mirooz.elitedangerous.lib.edtools.model.MassacreSystem;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class SystemCardComponent extends VBox{

    private final PopupManager popupManager = PopupManager.getInstance();
    private final CopyClipboardManager copyClipboardManager = CopyClipboardManager.getInstance();
    public SystemCardComponent(MassacreSystem system) {
        this.getStyleClass().add("massacre-card");

        // Layout horizontal compact
        HBox mainContent = new HBox();
        mainContent.setAlignment(Pos.CENTER_LEFT);
        mainContent.setSpacing(15);

        // Système source et cible
        VBox systemInfo = new VBox();
        systemInfo.setSpacing(2);

        Label sourceSystem = new Label(system.getSourceSystem());
        sourceSystem.getStyleClass().add("massacre-card-title");
        sourceSystem.setTooltip(new TooltipComponent(system.getSourceSystem()));
        sourceSystem.getStyleClass().add("clickable-system-source");
        sourceSystem.setOnMouseClicked(e -> onClickSystem(system.getSourceSystem(), e));

        Label targetSystem = new Label("→ " + system.getTargetSystem() + " / [" + system.getTargetCount() + "]");
        targetSystem.setTooltip(new TooltipComponent(system.getTargetSystem()));
        targetSystem.getStyleClass().add("massacre-card-subtitle");
        targetSystem.getStyleClass().add("clickable-system-target");
        targetSystem.setOnMouseClicked(e -> onClickSystem(system.getTargetSystem(), e));
        systemInfo.getChildren().addAll(sourceSystem, targetSystem);

        // Distance
        Label distance = new Label(system.getDistanceLy() + " AL");
        distance.getStyleClass().add("massacre-card-distance");

        // Pads
        HBox padsContainer = new HBox();
        padsContainer.setSpacing(5);

        //PADS
        Label LPad = addShipPad("L");
        if (system.getLargePads().isEmpty()) {
            LPad.setOpacity(0);
        }
        padsContainer.getChildren().add(LPad);
        padsContainer.getChildren().add(addShipPad("M"));
        padsContainer.getChildren().add(addShipPad("S"));
        // Factions - alignées avec l'en-tête
        HBox factionsContainer = new HBox();
        factionsContainer.setSpacing(5);
        factionsContainer.setAlignment(Pos.CENTER);

        // Fédération
        Label fedValue = getSuperFaction(system.getFed());
        fedValue.setTranslateX(30);
        // Empire
        Label impValue = getSuperFaction(system.getImp());
        impValue.setTranslateX(20);
        // Alliance
        Label allValue = getSuperFaction(system.getAll());
        allValue.setTranslateX(13);
        // Indépendant
        Label indValue = getSuperFaction(system.getInd());
        indValue.setTranslateX(-2);

        factionsContainer.getChildren().addAll(fedValue, impValue, allValue, indValue);

        // RES
        Label resValue = new Label(system.getResRings());
        resValue.getStyleClass().add("massacre-card-value");
        resValue.setTranslateX(-30);

        // Assemblage du contenu horizontal
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
        popupManager.showPopup("Système copié", event.getSceneX(), event.getSceneY(),stage); }


    private static Label getSuperFaction(String factionNbre) {
        Label fedValue = new Label(factionNbre);
        fedValue.getStyleClass().add("massacre-card-faction-number");
        return fedValue;
    }
}
