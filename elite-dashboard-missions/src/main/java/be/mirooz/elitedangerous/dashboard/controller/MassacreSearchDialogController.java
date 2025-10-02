package be.mirooz.elitedangerous.dashboard.controller;

import be.mirooz.elitedangerous.dashboard.model.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.service.EdToolsService;
import be.mirooz.elitedangerous.dashboard.ui.CopyClipboardManager;
import be.mirooz.elitedangerous.dashboard.ui.PopupManager;
import be.mirooz.elitedangerous.dashboard.ui.component.TooltipComponent;
import be.mirooz.elitedangerous.lib.edtools.model.MassacreSystem;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * Contrôleur pour la fenêtre de recherche de systèmes massacre
 */
public class MassacreSearchDialogController implements Initializable {

    @FXML
    private TextField referenceSystemField;

    @FXML
    private Spinner<Integer> maxDistanceSpinner;

    @FXML
    private Spinner<Integer> minSourcesSpinner;

    @FXML
    private ScrollPane resultsScrollPane;

    @FXML
    private VBox resultsContainer;

    @FXML
    private Button searchButton;

    @FXML
    private Button closeButton;

    @FXML
    private ImageView fedHeaderIcon;

    @FXML
    private ImageView impHeaderIcon;

    @FXML
    private ImageView allHeaderIcon;

    @FXML
    private ImageView indHeaderIcon;

    @FXML

    private StackPane popupContainer;

    private final PopupManager popupManager = PopupManager.getInstance();
    private final CopyClipboardManager copyClipboardManager = CopyClipboardManager.getInstance();
    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();
    private final EdToolsService edToolsService = EdToolsService.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Configurer le système de référence par défaut
        String currentSystem = commanderStatus.getCurrentStarSystemString();
        if (currentSystem != null && !currentSystem.trim().isEmpty()) {
            referenceSystemField.setText(currentSystem);
        }

        // Charger les icônes des factions dans l'en-tête
        loadFactionHeaderIcons();
    }

    private void loadFactionHeaderIcons() {
        fedHeaderIcon.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/fed.png"))));
        impHeaderIcon.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/empire.png"))));
        allHeaderIcon.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/alliance.png"))));
        indHeaderIcon.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/independant.png"))));
    }

    @FXML
    private void searchMassacreSystems() {
        String referenceSystem = referenceSystemField.getText();
        if (referenceSystem == null || referenceSystem.trim().isEmpty()) {
            referenceSystem=commanderStatus.getCurrentStarSystemString();
            return;
        }

        int maxDistance = maxDistanceSpinner.getValue();
        int minSources = minSourcesSpinner.getValue();

        try {
            searchButton.setDisable(true);
            searchButton.setText("RECHERCHE...");

            List<MassacreSystem> systems = edToolsService.findMassacreSystems(referenceSystem, maxDistance, minSources);
            displayResults(systems);


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            searchButton.setDisable(false);
            searchButton.setText("RECHERCHER");
        }
    }

    private void displayResults(List<MassacreSystem> systems) {
        // Vider toutes les cartes
        resultsContainer.getChildren().clear();

        for (MassacreSystem system : systems) {
            VBox card = createMassacreCard(system);
            resultsContainer.getChildren().add(card);
        }
    }

    private Label addShipPad(String pad) {

        Label mediumPad = new Label(pad);
        mediumPad.getStyleClass().add("massacre-card-pad");
        return mediumPad;
    }


    private void onClickSystem(String systemName, javafx.scene.input.MouseEvent event) {
        copyClipboardManager.copyToClipboard(systemName);
        popupManager.showPopup("Système copié",event.getSceneX(),event.getSceneY(),this.popupContainer);
    }
    private VBox createMassacreCard(MassacreSystem system) {
        VBox card = new VBox();
        card.getStyleClass().add("massacre-card");

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

        Label targetSystem = new Label("→ " + system.getTargetSystem() + "["+system.getTargetCount()+"]");
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
        Label LPad= addShipPad("L");
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

        card.getChildren().add(mainContent);

        return card;
    }

    private static Label getSuperFaction(String factionNbre) {
        Label fedValue = new Label(factionNbre);
        fedValue.getStyleClass().add("massacre-card-faction-number");
        return fedValue;
    }

    @FXML
    private void closeDialog() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

}
