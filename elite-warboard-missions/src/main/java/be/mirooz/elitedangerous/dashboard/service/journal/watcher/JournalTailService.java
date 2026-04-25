package be.mirooz.elitedangerous.dashboard.service.journal.watcher;

import be.mirooz.elitedangerous.dashboard.handlers.dispatcher.JournalEventDispatcher;
import be.mirooz.elitedangerous.dashboard.service.persistence.PersistenceService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.stream.Stream;

/**
 * Tailer du journal : lecture incrémentale + reprise. Les lignes plus anciennes que le
 * {@link be.mirooz.elitedangerous.dashboard.persistence.JournalCursor#lastTimestamp} déjà
 * enregistré sont ignorées (y compris si Apache {@link Tailer} repart du début après
 * troncature/rewrite du fichier) — plus de cache LRU de lignes.
 */
public class JournalTailService {

    private static final JournalTailService INSTANCE = new JournalTailService();
    public static JournalTailService getInstance() { return INSTANCE; }

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private Tailer tailer;
    private Thread tailerThread;

    private JournalTailService() {}

    public void start(File journalFile, boolean readNow) {
        stop();
        JournalFileTracker.getInstance().setCurrentFile(journalFile);

        if (readNow) {
            readExistingContent(journalFile);
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
        if (line == null || line.isBlank()) {
            return;
        }
        String minExclusive = normalizeTimestampBound(
                PersistenceService.getInstance().getCursor().getLastTimestamp());
        try {
            JsonNode node = objectMapper.readTree(line);
            if (!isEventStrictlyAfterMin(node, minExclusive)) {
                return;
            }
            JournalEventDispatcher.getInstance().dispatch(node);
        } catch (Exception e) {
            // ligne JSON invalide → ignorée
        }
    }

    /**
     * Lit le fichier en ne dispatchant que les events dont le {@code timestamp} est
     * strictement postérieur au seuil figé en début de lecture (état du curseur à l’ouverture,
     * ex. journal tout neuf via le watcher).
     */
    private void readExistingContent(File journalFile) {
        if (!journalFile.exists() || journalFile.length() == 0) {
            return;
        }

        String minExclusive = normalizeTimestampBound(
                PersistenceService.getInstance().getCursor().getLastTimestamp());
        String label = (minExclusive == null) ? "full" : "ts > " + minExclusive;
        System.out.println("[Tailer] Reading existing: " + journalFile.getName() + " (" + label + ")");

        try (Stream<String> lines = Files.lines(journalFile.toPath(), StandardCharsets.UTF_8)) {
            lines.forEach(line -> {
                if (line == null || line.isBlank()) {
                    return;
                }
                try {
                    JsonNode node = objectMapper.readTree(line);
                    if (!isEventStrictlyAfterMin(node, minExclusive)) {
                        return;
                    }
                    JournalEventDispatcher.getInstance().dispatch(node);
                } catch (Exception e) {
                    // ignorée
                }
            });
        } catch (Exception e) {
            System.err.println("[Tailer] Error reading existing content: " + e.getMessage());
        }
        System.out.println("[Tailer] Finished reading existing content");
    }

    /** {@code null} / vide → pas de filtre (rejouer toutes les lignes parsables). */
    private static String normalizeTimestampBound(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw;
    }

    /**
     * Même règle que {@link be.mirooz.elitedangerous.dashboard.service.journal.JournalService}
     * en reprise : ignorer les events &lt;= {@code minExclusiveTimestamp}.
     */
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

    public void stop() {
        if (tailer != null) {
            tailer.stop();
            tailer = null;
        }
        if (tailerThread != null) {
            tailerThread.interrupt();
            tailerThread = null;
        }
    }
}
