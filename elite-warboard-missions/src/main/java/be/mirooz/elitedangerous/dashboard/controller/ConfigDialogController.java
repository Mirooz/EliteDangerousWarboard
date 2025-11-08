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
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;

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
    private Label windowToggleSectionLabel;

    @FXML
    private CheckBox windowToggleEnabledCheckBox;

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
    private Label tabSwitchSectionLabel;

    @FXML
    private CheckBox tabSwitchEnabledCheckBox;

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
        
        // Initialiser les valeurs du toggle de fenêtre
        windowToggleEnabledCheckBox.setSelected(preferencesService.isWindowToggleEnabled());
        updateWindowToggleBindDisplay();
        
        // Initialiser les valeurs du changement de tab
        tabSwitchEnabledCheckBox.setSelected(preferencesService.isTabSwitchEnabled());
        updateTabSwitchBindDisplays();
        
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
        
        // Traductions pour la section toggle de fenêtre
        windowToggleSectionLabel.setText(localizationService.getString("config.window.toggle"));
        windowToggleEnabledCheckBox.setText(localizationService.getString("config.window.toggle.enabled"));
        windowToggleBindLabel.setText(localizationService.getString("config.window.toggle.bind"));
        windowToggleBindButton.setText(localizationService.getString("config.window.toggle.bind.button"));
        windowToggleBindDescriptionLabel.setText(localizationService.getString("config.window.toggle.bind.description"));
        
        // Traductions pour la section changement de tab
        tabSwitchSectionLabel.setText(localizationService.getString("config.tab.switch"));
        tabSwitchEnabledCheckBox.setText(localizationService.getString("config.tab.switch.enabled"));
        tabSwitchLeftLabel.setText(localizationService.getString("config.tab.switch.left"));
        tabSwitchRightLabel.setText(localizationService.getString("config.tab.switch.right"));
        
        browseJournalFolderButton.setText(localizationService.getString("config.browse"));
        saveButton.setText(localizationService.getString("config.save"));
        cancelButton.setText(localizationService.getString("config.cancel"));
        
        // Mettre à jour les affichages des binds
        updateWindowToggleBindDisplay();
        updateTabSwitchBindDisplays();
    }
    
    private void updateWindowToggleBindDisplay() {
        String controllerName = preferencesService.getWindowToggleHotasController();
        String componentName = preferencesService.getWindowToggleHotasComponent();
        
        if (controllerName != null && !controllerName.isEmpty() && 
            componentName != null && !componentName.isEmpty()) {
            // Bind HOTAS
            float value = preferencesService.getWindowToggleHotasValue();
            windowToggleBindValueLabel.setText(controllerName + " - " + componentName + " (" + String.format("%.2f", value) + ")");
            windowToggleUnbindButton.setVisible(true);
            windowToggleUnbindButton.setManaged(true);
            isKeyboardBind = false;
            capturedControllerName = controllerName;
            capturedComponentName = componentName;
            capturedComponentValue = value;
        } else {
            // Bind clavier
            int keyCode = preferencesService.getWindowToggleKeyboardKey();
            if (keyCode > 0) {
                windowToggleBindValueLabel.setText(getKeyName(keyCode));
                windowToggleUnbindButton.setVisible(true);
                windowToggleUnbindButton.setManaged(true);
                isKeyboardBind = true;
                capturedKeyCode = keyCode;
            } else {
                windowToggleBindValueLabel.setText(localizationService.getString("config.window.toggle.bind.none"));
                windowToggleUnbindButton.setVisible(false);
                windowToggleUnbindButton.setManaged(false);
                isKeyboardBind = false;
                capturedKeyCode = -1;
            }
        }
    }
    
    private void updateTabSwitchBindDisplays() {
        // Tab left
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
        
        // Tab right
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
        
        // Sauvegarder les paramètres du toggle de fenêtre
        preferencesService.setWindowToggleEnabled(windowToggleEnabledCheckBox.isSelected());
        
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
        }
        
        // Sauvegarder les paramètres du changement de tab
        preferencesService.setTabSwitchEnabled(tabSwitchEnabledCheckBox.isSelected());
        
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

    @FXML
    private void captureWindowToggleBind() {
        if (isCapturingBind) {
            stopBindCapture();
            return;
        }

        currentBindType = "windowToggle";
        isCapturingBind = true;
        windowToggleBindButton.setText("BIND...");
        windowToggleBindValueLabel.setText("Appuyez sur une touche clavier ou un bouton/composant HOTAS...");

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
        tabSwitchLeftBindLabel.setText("Appuyez sur une touche clavier ou un bouton/composant HOTAS...");

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
        tabSwitchRightBindLabel.setText("Appuyez sur une touche clavier ou un bouton/composant HOTAS...");

        startKeyboardCapture();
        startHotasCapture();
    }
    
    @FXML
    private void unbindWindowToggle() {
        capturedKeyCode = -1;
        capturedControllerName = null;
        capturedComponentName = null;
        isKeyboardBind = false;
        updateWindowToggleBindDisplay();
    }
    
    @FXML
    private void unbindTabSwitchLeft() {
        capturedTabLeftKeyCode = -1;
        capturedTabLeftControllerName = null;
        capturedTabLeftComponentName = null;
        isTabLeftKeyboardBind = false;
        updateTabSwitchBindDisplays();
    }
    
    @FXML
    private void unbindTabSwitchRight() {
        capturedTabRightKeyCode = -1;
        capturedTabRightControllerName = null;
        capturedTabRightComponentName = null;
        isTabRightKeyboardBind = false;
        updateTabSwitchBindDisplays();
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
                windowToggleBindValueLabel.setText(keyCode.getName());
                windowToggleUnbindButton.setVisible(true);
                windowToggleUnbindButton.setManaged(true);
            } else if ("tabSwitchLeft".equals(currentBindType)) {
                capturedTabLeftKeyCode = nativeKeyCode;
                isTabLeftKeyboardBind = true;
                tabSwitchLeftBindLabel.setText(keyCode.getName());
                tabSwitchLeftUnbindButton.setVisible(true);
                tabSwitchLeftUnbindButton.setManaged(true);
            } else if ("tabSwitchRight".equals(currentBindType)) {
                capturedTabRightKeyCode = nativeKeyCode;
                isTabRightKeyboardBind = true;
                tabSwitchRightBindLabel.setText(keyCode.getName());
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
            default:
                // Pour les lettres (A-Z)
                if (keyCode.isLetterKey()) {
                    char c = Character.toUpperCase(keyCode.getName().charAt(0));
                    return NativeKeyEvent.VC_A + (c - 'A');
                }
                // Pour les chiffres (0-9)
                if (keyCode.isDigitKey()) {
                    int digit = Character.getNumericValue(keyCode.getName().charAt(0));
                    return NativeKeyEvent.VC_0 + digit;
                }
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
        if ("windowToggle".equals(currentBindType)) {
            windowToggleBindButton.setText(localizationService.getString("config.window.toggle.bind.button"));
        } else if ("tabSwitchLeft".equals(currentBindType)) {
            tabSwitchLeftButton.setText(localizationService.getString("config.window.toggle.bind.button"));
        } else if ("tabSwitchRight".equals(currentBindType)) {
            tabSwitchRightButton.setText(localizationService.getString("config.window.toggle.bind.button"));
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
}


