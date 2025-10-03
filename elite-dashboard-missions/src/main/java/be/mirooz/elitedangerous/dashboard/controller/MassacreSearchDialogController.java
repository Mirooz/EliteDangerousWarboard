package be.mirooz.elitedangerous.dashboard.controller;

import be.mirooz.elitedangerous.dashboard.model.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.service.EdToolsService;
import be.mirooz.elitedangerous.dashboard.ui.manager.PopupManager;
import be.mirooz.elitedangerous.dashboard.ui.component.GenericListView;
import be.mirooz.elitedangerous.dashboard.ui.component.SystemCardComponent;
import be.mirooz.elitedangerous.lib.edtools.model.MassacreSystem;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

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
    private GenericListView<MassacreSystem> systemList;
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
    @FXML
    private ProgressIndicator loadingIndicator;

    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();
    private final EdToolsService edToolsService = EdToolsService.getInstance();

    private PopupManager popupManager = PopupManager.getInstance();
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Configurer le système de référence par défaut
        String currentSystem = commanderStatus.getCurrentStarSystem();
        if (currentSystem != null && !currentSystem.trim().isEmpty()) {
            referenceSystemField.setText(currentSystem);
        }
        // Charger les icônes des factions dans l'en-tête
        loadFactionHeaderIcons();
        systemList.setComponentFactory(SystemCardComponent::new);
        popupManager.attachToContainer(popupContainer);

    }

    private void loadFactionHeaderIcons() {
        fedHeaderIcon.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/fed.png"))));
        impHeaderIcon.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/empire.png"))));
        allHeaderIcon.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/alliance.png"))));
        indHeaderIcon.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/independant.png"))));
    }

    @FXML
    private void searchMassacreSystems() {
        if (referenceSystemField.getText() == null || referenceSystemField.getText().trim().isEmpty()) {
            referenceSystemField.setText(commanderStatus.getCurrentStarSystem());
        }
        String referenceSystem = referenceSystemField.getText();

        int maxDistance = maxDistanceSpinner.getValue();
        int minSources = minSourcesSpinner.getValue();

        searching(true);

        edToolsService.findMassacreSystems(referenceSystem, maxDistance, minSources)
                .thenAccept(systems -> Platform.runLater(() -> {
                    displayResults(systems);
                    searching(false);
                }))
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    Platform.runLater(() -> searching(false));
                    return null;
                });
    }

    private void searching(boolean isSearching) {
        if (isSearching){
        searchButton.setDisable(true);
        searchButton.setText("RECHERCHE...");
        systemList.getItems().clear();
        loadingIndicator.setVisible(true);}
        else {
            loadingIndicator.setVisible(false);
            searchButton.setDisable(false);
            searchButton.setText("RECHERCHER");
        }
    }

    private void displayResults(List<MassacreSystem> systems) {
        systemList.getItems().setAll(systems);
    }



    @FXML
    private void closeDialog() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

}
