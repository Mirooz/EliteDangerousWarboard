package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.backend.generated.model.NearbyExportsBestStationResult;
import be.mirooz.elitedangerous.dashboard.model.colonisation.construction.Colony;
import be.mirooz.elitedangerous.dashboard.model.colonisation.construction.Structure;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 * Service pour gérer les préférences utilisateur
 */
public class PreferencesService {
    private static PreferencesService instance;
    private final Properties preferences;
    private final Path preferencesFile;

    /** Cache JSON des stations d’achat suggérées (colonisation), à côté de {@code preferences.properties}. */
    private static final String COLONISATION_SUGGESTED_BUY_STATIONS_FILE = "colonisation-suggested-stations.json";

    /** Référence structure Colony ({@code construction_class.json}) par {@code marketId} du chantier. */
    private static final String COLONISATION_CONSTRUCTION_STRUCTURE_TYPES_FILE = "colonisation-construction-structure-types.properties";

    private static final ObjectMapper COLONISATION_SUGGESTED_STATIONS_JSON = createColonisationSuggestedStationsMapper();

    private volatile Properties colonisationConstructionStructureTypes;
    private final Object colonisationConstructionStructureTypesLock = new Object();

    private static ObjectMapper createColonisationSuggestedStationsMapper() {
        ObjectMapper m = new ObjectMapper();
        m.registerModule(new JavaTimeModule());
        m.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return m;
    }

    private PreferencesService() {
        this.preferences = new Properties();
        this.preferencesFile = Paths.get(System.getProperty("user.home"), ".elite-warboard", "preferences.properties");
        loadPreferences();
    }

    private Path colonisationSuggestedBuyStationsPath() {
        Path parent = preferencesFile.getParent();
        if (parent == null) {
            return Paths.get(COLONISATION_SUGGESTED_BUY_STATIONS_FILE);
        }
        return parent.resolve(COLONISATION_SUGGESTED_BUY_STATIONS_FILE);
    }

    private Path colonisationConstructionStructureTypesPath() {
        Path parent = preferencesFile.getParent();
        if (parent == null) {
            return Paths.get(COLONISATION_CONSTRUCTION_STRUCTURE_TYPES_FILE);
        }
        return parent.resolve(COLONISATION_CONSTRUCTION_STRUCTURE_TYPES_FILE);
    }

    private Properties loadColonisationConstructionStructureTypes() {
        Properties p = new Properties();
        Path file = colonisationConstructionStructureTypesPath();
        try {
            if (Files.isRegularFile(file)) {
                try (InputStream in = Files.newInputStream(file)) {
                    p.load(in);
                }
            }
        } catch (IOException e) {
            System.err.println("Préférences: lecture types structure colonisation : " + e.getMessage());
        }
        return p;
    }

