package be.mirooz.elitedangerous.dashboard.service.journal.watcher;

import be.mirooz.elitedangerous.dashboard.handlers.dispatcher.JournalEventDispatcher;
import be.mirooz.elitedangerous.dashboard.ui.UIRefreshManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;

import java.io.File;

public class JournalTailService {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private Tailer tailer;
    private final UIRefreshManager uiRefreshManager = UIRefreshManager.getInstance();

    public void startTailing(File journalFile) {
        JournalFileTracker.getInstance().setCurrentFile(journalFile);

        TailerListenerAdapter listener = new TailerListenerAdapter() {
            @Override
            public void handle(String line) {
                try {
                    JsonNode jsonNode = objectMapper.readTree(line);
                    JournalEventDispatcher.getInstance().dispatch(jsonNode);
                    javafx.application.Platform.runLater(() -> {
                        uiRefreshManager.refresh();
                    });
                } catch (Exception e) {
                    System.err.println("[Tailer] Ligne ignorée: " + e.getMessage());
                }
            }
        };

        // Stopper l’ancien tailer si besoin
        if (tailer != null) {
            tailer.stop();
        }

        // Crée un nouveau tailer qui lit en continu
        tailer = Tailer.create(journalFile, listener, 1000, true);
    }
}
