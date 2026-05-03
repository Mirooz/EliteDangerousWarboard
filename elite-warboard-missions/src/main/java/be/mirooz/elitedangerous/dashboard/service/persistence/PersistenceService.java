package be.mirooz.elitedangerous.dashboard.service.persistence;

import be.mirooz.elitedangerous.dashboard.model.registries.CommodityRegistry;
import be.mirooz.elitedangerous.dashboard.persistence.DashboardRegistryJsonPersistence;
import be.mirooz.elitedangerous.dashboard.persistence.JournalCursor;
import be.mirooz.elitedangerous.dashboard.persistence.JournalCursorStore;
import be.mirooz.elitedangerous.dashboard.persistence.RegistryStore;
import be.mirooz.elitedangerous.dashboard.service.webservice.eddn.EddnAppInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Orchestrateur unique de la persistance des registries + curseur de reprise des journaux.
 *
 * <p>Responsabilités :</p>
 * <ul>
 *   <li>enregistrer tous les {@link RegistryStore} du module et leur déléguer save/load/delete,</li>
 *   <li>mémoriser le {@link JournalCursor} courant (dernier event dispatché) et le persister,</li>
 *   <li>proposer un save debouncé ({@link #saveAllDebounced()}) pour ne pas réécrire à chaque
 *       event quand le stream de journaux tourne à plein régime,</li>
 *   <li>flusher un save en attente via un shutdown hook JVM,</li>
 *   <li>supprimer automatiquement tous les fichiers persistés en cas d'erreur de
 *       désérialisation → fallback vers un full replay des journaux.</li>
 * </ul>
 */
public class PersistenceService {

    private static final PersistenceService INSTANCE = new PersistenceService();

    /** Fenêtre de coalescing des saves debouncés. */
    private static final long DEBOUNCE_DELAY_MS = 2_000L;

    public static PersistenceService getInstance() {
        return INSTANCE;
    }

    private static final String DEFAULT_COMMANDER_SCOPE = "_unknown";

    private final Path persistenceRootDir;
    private Path commanderBaseDir;
    /** Version de l'app ayant écrit la persistance (ligne unique UTF-8). */
    private Path appPersistenceVersionFile;
    private String currentCommanderScope = DEFAULT_COMMANDER_SCOPE;
    private List<RegistryStore> stores = new ArrayList<>();
    private JournalCursorStore cursorStore;

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "PersistenceService-scheduler");
                t.setDaemon(true);
                return t;
            });
    private volatile ScheduledFuture<?> pendingSave;

    /** Si {@code true}, le hook JVM ne réécrit pas les snapshots (reset dossier commandant + sortie). */
    private volatile boolean skipJvmShutdownPersistenceFlush;

    private PersistenceService() {
        this.persistenceRootDir = Paths.get(System.getProperty("user.home"), ".elite-warboard");
        configureCommanderScope(DEFAULT_COMMANDER_SCOPE);

        Runtime.getRuntime().addShutdownHook(new Thread(this::flushOnShutdown,
                "PersistenceService-shutdown"));
    }

    /**
     * Active le scope de persistance pour le commandant courant (dossier par FID).
     *
     * @param commanderFid FID du commandant ; null/blank => scope par défaut.
     * @return {@code true} si le scope a changé.
     */
    public synchronized boolean useCommanderScope(String commanderFid) {
        String normalized = normalizeCommanderScope(commanderFid);
        if (normalized.equals(currentCommanderScope)) {
            return false;
        }
        cancelPendingSave();
        configureCommanderScope(normalized);
        System.out.println("[Persistence] Scope commandant activé: " + normalized
                + " (" + commanderBaseDir + ")");
        return true;
    }

    public synchronized Path getCurrentCommanderBaseDir() {
        return commanderBaseDir;
    }

    /**
     * Désactive la sauvegarde finale déclenchée par le shutdown hook JVM (évite de recréer des fichiers
     * après une suppression complète du dossier commandant).
     */
    public void setSkipJvmShutdownPersistenceFlush(boolean skip) {
        this.skipJvmShutdownPersistenceFlush = skip;
    }

    /**
     * Supprime récursivement le répertoire du commandant courant ({@code ~/.elite-warboard/commanders/&lt;fid&gt;/}),
     * y compris persistance, caches colonisation, etc.
     */
    public synchronized void deleteCurrentCommanderDirectoryRecursively() throws IOException {
        cancelPendingSave();
        Path root = commanderBaseDir;
        if (root == null || !Files.exists(root)) {
            return;
        }
        Path abs = root.toAbsolutePath().normalize();
        try (Stream<Path> walk = Files.walk(abs)) {
            List<Path> paths = walk.sorted(Comparator.reverseOrder()).toList();
            for (Path p : paths) {
                Files.deleteIfExists(p);
            }
        }
    }

    // -------- Resume API --------

    /**
     * Charge tous les registries + le curseur.
     *
     * @return {@code true} si un curseur valide a été chargé ET que tous les registries ont
     *         pu être restaurés ; {@code false} sinon (auquel cas l'appelant doit faire un
     *         full replay des journaux).
     */
    public boolean loadAll() {
        System.out.println("[Persistence] loadAll scope=" + currentCommanderScope
                + " dir=" + commanderBaseDir);
        String currentAppVersion = EddnAppInfo.version();
        String persistedAppVersion = readPersistedAppVersion();
        if (persistedAppVersion == null || !persistedAppVersion.equals(currentAppVersion)) {
            System.out.println("[Persistence] Version app (" + currentAppVersion
                    + ") ≠ persistance (" + (persistedAppVersion == null ? "absente" : persistedAppVersion)
                    + ") — purge + replay journal complet");
            deleteAll();
            writePersistedAppVersion(currentAppVersion);
            return false;
        }

        boolean cursorLoaded;
        try {
            cursorLoaded = cursorStore.loadIfExists();
        } catch (Exception e) {
            System.err.println("[Persistence] Echec du chargement du curseur, fallback full replay");
            e.printStackTrace();
            deleteAll();
            return false;
        }
        if (!cursorLoaded) {
            System.out.println("[Persistence] Pas de curseur — full replay des journaux");
            // Pas de curseur → même si des snapshots existaient, on ne sait pas jusqu'où
            // ils étaient à jour. On nettoie pour repartir propre sur full replay.
            deleteAllSnapshotsOnly();
            return false;
        }

        // Chargement indulgent : on continue même si un store échoue, on logue clairement
        // quel fichier pose problème plutôt que tout jeter silencieusement.
        boolean anyFailure = false;
        for (RegistryStore store : stores) {
            try {
                boolean loaded = store.loadIfExists();
                if (loaded) {
                    System.out.println("[Persistence] " + store.name() + " restauré");
                } else {
                    System.out.println("[Persistence] " + store.name() + " absent — première init");
                }
            } catch (Exception e) {
                anyFailure = true;
                System.err.println("[Persistence] Echec restauration " + store.name()
                        + " : " + e.getMessage());
                e.printStackTrace();
            }
        }

        if (anyFailure) {
            System.err.println("[Persistence] Au moins un store n'a pas pu être restauré → "
                    + "purge + fallback full replay pour éviter un état incohérent");
            deleteAll();
            return false;
        }

        CommodityRegistry.getInstance().ensureSeededFromClasspathIfEmpty();

        JournalCursor cursor = cursorStore.getCursor();
        if (cursor != null) {
            String lineInfo = cursor.getLastLineNumber() != null
                    ? " ligne " + cursor.getLastLineNumber()
                    : " (reprise par timestamp seul)";
            System.out.println("[Persistence] Cursor restauré : " + cursor.getLastJournalFile()
                    + " @ " + cursor.getLastTimestamp() + lineInfo);
        }
        return true;
    }

    /** Accès au curseur courant pour piloter la reprise dans {@code JournalService}. */
    public JournalCursor getCursor() {
        return cursorStore.getCursor();
    }

    /**
     * Met à jour le curseur en mémoire — appelé par le dispatcher à chaque event dispatché
     * hors batch.
     */
    public void updateCursor(String lastTimestamp, String lastJournalFile) {
        updateCursor(lastTimestamp, lastJournalFile, null);
    }

    /**
     * Met à jour le curseur en mémoire — appelé par le dispatcher à chaque event dispatché
     * hors batch. Si {@code lastLineNumber} est {@code null}, la ligne déjà stockée est conservée.
     */
    public void updateCursor(String lastTimestamp, String lastJournalFile, Integer lastLineNumber) {
        cursorStore.updateInMemory(lastTimestamp, lastJournalFile, lastLineNumber);
    }

    // -------- Save API --------

    /** Sauve tous les stores + le curseur, de façon synchrone. */
    public synchronized void saveAllNow() {
        cancelPendingSave();
        doSaveAll();
    }

    /**
     * Planifie un save global dans {@value #DEBOUNCE_DELAY_MS} ms. Les appels successifs
     * dans la fenêtre sont coalescés en une seule écriture disque.
     */
    public synchronized void saveAllDebounced() {
        cancelPendingSave();
        pendingSave = scheduler.schedule(this::doSaveAll, DEBOUNCE_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    /** Supprime tous les fichiers persistés (snapshots + curseur). */
    public synchronized void deleteAll() {
        cancelPendingSave();
        for (RegistryStore store : stores) {
            try {
                store.deleteIfExists();
            } catch (Exception e) {
                System.err.println("[Persistence] Suppression " + store.name() + " KO : " + e.getMessage());
            }
        }
        try {
            cursorStore.deleteIfExists();
        } catch (Exception e) {
            System.err.println("[Persistence] Suppression cursor KO : " + e.getMessage());
        }
        deletePersistedAppVersionFileQuietly();
    }

    // -------- Backward-compat (ancien PersistenceService) --------

    /** @deprecated utiliser {@link #loadAll()}. Conservé pour les anciens appels. */
    @Deprecated
    public void load() {
        loadAll();
    }

    /** @deprecated utiliser {@link #saveAllNow()} ou {@link #saveAllDebounced()}. */
    @Deprecated
    public void save() {
        saveAllNow();
    }

    /** @deprecated utiliser {@link #deleteAll()}. */
    @Deprecated
    public void delete() {
        deleteAll();
    }

    // -------- Internals --------

    /**
     * Tant qu’aucun événement journal {@code Commander} n’a fixé le FID, le scope reste {@value #DEFAULT_COMMANDER_SCOPE}
     * : on n’écrit pas sur disque (évite un dossier fantôme à la fermeture sans jeu / sans journal).
     */
    private boolean hasResolvedCommanderScope() {
        return !DEFAULT_COMMANDER_SCOPE.equals(currentCommanderScope);
    }

    private void doSaveAll() {
        if (!hasResolvedCommanderScope()) {
            System.out.println("[Persistence] saveAll ignoré — commandant non identifié (scope=" + currentCommanderScope + ")");
            return;
        }
        try {
            System.out.println("[Persistence] saveAll scope=" + currentCommanderScope
                    + " dir=" + commanderBaseDir);
            for (RegistryStore store : stores) {
                try {
                    store.save();
                } catch (Exception e) {
                    System.err.println("[Persistence] Save " + store.name() + " KO : " + e.getMessage());
                    e.printStackTrace();
                }
            }
            cursorStore.save();
            writePersistedAppVersion(EddnAppInfo.version());
        } catch (Exception e) {
            System.err.println("[Persistence] Save global KO");
            e.printStackTrace();
        }
    }

    private void deleteAllSnapshotsOnly() {
        for (RegistryStore store : stores) {
            try {
                store.deleteIfExists();
            } catch (Exception ignored) {
                // best effort
            }
        }
    }

    private void cancelPendingSave() {
        if (pendingSave != null) {
            pendingSave.cancel(false);
            pendingSave = null;
        }
    }

    private void flushOnShutdown() {
        try {
            if (skipJvmShutdownPersistenceFlush) {
                System.out.println("[Persistence] Flush shutdown ignoré (reset données commandant).");
                return;
            }
            // Politique "save only on shutdown" : même si le close handler de l'app a déjà
            // sauvé, on re-sauve ici par sécurité (pas d'opération coûteuse si l'état n'a
            // pas changé, et ça couvre les fermetures non propres type Ctrl+C / kill).
            cancelPendingSave();
            if (!hasResolvedCommanderScope()) {
                System.out.println("[Persistence] Sauvegarde finale (shutdown hook) ignorée — commandant non identifié.");
                return;
            }
            System.out.println("[Persistence] Sauvegarde finale (shutdown hook)...");
            doSaveAll();
        } catch (Exception e) {
            System.err.println("[Persistence] Flush shutdown KO : " + e.getMessage());
        } finally {
            scheduler.shutdownNow();
        }
    }

    private void configureCommanderScope(String commanderScope) {
        currentCommanderScope = normalizeCommanderScope(commanderScope);
        commanderBaseDir = persistenceRootDir.resolve("commanders").resolve(currentCommanderScope);
        appPersistenceVersionFile = commanderBaseDir.resolve("app-persistence-version.txt");
        cursorStore = new JournalCursorStore(commanderBaseDir.resolve("journal-cursor.json"));
        stores = new ArrayList<>(DashboardRegistryJsonPersistence.buildRegistryStores(commanderBaseDir));
    }

    private String readPersistedAppVersion() {
        if (appPersistenceVersionFile == null || !Files.exists(appPersistenceVersionFile)) {
            return null;
        }
        try {
            String raw = Files.readString(appPersistenceVersionFile, StandardCharsets.UTF_8).trim();
            return raw.isEmpty() ? null : raw;
        } catch (IOException e) {
            System.err.println("[Persistence] Lecture app-persistence-version.txt KO : " + e.getMessage());
            return null;
        }
    }

    private void writePersistedAppVersion(String version) {
        if (appPersistenceVersionFile == null || version == null || version.isBlank()) {
            return;
        }
        try {
            Path parent = appPersistenceVersionFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(appPersistenceVersionFile, version.strip(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[Persistence] Écriture app-persistence-version.txt KO : " + e.getMessage());
        }
    }

    private void deletePersistedAppVersionFileQuietly() {
        if (appPersistenceVersionFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(appPersistenceVersionFile);
        } catch (IOException e) {
            System.err.println("[Persistence] Suppression app-persistence-version.txt KO : " + e.getMessage());
        }
    }

    private static String normalizeCommanderScope(String commanderFid) {
        if (commanderFid == null || commanderFid.isBlank()) {
            return DEFAULT_COMMANDER_SCOPE;
        }
        String safe = commanderFid.strip().replaceAll("[^a-zA-Z0-9._-]", "_");
        return safe.isBlank() ? DEFAULT_COMMANDER_SCOPE : safe;
    }
}
