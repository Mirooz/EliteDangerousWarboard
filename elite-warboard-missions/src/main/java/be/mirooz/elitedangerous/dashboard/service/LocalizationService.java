package be.mirooz.elitedangerous.dashboard.service;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Service de localisation pour gérer les traductions
 */
public class LocalizationService {
    private static LocalizationService instance;
    private ResourceBundle currentBundle;
    private Locale currentLocale;
    private final CopyOnWriteArrayList<Consumer<Locale>> languageChangeListeners = new CopyOnWriteArrayList<>();

    private LocalizationService() {
        // Charger la langue depuis les préférences
        PreferencesService preferencesService = PreferencesService.getInstance();
        String savedLanguage = preferencesService.getLanguage();
        setLanguage(savedLanguage);
    }

    public static LocalizationService getInstance() {
        if (instance == null) {
            instance = new LocalizationService();
        }
        return instance;
    }

    /**
     * Change la locale de l'application
     */
    public void setLocale(Locale locale) {
        this.currentLocale = locale;
        try {
            this.currentBundle = ResourceBundle.getBundle("messages", locale);
        } catch (Exception e) {
            // Fallback vers le français si la locale demandée n'existe pas
            this.currentBundle = ResourceBundle.getBundle("messages", Locale.FRENCH);
            this.currentLocale = Locale.FRENCH;
        }
        // Notifier tous les listeners du changement de langue
        notifyLanguageChange(locale);
    }

    /**
     * Change la locale par langue
     */
    public void setLanguage(String language) {
        switch (language.toLowerCase()) {
            case "en":
            case "english":
                setLocale(Locale.ENGLISH);
                break;
            case "fr":
            case "french":
            case "français":
            default:
                setLocale(Locale.FRENCH);
                break;
        }
    }

    /**
     * Récupère une traduction
     */
    public String getString(String key) {
        try {
            return currentBundle.getString(key);
        } catch (Exception e) {
            return key; // Retourne la clé si la traduction n'existe pas
        }
    }

    /**
     * Récupère une traduction avec paramètres
     */
    public String getString(String key, Object... params) {
        try {
            String template = currentBundle.getString(key);
            return String.format(template, params);
        } catch (Exception e) {
            return key; // Retourne la clé si la traduction n'existe pas
        }
    }

    /**
     * Récupère la locale actuelle
     */
    public Locale getCurrentLocale() {
        return currentLocale;
    }

    /**
     * Vérifie si l'application est en anglais
     */
    public boolean isEnglish() {
        return Locale.ENGLISH.equals(currentLocale);
    }

    /**
     * Vérifie si l'application est en français
     */
    public boolean isFrench() {
        return Locale.FRENCH.equals(currentLocale);
    }

    /**
     * Ajoute un listener pour les changements de langue
     */
    public void addLanguageChangeListener(Consumer<Locale> listener) {
        languageChangeListeners.add(listener);
    }

    /**
     * Supprime un listener pour les changements de langue
     */
    public void removeLanguageChangeListener(Consumer<Locale> listener) {
        languageChangeListeners.remove(listener);
    }

    /**
     * Notifie tous les listeners du changement de langue
     */
    private void notifyLanguageChange(Locale newLocale) {
        for (Consumer<Locale> listener : languageChangeListeners) {
            try {
                listener.accept(newLocale);
            } catch (Exception e) {
                System.err.println("Erreur lors de la notification du changement de langue: " + e.getMessage());
            }
        }
    }
}
