package be.mirooz.elitedangerous.dashboard.controller;

import be.mirooz.elitedangerous.dashboard.controller.ui.context.DashboardContext;
import be.mirooz.elitedangerous.dashboard.controller.ui.manager.UIManager;
import be.mirooz.elitedangerous.dashboard.model.registries.combat.MissionsRegistry;
import be.mirooz.elitedangerous.dashboard.controller.ui.component.CommanderStatusComponent;
import be.mirooz.elitedangerous.dashboard.controller.ui.manager.CopyClipboardManager;
import be.mirooz.elitedangerous.dashboard.controller.ui.manager.PopupManager;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Contrôleur pour le pied de page du dashboard
 */
public class FooterController implements Initializable, IRefreshable, IBatchListener {

    // Constantes pour garantir l'alignement


    @FXML
    private Label commanderLabel;

    @FXML
    private Label systemLabel;

    @FXML
    private Label stationLabel;

    @FXML
    private Label commanderHeaderLabel;

    @FXML
    private Label systemHeaderLabel;

    @FXML
    private Label stationHeaderLabel;



    private final CommanderStatusComponent commanderStatusComponent = CommanderStatusComponent.getInstance();
    private final MissionsRegistry missionsRegistry = MissionsRegistry.getInstance();
    private final DashboardContext dashboardContext = DashboardContext.getInstance();
    private final LocalizationService localizationService = LocalizationService.getInstance();
    private final CopyClipboardManager copyClipboardManager = CopyClipboardManager.getInstance();
    private final PopupManager popupManager = PopupManager.getInstance();
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        UIManager.getInstance().register(this);
        updateTranslations();

        // Écouter les changements de langue
        localizationService.addLanguageChangeListener(locale -> {
            updateTranslations();
        });

        // Ajouter la fonctionnalité de copie au clic sur le système
        systemLabel.setOnMouseClicked(this::onSystemLabelClicked);
        systemLabel.getStyleClass().add("clickable-system-source");
    }

    private void updateTranslations() {
        commanderHeaderLabel.setText(localizationService.getString("footer.commander"));
        systemHeaderLabel.setText(localizationService.getString("footer.system"));
        stationHeaderLabel.setText(localizationService.getString("footer.station"));

    }
    @Override
    public void onBatchStart(){
        stationLabel.textProperty().unbind();
        systemLabel.textProperty().unbind();
        commanderLabel.textProperty().unbind();
    }

    @Override
    public void onBatchEnd() {
        stationLabel.textProperty().bind(commanderStatusComponent.getCurrentStationName());
        systemLabel.textProperty().bind(commanderStatusComponent.getCurrentStarSystem());
        commanderLabel.textProperty().bind(commanderStatusComponent.getCommanderName());
    }



    /**
     * Gère le clic sur le label du système pour copier le nom dans le presse-papier
     */
    private void onSystemLabelClicked(MouseEvent event) {
        String systemName = systemLabel.getText();
        if (systemName != null && !systemName.isEmpty() && !systemName.equals("[NON IDENTIFIÉ]")) {
            copyClipboardManager.copyToClipboard(systemName);
            Stage stage = (Stage) systemLabel.getScene().getWindow();
            popupManager.showPopup(localizationService.getString("system.copied"), event.getSceneX(), event.getSceneY(), stage);
        }
    }

    @Override
    public void refreshUI() {
    }
}
