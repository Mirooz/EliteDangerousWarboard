package be.mirooz.elitedangerous.dashboard.controller;

import be.mirooz.elitedangerous.dashboard.controller.ui.context.DashboardContext;
import be.mirooz.elitedangerous.dashboard.controller.ui.manager.UIManager;
import be.mirooz.elitedangerous.dashboard.model.registries.MissionsRegistry;
import be.mirooz.elitedangerous.dashboard.controller.ui.component.CommanderStatusComponent;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

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
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        UIManager.getInstance().register(this);
        updateTranslations();
        
        // Écouter les changements de langue
        localizationService.addLanguageChangeListener(locale -> {
            updateTranslations();
        });
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



    @Override
    public void refreshUI() {
    }
}
