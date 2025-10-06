package be.mirooz.elitedangerous.dashboard.controller;

import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.service.PreferencesService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Contrôleur pour la fenêtre de configuration
 */
public class ConfigDialogController implements Initializable {

    @FXML
    private Label configTitleLabel;

    @FXML
    private Label languageSectionLabel;

    @FXML
    private RadioButton frenchRadioButton;

    @FXML
    private RadioButton englishRadioButton;

    @FXML
    private Button saveButton;

    @FXML
    private Button cancelButton;

    private final LocalizationService localizationService = LocalizationService.getInstance();
    private final PreferencesService preferencesService = PreferencesService.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateTranslations();
        
        // Initialiser la sélection selon la langue actuelle
        if (localizationService.isFrench()) {
            frenchRadioButton.setSelected(true);
        } else {
            englishRadioButton.setSelected(true);
        }
    }

    private void updateTranslations() {
        configTitleLabel.setText(localizationService.getString("config.title"));
        languageSectionLabel.setText(localizationService.getString("config.language"));
        saveButton.setText(localizationService.getString("config.save"));
        cancelButton.setText(localizationService.getString("config.cancel"));
    }

    @FXML
    private void selectFrench() {
        englishRadioButton.setSelected(false);
    }

    @FXML
    private void selectEnglish() {
        frenchRadioButton.setSelected(false);
    }

    @FXML
    private void saveConfig() {
        // Sauvegarder la langue sélectionnée
        if (frenchRadioButton.isSelected()) {
            preferencesService.setLanguage("fr");
            localizationService.setLanguage("fr");
        } else {
            preferencesService.setLanguage("en");
            localizationService.setLanguage("en");
        }
        
        // Fermer la fenêtre
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void cancelConfig() {
        // Fermer la fenêtre sans sauvegarder
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
}
