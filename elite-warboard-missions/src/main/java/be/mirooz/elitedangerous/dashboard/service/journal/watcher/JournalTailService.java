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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

public class JournalTailService {

    private static final JournalTailService INSTANCE = new JournalTailService();
    public static JournalTailService getInstance() { return INSTANCE; }

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Déduplication des lignes : Apache Commons {@link Tailer} considère qu'un fichier dont la
     * taille diminue a été "rotaté" et reprend sa lecture depuis le début. Or un simple Ctrl+S
     * dans un éditeur réécrit le journal (truncate + rewrite) → on re-reçoit toutes les lignes.
     * On garde donc en mémoire un cache borné des dernières lignes dispatchées ; si une ligne
     * déjà vue réapparaît, on la skip. Borne ajustée pour couvrir un fichier journal typique
     * (&lt; 10 000 events / session). Le cache est reset à chaque {@link #start(File, boolean)}.
     */
    private static final int DEDUP_CACHE_SIZE = 50_000;
    private final LinkedHashMap<String, Boolean> recentLines =
            new LinkedHashMap<>(DEDUP_CACHE_SIZE, 0.75f, false) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                    return size() > DEDUP_CACHE_SIZE;
                }
            };

    private Tailer tailer;
    private Thread tailerThread;

    private JournalTailService() {}

    public void start(File journalFile, boolean readNow) {
        stop();
        JournalFileTracker.getInstance().setCurrentFile(journalFile);
        resetDedupCache();

        // Lire le contenu existant : soit pour dispatch + seed du cache (readNow=true), soit
        // uniquement pour seed du cache (readNow=false : contenu déjà dispatché ailleurs — typique
        // du flow JournalService.parseAllJournalFiles). Dans les deux cas, le cache finit peuplé
        // avec toutes les lignes déjà vues, de sorte qu'un Tailer qui reprend à zéro après un
        // Ctrl+S/rotation voit ces lignes comme des duplicatas et ne les redispatche pas.
        if (readNow) {
            readExistingContent(journalFile, true);
        } else {
            readExistingContent(journalFile, false);
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
     * Traite une ligne du journal (lecture + dispatch), après filtrage des duplicatas.
     */
    private void processLine(String line) {
        if (line == null || line.isBlank()) return;
        if (isDuplicate(line)) return;
        try {
            JsonNode jsonNode = objectMapper.readTree(line);
            JournalEventDispatcher.getInstance().dispatch(jsonNode);
        } catch (Exception e) {
            // ligne JSON invalide → ignorée
            // System.err.println("[Tailer] Ligne ignorée: " + e.getMessage());
        }
    }

    /**
     * Lit tout le contenu existant du fichier journal.
     *
     * @param dispatch si {@code true}, chaque ligne est dispatchée via {@link JournalEventDispatcher}
     *                 en plus d'être enregistrée dans le cache de dédup. Si {@code false}, seules
     *                 les entrées de cache sont créées — utile quand le contenu a déjà été dispatché
     *                 en amont (cf. flow batch).
     */
    private void readExistingContent(File journalFile, boolean dispatch) {
        if (!journalFile.exists() || journalFile.length() == 0) return;

        System.out.println("[Tailer] " + (dispatch ? "Reading" : "Seeding dedup cache from")
                + ": " + journalFile.getName());
        try (Stream<String> lines = Files.lines(journalFile.toPath(), StandardCharsets.UTF_8)) {
            if (dispatch) {
                lines.forEach(this::processLine);
            } else {
                lines.forEach(this::markAsSeen);
            }
        } catch (Exception e) {
            System.err.println("[Tailer] Error reading existing content: " + e.getMessage());
        }
        System.out.println("[Tailer] Finished " + (dispatch ? "reading" : "seeding")
                + " existing content");
    }

    private synchronized boolean isDuplicate(String line) {
        return recentLines.put(line, Boolean.TRUE) != null;
    }

    private synchronized void markAsSeen(String line) {
        if (line == null || line.isBlank()) return;
        recentLines.put(line, Boolean.TRUE);
    }

    private synchronized void resetDedupCache() {
        recentLines.clear();
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
