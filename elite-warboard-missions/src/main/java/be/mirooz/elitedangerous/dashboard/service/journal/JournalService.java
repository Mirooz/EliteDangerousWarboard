package be.mirooz.elitedangerous.dashboard.service.journal;

import be.mirooz.elitedangerous.dashboard.controller.ui.component.DialogComponent;
import be.mirooz.elitedangerous.dashboard.controller.ui.manager.PopupManager;
import be.mirooz.elitedangerous.dashboard.handlers.events.journalevents.CommanderHandler;
import be.mirooz.elitedangerous.dashboard.model.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.model.Mission;
import be.mirooz.elitedangerous.dashboard.model.registries.MissionsRegistry;
import be.mirooz.elitedangerous.dashboard.handlers.dispatcher.JournalEventDispatcher;
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
    private static final String SHIPYARD_FILE = "Shipyard.json";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();
    private final String currentShip = null;
    private final PreferencesService preferencesService = PreferencesService.getInstance();
    private final LocalizationService localizationService = LocalizationService.getInstance();
    private final PopupManager popupManager = PopupManager.getInstance();

    private final MissionsRegistry missionsRegistry = MissionsRegistry.getInstance();
    private final JournalEventDispatcher dispatcher;


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
            // Le premier fichier est le plus récent (trié par date décroissante)
            File latestJournal = journalFiles.get(0);
            List<String> lines = Files.readAllLines(latestJournal.toPath());
            for (String line : lines) {
                try {
                    JsonNode jsonNode = objectMapper.readTree(line);
                    if ("Commander".equals(jsonNode.get("event").asText())) {
                        dispatcher.dispatch(jsonNode);
                        return;
                    }
                } catch (Exception e) {
                    // Ignorer les lignes malformées
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de l'extraction du nom du commandant: " + e.getMessage());
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

        LocalDate oneWeekAgo = LocalDate.now().minusDays(7);

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
            Collections.reverse(journalFiles);
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
                List<String> lines = Files.readAllLines(journalFile.toPath());

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
                    DialogComponent dialog = new DialogComponent("/fxml/config-dialog.fxml", "/css/elite-theme.css", "Configuration", 550, 450);
                    dialog.init(stage);
                    dialog.showAndWait();
                }
            } catch (Exception e) {
                System.err.println("Erreur lors de l'ouverture de la fenêtre de configuration: " + e.getMessage());
            }
        });
    }

}
