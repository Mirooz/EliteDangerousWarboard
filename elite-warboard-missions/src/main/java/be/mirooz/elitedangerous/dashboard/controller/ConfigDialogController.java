package be.mirooz.elitedangerous.dashboard.controller;

import be.mirooz.elitedangerous.dashboard.service.DashboardService;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.service.PreferencesService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
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
    private Label configSubtitleLabel;

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

    @FXML
    private Label journalFolderSectionLabel;

    @FXML
    private Label journalFolderDescriptionLabel;

    @FXML
    private TextField journalFolderTextField;

    @FXML
    private Button browseJournalFolderButton;

    @FXML
    private Label journalDaysSectionLabel;

    @FXML
    private Label journalDaysLabel;

    @FXML
    private Spinner<Integer> journalDaysSpinner;

    @FXML
    private Label journalDaysUnitLabel;

    @FXML
    private Label journalDaysDescriptionLabel;

    private final LocalizationService localizationService = LocalizationService.getInstance();
    private final PreferencesService preferencesService = PreferencesService.getInstance();
    private final DashboardService dashboardService = DashboardService.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateTranslations();
        
        // Initialiser la sélection selon la langue actuelle
        if (localizationService.isFrench()) {
            frenchRadioButton.setSelected(true);
        } else {
            englishRadioButton.setSelected(true);
        }
        
        // Initialiser le champ du dossier journal
        journalFolderTextField.setText(preferencesService.getJournalFolder());
        
        // Initialiser le spinner du nombre de jours
        journalDaysSpinner.getValueFactory().setValue(preferencesService.getJournalDays());
        
        // Stocker les valeurs initiales pour détecter les changements
        originalJournalFolder = preferencesService.getJournalFolder();
        originalJournalDays = preferencesService.getJournalDays();
    }
    
    private String originalJournalFolder;
    private int originalJournalDays;

    private void updateTranslations() {
        configTitleLabel.setText(localizationService.getString("config.title"));
        configSubtitleLabel.setText(localizationService.getString("config.subtitle"));
        languageSectionLabel.setText(localizationService.getString("config.language"));
        journalFolderSectionLabel.setText(localizationService.getString("config.journal.folder"));
        
        // Traiter les retours à la ligne pour la description
        String description = localizationService.getString("config.journal.description");
        journalFolderDescriptionLabel.setText(description.replace("\\n", "\n"));
        
        // Traductions pour la section nombre de jours
        journalDaysSectionLabel.setText(localizationService.getString("config.journal.days"));
        journalDaysLabel.setText(localizationService.getString("config.journal.days.label"));
        journalDaysUnitLabel.setText(localizationService.getString("config.journal.days.unit"));
        journalDaysDescriptionLabel.setText(localizationService.getString("config.journal.days.description"));
        
        browseJournalFolderButton.setText(localizationService.getString("config.browse"));
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
        
        // Sauvegarder le dossier journal
        String newJournalFolder = journalFolderTextField.getText();
        preferencesService.setJournalFolder(newJournalFolder);
        
        // Sauvegarder le nombre de jours
        int newJournalDays = journalDaysSpinner.getValue();
        preferencesService.setJournalDays(newJournalDays);
        
        // Si le dossier journal ou le nombre de jours a changé, relancer le chargement des missions
        if (!newJournalFolder.equals(originalJournalFolder) || newJournalDays != originalJournalDays) {
            // Relancer le chargement des missions dans un thread séparé
            dashboardService.initActiveMissions();
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

    @FXML
    private void browseJournalFolder() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Sélectionner le dossier Journal Elite Dangerous");
        
        // Définir le répertoire initial
        String currentPath = journalFolderTextField.getText();
        if (currentPath != null && !currentPath.isEmpty()) {
            try {
                java.io.File initialDir = new java.io.File(currentPath);
                if (initialDir.exists()) {
                    directoryChooser.setInitialDirectory(initialDir);
                }
            } catch (Exception e) {
                // Ignorer les erreurs de chemin
            }
        }
        
        Stage stage = (Stage) browseJournalFolderButton.getScene().getWindow();
        java.io.File selectedDirectory = directoryChooser.showDialog(stage);
        
        if (selectedDirectory != null) {
            journalFolderTextField.setText(selectedDirectory.getAbsolutePath());
        }
    }
}


