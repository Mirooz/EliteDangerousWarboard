package be.mirooz.elitedangerous.dashboard.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.*;

/**
 * Service pour gérer les logs de l'application
 * Enregistre les logs dans un fichier dans le dossier des préférences
 */
public class LoggingService {
    private static LoggingService instance;
    private File logFile;
    private PrintStream originalOut;
    private PrintStream originalErr;
    private FileOutputStream logFileOutputStream;
    private TeePrintStream teeOut;
    private TeePrintStream teeErr;
    private FileHandler fileHandler;

    private LoggingService() {
        // Ne pas initialiser ici, on le fera dans initialize()
    }

    public static LoggingService getInstance() {
        if (instance == null) {
            instance = new LoggingService();
        }
        return instance;
    }

    /**
     * Initialise le service de logging
     * Doit être appelé au démarrage de l'application
     */
    public void initialize() {
        try {
            // Créer le dossier des préférences s'il n'existe pas
            Path preferencesDir = Paths.get(System.getProperty("user.home"), ".elite-wardboard");
            Files.createDirectories(preferencesDir);

            // Supprimer les anciens fichiers de log pour ne garder que le dernier
            File[] oldLogFiles = preferencesDir.toFile().listFiles((dir, name) -> 
                name.startsWith("elite-warboard_") && name.endsWith(".log")
            );
            if (oldLogFiles != null) {
                for (File oldLog : oldLogFiles) {
                    try {
                        Files.deleteIfExists(oldLog.toPath());
                    } catch (IOException e) {
                        // Ignorer les erreurs de suppression
                    }
                }
            }

            // Créer le fichier de log avec timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            logFile = new File(preferencesDir.toFile(), "elite-warboard_" + timestamp + ".log");

            // Désactiver les logs verbeux de JavaFX CSS parser et autres loggers internes
            Logger cssLogger = Logger.getLogger("javafx.css");
            if (cssLogger != null) {
                cssLogger.setLevel(Level.SEVERE);
                cssLogger.setUseParentHandlers(false);
            }
            
            // Désactiver les autres loggers verbeux de JavaFX
            String[] verboseLoggers = {
                "com.sun.javafx.css",
                "javafx.scene",
                "javafx.css.parser",
                "com.sun.javafx.scene"
            };
            
            for (String loggerName : verboseLoggers) {
                Logger logger = Logger.getLogger(loggerName);
                if (logger != null) {
                    logger.setLevel(Level.SEVERE);
                    logger.setUseParentHandlers(false);
                }
            }
            
            // Configurer le logger root pour ne capturer que les logs importants
            Logger rootLogger = Logger.getLogger("");
            rootLogger.setLevel(Level.WARNING); // Seulement WARNING et plus sévère

            // Rediriger System.out et System.err vers le fichier ET la console
            originalOut = System.out;
            originalErr = System.err;

            logFileOutputStream = new FileOutputStream(logFile, true);
            teeOut = new TeePrintStream(originalOut, logFileOutputStream);
            teeErr = new TeePrintStream(originalErr, logFileOutputStream);

            System.setOut(teeOut);
            System.setErr(teeErr);

            System.out.println("✅ Service de logging initialisé - Fichier: " + logFile.getAbsolutePath());
            System.out.println("📅 Démarrage de l'application: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        } catch (IOException e) {
            System.err.println("❌ Erreur lors de l'initialisation du service de logging: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Arrête le service de logging et restaure les flux originaux
     */
    public void shutdown() {
        try {
            System.out.println("📅 Arrêt de l'application: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            // Restaurer les flux originaux
            if (teeOut != null) {
                System.setOut(originalOut);
                teeOut.close();
            }
            if (teeErr != null) {
                System.setErr(originalErr);
                teeErr.close();
            }
            if (logFileOutputStream != null) {
                logFileOutputStream.close();
            }
            if (fileHandler != null) {
                fileHandler.close();
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de l'arrêt du service de logging: " + e.getMessage());
        }
    }

    /**
     * Classe pour rediriger les sorties vers la console ET le fichier
     */
    private static class TeePrintStream extends PrintStream {
        private final PrintStream console;
        private final FileOutputStream file;

        public TeePrintStream(PrintStream console, FileOutputStream file) {
            super(file, true);
            this.console = console;
            this.file = file;
        }

        @Override
        public void write(int b) {
            console.write(b);
            try {
                file.write(b);
            } catch (IOException e) {
                // Ignorer les erreurs d'écriture dans le fichier
            }
        }

        @Override
        public void write(byte[] buf, int off, int len) {
            console.write(buf, off, len);
            try {
                file.write(buf, off, len);
            } catch (IOException e) {
                // Ignorer les erreurs d'écriture dans le fichier
            }
        }

        @Override
        public void flush() {
            console.flush();
            try {
                file.flush();
            } catch (IOException e) {
                // Ignorer les erreurs de flush
            }
        }

        @Override
        public void close() {
            console.close();
            try {
                file.close();
            } catch (IOException e) {
                // Ignorer les erreurs de fermeture
            }
        }
    }

    /**
     * Retourne le chemin du fichier de log actuel
     */
    public File getLogFile() {
        return logFile;
    }
}

