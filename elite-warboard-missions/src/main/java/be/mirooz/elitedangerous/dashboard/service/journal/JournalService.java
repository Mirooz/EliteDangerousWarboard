package be.mirooz.elitedangerous.dashboard.service.journal;

import be.mirooz.elitedangerous.dashboard.controller.ui.component.DialogComponent;
import be.mirooz.elitedangerous.dashboard.controller.ui.manager.PopupManager;
import be.mirooz.elitedangerous.dashboard.model.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.model.commander.Mission;
import be.mirooz.elitedangerous.dashboard.model.events.Cargo;
import be.mirooz.elitedangerous.dashboard.model.registries.combat.MissionsRegistry;
import be.mirooz.elitedangerous.dashboard.handlers.dispatcher.JournalEventDispatcher;
import be.mirooz.elitedangerous.dashboard.service.analytics.AnalyticsClient;
import be.mirooz.elitedangerous.dashboard.service.journal.watcher.JournalTailService;
import be.mirooz.elitedangerous.dashboard.service.journal.watcher.JournalWatcherService;
import be.mirooz.elitedangerous.dashboard.controller.ui.context.DashboardContext;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.service.PreferencesService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service pour lire et parser les fichiers Journal Elite Dangerous
 */
public class JournalService {

    private static final String JOURNAL_PREFIX = "Journal.";
    private static final String CARGO_FILE = "Cargo.json";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();
    private final PreferencesService preferencesService = PreferencesService.getInstance();
    private final LocalizationService localizationService = LocalizationService.getInstance();
    private final PopupManager popupManager = PopupManager.getInstance();

    private final MissionsRegistry missionsRegistry = MissionsRegistry.getInstance();
    private final JournalEventDispatcher dispatcher;


    private AnalyticsClient analyticsClient = AnalyticsClient.getInstance();
    private JournalService() {
        this.dispatcher = JournalEventDispatcher.getInstance();
    }

    private static final JournalService INSTANCE = new JournalService();

    public static JournalService getInstance() {
        return INSTANCE;
    }

