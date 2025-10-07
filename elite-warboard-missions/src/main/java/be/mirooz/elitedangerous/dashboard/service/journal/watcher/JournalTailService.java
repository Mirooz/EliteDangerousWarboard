package be.mirooz.elitedangerous.dashboard.service.journal.watcher;

import be.mirooz.elitedangerous.dashboard.handlers.dispatcher.JournalEventDispatcher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;

import java.io.File;

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
                try {
                    JsonNode jsonNode = objectMapper.readTree(line);
                    JournalEventDispatcher.getInstance().dispatch(jsonNode);
                } catch (Exception e) {
                    System.err.println("[Tailer] Ligne ignorée: " + e.getMessage());
                }
            }
        };

        // Stopper l'ancien tailer s'il existe
        stop();

        // Lire d'abord tout le contenu existant du fichier
        if (readNow)
            readExistingContent(journalFile);

        // Puis commencer à tracker les nouvelles lignes
        tailer = new Tailer(journalFile, listener, 1000, true);
        tailerThread = new Thread(tailer, "JournalTailThread");
        tailerThread.setDaemon(true); // ✅ ne bloque pas la fermeture
        tailerThread.start();
    }

    /**
     * Lit tout le contenu existant du fichier journal
     */
    private void readExistingContent(File journalFile) {
        try {
            if (journalFile.exists() && journalFile.length() > 0) {
                System.out.println("[Tailer] Reading existing content from: " + journalFile.getName());
                java.nio.file.Files.lines(journalFile.toPath())
                    .forEach(line -> {
                        try {
                            JsonNode jsonNode = objectMapper.readTree(line);
                            JournalEventDispatcher.getInstance().dispatch(jsonNode);
                        } catch (Exception e) {
                            // Ignorer les lignes malformées
                        }
                    });
                System.out.println("[Tailer] Finished reading existing content");
            }
        } catch (Exception e) {
            System.err.println("[Tailer] Error reading existing content: " + e.getMessage());
        }
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
