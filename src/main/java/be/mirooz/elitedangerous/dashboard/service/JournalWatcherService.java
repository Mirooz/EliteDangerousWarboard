package be.mirooz.elitedangerous.dashboard.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

public class JournalWatcherService implements Runnable {
    private final Path journalDir;
    private final JournalTailService tailService;

    public JournalWatcherService(String journalFolder, JournalTailService tailService) {
        this.journalDir = Paths.get(journalFolder);
        this.tailService = tailService;
    }

    @Override
    public void run() {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            journalDir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

            System.out.println("[Watcher] Watching directory: " + journalDir);

            while (true) {
                WatchKey key = watchService.take();

                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                        Path createdPath = journalDir.resolve((Path) event.context());
                        String filename = createdPath.getFileName().toString();

                        if (filename.matches("^Journal\\..*\\.log$")) {
                            System.out.println("[Watcher] New journal detected: " + filename);
                            tailService.startTailing(createdPath.toFile());
                        }

                    }
                }

                if (!key.reset()) break;
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("[Watcher] Error: " + e.getMessage());
        }
    }
}
