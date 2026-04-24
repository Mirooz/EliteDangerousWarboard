package be.mirooz.elitedangerous.dashboard.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Store du curseur de reprise de lecture des journaux. Un fichier unique stockant le dernier
 * timestamp + nom de fichier journal ayant été entièrement dispatché.
 *
 * <p>Différent des autres {@link RegistryStore} : il détient son propre état (le curseur
 * courant) plutôt que de lire/écrire un singleton — d'où l'accès {@link #getCursor()} /
 * {@link #updateInMemory(String, String)}.</p>
 */
public class JournalCursorStore {

    private final Path file;
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);

    private JournalCursor cursor = new JournalCursor();

    public JournalCursorStore(Path file) {
        this.file = file;
    }

    /** Met à jour le curseur en mémoire — à appeler après chaque dispatch hors batch. */
    public synchronized void updateInMemory(String lastTimestamp, String lastJournalFile) {
        if (lastTimestamp == null || lastTimestamp.isBlank()) {
            return;
        }
        cursor.setLastTimestamp(lastTimestamp);
        if (lastJournalFile != null && !lastJournalFile.isBlank()) {
            cursor.setLastJournalFile(lastJournalFile);
        }
    }

    /** @return copie du curseur courant (ou {@code null} si jamais initialisé). */
    public synchronized JournalCursor getCursor() {
        if (cursor.getLastTimestamp() == null || cursor.getLastTimestamp().isBlank()) {
            return null;
        }
        return new JournalCursor(cursor.getLastTimestamp(), cursor.getLastJournalFile());
    }

    public synchronized void save() {
        if (cursor.getLastTimestamp() == null || cursor.getLastTimestamp().isBlank()) {
            return; // rien à persister
        }
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            mapper.writeValue(file.toFile(), cursor);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot save journal cursor to " + file, e);
        }
    }

    public synchronized boolean loadIfExists() {
        if (!Files.exists(file)) {
            return false;
        }
        try {
            JournalCursor loaded = mapper.readValue(file.toFile(), JournalCursor.class);
            if (loaded == null || loaded.getLastTimestamp() == null || loaded.getLastTimestamp().isBlank()) {
                return false;
            }
            this.cursor = loaded;
            return true;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load journal cursor from " + file, e);
        }
    }

    public synchronized void deleteIfExists() {
        try {
            Files.deleteIfExists(file);
            cursor = new JournalCursor();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot delete journal cursor file " + file, e);
        }
    }
}
