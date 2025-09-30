package be.mirooz.elitedangerous.dashboard.handlers.files;

import java.io.File;

public class JournalFileTracker {
    private static final JournalFileTracker INSTANCE = new JournalFileTracker();

    private File currentFile;
    private long lastKnownPosition = 0; // offset ou index de ligne

    private JournalFileTracker() {}

    public static JournalFileTracker getInstance() {
        return INSTANCE;
    }

    public synchronized void setCurrentFile(File file) {
        this.currentFile = file;
        this.lastKnownPosition = 0; // reset car nouveau fichier
        System.out.println("Now tracking: " + file.getName());
    }

    public synchronized File getCurrentFile() {
        return currentFile;
    }

    public synchronized long getLastKnownPosition() {
        return lastKnownPosition;
    }

    public synchronized void updatePosition(long newPosition) {
        this.lastKnownPosition = newPosition;
    }
}
