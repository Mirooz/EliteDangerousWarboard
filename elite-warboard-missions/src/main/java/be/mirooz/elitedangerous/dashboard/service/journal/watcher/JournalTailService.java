package be.mirooz.elitedangerous.dashboard.service.journal.watcher;

import be.mirooz.elitedangerous.dashboard.handlers.dispatcher.JournalEventDispatcher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.stream.Stream;

public class JournalTailService {

    private static final JournalTailService INSTANCE = new JournalTailService();
    public static JournalTailService getInstance() { return INSTANCE; }

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private Tailer tailer;
    private Thread tailerThread;

    private JournalTailService() {}

    public void start(File journalFile, boolean readNow) {
        JournalFileTracker.getInstance().setCurrentFile(journalFile);

        TailerListenerAdapter listener = new TailerListenerAdapter() {
            @Override
            public void handle(String line) {
                processLine(line);
            }
        };

        // Stopper l'ancien tailer s'il existe
        stop();

        // Lire d'abord tout le contenu existant du fichier
        if (readNow) {
            readExistingContent(journalFile);
        }

        // Puis commencer à tracker les nouvelles lignes
        tailer = new Tailer(journalFile, StandardCharsets.UTF_8, listener, 500, true, false, IOUtils.DEFAULT_BUFFER_SIZE);
        tailerThread = new Thread(tailer, "JournalTailThread");
        tailerThread.setDaemon(true);
        tailerThread.start();
    }

    /**
     * Traite une ligne du journal (lecture + dispatch)
     */
    private void processLine(String line) {
        if (line == null || line.isBlank()) return;
        try {
            JsonNode jsonNode = objectMapper.readTree(line);
            JournalEventDispatcher.getInstance().dispatch(jsonNode);
        } catch (Exception e) {
            // ligne JSON invalide → ignorée
            // System.err.println("[Tailer] Ligne ignorée: " + e.getMessage());
        }
    }

    /**
     * Lit tout le contenu existant du fichier journal avant de démarrer le tailer
     */
    private void readExistingContent(File journalFile) {
        if (!journalFile.exists() || journalFile.length() == 0) return;

        System.out.println("[Tailer] Reading existing content from: " + journalFile.getName());
        try (Stream<String> lines = Files.lines(journalFile.toPath(), StandardCharsets.UTF_8)) {
            lines.forEach(this::processLine);
        } catch (Exception e) {
            System.err.println("[Tailer] Error reading existing content: " + e.getMessage());
        }
        System.out.println("[Tailer] Finished reading existing content");
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