    /**
     * Récupère toutes les missions des 7 derniers jours
     */
    public List<Mission> getMissionsFromLastWeek() {
        try {
            // D'abord extraire le nom du commandant du fichier le plus récent
            extractCommanderNameFromLatestJournal();

            // Démarrer la session analytics (appel HTTP au backend)
            String commanderName = CommanderStatus.getInstance().getCommanderName();
            if (commanderName == null || commanderName.isEmpty()) {
                commanderName = "Unknown";
            }
            // Récupérer la version de l'application depuis le pom.xml parent
            analyticsClient.startSession(commanderName);

            // Utiliser la nouvelle méthode qui traite tous les fichiers et met à jour les missions
            return parseAllJournalFiles();
        } catch (Exception e) {
            System.err.println("Erreur lors de la lecture des journaux: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Extrait le nom du commandant du fichier journal le plus récent
     */
    private void extractCommanderNameFromLatestJournal() {
        try {
            List<File> journalFiles = getJournalFilesFromLastWeek();
            if (journalFiles.isEmpty()) {
                System.err.println("Aucun fichier journal trouvé");
                return;
            }
            journalFiles.sort(Comparator.comparing(File::getName));

            // Parcourt tous les fichiers, du plus récent au plus ancien
            for (File journal : journalFiles) {
                List<String> lines = Files.readAllLines(journal.toPath(), StandardCharsets.UTF_8);


                for (String line : lines) {
                    try {
                        JsonNode jsonNode = objectMapper.readTree(line);
                        if ("Commander".equals(jsonNode.path("event").asText())) {
                            dispatcher.dispatch(jsonNode);
                            return; // stop dès qu'on trouve
                        }
                    } catch (Exception e) {
                        // Ignorer les lignes malformées
                    }
                }
            }

            System.err.println("Aucun event 'Commander' trouvé dans les journaux récents.");

        } catch (Exception e) {
            System.err.println("Erreur lors de l'extraction du nom du commandant : " + e.getMessage());
        }
    }


    /**
     * Vérifie si un fichier journal appartient au commandant identifié
     */
    private boolean isJournalFromCommander(File journalFile) {
        if (getCommanderFid() == null ||getCommanderFid().isEmpty()) {
            return false;
        }

        try {
            List<String> lines = Files.readAllLines(journalFile.toPath());
            for (String line : lines) {
                try {
                    JsonNode jsonNode = objectMapper.readTree(line);
                    if ("Commander".equals(jsonNode.get("event").asText())) {
                        String fileFID = jsonNode.get("FID").asText();
                        return getCommanderFid().equals(fileFID);
                    }
                } catch (Exception e) {
                    // Ignorer les lignes malformées
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la vérification du commandant pour " + journalFile.getName() + ": " + e.getMessage());
        }

        return false; // Par défaut, ne pas inclure le fichier
    }

    /**
     * Récupère le nom du commandant
     */
    public String getCommanderName() {
        return commanderStatus.getCommanderName();
    }
    public String getCommanderFid() {
        return commanderStatus.getFID();
    }

    /**
     * Récupère les fichiers Journal des 7 derniers jours
     */
    private List<File> getJournalFilesFromLastWeek() throws IOException {
        List<File> journalFiles = new ArrayList<>();
        String journalPath = preferencesService.getJournalFolder();
        System.out.println("Journal path : " + journalPath);
        Path journalDir = Paths.get(journalPath);

        if (!Files.exists(journalDir)) {
            System.err.println("Dossier Journal introuvable: " + journalPath);
            return journalFiles;
        }

        int journalDays = preferencesService.getJournalDays();
        LocalDate oneWeekAgo = LocalDate.now().minusDays(journalDays);

        try (Stream<Path> paths = Files.list(journalDir)) {
            paths.filter(path -> {
                        String filename = path.getFileName().toString();
                        return filename.startsWith(JOURNAL_PREFIX) && filename.endsWith(".log");
                    })
                    .filter(path -> isFileFromLastWeek(path, oneWeekAgo))
                    .sorted((p1, p2) -> p2.getFileName().toString().compareTo(p1.getFileName().toString()))
                    .forEach(path -> journalFiles.add(path.toFile()));
        }
        return journalFiles;
    }

    /**
     * Vérifie si un fichier Journal est de la semaine dernière
     */
    private boolean isFileFromLastWeek(Path filePath, LocalDate oneWeekAgo) {
        try {
            String fileName = filePath.getFileName().toString();
            // Format: Journal.2025-09-19T121254.01
            String datePart = fileName.substring(JOURNAL_PREFIX.length(), JOURNAL_PREFIX.length() + 10);
            LocalDate fileDate = LocalDate.parse(datePart);
            return !fileDate.isBefore(oneWeekAgo);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Parse tous les fichiers Journal et met à jour les missions existantes
     */
    private List<Mission> parseAllJournalFiles() {

        try {
            List<File> journalFiles = getJournalFilesFromLastWeek();

            journalFiles.sort(Comparator.comparing(File::getName));
            // Filtrer les fichiers pour ne garder que ceux du commandant identifié
            journalFiles = journalFiles.stream()
                    .filter(this::isJournalFromCommander)
                    .collect(Collectors.toList());

            System.out.println("Fichiers journal du commandant " + getCommanderName() + ": " + journalFiles.size());

            // Vérifier s'il n'y a aucun fichier de journal
            if (journalFiles.isEmpty()) {
                // Si aucun fichier de préférences n'existe, ouvrir la fenêtre de configuration
                if (!preferencesService.hasPreferencesFile()) {
                    openConfigDialog();
                } else {
                    showNoJournalsWarning();
                }
                return new ArrayList<>();
            }

            // Traiter les fichiers dans l'ordre chronologique (plus ancien en premier)
            // Les fichiers sont déjà triés par date décroissante, on les inverse
            dispatchAllEvents(journalFiles);

            if (!journalFiles.isEmpty()) {
                File latestJournal = journalFiles.get(journalFiles.size() - 1);
                JournalWatcherService.getInstance().start(preferencesService.getJournalFolder());
                JournalTailService.getInstance().start(latestJournal,false);

            }

        } catch (Exception e) {
            System.err.println("Erreur lors de la lecture des journaux: " + e.getMessage());
        }
        return new ArrayList<>(missionsRegistry.getGlobalMissionMap().values());

    }

    private void dispatchAllEvents(List<File> journalFiles) {
        DashboardContext.getInstance().setBatchLoading(true);
        for (File journalFile : journalFiles) {

            try {
                List<String> lines = Files.readAllLines(journalFile.toPath(), StandardCharsets.UTF_8);
                for (String line : lines) {
                    try {
                        JsonNode jsonNode = objectMapper.readTree(line);
                        dispatcher.dispatch(jsonNode);
                    } catch (Exception e) {
                        // Ignorer les lignes malformées
                    }
                }
            } catch (IOException e) {
                System.err.println("Erreur lors de la lecture du fichier " + journalFile.getName() + ": " + e.getMessage());
            }
        }
        DashboardContext.getInstance().setBatchLoading(false);
    }

    /**
     * Génère un résumé des missions
     */

    /**
     * Affiche un message d'avertissement quand aucun fichier de journal n'est trouvé
     */
    private void showNoJournalsWarning() {
        Platform.runLater(() -> {
            try {
                String warningMessage = localizationService.getString("warning.no_journals");
                // Afficher le popup au centre de l'écran
                javafx.stage.Window primaryWindow = javafx.stage.Stage.getWindows().stream()
                        .filter(window -> window.isShowing() && !window.getScene().getRoot().getChildrenUnmodifiable().isEmpty())
                        .findFirst()
                        .orElse(null);
                
                if (primaryWindow != null) {
                    // Afficher au centre (les coordonnées seront ignorées car on utilise CENTER)
                    popupManager.showWarningPopup(warningMessage, 0, 0, primaryWindow);
                }
            } catch (Exception e) {
                System.err.println("Erreur lors de l'affichage du message d'avertissement: " + e.getMessage());
            }
        });
    }

    /**
     * Ouvre automatiquement la fenêtre de configuration
     */
    private void openConfigDialog() {
        Platform.runLater(() -> {
            try {
                javafx.stage.Window primaryWindow = javafx.stage.Stage.getWindows().stream()
                        .filter(window -> window.isShowing() && !window.getScene().getRoot().getChildrenUnmodifiable().isEmpty())
                        .findFirst()
                        .orElse(null);
                
                if (primaryWindow instanceof javafx.stage.Stage stage) {
                    DialogComponent dialog = new DialogComponent("/fxml/combat/config-dialog.fxml", "/css/elite-theme.css", "Configuration", 900, 800);
                    dialog.init(stage);
                    dialog.showAndWait();
                }
            } catch (Exception e) {
                System.err.println("Erreur lors de l'ouverture de la fenêtre de configuration: " + e.getMessage());
            }
        });
    }

    /**
     * Lit et parse le fichier Cargo.json
     * @return L'objet Cargo parsé ou null si erreur
     */
    public Cargo readCargoFile() {
        try {
            String journalFolder = preferencesService.getJournalFolder();
            if (journalFolder == null || journalFolder.isEmpty()) {
                System.out.println("⚠️ Dossier journal non configuré");
                return null;
            }
            
            Path cargoFilePath = Paths.get(journalFolder, CARGO_FILE);
            if (!Files.exists(cargoFilePath)) {
                System.out.println("⚠️ Fichier Cargo.json non trouvé: " + cargoFilePath);
                return null;
            }
            
            String cargoContent = Files.readString(cargoFilePath);
            if (cargoContent == null || cargoContent.trim().isEmpty()) {
                System.out.println("⚠️ Fichier Cargo.json vide");
                return null;
            }
            
            JsonNode cargoNode = objectMapper.readTree(cargoContent);
            return parseCargoFromJson(cargoNode);
            
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la lecture du fichier Cargo.json: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Parse un JsonNode en objet Cargo
     */
    private Cargo parseCargoFromJson(JsonNode jsonNode) {
        Cargo cargo = new Cargo();
        
        // Fonctions utilitaires locales
        java.util.function.Function<String, String> getText = field -> 
            jsonNode.has(field) && !jsonNode.get(field).isNull() ? jsonNode.get(field).asText() : null;
        java.util.function.Function<String, Integer> getInt = field -> 
            jsonNode.has(field) && !jsonNode.get(field).isNull() ? jsonNode.get(field).asInt() : null;
        
        // Champs de base
        cargo.setTimestamp(getText.apply("timestamp"));
        cargo.setEvent(getText.apply("event"));
        cargo.setVessel(getText.apply("Vessel"));
        if (getInt.apply("Count") != null) cargo.setCount(getInt.apply("Count"));
        
        // Parse inventory si présent
        if (jsonNode.has("Inventory") && jsonNode.get("Inventory").isArray()) {
            JsonNode inventoryNode = jsonNode.get("Inventory");
            List<Cargo.Inventory> inventory = new ArrayList<>();
            
            for (JsonNode itemNode : inventoryNode) {
                Cargo.Inventory item = parseInventoryItem(itemNode);
                inventory.add(item);
            }
            
            cargo.setInventory(inventory);
        }
        
        return cargo;
    }
    
    /**
     * Parse un item d'inventaire depuis un JsonNode
     */
    private Cargo.Inventory parseInventoryItem(JsonNode itemNode) {
        Cargo.Inventory item = new Cargo.Inventory();
        
        // Fonctions utilitaires locales
        java.util.function.Function<String, String> getText = field -> 
            itemNode.has(field) && !itemNode.get(field).isNull() ? itemNode.get(field).asText() : null;
        java.util.function.Function<String, Integer> getInt = field -> 
            itemNode.has(field) && !itemNode.get(field).isNull() ? itemNode.get(field).asInt() : null;
        
        // Champs
        item.setName(getText.apply("Name"));
        item.setNameLocalised(getText.apply("Name_Localised"));
        if (getInt.apply("Count") != null) item.setCount(getInt.apply("Count"));
        if (getInt.apply("Stolen") != null) item.setStolen(getInt.apply("Stolen"));
        
        return item;
    }


}
