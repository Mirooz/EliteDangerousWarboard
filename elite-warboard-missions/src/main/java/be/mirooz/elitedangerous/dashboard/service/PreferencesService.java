package be.mirooz.elitedangerous.dashboard.service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Service pour gérer les préférences utilisateur
 */
public class PreferencesService {
    private static PreferencesService instance;
    private final Properties preferences;
    private final Path preferencesFile;

    private PreferencesService() {
        this.preferences = new Properties();
        this.preferencesFile = Paths.get(System.getProperty("user.home"), ".elite-wardboard", "preferences.properties");
        loadPreferences();
    }

    public static PreferencesService getInstance() {
        if (instance == null) {
            instance = new PreferencesService();
        }
        return instance;
    }

    /**
     * Charge les préférences depuis le fichier
     */
    private void loadPreferences() {
        try {
            if (Files.exists(preferencesFile)) {
                try (FileInputStream fis = new FileInputStream(preferencesFile.toFile())) {
                    preferences.load(fis);
                }
            }
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement des préférences: " + e.getMessage());
        }
    }

    /**
     * Sauvegarde les préférences dans le fichier
     */
    private void savePreferences() {
        try {
            // Créer le répertoire s'il n'existe pas
            Files.createDirectories(preferencesFile.getParent());
            
            try (FileOutputStream fos = new FileOutputStream(preferencesFile.toFile())) {
                preferences.store(fos, "Elite Dangerous Dashboard Preferences");
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la sauvegarde des préférences: " + e.getMessage());
        }
    }

    /**
     * Définit la langue préférée
     */
    public void setLanguage(String language) {
        preferences.setProperty("language", language);
        savePreferences();
    }

    /**
     * Récupère la langue préférée
     */
    public String getLanguage() {
        return preferences.getProperty("language", "en"); // Par défaut anglais
    }

    /**
     * Définit une préférence
     */
    public void setPreference(String key, String value) {
        preferences.setProperty(key, value);
        savePreferences();
    }

    /**
     * Récupère une préférence
     */
    public String getPreference(String key, String defaultValue) {
        return preferences.getProperty(key, defaultValue);
    }

    /**
     * Définit le dossier journal Elite Dangerous
     */
    public void setJournalFolder(String journalFolder) {
        preferences.setProperty("journal.folder", journalFolder);
        savePreferences();
    }

    /**
     * Récupère le dossier journal Elite Dangerous
     */
    public String getJournalFolder() {
        // Chemin par défaut pour Windows
        String defaultPath = System.getProperty("user.home") + "\\Saved Games\\Frontier Developments\\Elite Dangerous";
        return preferences.getProperty("journal.folder", defaultPath);
    }

    /**
     * Définit le nombre de jours pour la lecture des journaux
     */
    public void setJournalDays(int days) {
        preferences.setProperty("journal.days", String.valueOf(days));
        savePreferences();
    }

    /**
     * Récupère le nombre de jours pour la lecture des journaux
     */
    public int getJournalDays() {
        String daysStr = preferences.getProperty("journal.days", "30");
        try {
            return Integer.parseInt(daysStr);
        } catch (NumberFormatException e) {
            return 30; // Valeur par défaut si parsing échoue
        }
    }

    /**
     * Vérifie si un fichier de préférences existe
     */
    public boolean hasPreferencesFile() {
        return Files.exists(preferencesFile);
    }

    /**
     * Active ou désactive le toggle de fenêtre
     */
    public void setWindowToggleEnabled(boolean enabled) {
        preferences.setProperty("window.toggle.enabled", String.valueOf(enabled));
        savePreferences();
    }

    /**
     * Vérifie si le toggle de fenêtre est activé
     */
    public boolean isWindowToggleEnabled() {
        String value = preferences.getProperty("window.toggle.enabled", "true");
        return Boolean.parseBoolean(value);
    }

    /**
     * Définit la touche clavier pour le toggle (code de touche NativeKeyEvent)
     */
    public void setWindowToggleKeyboardKey(int keyCode) {
        preferences.setProperty("window.toggle.keyboard.key", String.valueOf(keyCode));
        savePreferences();
    }

    /**
     * Récupère la touche clavier pour le toggle (par défaut: Espace = 57)
     */
    public int getWindowToggleKeyboardKey() {
        String value = preferences.getProperty("window.toggle.keyboard.key", "57"); // VC_SPACE = 57
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 57; // Espace par défaut
        }
    }

    /**
     * Définit le nom du contrôleur HOTAS pour le toggle
     */
    public void setWindowToggleHotasController(String controllerName) {
        preferences.setProperty("window.toggle.hotas.controller", controllerName != null ? controllerName : "");
        savePreferences();
    }

    /**
     * Récupère le nom du contrôleur HOTAS pour le toggle
     */
    public String getWindowToggleHotasController() {
        return preferences.getProperty("window.toggle.hotas.controller", "TWCS Throttle");
    }

    /**
     * Définit le nom du composant HOTAS pour le toggle
     */
    public void setWindowToggleHotasComponent(String componentName) {
        preferences.setProperty("window.toggle.hotas.component", componentName != null ? componentName : "");
        savePreferences();
    }

