package be.mirooz.elitedangerous.dashboard.service.journal;

import be.mirooz.elitedangerous.dashboard.persistence.JournalCursor;
import be.mirooz.elitedangerous.dashboard.service.*;
import be.mirooz.elitedangerous.dashboard.service.journal.watcher.JournalFileTracker;
import be.mirooz.elitedangerous.dashboard.service.persistence.PersistenceService;
import be.mirooz.elitedangerous.dashboard.service.listeners.CargoEventNotificationService;
import be.mirooz.elitedangerous.dashboard.view.common.DialogComponent;
import be.mirooz.elitedangerous.dashboard.view.common.managers.PopupManager;
import be.mirooz.elitedangerous.dashboard.model.registries.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.model.commander.Mission;
import be.mirooz.elitedangerous.dashboard.model.events.Cargo;
import be.mirooz.elitedangerous.dashboard.model.registries.combat.MissionsRegistry;
import be.mirooz.elitedangerous.dashboard.handlers.dispatcher.JournalEventDispatcher;
import be.mirooz.elitedangerous.dashboard.service.webservice.AnalyticsService;
import be.mirooz.elitedangerous.dashboard.service.webservice.CapiApiService;
import be.mirooz.elitedangerous.dashboard.service.journal.watcher.JournalTailService;
import be.mirooz.elitedangerous.dashboard.service.journal.watcher.JournalWatcherService;
import be.mirooz.elitedangerous.dashboard.view.common.context.DashboardContext;
import be.mirooz.elitedangerous.dashboard.service.listeners.ColonisationNotificationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service pour lire et parser les fichiers Journal Elite Dangerous
 */
public class JournalService {

    private static final String JOURNAL_PREFIX = "Journal.";
    private static final String CARGO_FILE = "Cargo.json";
    private static final Duration SKIP_FLEET_CAPI_IF_JOURNAL_CARRIER_ACTIVITY_WITHIN = Duration.ofMinutes(20);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();
    private final PreferencesService preferencesService = PreferencesService.getInstance();
    private final LocalizationService localizationService = LocalizationService.getInstance();
    private final PopupManager popupManager = PopupManager.getInstance();

    private final MissionsRegistry missionsRegistry = MissionsRegistry.getInstance();
    private final JournalEventDispatcher dispatcher;


    private AnalyticsService analyticsService = AnalyticsService.getInstance();
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
            analyticsService.startSession(commanderName);

            // Utiliser la nouvelle méthode qui traite tous les fichiers et met à jour les missions
            List<Mission> missions = parseAllJournalFiles();

            // Données fleet CAPI : après tout le chargement journal (ordres, stocks journal, etc.)
            // Le fallback "load depuis disque" a disparu : parseAllJournalFiles() a déjà
            // appelé PersistenceService.loadAll() en amont si un snapshot valide existait.
            boolean forceFleetRefreshAfterCommanderSwitch =
                    AppLifecycleService.getInstance().consumeCommanderSwitchFleetRefreshFlag();
            boolean hasRecentCarrierActivity = CarrierTradeService.getInstance()
                    .hasRecentJournalCarrierActivity(SKIP_FLEET_CAPI_IF_JOURNAL_CARRIER_ACTIVITY_WITHIN);
            String fid = getCommanderFid();
            if ((forceFleetRefreshAfterCommanderSwitch || !hasRecentCarrierActivity)
                    && fid != null && !fid.isBlank()) {
                CompletableFuture.runAsync(() -> {
                    boolean profileOk = CapiApiService.getInstance().checkCapiAuthentication();
                    if (profileOk) {
                        CapiApiService.getInstance().fetchFleetCarrierData();
                    }
                });
            }

