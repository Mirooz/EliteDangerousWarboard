package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.backend.analytics.AnalyticsClient;
import be.mirooz.elitedangerous.dashboard.model.registries.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.view.common.context.DashboardContext;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.logging.*;
import java.util.stream.Stream;

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
     * Dernier {@code elite-warboard_*.log} sous {@code ~/.elite-warboard} : envoi best-effort au backend
     * avec le fichier, puis {@link #initialize()} pourra supprimer les anciens logs.
     */
    public void reportSessionLogError() {
        if (!PreferencesService.getInstance().isSendErrorLogsEnabled()) {
            return;
        }

        Path dir = Paths.get(System.getProperty("user.home"), ".elite-warboard");

        if (!Files.isDirectory(dir)) {
            return;
        }

        Path previousLog;

        try (Stream<Path> stream = Files.list(dir)) {
            previousLog = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String n = p.getFileName().toString();
                        return n.startsWith("elite-warboard_") && n.endsWith(".log");
                    })
                    .max(Comparator.comparing(p -> p.getFileName().toString()))
                    .orElse(null);

        } catch (IOException e) {
            return;
        }

        if (previousLog == null) {
            return;
        }

        final byte[] content;

        try {
            content = Files.readAllBytes(previousLog);
        } catch (IOException e) {
            return;
        }

        final String logFileName = previousLog.getFileName().toString();
        final byte[] payload = content;
        if (!containsError(payload)) {
            return;
        }
        Thread t = new Thread(() -> {
            try {
                while (DashboardContext.getInstance().isBatchLoading()) {
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            CommanderStatus cs = CommanderStatus.getInstance();
            String commanderName = blankToNull(cs.getCommanderName());
            String fid = blankToNull(cs.getFID());
            AnalyticsClient.getInstance().postClientErrorReport(
                    commanderName,
                    fid,
                    payload,
                    logFileName
            );

        }, "previous-log-error-report");

        t.setDaemon(true);
        t.start();
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
    private boolean containsError(byte[] content) {
        String log = new String(content, StandardCharsets.UTF_8).toLowerCase();
        return log.contains("exception");
    }
    /**
     * Initialise le service de logging
     * Doit être appelé au démarrage de l'application
     */
    public void initialize() {
        try {
            // Créer le dossier des préférences s'il n'existe pas
            Path preferencesDir = Paths.get(System.getProperty("user.home"), ".elite-warboard");
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
                //cssLogger.setLevel(Level.SEVERE);
                //cssLogger.setUseParentHandlers(false);
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
                   // logger.setLevel(Level.SEVERE);
                    //logger.setUseParentHandlers(false);
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

