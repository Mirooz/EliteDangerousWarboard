package be.mirooz.elitedangerous.dashboard.controller;

import be.mirooz.elitedangerous.dashboard.model.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.service.EdToolsService;
import be.mirooz.elitedangerous.lib.edtools.model.MassacreSystem;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
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
            showAlert("Erreur", "Veuillez saisir un système de référence.");
            return;
        }

        int maxDistance = maxDistanceSpinner.getValue();
        int minSources = minSourcesSpinner.getValue();

        try {
            searchButton.setDisable(true);
            searchButton.setText("RECHERCHE...");

            List<MassacreSystem> systems = edToolsService.findMassacreSystems(referenceSystem, maxDistance, minSources);
            displayResults(systems);

            if (systems.isEmpty()) {
                showAlert("Information", "Aucun système massacre trouvé avec ces critères.");
            }

        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors de la recherche: " + e.getMessage());
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

    private Tooltip createDelayedTooltip(String text) {
        Tooltip tooltip = new Tooltip(text);
        tooltip.setShowDelay(Duration.millis(300)); // 0.3 seconde de délai
        tooltip.setHideDelay(Duration.millis(100));
        return tooltip;
    }
    private void copySystemToClipboard(String systemName, javafx.scene.input.MouseEvent event) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(systemName);
        clipboard.setContent(content);

        showSystemCopiedPopup("Système copié", event);
    }
    private void showSystemCopiedPopup(String message, javafx.scene.input.MouseEvent event) {
        if (popupContainer == null) return;

        // Créer le popup
        VBox popup = new VBox();
        popup.getStyleClass().add("system-copied-popup");
        popup.setAlignment(Pos.CENTER);
        popup.setSpacing(3);
        popup.setPadding(new Insets(8, 16, 8, 16));

        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("popup-title");

        popup.getChildren().add(messageLabel);

        // Taille compacte
        popup.setMinSize(120, 40);
        popup.setPrefSize(120, 40);
        popup.setMaxSize(120, 40);

        // Positionner le popup avec le coin gauche en bas à droite de la souris
        double mouseX = event.getSceneX();
        double mouseY = event.getSceneY();
        popup.setTranslateX(mouseX);
        popup.setTranslateY(mouseY);
        
        // Désactiver l'alignement par défaut du StackPane
        StackPane.setAlignment(popup, Pos.TOP_LEFT);

        // Ajouter au conteneur
        popupContainer.getChildren().add(popup);

        // Animation d'apparition et disparition
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), popup);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        PauseTransition pause = new PauseTransition(Duration.millis(1000));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), popup);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        SequentialTransition sequence = new SequentialTransition(fadeIn, pause, fadeOut);
        sequence.setOnFinished(e -> popupContainer.getChildren().remove(popup));
        sequence.play();
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

        Label title = new Label(system.getSourceSystem());
        title.getStyleClass().add("massacre-card-title");
        // Ajouter tooltip et clic pour copier le système d'origine
        title.setTooltip(createDelayedTooltip(system.getSourceSystem()));
        title.getStyleClass().add("clickable-system-source");
        title.setOnMouseClicked(e -> copySystemToClipboard(system.getSourceSystem(), e));

        Label subtitle = new Label("→ " + system.getTargetSystem() + "["+system.getTargetCount()+"]");
        subtitle.getStyleClass().add("massacre-card-subtitle");

        systemInfo.getChildren().addAll(title, subtitle);

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

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