    private void saveColonisationConstructionStructureTypes(Properties p) {
        Path file = colonisationConstructionStructureTypesPath();
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (OutputStream out = Files.newOutputStream(file)) {
                p.store(out, "Colonisation construction structures (Colony JSON) by MarketID");
            }
        } catch (IOException e) {
            System.err.println("Préférences: enregistrement types structure colonisation : " + e.getMessage());
        }
    }

    private Properties colonisationConstructionStructureTypes() {
        Properties local = colonisationConstructionStructureTypes;
        if (local != null) {
            return local;
        }
        synchronized (colonisationConstructionStructureTypesLock) {
            if (colonisationConstructionStructureTypes == null) {
                colonisationConstructionStructureTypes = loadColonisationConstructionStructureTypes();
            }
            return colonisationConstructionStructureTypes;
        }
    }

    /**
     * Structure Colony ({@code construction_class.json}) associée au chantier / station
     * ({@code marketId}), persistée sous {@code ~/.elite-warboard/colonisation-construction-structure-types.properties}.
     */
    public Optional<Structure> getColonisationUserConstructionStructure(long marketId) {
        if (marketId <= 0) {
            return Optional.empty();
        }
        String v = colonisationConstructionStructureTypes().getProperty(String.valueOf(marketId));
        if (v == null || v.isBlank()) {
            return Optional.empty();
        }
        return Colony.structureFromPersistedKey(v.strip());
    }

    /**
     * Enregistre ou efface la structure utilisateur pour ce {@code marketId}.
     *
     * @param structure {@code null} pour supprimer l’entrée.
     */
    public void setColonisationUserConstructionStructure(long marketId, Structure structure) {
        if (marketId <= 0) {
            return;
        }
        synchronized (colonisationConstructionStructureTypesLock) {
            Properties p = loadColonisationConstructionStructureTypes();
            String key = String.valueOf(marketId);
            if (structure == null) {
                p.remove(key);
            } else {
                p.setProperty(key, Colony.persistedStructureKey(structure));
            }
            saveColonisationConstructionStructureTypes(p);
            colonisationConstructionStructureTypes = p;
        }
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
        if (value == null) {
            // Si la valeur est null, supprimer la préférence
            preferences.remove(key);
        } else {
            preferences.setProperty(key, value);
        }
        savePreferences();
    }

    /**
     * Supprime une préférence
     */
    public void removePreference(String key) {
        preferences.remove(key);
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
        String daysStr = preferences.getProperty("journal.days", "180");
        try {
            return Integer.parseInt(daysStr);
        } catch (NumberFormatException e) {
            return 60; // Valeur par défaut si parsing échoue
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
        String value = preferences.getProperty("window.toggle.enabled", "false");
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
        try {
            String value = preferences.getProperty("window.toggle.keyboard.key", "-1"); // VC_SPACE = 57

            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return -1; // Espace par défaut
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
        return preferences.getProperty("window.toggle.hotas.component", null);
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
        String value = preferences.getProperty("window.toggle.hotas.value", "0");
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return 0; // Valeur par défaut
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
        String value = preferences.getProperty("tab.switch.left.hotas.value", "0");
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return 0;
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
        String value = preferences.getProperty("tab.switch.right.hotas.value", "0");
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Enregistre les stations d’achat suggérées pour un chantier ({@code buildingMarketId}) dans
     * {@code ~/.elite-warboard/colonisation-suggested-stations.json}.
     */
    public void persistColonisationSuggestedBuyStations(long buildingMarketId, List<NearbyExportsBestStationResult> stations) {
        if (buildingMarketId <= 0) {
            return;
        }
        Path file = colonisationSuggestedBuyStationsPath();
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            var root = COLONISATION_SUGGESTED_STATIONS_JSON.createObjectNode();
            root.put("buildingMarketId", buildingMarketId);
            root.set("stations", COLONISATION_SUGGESTED_STATIONS_JSON.valueToTree(stations != null ? stations : List.of()));
            COLONISATION_SUGGESTED_STATIONS_JSON.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), root);
        } catch (IOException e) {
            System.err.println("Préférences: échec enregistrement stations d'achat colonisation : " + e.getMessage());
        }
    }

    /**
     * Recharge les stations d’achat persistées si le fichier existe et correspond au {@code MarketID} du chantier.
     *
     * @return vide si pas de fichier ou market incohérent ; sinon la liste (éventuellement vide).
     */
    public Optional<List<NearbyExportsBestStationResult>> loadColonisationSuggestedBuyStationsIfMatches(long buildingMarketId) {
        if (buildingMarketId <= 0) {
            return Optional.empty();
        }
        Path file = colonisationSuggestedBuyStationsPath();
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            JsonNode root = COLONISATION_SUGGESTED_STATIONS_JSON.readTree(file.toFile());
            if (root == null || root.path("buildingMarketId").asLong(0L) != buildingMarketId) {
                return Optional.empty();
            }
            JsonNode arr = root.get("stations");
            if (arr == null || !arr.isArray()) {
                return Optional.of(List.of());
            }
            List<NearbyExportsBestStationResult> list = COLONISATION_SUGGESTED_STATIONS_JSON.convertValue(
                    arr, new TypeReference<List<NearbyExportsBestStationResult>>() { });
            return Optional.of(list != null ? List.copyOf(list) : List.of());
        } catch (IOException e) {
            System.err.println("Préférences: échec lecture stations d'achat colonisation : " + e.getMessage());
            return Optional.empty();
        }
    }

    /** Supprime le fichier cache des stations d’achat colonisation (ex. reset chantier). */
    public void removeColonisationSuggestedBuyStationsFile() {
        try {
            Files.deleteIfExists(colonisationSuggestedBuyStationsPath());
        } catch (IOException e) {
            System.err.println("Préférences: impossible de supprimer le cache stations d'achat colonisation : " + e.getMessage());
        }
    }
}


