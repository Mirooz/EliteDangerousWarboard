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
     * Vérifie si un fichier de préférences existe
     */
    public boolean hasPreferencesFile() {
        return Files.exists(preferencesFile);
    }
}


