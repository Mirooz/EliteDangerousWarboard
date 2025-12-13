package be.mirooz.elitedangerous.dashboard.service.journal.watcher;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class JournalWatcherService implements Runnable {

    private static final JournalWatcherService INSTANCE = new JournalWatcherService();
    public static JournalWatcherService getInstance() {
        return INSTANCE;
    }

    private Thread watcherThread;
    private volatile boolean running = false;
    private WatchService watchService;
    private final Set<Path> tailedFiles = ConcurrentHashMap.newKeySet();

    private Path journalDir;
    private final JournalTailService tailService = JournalTailService.getInstance();

    private JournalWatcherService() {}

    public synchronized void start(String journalFolder) {
        if (running) {
            System.out.println("[Watcher] Already running, ignoring start()");
            return;
        }

        this.journalDir = Paths.get(journalFolder);
        this.running = true;

        watcherThread = new Thread(this, "JournalWatcherThread");
        watcherThread.setDaemon(true);
        watcherThread.start();
    }

    @Override
    public void run() {
        System.out.println("[Watcher] Thread started");

        try {
            watchService = FileSystems.getDefault().newWatchService();
            journalDir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

            while (running) {
                WatchKey key;

                try {
                    key = watchService.take(); // bloque proprement
                } catch (InterruptedException e) {
                    // arrêt volontaire
                    break;
                }

                for (WatchEvent<?> event : key.pollEvents()) {

                    if (event.kind() != StandardWatchEventKinds.ENTRY_CREATE) {
                        continue;
                    }

                    Path relativePath = (Path) event.context();
                    Path createdPath = journalDir.resolve(relativePath);
                    String filename = createdPath.getFileName().toString();

                    if (!filename.matches("^Journal\\..*\\.log$")) {
                        continue;
                    }

                    // Empêche de tail le même fichier plusieurs fois
                    if (!tailedFiles.add(createdPath)) {
                        continue;
                    }

                    System.out.println("[Watcher] New journal detected: " + filename);

                    try {
                        tailService.start(createdPath.toFile(), true);
                    } catch (Exception ex) {
                        System.err.println("[Watcher] Failed to start tail for " + filename);
                        ex.printStackTrace();
                    }
                }

                // IMPORTANT : reset obligatoire
                if (!key.reset()) {
                    System.err.println("[Watcher] WatchKey invalid, stopping watcher");
                    break;
                }
            }

        } catch (IOException e) {
            if (running) {
                System.err.println("[Watcher] IO error: " + e.getMessage());
                e.printStackTrace();
            }
        } finally {
            running = false;

            // 🔥 FERMETURE EXPLICITE DU WATCHSERVICE
            if (watchService != null) {
                try {
                    watchService.close();
                } catch (IOException ignored) {
                }
            }

            System.out.println("[Watcher] Thread stopped");
        }
    }

    public void stop() {
        running = false;
        if (watcherThread != null) {
            watcherThread.interrupt();
            watcherThread = null;
        }
    }
}
