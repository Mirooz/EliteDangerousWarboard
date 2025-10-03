package be.mirooz.elitedangerous.dashboard.service.journal.watcher;

import java.io.IOException;
import java.nio.file.*;

public class JournalWatcherService implements Runnable {

    private static final JournalWatcherService INSTANCE = new JournalWatcherService();
    public static JournalWatcherService getInstance() {
        return INSTANCE;
    }

    private Thread watcherThread;
    private volatile boolean running = false;

    private Path journalDir;
    private final JournalTailService tailService = JournalTailService.getInstance();

    private JournalWatcherService() {}

    public void start(String journalFolder) {
        // Si déjà en cours, on arrête avant de redémarrer
        stop();

        this.journalDir = Paths.get(journalFolder);
        this.running = true;

        watcherThread = new Thread(this, "JournalWatcherThread");
        watcherThread.setDaemon(true); // ✅ ne bloque pas la fermeture de la JVM
        watcherThread.start();

        System.out.println("[Watcher] Service started on folder: " + journalDir);
    }

    @Override
    public void run() {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            journalDir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

            while (running && !Thread.currentThread().isInterrupted()) {
                WatchKey key;
                try {
                    key = watchService.take();
                } catch (InterruptedException e) {
                    break;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                        Path createdPath = journalDir.resolve((Path) event.context());
                        String filename = createdPath.getFileName().toString();

                        if (filename.matches("^Journal\\..*\\.log$")) {
                            System.out.println("[Watcher] New journal detected: " + filename);
                            tailService.start(createdPath.toFile());
                        }
                    }
                }

                if (!key.reset()) break;
            }

        } catch (IOException e) {
            if (running) { // si ce n'est pas juste un arrêt volontaire
                System.err.println("[Watcher] Error: " + e.getMessage());
            }
        } finally {
            System.out.println("[Watcher] Stopped.");
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