    /**
     * Récupère le nom du composant HOTAS pour le toggle
     */
    public String getWindowToggleHotasComponent() {
        return preferences.getProperty("window.toggle.hotas.component", "Commande de pouce");
    }

    /**
     * Définit la valeur du composant HOTAS pour le toggle
     */
    public void setWindowToggleHotasValue(float value) {
        preferences.setProperty("window.toggle.hotas.value", String.valueOf(value));
        savePreferences();
    }

    /**
     * Récupère la valeur du composant HOTAS pour le toggle
     */
    public float getWindowToggleHotasValue() {
        String value = preferences.getProperty("window.toggle.hotas.value", "0.25");
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return 0.25f; // Valeur par défaut
        }
    }

    /**
     * Active ou désactive le changement d'onglet
     */
    public void setTabSwitchEnabled(boolean enabled) {
        preferences.setProperty("tab.switch.enabled", String.valueOf(enabled));
        savePreferences();
    }

    /**
     * Vérifie si le changement d'onglet est activé
     */
    public boolean isTabSwitchEnabled() {
        String value = preferences.getProperty("tab.switch.enabled", "false");
        return Boolean.parseBoolean(value);
    }

    /**
     * Définit la touche clavier pour changer vers l'onglet précédent
     */
    public void setTabSwitchLeftKeyboardKey(int keyCode) {
        preferences.setProperty("tab.switch.left.keyboard.key", String.valueOf(keyCode));
        savePreferences();
    }

    /**
     * Récupère la touche clavier pour changer vers l'onglet précédent
     */
    public int getTabSwitchLeftKeyboardKey() {
        String value = preferences.getProperty("tab.switch.left.keyboard.key", "-1");
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Définit le nom du contrôleur HOTAS pour changer vers l'onglet précédent
     */
    public void setTabSwitchLeftHotasController(String controllerName) {
        preferences.setProperty("tab.switch.left.hotas.controller", controllerName != null ? controllerName : "");
        savePreferences();
    }

    /**
     * Récupère le nom du contrôleur HOTAS pour changer vers l'onglet précédent
     */
    public String getTabSwitchLeftHotasController() {
        return preferences.getProperty("tab.switch.left.hotas.controller", "");
    }

    /**
     * Définit le nom du composant HOTAS pour changer vers l'onglet précédent
     */
    public void setTabSwitchLeftHotasComponent(String componentName) {
        preferences.setProperty("tab.switch.left.hotas.component", componentName != null ? componentName : "");
        savePreferences();
    }

    /**
     * Récupère le nom du composant HOTAS pour changer vers l'onglet précédent
     */
    public String getTabSwitchLeftHotasComponent() {
        return preferences.getProperty("tab.switch.left.hotas.component", "");
    }

    /**
     * Définit la valeur du composant HOTAS pour changer vers l'onglet précédent
     */
    public void setTabSwitchLeftHotasValue(float value) {
        preferences.setProperty("tab.switch.left.hotas.value", String.valueOf(value));
        savePreferences();
    }

    /**
     * Récupère la valeur du composant HOTAS pour changer vers l'onglet précédent
     */
    public float getTabSwitchLeftHotasValue() {
        String value = preferences.getProperty("tab.switch.left.hotas.value", "0.25");
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return 0.25f;
        }
    }

    /**
     * Définit la touche clavier pour changer vers l'onglet suivant
     */
    public void setTabSwitchRightKeyboardKey(int keyCode) {
        preferences.setProperty("tab.switch.right.keyboard.key", String.valueOf(keyCode));
        savePreferences();
    }

    /**
     * Récupère la touche clavier pour changer vers l'onglet suivant
     */
    public int getTabSwitchRightKeyboardKey() {
        String value = preferences.getProperty("tab.switch.right.keyboard.key", "-1");
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Définit le nom du contrôleur HOTAS pour changer vers l'onglet suivant
     */
    public void setTabSwitchRightHotasController(String controllerName) {
        preferences.setProperty("tab.switch.right.hotas.controller", controllerName != null ? controllerName : "");
        savePreferences();
    }

    /**
     * Récupère le nom du contrôleur HOTAS pour changer vers l'onglet suivant
     */
    public String getTabSwitchRightHotasController() {
        return preferences.getProperty("tab.switch.right.hotas.controller", "");
    }

    /**
     * Définit le nom du composant HOTAS pour changer vers l'onglet suivant
     */
    public void setTabSwitchRightHotasComponent(String componentName) {
        preferences.setProperty("tab.switch.right.hotas.component", componentName != null ? componentName : "");
        savePreferences();
    }

    /**
     * Récupère le nom du composant HOTAS pour changer vers l'onglet suivant
     */
    public String getTabSwitchRightHotasComponent() {
        return preferences.getProperty("tab.switch.right.hotas.component", "");
    }

    /**
     * Définit la valeur du composant HOTAS pour changer vers l'onglet suivant
     */
    public void setTabSwitchRightHotasValue(float value) {
        preferences.setProperty("tab.switch.right.hotas.value", String.valueOf(value));
        savePreferences();
    }

    /**
     * Récupère la valeur du composant HOTAS pour changer vers l'onglet suivant
     */
    public float getTabSwitchRightHotasValue() {
        String value = preferences.getProperty("tab.switch.right.hotas.value", "0.25");
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return 0.25f;
        }
    }
}


