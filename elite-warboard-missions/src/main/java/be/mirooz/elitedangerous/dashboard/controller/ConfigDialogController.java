package be.mirooz.elitedangerous.dashboard.controller;

import be.mirooz.elitedangerous.dashboard.service.DashboardService;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.service.PreferencesService;
import be.mirooz.elitedangerous.dashboard.service.WindowToggleService;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;

import java.awt.Desktop;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    @FXML
    private Label vrModeSectionLabel;

    @FXML
    private CheckBox vrModeEnabledCheckBox;

    @FXML
    private VBox bindingsContainer;

    @FXML
    private Label windowToggleBindLabel;

    @FXML
    private Label windowToggleBindValueLabel;

    @FXML
    private Button windowToggleBindButton;

    @FXML
    private Button windowToggleUnbindButton;

    @FXML
    private Label windowToggleBindDescriptionLabel;

    @FXML
    private Label tabSwitchLeftLabel;

    @FXML
    private Button tabSwitchLeftButton;

    @FXML
    private Label tabSwitchLeftBindLabel;

    @FXML
    private Button tabSwitchLeftUnbindButton;

    @FXML
    private Label tabSwitchRightLabel;

    @FXML
    private Button tabSwitchRightButton;

    @FXML
    private Label tabSwitchRightBindLabel;

    @FXML
    private Button tabSwitchRightUnbindButton;

    @FXML
    private Label donateSectionLabel;

    @FXML
    private Label donateDescriptionLabel;

    @FXML
    private ImageView donateButtonImage;

    private final LocalizationService localizationService = LocalizationService.getInstance();
    private final PreferencesService preferencesService = PreferencesService.getInstance();
    private final DashboardService dashboardService = DashboardService.getInstance();
    private final WindowToggleService windowToggleService = WindowToggleService.getInstance();
    
    private boolean isCapturingBind = false;
    private String currentBindType = null; // "windowToggle", "tabSwitchLeft", "tabSwitchRight"
    private NativeKeyListener keyCaptureListener = null;
    private Thread hotasCaptureThread = null;
    
    // Valeurs capturées pour window toggle
    private int capturedKeyCode = -1;
    private String capturedControllerName = null;
    private String capturedComponentName = null;
    private float capturedComponentValue = 0.0f;
    private boolean isKeyboardBind = false;
    
    // Valeurs capturées pour tab switch left
    private int capturedTabLeftKeyCode = -1;
    private String capturedTabLeftControllerName = null;
    private String capturedTabLeftComponentName = null;
    private float capturedTabLeftComponentValue = 0.0f;
    private boolean isTabLeftKeyboardBind = false;
    
    // Valeurs capturées pour tab switch right
    private int capturedTabRightKeyCode = -1;
    private String capturedTabRightControllerName = null;
    private String capturedTabRightComponentName = null;
    private float capturedTabRightComponentValue = 0.0f;
    private boolean isTabRightKeyboardBind = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Mettre en pause le service pendant que la fenêtre de config est ouverte
        windowToggleService.pause();
        
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
        
        // Initialiser le VR mode (activé si au moins un des services est activé)
        boolean vrModeEnabled = preferencesService.isWindowToggleEnabled() || preferencesService.isTabSwitchEnabled();
        vrModeEnabledCheckBox.setSelected(vrModeEnabled);
        // S'assurer que la checkbox est activée
        vrModeEnabledCheckBox.setDisable(false);
        
        // Initialiser les affichages des binds
        updateWindowToggleBindDisplay();
        updateTabSwitchBindDisplays();
        
        // Mettre à jour l'état des bindings selon le VR mode
        updateBindingsEnabledState();
        
        // Charger l'image du bouton de don
        loadDonateButtonImage();
        
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
        
        // Traductions pour la section VR mode
        vrModeSectionLabel.setText("VR MODE");
        vrModeEnabledCheckBox.setText(localizationService.getString("config.vr.mode.enabled"));
        windowToggleBindLabel.setText(localizationService.getString("config.window.toggle.bind.label"));
        tabSwitchLeftLabel.setText(localizationService.getString("config.tab.switch.left"));
        tabSwitchRightLabel.setText(localizationService.getString("config.tab.switch.right"));
        
        // Mettre à jour le texte des boutons BIND
        windowToggleBindButton.setText(localizationService.getString("config.window.toggle.bind.button"));
        tabSwitchLeftButton.setText(localizationService.getString("config.window.toggle.bind.button"));
        tabSwitchRightButton.setText(localizationService.getString("config.window.toggle.bind.button"));
        
        browseJournalFolderButton.setText(localizationService.getString("config.browse"));
        saveButton.setText(localizationService.getString("config.save"));
        cancelButton.setText(localizationService.getString("config.cancel"));
        
        // Traductions pour la section don
        donateSectionLabel.setText(localizationService.getString("config.donate.title"));
        donateDescriptionLabel.setText(localizationService.getString("config.donate.description"));
        
        // Mettre à jour les affichages des binds
        updateWindowToggleBindDisplay();
        updateTabSwitchBindDisplays();
    }
    
    private void updateWindowToggleBindDisplay() {
        // Utiliser les variables capturées en priorité (pour gérer le unbind)
        if (capturedControllerName != null && !capturedControllerName.isEmpty() && 
            capturedComponentName != null && !capturedComponentName.isEmpty()) {
            // Bind HOTAS (depuis variables capturées)
            windowToggleBindValueLabel.setText(capturedControllerName + " - " + capturedComponentName + " (" + String.format("%.2f", capturedComponentValue) + ")");
            windowToggleUnbindButton.setVisible(true);
            windowToggleUnbindButton.setManaged(true);
            isKeyboardBind = false;
        } else if (capturedKeyCode > 0) {
            // Bind clavier (depuis variables capturées)
            windowToggleBindValueLabel.setText(getKeyName(capturedKeyCode));
            windowToggleUnbindButton.setVisible(true);
            windowToggleUnbindButton.setManaged(true);
            isKeyboardBind = true;
        } else {
            // Charger depuis les préférences si les variables capturées ne sont pas définies
            String controllerName = preferencesService.getWindowToggleHotasController();
            String componentName = preferencesService.getWindowToggleHotasComponent();
            
            if (controllerName != null && !controllerName.isEmpty() && 
                componentName != null && !componentName.isEmpty()) {
                // Bind HOTAS (depuis préférences)
                float value = preferencesService.getWindowToggleHotasValue();
                windowToggleBindValueLabel.setText(controllerName + " - " + componentName + " (" + String.format("%.2f", value) + ")");
                windowToggleUnbindButton.setVisible(true);
                windowToggleUnbindButton.setManaged(true);
                isKeyboardBind = false;
                capturedControllerName = controllerName;
                capturedComponentName = componentName;
                capturedComponentValue = value;
            } else {
                // Bind clavier (depuis préférences)
                int keyCode = preferencesService.getWindowToggleKeyboardKey();
                if (keyCode > 0) {
                    windowToggleBindValueLabel.setText(getKeyName(keyCode));
                    windowToggleUnbindButton.setVisible(true);
                    windowToggleUnbindButton.setManaged(true);
                    isKeyboardBind = true;
                    capturedKeyCode = keyCode;
                } else {
                    // Aucun bind
                    windowToggleBindValueLabel.setText(localizationService.getString("config.window.toggle.bind.none"));
                    windowToggleUnbindButton.setVisible(false);
                    windowToggleUnbindButton.setManaged(false);
                    isKeyboardBind = false;
                    capturedKeyCode = -1;
                }
            }
        }
    }
    
    private void updateTabSwitchBindDisplays() {
        // Tab left - Utiliser les variables capturées en priorité
        if (capturedTabLeftControllerName != null && !capturedTabLeftControllerName.isEmpty() && 
            capturedTabLeftComponentName != null && !capturedTabLeftComponentName.isEmpty()) {
            // Bind HOTAS (depuis variables capturées)
            tabSwitchLeftBindLabel.setText(capturedTabLeftControllerName + " - " + capturedTabLeftComponentName + " (" + String.format("%.2f", capturedTabLeftComponentValue) + ")");
            tabSwitchLeftUnbindButton.setVisible(true);
            tabSwitchLeftUnbindButton.setManaged(true);
            isTabLeftKeyboardBind = false;
        } else if (capturedTabLeftKeyCode > 0) {
            // Bind clavier (depuis variables capturées)
            tabSwitchLeftBindLabel.setText(getKeyName(capturedTabLeftKeyCode));
            tabSwitchLeftUnbindButton.setVisible(true);
            tabSwitchLeftUnbindButton.setManaged(true);
            isTabLeftKeyboardBind = true;
        } else {
            // Charger depuis les préférences
            String leftControllerName = preferencesService.getTabSwitchLeftHotasController();
            String leftComponentName = preferencesService.getTabSwitchLeftHotasComponent();
            
            if (leftControllerName != null && !leftControllerName.isEmpty() && 
                leftComponentName != null && !leftComponentName.isEmpty()) {
                float value = preferencesService.getTabSwitchLeftHotasValue();
                tabSwitchLeftBindLabel.setText(leftControllerName + " - " + leftComponentName + " (" + String.format("%.2f", value) + ")");
                tabSwitchLeftUnbindButton.setVisible(true);
                tabSwitchLeftUnbindButton.setManaged(true);
                isTabLeftKeyboardBind = false;
                capturedTabLeftControllerName = leftControllerName;
                capturedTabLeftComponentName = leftComponentName;
                capturedTabLeftComponentValue = value;
            } else {
                int keyCode = preferencesService.getTabSwitchLeftKeyboardKey();
                if (keyCode > 0) {
                    tabSwitchLeftBindLabel.setText(getKeyName(keyCode));
                    tabSwitchLeftUnbindButton.setVisible(true);
                    tabSwitchLeftUnbindButton.setManaged(true);
                    isTabLeftKeyboardBind = true;
                    capturedTabLeftKeyCode = keyCode;
                } else {
                    tabSwitchLeftBindLabel.setText(localizationService.getString("config.tab.switch.bind.none"));
                    tabSwitchLeftUnbindButton.setVisible(false);
                    tabSwitchLeftUnbindButton.setManaged(false);
                    isTabLeftKeyboardBind = false;
                    capturedTabLeftKeyCode = -1;
                }
            }
        }
        
        // Tab right - Utiliser les variables capturées en priorité
        if (capturedTabRightControllerName != null && !capturedTabRightControllerName.isEmpty() && 
            capturedTabRightComponentName != null && !capturedTabRightComponentName.isEmpty()) {
            // Bind HOTAS (depuis variables capturées)
            tabSwitchRightBindLabel.setText(capturedTabRightControllerName + " - " + capturedTabRightComponentName + " (" + String.format("%.2f", capturedTabRightComponentValue) + ")");
            tabSwitchRightUnbindButton.setVisible(true);
            tabSwitchRightUnbindButton.setManaged(true);
            isTabRightKeyboardBind = false;
        } else if (capturedTabRightKeyCode > 0) {
            // Bind clavier (depuis variables capturées)
            tabSwitchRightBindLabel.setText(getKeyName(capturedTabRightKeyCode));
            tabSwitchRightUnbindButton.setVisible(true);
            tabSwitchRightUnbindButton.setManaged(true);
            isTabRightKeyboardBind = true;
        } else {
            // Charger depuis les préférences
            String rightControllerName = preferencesService.getTabSwitchRightHotasController();
            String rightComponentName = preferencesService.getTabSwitchRightHotasComponent();
            
            if (rightControllerName != null && !rightControllerName.isEmpty() && 
                rightComponentName != null && !rightComponentName.isEmpty()) {
                float value = preferencesService.getTabSwitchRightHotasValue();
                tabSwitchRightBindLabel.setText(rightControllerName + " - " + rightComponentName + " (" + String.format("%.2f", value) + ")");
                tabSwitchRightUnbindButton.setVisible(true);
                tabSwitchRightUnbindButton.setManaged(true);
                isTabRightKeyboardBind = false;
                capturedTabRightControllerName = rightControllerName;
                capturedTabRightComponentName = rightComponentName;
                capturedTabRightComponentValue = value;
            } else {
                int keyCode = preferencesService.getTabSwitchRightKeyboardKey();
                if (keyCode > 0) {
                    tabSwitchRightBindLabel.setText(getKeyName(keyCode));
                    tabSwitchRightUnbindButton.setVisible(true);
                    tabSwitchRightUnbindButton.setManaged(true);
                    isTabRightKeyboardBind = true;
                    capturedTabRightKeyCode = keyCode;
                } else {
                    tabSwitchRightBindLabel.setText(localizationService.getString("config.tab.switch.bind.none"));
                    tabSwitchRightUnbindButton.setVisible(false);
                    tabSwitchRightUnbindButton.setManaged(false);
                    isTabRightKeyboardBind = false;
                    capturedTabRightKeyCode = -1;
                }
            }
        }
    }

    @FXML
    private void selectFrench() {
        englishRadioButton.setSelected(false);
        localizationService.setLanguage("fr");
        updateTranslations();
    }

    @FXML
    private void selectEnglish() {
        frenchRadioButton.setSelected(false);
        localizationService.setLanguage("en");
        updateTranslations();
    }

    @FXML
    private void onVrModeChanged() {
        // S'assurer que la checkbox reste activée
        vrModeEnabledCheckBox.setDisable(false);
        updateBindingsEnabledState();
    }

    /**
     * Met à jour l'état activé/désactivé des bindings selon le VR mode
     */
    private void updateBindingsEnabledState() {
        boolean vrModeEnabled = vrModeEnabledCheckBox.isSelected();
        
        // Désactiver/griser tous les bindings si VR mode est désactivé
        windowToggleBindButton.setDisable(!vrModeEnabled);
        windowToggleUnbindButton.setDisable(!vrModeEnabled);
        windowToggleBindValueLabel.setDisable(!vrModeEnabled);
        tabSwitchLeftButton.setDisable(!vrModeEnabled);
        tabSwitchLeftUnbindButton.setDisable(!vrModeEnabled);
        tabSwitchLeftBindLabel.setDisable(!vrModeEnabled);
        tabSwitchRightButton.setDisable(!vrModeEnabled);
        tabSwitchRightUnbindButton.setDisable(!vrModeEnabled);
        tabSwitchRightBindLabel.setDisable(!vrModeEnabled);
        
        // Appliquer un style grisé si désactivé
        if (vrModeEnabled) {
            bindingsContainer.getStyleClass().remove("disabled-container");
        } else {
            if (!bindingsContainer.getStyleClass().contains("disabled-container")) {
                bindingsContainer.getStyleClass().add("disabled-container");
            }
        }
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
        
        // Sauvegarder les paramètres VR mode
        boolean vrModeEnabled = vrModeEnabledCheckBox.isSelected();
        preferencesService.setWindowToggleEnabled(vrModeEnabled);
        preferencesService.setTabSwitchEnabled(false);
        
        if (isKeyboardBind && capturedKeyCode != -1) {
            // Sauvegarder bind clavier
            preferencesService.setWindowToggleKeyboardKey(capturedKeyCode);
            // Effacer bind HOTAS pour indiquer qu'on utilise le clavier
            preferencesService.setWindowToggleHotasController("");
            preferencesService.setWindowToggleHotasComponent("");
        } else if (!isKeyboardBind && capturedControllerName != null && capturedComponentName != null) {
            // Sauvegarder bind HOTAS
            preferencesService.setWindowToggleHotasController(capturedControllerName);
            preferencesService.setWindowToggleHotasComponent(capturedComponentName);
            preferencesService.setWindowToggleHotasValue(capturedComponentValue);
            // Effacer bind clavier pour indiquer qu'on utilise le HOTAS (mettre une valeur invalide)
            preferencesService.setWindowToggleKeyboardKey(-1);
        } else {
            // Unbind : effacer tous les binds
            preferencesService.setWindowToggleKeyboardKey(-1);
            preferencesService.setWindowToggleHotasController("");
            preferencesService.setWindowToggleHotasComponent("");
        }
        
        // Tab left
        if (isTabLeftKeyboardBind && capturedTabLeftKeyCode != -1) {
            preferencesService.setTabSwitchLeftKeyboardKey(capturedTabLeftKeyCode);
            preferencesService.setTabSwitchLeftHotasController("");
            preferencesService.setTabSwitchLeftHotasComponent("");
        } else if (!isTabLeftKeyboardBind && capturedTabLeftControllerName != null && capturedTabLeftComponentName != null) {
            preferencesService.setTabSwitchLeftHotasController(capturedTabLeftControllerName);
            preferencesService.setTabSwitchLeftHotasComponent(capturedTabLeftComponentName);
            preferencesService.setTabSwitchLeftHotasValue(capturedTabLeftComponentValue);
            preferencesService.setTabSwitchLeftKeyboardKey(-1);
        } else {
            // Unbind : effacer tous les binds
            preferencesService.setTabSwitchLeftKeyboardKey(-1);
            preferencesService.setTabSwitchLeftHotasController("");
            preferencesService.setTabSwitchLeftHotasComponent("");
        }
        
        // Tab right
        if (isTabRightKeyboardBind && capturedTabRightKeyCode != -1) {
            preferencesService.setTabSwitchRightKeyboardKey(capturedTabRightKeyCode);
            preferencesService.setTabSwitchRightHotasController("");
            preferencesService.setTabSwitchRightHotasComponent("");
        } else if (!isTabRightKeyboardBind && capturedTabRightControllerName != null && capturedTabRightComponentName != null) {
            preferencesService.setTabSwitchRightHotasController(capturedTabRightControllerName);
            preferencesService.setTabSwitchRightHotasComponent(capturedTabRightComponentName);
            preferencesService.setTabSwitchRightHotasValue(capturedTabRightComponentValue);
            preferencesService.setTabSwitchRightKeyboardKey(-1);
        } else {
            // Unbind : effacer tous les binds
            preferencesService.setTabSwitchRightKeyboardKey(-1);
            preferencesService.setTabSwitchRightHotasController("");
            preferencesService.setTabSwitchRightHotasComponent("");
        }
        
        // Si le dossier journal ou le nombre de jours a changé, relancer le chargement des missions
        if (!newJournalFolder.equals(originalJournalFolder) || newJournalDays != originalJournalDays) {
            // Relancer le chargement des missions dans un thread séparé
            dashboardService.initActiveMissions();
        }
        
        // Reprendre le service (la fenêtre va se fermer)
        windowToggleService.resume();
        
        // Redémarrer le service pour appliquer les nouvelles configurations
        windowToggleService.restart();
        
        // Fermer la fenêtre
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void cancelConfig() {
        // Arrêter la capture si active
        if (isCapturingBind) {
            stopBindCapture();
        }
        
        // Reprendre le service (la fenêtre va se fermer)
        windowToggleService.resume();
        
        // Fermer la fenêtre sans sauvegarder
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void browseJournalFolder() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Elite Dangerous Journal Folder");
        
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

    @FXML
    private void captureWindowToggleBind() {
        if (isCapturingBind) {
            stopBindCapture();
            return;
        }

        currentBindType = "windowToggle";
        isCapturingBind = true;
        windowToggleBindButton.setText("BIND...");
        windowToggleBindValueLabel.setText(localizationService.getString("config.window.toggle.bind.capturing"));
        // Changer le style du bouton en orange pendant la capture
        windowToggleBindButton.getStyleClass().add("elite-button-capturing");

        // Démarrer la capture clavier
        startKeyboardCapture();
        
        // Démarrer la capture HOTAS
        startHotasCapture();
    }
    
    @FXML
    private void captureTabSwitchLeftBind() {
        if (isCapturingBind) {
            stopBindCapture();
            return;
        }

        currentBindType = "tabSwitchLeft";
        isCapturingBind = true;
        tabSwitchLeftButton.setText("BIND...");
        tabSwitchLeftBindLabel.setText(localizationService.getString("config.window.toggle.bind.capturing"));
        // Changer le style du bouton en orange pendant la capture
        tabSwitchLeftButton.getStyleClass().add("elite-button-capturing");

        startKeyboardCapture();
        startHotasCapture();
    }
    
    @FXML
    private void captureTabSwitchRightBind() {
        if (isCapturingBind) {
            stopBindCapture();
            return;
        }

        currentBindType = "tabSwitchRight";
        isCapturingBind = true;
        tabSwitchRightButton.setText("BIND...");
        tabSwitchRightBindLabel.setText(localizationService.getString("config.window.toggle.bind.capturing"));
        // Changer le style du bouton en orange pendant la capture
        tabSwitchRightButton.getStyleClass().add("elite-button-capturing");

        startKeyboardCapture();
        startHotasCapture();
    }
    
    @FXML
    private void unbindWindowToggle() {
        capturedKeyCode = -1;
        capturedControllerName = null;
        capturedComponentName = null;
        capturedComponentValue = 0.0f;
        isKeyboardBind = false;
        // Mettre à jour directement l'affichage sans recharger depuis les préférences
        windowToggleBindValueLabel.setText(localizationService.getString("config.window.toggle.bind.none"));
        windowToggleUnbindButton.setVisible(false);
        windowToggleUnbindButton.setManaged(false);
    }
    
    @FXML
    private void unbindTabSwitchLeft() {
        capturedTabLeftKeyCode = -1;
        capturedTabLeftControllerName = null;
        capturedTabLeftComponentName = null;
        capturedTabLeftComponentValue = 0.0f;
        isTabLeftKeyboardBind = false;
        // Mettre à jour directement l'affichage sans recharger depuis les préférences
        tabSwitchLeftBindLabel.setText(localizationService.getString("config.tab.switch.bind.none"));
        tabSwitchLeftUnbindButton.setVisible(false);
        tabSwitchLeftUnbindButton.setManaged(false);
    }
    
    @FXML
    private void unbindTabSwitchRight() {
        capturedTabRightKeyCode = -1;
        capturedTabRightControllerName = null;
        capturedTabRightComponentName = null;
        capturedTabRightComponentValue = 0.0f;
        isTabRightKeyboardBind = false;
        // Mettre à jour directement l'affichage sans recharger depuis les préférences
        tabSwitchRightBindLabel.setText(localizationService.getString("config.tab.switch.bind.none"));
        tabSwitchRightUnbindButton.setVisible(false);
        tabSwitchRightUnbindButton.setManaged(false);
    }

    private void startKeyboardCapture() {
        // Utiliser un EventFilter sur la scène pour capturer les touches JavaFX
        Platform.runLater(() -> {
            Stage stage = (Stage) windowToggleBindButton.getScene().getWindow();
            if (stage != null && stage.getScene() != null) {
                Scene scene = stage.getScene();
                scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressForCapture);
            }
        });
        System.out.println("🎹 Capture clavier démarrée - Appuyez sur une touche...");
    }

    /**
     * Gère les pressions de touches pour la capture (via JavaFX)
     */
    private void handleKeyPressForCapture(KeyEvent event) {
        if (!isCapturingBind || currentBindType == null) {
            return;
        }

        // Convertir KeyCode JavaFX en code NativeKeyEvent
        KeyCode keyCode = event.getCode();
        int nativeKeyCode = convertKeyCodeToNative(keyCode);
        
        if (nativeKeyCode > 0) {
            System.out.println("🔑 Touche pressée pendant capture: " + nativeKeyCode + " (" + keyCode.getName() + ")");
            
            // Retirer le listener de la scène
            Stage stage = (Stage) windowToggleBindButton.getScene().getWindow();
            if (stage != null && stage.getScene() != null) {
                stage.getScene().removeEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressForCapture);
            }
            
            // Mettre à jour selon le type de bind
            if ("windowToggle".equals(currentBindType)) {
                capturedKeyCode = nativeKeyCode;
                isKeyboardBind = true;
                windowToggleBindValueLabel.setText(getKeyName(nativeKeyCode));
                windowToggleUnbindButton.setVisible(true);
                windowToggleUnbindButton.setManaged(true);
            } else if ("tabSwitchLeft".equals(currentBindType)) {
                capturedTabLeftKeyCode = nativeKeyCode;
                isTabLeftKeyboardBind = true;
                tabSwitchLeftBindLabel.setText(getKeyName(nativeKeyCode));
                tabSwitchLeftUnbindButton.setVisible(true);
                tabSwitchLeftUnbindButton.setManaged(true);
            } else if ("tabSwitchRight".equals(currentBindType)) {
                capturedTabRightKeyCode = nativeKeyCode;
                isTabRightKeyboardBind = true;
                tabSwitchRightBindLabel.setText(getKeyName(nativeKeyCode));
                tabSwitchRightUnbindButton.setVisible(true);
                tabSwitchRightUnbindButton.setManaged(true);
            }
            
            stopBindCapture();
            
            // Consommer l'événement pour éviter qu'il soit traité ailleurs
            event.consume();
        }
    }

    /**
     * Convertit KeyCode JavaFX en code NativeKeyEvent
     */
    private int convertKeyCodeToNative(KeyCode keyCode) {
        // Mapping manuel des touches courantes
        switch (keyCode) {
            case SPACE: return NativeKeyEvent.VC_SPACE;
            case ENTER: return NativeKeyEvent.VC_ENTER;
            case ESCAPE: return NativeKeyEvent.VC_ESCAPE;
            case TAB: return NativeKeyEvent.VC_TAB;
            case BACK_SPACE: return NativeKeyEvent.VC_BACKSPACE;
            case DELETE: return NativeKeyEvent.VC_DELETE;
            case UP: return NativeKeyEvent.VC_UP;
            case DOWN: return NativeKeyEvent.VC_DOWN;
            case LEFT: return NativeKeyEvent.VC_LEFT;
            case RIGHT: return NativeKeyEvent.VC_RIGHT;
            case F1: return NativeKeyEvent.VC_F1;
            case F2: return NativeKeyEvent.VC_F2;
            case F3: return NativeKeyEvent.VC_F3;
            case F4: return NativeKeyEvent.VC_F4;
            case F5: return NativeKeyEvent.VC_F5;
            case F6: return NativeKeyEvent.VC_F6;
            case F7: return NativeKeyEvent.VC_F7;
            case F8: return NativeKeyEvent.VC_F8;
            case F9: return NativeKeyEvent.VC_F9;
            case F10: return NativeKeyEvent.VC_F10;
            case F11: return NativeKeyEvent.VC_F11;
            case F12: return NativeKeyEvent.VC_F12;
            case SHIFT: return NativeKeyEvent.VC_SHIFT;
            case CONTROL: return NativeKeyEvent.VC_CONTROL;
            case ALT: return NativeKeyEvent.VC_ALT;
            // Mapping explicite pour toutes les lettres
            case A: return NativeKeyEvent.VC_A;
            case B: return NativeKeyEvent.VC_B;
            case C: return NativeKeyEvent.VC_C;
            case D: return NativeKeyEvent.VC_D;
            case E: return NativeKeyEvent.VC_E;
            case F: return NativeKeyEvent.VC_F;
            case G: return NativeKeyEvent.VC_G;
            case H: return NativeKeyEvent.VC_H;
            case I: return NativeKeyEvent.VC_I;
            case J: return NativeKeyEvent.VC_J;
            case K: return NativeKeyEvent.VC_K;
            case L: return NativeKeyEvent.VC_L;
            case M: return NativeKeyEvent.VC_M;
            case N: return NativeKeyEvent.VC_N;
            case O: return NativeKeyEvent.VC_O;
            case P: return NativeKeyEvent.VC_P;
            case Q: return NativeKeyEvent.VC_Q;
            case R: return NativeKeyEvent.VC_R;
            case S: return NativeKeyEvent.VC_S;
            case T: return NativeKeyEvent.VC_T;
            case U: return NativeKeyEvent.VC_U;
            case V: return NativeKeyEvent.VC_V;
            case W: return NativeKeyEvent.VC_W;
            case X: return NativeKeyEvent.VC_X;
            case Y: return NativeKeyEvent.VC_Y;
            case Z: return NativeKeyEvent.VC_Z;
            // Mapping explicite pour les chiffres
            case DIGIT0: return NativeKeyEvent.VC_0;
            case DIGIT1: return NativeKeyEvent.VC_1;
            case DIGIT2: return NativeKeyEvent.VC_2;
            case DIGIT3: return NativeKeyEvent.VC_3;
            case DIGIT4: return NativeKeyEvent.VC_4;
            case DIGIT5: return NativeKeyEvent.VC_5;
            case DIGIT6: return NativeKeyEvent.VC_6;
            case DIGIT7: return NativeKeyEvent.VC_7;
            case DIGIT8: return NativeKeyEvent.VC_8;
            case DIGIT9: return NativeKeyEvent.VC_9;
            default:
                return -1;
        }
    }

    private void startHotasCapture() {
        hotasCaptureThread = new Thread(() -> {
            try {
                Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
                
                var activeControllers = Arrays.stream(controllers)
                        .filter(c -> c.getType() == Controller.Type.STICK
                                || c.getType() == Controller.Type.GAMEPAD
                                || c.getType() == Controller.Type.WHEEL)
                        .toList();

                if (activeControllers.isEmpty()) {
                    return;
                }

                // Initialiser les états précédents en lisant une première fois
                Map<Controller, float[]> lastStates = new HashMap<>();
                for (Controller ctrl : activeControllers) {
                    if (ctrl.poll()) {
                        float[] initialState = new float[ctrl.getComponents().length];
                        for (int i = 0; i < ctrl.getComponents().length; i++) {
                            float val = ctrl.getComponents()[i].getPollData();
                            if (Math.abs(val) < 0.05f) val = 0.0f;
                            initialState[i] = val;
                        }
                        lastStates.put(ctrl, initialState);
                    }
                }

                // Attendre un peu pour s'assurer que les valeurs initiales sont bien lues
                Thread.sleep(100);

                while (isCapturingBind && !Thread.currentThread().isInterrupted()) {
                    for (Controller ctrl : activeControllers) {
                        if (!ctrl.poll()) {
                            continue;
                        }

                        var components = ctrl.getComponents();
                        float[] prevValues = lastStates.get(ctrl);

                        for (int i = 0; i < components.length; i++) {
                            var comp = components[i];
                            float value = comp.getPollData();
                            String name = comp.getName();

                            // Ignorer le bruit analogique
                            if (Math.abs(value) < 0.05f) value = 0.0f;

                            // Détecter un changement significatif (transition)
                            float prevValue = prevValues[i];
                            if (prevValue != value && Math.abs(value - prevValue) > 0.05f) {
                                // Mettre à jour l'état précédent
                                prevValues[i] = value;
                                
                                // Capturer seulement si la nouvelle valeur est significative
                                if (Math.abs(value) > 0.1f && isCapturingBind) {
                                    // Créer des copies finales pour la lambda
                                    final String finalControllerName = ctrl.getName();
                                    final String finalComponentName = name;
                                    //SI analogique, on prend valeur max
                                    if (comp.isAnalog()) {
                                        if (value > 0f) {
                                            value = 1f;
                                        } else if (value < 0f) {
                                            value = -1f;
                                        } else {
                                            value = 0f;
                                        }
                                    }

                                    final float finalValue = value;
                                    final String finalBindType = currentBindType;
                                    
                                    Platform.runLater(() -> {
                                        if ("windowToggle".equals(finalBindType)) {
                                            capturedControllerName = finalControllerName;
                                            capturedComponentName = finalComponentName;
                                            capturedComponentValue = finalValue;
                                            isKeyboardBind = false;
                                            windowToggleBindValueLabel.setText(
                                                capturedControllerName + " - " + 
                                                capturedComponentName + " (" + 
                                                String.format("%.2f", capturedComponentValue) + ")"
                                            );
                                            windowToggleUnbindButton.setVisible(true);
                                            windowToggleUnbindButton.setManaged(true);
                                        } else if ("tabSwitchLeft".equals(finalBindType)) {
                                            capturedTabLeftControllerName = finalControllerName;
                                            capturedTabLeftComponentName = finalComponentName;
                                            capturedTabLeftComponentValue = finalValue;
                                            isTabLeftKeyboardBind = false;
                                            tabSwitchLeftBindLabel.setText(
                                                capturedTabLeftControllerName + " - " + 
                                                capturedTabLeftComponentName + " (" + 
                                                String.format("%.2f", capturedTabLeftComponentValue) + ")"
                                            );
                                            tabSwitchLeftUnbindButton.setVisible(true);
                                            tabSwitchLeftUnbindButton.setManaged(true);
                                        } else if ("tabSwitchRight".equals(finalBindType)) {
                                            capturedTabRightControllerName = finalControllerName;
                                            capturedTabRightComponentName = finalComponentName;
                                            capturedTabRightComponentValue = finalValue;
                                            isTabRightKeyboardBind = false;
                                            tabSwitchRightBindLabel.setText(
                                                capturedTabRightControllerName + " - " + 
                                                capturedTabRightComponentName + " (" + 
                                                String.format("%.2f", capturedTabRightComponentValue) + ")"
                                            );
                                            tabSwitchRightUnbindButton.setVisible(true);
                                            tabSwitchRightUnbindButton.setManaged(true);
                                        }
                                        
                                        // Retirer le listener de la scène
                                        Stage stage = (Stage) windowToggleBindButton.getScene().getWindow();
                                        if (stage != null && stage.getScene() != null) {
                                            stage.getScene().removeEventFilter(KeyEvent.KEY_PRESSED, ConfigDialogController.this::handleKeyPressForCapture);
                                        }
                                        
                                        stopBindCapture();
                                    });
                                    return;
                                }
                            }
                        }
                    }

                    Thread.sleep(50); // Poll plus fréquemment pour la capture
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "HotasBindCaptureThread");
        
        hotasCaptureThread.start();
    }

    private void stopBindCapture() {
        isCapturingBind = false;
        
        // Retirer le listener de la scène
        try {
            Stage stage = (Stage) windowToggleBindButton.getScene().getWindow();
            if (stage != null && stage.getScene() != null) {
                stage.getScene().removeEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressForCapture);
            }
        } catch (Exception ignored) {}
        
        // Réinitialiser les boutons selon le type
        String bindButtonText = localizationService.getString("config.window.toggle.bind.button");
        if ("windowToggle".equals(currentBindType)) {
            windowToggleBindButton.setText(bindButtonText);
            windowToggleBindButton.getStyleClass().remove("elite-button-capturing");
        } else if ("tabSwitchLeft".equals(currentBindType)) {
            tabSwitchLeftButton.setText(bindButtonText);
            tabSwitchLeftButton.getStyleClass().remove("elite-button-capturing");
        } else if ("tabSwitchRight".equals(currentBindType)) {
            tabSwitchRightButton.setText(bindButtonText);
            tabSwitchRightButton.getStyleClass().remove("elite-button-capturing");
        }
        
        if (keyCaptureListener != null) {
            try {
                GlobalScreen.removeNativeKeyListener(keyCaptureListener);
            } catch (Exception ignored) {}
            keyCaptureListener = null;
        }
        
        if (hotasCaptureThread != null && hotasCaptureThread.isAlive()) {
            hotasCaptureThread.interrupt();
        }
        
        currentBindType = null;
        
        // Ne pas redémarrer le service ici car on est toujours dans la fenêtre de config
        // Le service sera redémarré après la sauvegarde
    }

    private String getKeyName(int keyCode) {
        try {
            return NativeKeyEvent.getKeyText(keyCode);
        } catch (Exception e) {
            return "Key " + keyCode;
        }
    }

    private void loadDonateButtonImage() {
        try {
            Image donateImage = new Image(getClass().getResourceAsStream("/images/exploration/readme/donate.png"));
            donateButtonImage.setImage(donateImage);
        } catch (Exception e) {
            Logger.getLogger(ConfigDialogController.class.getName())
                .log(Level.WARNING, "Impossible de charger l'image du bouton de don", e);
        }
    }

    @FXML
    private void openDonateLink(MouseEvent event) {
        try {
            URI uri = new URI("https://www.paypal.com/donate/?hosted_button_id=2GSWMTWB4SHA2");
            Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
            if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(uri);
            } else {
                Logger.getLogger(ConfigDialogController.class.getName())
                    .log(Level.WARNING, "Impossible d'ouvrir le navigateur pour le lien de don");
            }
        } catch (Exception e) {
            Logger.getLogger(ConfigDialogController.class.getName())
                .log(Level.SEVERE, "Erreur lors de l'ouverture du lien de don", e);
        }
    }
}