            return missions;
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
        if (getCommanderFid() == null || getCommanderFid().isEmpty()) {
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
     * Récupère tous les fichiers Journal disponibles dans le dossier configuré.
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

        try (Stream<Path> paths = Files.list(journalDir)) {
            paths.filter(path -> {
                        String filename = path.getFileName().toString();
                        return filename.startsWith(JOURNAL_PREFIX) && filename.endsWith(".log");
                    })
                    .sorted((p1, p2) -> p2.getFileName().toString().compareTo(p1.getFileName().toString()))
                    .forEach(path -> journalFiles.add(path.toFile()));
        }
        return journalFiles;
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

            // Tente une reprise incrémentale si un curseur + snapshots valides existent
            // ~/.elite-warboard/. Sinon on retombe sur le replay complet.
            PersistenceService.getInstance().useCommanderScope(getCommanderFid());
            boolean resumed = PersistenceService.getInstance().loadAll();
            if (resumed) {
                JournalCursor cursor = PersistenceService.getInstance().getCursor();
                String lastFile = cursor.getLastJournalFile();
                if (lastFile != null && !lastFile.isBlank()) {
                    // Les noms Journal.YYYY-MM-DDTHHMMSS.NN.log sont lexico-chrono.
                    journalFiles = journalFiles.stream()
                            .filter(f -> f.getName().compareTo(lastFile) >= 0)
                            .collect(Collectors.toList());
                }
                System.out.println("[JournalService] Resume depuis " + lastFile
                        + " @ " + cursor.getLastTimestamp()
                        + (cursor.getLastLineNumber() != null ? " ligne " + cursor.getLastLineNumber() : "")
                        + " (" + journalFiles.size() + " fichiers à scanner)");
                dispatchIncremental(journalFiles, cursor);
            } else {
                dispatchAllEvents(journalFiles);
            }

            if (!journalFiles.isEmpty()) {
                File latestJournal = journalFiles.get(journalFiles.size() - 1);
                JournalWatcherService.getInstance().start(preferencesService.getJournalFolder());
                JournalTailService.getInstance().start(latestJournal, false);

            }

        } catch (Exception e) {
            System.err.println("Erreur lors de la lecture des journaux: " + e.getMessage());
        } finally {
            // Fin du scan batch : débloquer l'UI, charger l’état colonisation persisté, puis
            // re-synchroniser Cargo.json → jsonShipCargo (et notifier les vues minage, etc.)
            // pour tous les cas (fichiers vides, erreur, ou replay complet).
            ColonisationService.getInstance().loadPersistedUiStateAfterJournalBatch();
            DashboardContext.getInstance().setBatchLoading(false);
            CargoEventNotificationService.getInstance().notifyCargoEvent();
            NavRouteService.getInstance().loadAndStoreNavRoute();
            ColonisationNotificationService.getInstance().notifyColonisationDataChanged();
            // Rafraîchissement UI global : {@link DashboardService#initActiveMissions()} (finally)
            // après tous les {@code onBatchEnd}, pour respecter l’ordre liaisons → contenus.
        }
        return missionsRegistry.snapshotMissions();

    }

    private void dispatchAllEvents(List<File> journalFiles) {
        dispatchBatch(journalFiles, null);
    }

    /**
     * Rejoue les lignes non encore appliquées selon le curseur : préférence au numéro de ligne
     * sur le fichier {@code resume.getLastJournalFile()} ; si {@code lastLineNumber} est absent
     * (curseur ancien), repli sur le timestamp strictement postérieur.
     */
    private void dispatchIncremental(List<File> journalFiles, JournalCursor resume) {
        dispatchBatch(journalFiles, resume);
    }

    private void dispatchBatch(List<File> journalFiles, JournalCursor resume) {
        DashboardContext.getInstance().setBatchLoading(true);
        String resumeFile = resume != null ? blankToNull(resume.getLastJournalFile()) : null;
        String resumeTs = resume != null ? blankToNull(resume.getLastTimestamp()) : null;
        Integer resumeLine = resume != null ? resume.getLastLineNumber() : null;

        for (File journalFile : journalFiles) {
            // On trace le fichier courant pour que le dispatcher renseigne correctement le
            // curseur avec le nom du journal en cours.
            JournalFileTracker.getInstance().setCurrentFile(journalFile);
            try {
                List<String> lines = Files.readAllLines(journalFile.toPath(), StandardCharsets.UTF_8);
                int physicalLineNo = 0;
                for (String line : lines) {
                    physicalLineNo++;
                    try {
                        if (line == null || line.isBlank()) {
                            continue;
                        }
                        JsonNode jsonNode = objectMapper.readTree(line);
                        if (shouldSkipBatchLine(journalFile, physicalLineNo, jsonNode, resumeFile, resumeTs, resumeLine)) {
                            continue;
                        }
                        dispatcher.dispatch(jsonNode, physicalLineNo);
                    } catch (Exception e) {
                        // Ignorer les lignes malformées
                    }
                }
            } catch (IOException e) {
                System.err.println("Erreur lors de la lecture du fichier " + journalFile.getName() + ": " + e.getMessage());
            }
        }
        // Pas de save ici : la politique est "save only on shutdown" (close handler de
        // EliteDashboardApp + shutdown hook JVM). Le curseur en mémoire a été mis à jour
        // par le dispatcher à chaque event ; il sera flushé à la fermeture de l'app.
        // Fin de batch, Cargo.json, colonisation : {@link #parseAllJournalFiles()} finally
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s;
    }

    /**
     * @return {@code true} si la ligne ne doit pas être dispatchée (déjà couverte par le curseur).
     */
    private static boolean shouldSkipBatchLine(
            File journalFile,
            int physicalLineNo,
            JsonNode jsonNode,
            String resumeFile,
            String resumeTs,
            Integer resumeLine) {
        if (resumeTs == null && resumeFile == null && resumeLine == null) {
            return false;
        }
        boolean sameFile = resumeFile != null
                && journalFile.getName().equalsIgnoreCase(resumeFile.trim());
        if (resumeLine != null && resumeFile != null) {
            if (sameFile) {
                return physicalLineNo <= resumeLine;
            }
            // Fichiers d’après le journal du curseur : tout le contenu est nouveau.
            return false;
        }
        // Curseur sans lastLineNumber : compatibilité — filtre par timestamp sur tous les fichiers.
        if (resumeTs != null) {
            JsonNode ts = jsonNode.get("timestamp");
            if (ts != null && !ts.isNull() && ts.asText().compareTo(resumeTs) <= 0) {
                return true;
            }
        }
        return false;
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
                    DialogComponent dialog = new DialogComponent("/fxml/combat/config-dialog.fxml", "/css/elite-theme.css", "Configuration", 960, 840);
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
     *
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

        // Champs (Name / name — certains outils écrivent en camelCase JSON différent)
        String nm = getText.apply("Name");
        if (nm == null || nm.isBlank()) {
            nm = getText.apply("name");
        }
        item.setName(nm);
        String loc = getText.apply("Name_Localised");
        if (loc == null || loc.isBlank()) {
            loc = getText.apply("name_localised");
        }
        item.setNameLocalised(loc);
        if (getInt.apply("Count") != null) item.setCount(getInt.apply("Count"));
        if (getInt.apply("Stolen") != null) item.setStolen(getInt.apply("Stolen"));

        return item;
    }


}
