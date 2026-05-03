package be.mirooz.elitedangerous.dashboard.service.journal.watcher;

import be.mirooz.elitedangerous.dashboard.handlers.dispatcher.JournalEventDispatcher;
import be.mirooz.elitedangerous.dashboard.persistence.JournalCursor;
import be.mirooz.elitedangerous.dashboard.service.persistence.PersistenceService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.stream.Stream;

/**
 * Tailer du journal : lecture incrémentale + reprise.
 * <p>La reprise sur le fichier courant utilise le numéro de ligne physique (1-based) du curseur
 * lorsqu’il est disponible ; sinon repli sur le timestamp comme pour les anciens curseurs.
 * Sur un autre fichier {@code Journal.YYYY-MM-DDTHHmmss.N.log} que celui du curseur, tout le flux est accepté (les
 * journaux ne sont pas réécrits en arrière). {@code Fileheader} est toujours accepté.</p>
 */
public class JournalTailService {

    private static final JournalTailService INSTANCE = new JournalTailService();
    public static JournalTailService getInstance() { return INSTANCE; }

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private Tailer tailer;
    private Thread tailerThread;

    /** Prochaine ligne physique (1-based) attendue dans le fichier courant. */
    private int nextDispatchLineNumber = 1;

    private JournalTailService() {}

    public void start(File journalFile, boolean readNow) {
        stop();
        nextDispatchLineNumber = 1;
        JournalFileTracker.getInstance().setCurrentFile(journalFile);

        if (readNow) {
            readExistingContent(journalFile);
        } else {
            nextDispatchLineNumber = countPhysicalLines(journalFile) + 1;
        }

        TailerListenerAdapter listener = new TailerListenerAdapter() {
            @Override
            public void handle(String line) {
                processLine(line);
            }
        };
        tailer = new Tailer(journalFile, StandardCharsets.UTF_8, listener, 500, true, false, IOUtils.DEFAULT_BUFFER_SIZE);
        tailerThread = new Thread(tailer, "JournalTailThread");
        tailerThread.setDaemon(true);
        tailerThread.start();
    }

    /**
     * Traite une ligne issue du Tailer (curseur pris en compte à chaque ligne, après
     * chaque {@code dispatch} le seuil avance).
     */
    private synchronized void processLine(String line) {
        if (line == null) {
            return;
        }
        int physicalLineNo = nextDispatchLineNumber++;
        if (line.isBlank()) {
            return;
        }
        File journalFile = JournalFileTracker.getInstance().getCurrentFile();
        JournalCursor cursorSnap = PersistenceService.getInstance().getCursor();
        try {
            JsonNode node = objectMapper.readTree(line);
            if (!shouldDispatchTailLine(journalFile, cursorSnap, physicalLineNo, node)) {
                return;
            }
            JournalEventDispatcher.getInstance().dispatch(node, physicalLineNo);
        } catch (Exception e) {
            // ligne JSON invalide → ignorée
        }
    }

    /**
     * Lit le fichier en ne dispatchant que les events au-delà du curseur (ligne ou timestamp).
     */
    private void readExistingContent(File journalFile) {
        if (!journalFile.exists() || journalFile.length() == 0) {
            return;
        }

        JournalCursor cursorSnap = PersistenceService.getInstance().getCursor();
        String label = describeResumeLabel(journalFile, cursorSnap);
        System.out.println("[Tailer] Reading existing: " + journalFile.getName() + " (" + label + ")");

        final int[] lastPhysicalLine = {0};
        try (Stream<String> lines = Files.lines(journalFile.toPath(), StandardCharsets.UTF_8)) {
            lines.forEach(line -> {
                lastPhysicalLine[0]++;
                int physicalLineNo = lastPhysicalLine[0];
                if (line == null || line.isBlank()) {
                    return;
                }
                try {
                    JsonNode node = objectMapper.readTree(line);
                    if (!shouldDispatchTailLine(journalFile, cursorSnap, physicalLineNo, node)) {
                        return;
                    }
                    JournalEventDispatcher.getInstance().dispatch(node, physicalLineNo);
                } catch (Exception e) {
                    // ignorée
                }
            });
        } catch (Exception e) {
            System.err.println("[Tailer] Error reading existing content: " + e.getMessage());
        }
        nextDispatchLineNumber = lastPhysicalLine[0] + 1;
        System.out.println("[Tailer] Finished reading existing content");
    }

    private static String describeResumeLabel(File journalFile, JournalCursor cursorSnap) {
        if (cursorSnap == null) {
            return "full (pas de curseur)";
        }
        String cursorFile = cursorSnap.getLastJournalFile();
        if (cursorFile == null || cursorFile.isBlank() || journalFile == null
                || !journalFile.getName().equalsIgnoreCase(cursorFile.trim())) {
            return "full (nouveau fichier ou fichier curseur différent)";
        }
        if (cursorSnap.getLastLineNumber() != null) {
            return "ligne > " + cursorSnap.getLastLineNumber();
        }
        String ts = cursorSnap.getLastTimestamp();
        return (ts == null || ts.isBlank()) ? "full" : "ts > " + ts;
    }

    private static int countPhysicalLines(File journalFile) {
        if (journalFile == null || !journalFile.exists() || journalFile.length() == 0) {
            return 0;
        }
        try (Stream<String> s = Files.lines(journalFile.toPath(), StandardCharsets.UTF_8)) {
            long n = s.count();
            if (n > Integer.MAX_VALUE - 16) {
                return Integer.MAX_VALUE - 16;
            }
            return (int) n;
        } catch (IOException e) {
            System.err.println("[Tailer] countPhysicalLines: " + e.getMessage());
            return 0;
        }
    }

    /** {@code null} / vide → pas de filtre temporel (legacy). */
    private static String normalizeTimestampBound(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw;
    }

    private static boolean isEventStrictlyAfterMin(JsonNode node, String minExclusiveTimestamp) {
        if (minExclusiveTimestamp == null) {
            return true;
        }
        JsonNode ts = node.get("timestamp");
        if (ts == null || ts.isNull()) {
            return true;
        }
        return ts.asText().compareTo(minExclusiveTimestamp) > 0;
    }

    private static boolean shouldDispatchTailLine(
            File journalFile, JournalCursor cursorSnap, int physicalLineNo, JsonNode node) {
        if (isFileheaderEvent(node)) {
            return true;
        }
        if (cursorSnap == null) {
            return true;
        }
        String cursorFile = cursorSnap.getLastJournalFile();
        if (journalFile != null && cursorFile != null && !cursorFile.isBlank()
                && journalFile.getName().equalsIgnoreCase(cursorFile.trim())) {
            Integer lastLine = cursorSnap.getLastLineNumber();
            if (lastLine != null) {
                return physicalLineNo > lastLine;
            }
            String minTs = normalizeTimestampBound(cursorSnap.getLastTimestamp());
            return isEventStrictlyAfterMin(node, minTs);
        }
        return true;
    }

    private static boolean isFileheaderEvent(JsonNode node) {
        JsonNode ev = node.get("event");
        return ev != null && "Fileheader".equals(ev.asText());
    }

    public void stop() {
        if (tailer != null) {
            tailer.stop();
            tailer = null;
        }
        if (tailerThread != null) {
            tailerThread.interrupt();
            tailerThread = null;
        }
        nextDispatchLineNumber = 1;
    }
}
