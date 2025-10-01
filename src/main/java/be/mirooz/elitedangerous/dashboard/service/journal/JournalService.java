package be.mirooz.elitedangerous.dashboard.service.journal;

import be.mirooz.elitedangerous.dashboard.handlers.events.CommanderHandler;
import be.mirooz.elitedangerous.dashboard.model.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.model.Mission;
import be.mirooz.elitedangerous.dashboard.model.MissionStatus;
import be.mirooz.elitedangerous.dashboard.model.MissionsList;
import be.mirooz.elitedangerous.dashboard.handlers.dispatcher.JournalEventDispatcher;
import be.mirooz.elitedangerous.dashboard.service.journal.watcher.JournalTailService;
import be.mirooz.elitedangerous.dashboard.service.journal.watcher.JournalWatcherService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service pour lire et parser les fichiers Journal Elite Dangerous
 */
public class JournalService {

    private static final String JOURNAL_PATH = System.getProperty("journal.folder");
    private static final String JOURNAL_PREFIX = "Journal.";
    private static final String SHIPYARD_FILE = "Shipyard.json";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();
    private String currentShip = null;

    private final Map<String, Mission> globalMissionList;
    private final JournalEventDispatcher dispatcher;


    private JournalService() {
        this.dispatcher = JournalEventDispatcher.getInstance();
        this.globalMissionList = MissionsList.getInstance().getGlobalMissionMap();
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
                    if (new CommanderHandler().getEventType().equals(jsonNode.get("event").asText())) {
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
                    if (new CommanderHandler().getEventType().equals(jsonNode.get("event").asText())) {
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
        return commanderStatus.getCommanderNameString();
    }
    public String getCommanderFid() {
        return commanderStatus.getFIDString();
    }

    /**
     * Récupère les fichiers Journal des 7 derniers jours
     */
    private List<File> getJournalFilesFromLastWeek() throws IOException {
        List<File> journalFiles = new ArrayList<>();
        System.out.println("Journal path : " + JOURNAL_PATH);
        Path journalDir = Paths.get(JOURNAL_PATH);

        if (!Files.exists(journalDir)) {
            System.err.println("Dossier Journal introuvable: " + JOURNAL_PATH);
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

            // Traiter les fichiers dans l'ordre chronologique (plus ancien en premier)
            // Les fichiers sont déjà triés par date décroissante, on les inverse
            Collections.reverse(journalFiles);
            for (File journalFile : journalFiles) {
                Map<String, Mission> fileMissionMap = new HashMap<>();

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

            if (!journalFiles.isEmpty()) {
                File latestJournal = journalFiles.get(journalFiles.size() - 1);

                // Lancer le tail sur le dernier fichier
                JournalTailService tailService = new JournalTailService();
                tailService.startTailing(latestJournal);

                // Lancer un watcher pour basculer si un nouveau fichier apparaît
                new Thread(new JournalWatcherService(JOURNAL_PATH, tailService)).start();
            }

        } catch (Exception e) {
            System.err.println("Erreur lors de la lecture des journaux: " + e.getMessage());
        }

        return new ArrayList<>(globalMissionList.values());
    }

    /**
     * Génère un résumé des missions
     */
    public String generateMissionSummary() {
        List<Mission> missions = getMissionsFromLastWeek();

        long activeCount = missions.stream().filter(m -> m.getStatus() == MissionStatus.ACTIVE).count();
        long completedCount = missions.stream().filter(m -> m.getStatus() == MissionStatus.COMPLETED).count();
        long failedCount = missions.stream().filter(m -> m.getStatus() == MissionStatus.FAILED).count();

        long totalReward = missions.stream()
                .filter(m -> m.getStatus() == MissionStatus.COMPLETED)
                .mapToLong(Mission::getReward)
                .sum();

        StringBuilder summary = new StringBuilder();
        summary.append("=== RÉSUMÉ DES MISSIONS (7 DERNIERS JOURS) ===\n");
        summary.append(String.format("Missions actives: %d\n", activeCount));
        summary.append(String.format("Missions complétées: %d\n", completedCount));
        summary.append(String.format("Missions abandonnées: %d\n", failedCount));
        summary.append(String.format("Total des récompenses: %,d Cr\n", totalReward));
        summary.append("\n=== MISSIONS ACTIVES ===\n");

        missions.stream()
                .filter(m -> m.getStatus() == MissionStatus.ACTIVE)
                .sorted((m1, m2) -> m1.getFaction().compareTo(m2.getFaction()))
                .forEach(mission -> {

                    String killsText;
                    if (mission.getStatus() == MissionStatus.COMPLETED) {
                        // Pour les missions complétées, afficher y/y
                        int targetCount = mission.getTargetCount();
                        killsText = String.format("%d/%d", targetCount, targetCount);
                    } else {
                        // Pour les missions actives, afficher x/y
                        int currentCount = mission.getCurrentCount();
                        int targetCount = mission.getTargetCount();
                        killsText = String.format("%d/%d", currentCount, targetCount);
                    }

                    // Informations temporelles
                    String timeInfo = "";
                    if (mission.getAcceptedTime() != null) {
                        timeInfo = " - Accepté: " + mission.getAcceptedTime().format(DateTimeFormatter.ofPattern("dd/MM HH:mm"));
                        if (mission.getExpiry() != null) {
                            long hoursRemaining = java.time.Duration.between(LocalDateTime.now(), mission.getExpiry()).toHours();
                            if (hoursRemaining > 0) {
                                timeInfo += " (Restant: " + hoursRemaining + "h)";
                            } else {
                                timeInfo += " (Expirée)";
                            }
                        }
                    }

                    summary.append(String.format("- %s (%s): %s kills - %,d Cr%s\n",
                            mission.getName(),
                            mission.getFaction(),
                            killsText,
                            mission.getReward(),
                            timeInfo));
                });

        return summary.toString();
    }
}
