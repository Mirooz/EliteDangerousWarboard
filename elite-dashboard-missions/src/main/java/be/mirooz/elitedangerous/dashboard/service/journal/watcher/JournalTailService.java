package be.mirooz.elitedangerous.dashboard.service.journal.watcher;

import be.mirooz.elitedangerous.dashboard.handlers.dispatcher.JournalEventDispatcher;
import be.mirooz.elitedangerous.dashboard.ui.UIRefreshManager;
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
    private final UIRefreshManager uiRefreshManager = UIRefreshManager.getInstance();

    private JournalTailService() {}

    public void start(File journalFile) {
        JournalFileTracker.getInstance().setCurrentFile(journalFile);

        TailerListenerAdapter listener = new TailerListenerAdapter() {
            @Override
            public void handle(String line) {
                try {
                    JsonNode jsonNode = objectMapper.readTree(line);
                    JournalEventDispatcher.getInstance().dispatch(jsonNode);
                    javafx.application.Platform.runLater(uiRefreshManager::refresh);
                } catch (Exception e) {
                    System.err.println("[Tailer] Ligne ignorée: " + e.getMessage());
                }
            }
        };

        // Stopper l'ancien tailer s'il existe
        stop();

        tailer = new Tailer(journalFile, listener, 1000, true);
        tailerThread = new Thread(tailer, "JournalTailThread");
        tailerThread.setDaemon(true); // ✅ ne bloque pas la fermeture
        tailerThread.start();
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
