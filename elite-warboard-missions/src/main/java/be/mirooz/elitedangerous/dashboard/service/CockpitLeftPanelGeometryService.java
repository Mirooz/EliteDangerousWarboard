package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.dashboard.overlay.panel.BlueLeftPanelPositionDetector;
import be.mirooz.elitedangerous.dashboard.overlay.panel.CockpitLeftPanelGeometry;
import be.mirooz.elitedangerous.dashboard.overlay.panel.PanelCorners;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.WatchService;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Quadrilatère cockpit en coordonnées normalisées : calcul OpenCV sur une image de calibration
 * (tracé bleu), rechargée quand un fichier est ajouté ou modifié dans le dossier utilisateur.
 * <p>
 * Dossier surveillé : {@code ~/.elite-warboard/calibration/} — fichiers dont le nom correspond à
 * {@code leftpanelposition*.(bmp|png|jpg|jpeg)} (insensible à la casse) ; le plus récemment modifié
 * est utilisé. Sinon repli sur la ressource embarquée {@value #BUNDLED_CALIBRATION_RESOURCE}.
 * <p>
 * En cas d’échec OpenCV ou de détection, repli sur les constantes {@link CockpitLeftPanelGeometry}.
 */
public final class CockpitLeftPanelGeometryService {

    private static final CockpitLeftPanelGeometryService INSTANCE = new CockpitLeftPanelGeometryService();

    public static final String BUNDLED_CALIBRATION_RESOURCE = "/images/overlay/leftpanelposition2.bmp";

    private static final Path CALIBRATION_DIR = Path.of(
            System.getProperty("user.home"), ".elite-warboard", "calibration");

    private static final Pattern CALIBRATION_FILENAME = Pattern.compile(
            "(?i)leftpanelposition.*\\.(bmp|png|jpe?g)$");

    private final Object reloadLock = new Object();
    private final Object debounceLock = new Object();
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean openCvReady = new AtomicBoolean(false);
    private final AtomicBoolean openCvFailed = new AtomicBoolean(false);

    private volatile NormQuad current = NormQuad.fromStaticFallback();
    private WatchService watchService;
    private Thread watchThread;
    private ScheduledExecutorService debouncer;
    private ScheduledFuture<?> pendingReload;

    private CockpitLeftPanelGeometryService() {
    }

    public static CockpitLeftPanelGeometryService getInstance() {
        return INSTANCE;
    }

    /**
     * Démarre la surveillance du dossier calibration et effectue un premier chargement (hors FX).
     */
    public void start() {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        try {
            Files.createDirectories(CALIBRATION_DIR);
        } catch (IOException e) {
            System.err.println("[CockpitGeom] Impossible de créer " + CALIBRATION_DIR + ": " + e.getMessage());
        }
        debouncer = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cockpit-geom-debounce");
            t.setDaemon(true);
            return t;
        });
        scheduleReload("startup");
        try {
            watchService = CALIBRATION_DIR.getFileSystem().newWatchService();
            CALIBRATION_DIR.register(
                    watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY);
            watchThread = new Thread(this::watchLoop, "cockpit-calibration-watch");
            watchThread.setDaemon(true);
            watchThread.start();
        } catch (IOException e) {
            System.err.println("[CockpitGeom] WatchService désactivé: " + e.getMessage());
        }
    }

    /**
     * Arrêt propre (fermeture app).
     */
    public void stop() {
        if (!started.get()) {
            return;
        }
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException ignored) {
            }
        }
        if (watchThread != null) {
            watchThread.interrupt();
        }
        synchronized (debounceLock) {
            if (pendingReload != null) {
                pendingReload.cancel(false);
            }
        }
        if (debouncer != null) {
            debouncer.shutdown();
        }
    }

    /**
     * Coins du panneau sur l’écran jeu, à partir des ratios courants et des {@code visualBounds}.
     */
    public PanelCorners panelCornersOnScreen(Rectangle2D visual) {
        NormQuad q = current;
        double bx = visual.getMinX();
        double by = visual.getMinY();
        double w = visual.getWidth();
        double h = visual.getHeight();
        return new PanelCorners(
                new Point2D(bx + w * q.tlX, by + h * q.tlY),
                new Point2D(bx + w * q.trX, by + h * q.trY),
                new Point2D(bx + w * q.brX, by + h * q.brY),
                new Point2D(bx + w * q.blX, by + h * q.blY)
        );
    }

    /** Pour tests / outils : force un recalcul immédiat (bloquant). */
    public void reloadNowForTests() {
        reloadFromDisk("manual");
    }

    private void watchLoop() {
        if (watchService == null) {
            return;
        }
        try {
            while (!Thread.currentThread().isInterrupted()) {
                WatchKey key = watchService.take();
                for (WatchEvent<?> ev : key.pollEvents()) {
                    Object ctx = ev.context();
                    if (ctx instanceof Path fn) {
                        String name = fn.toString();
                        if (CALIBRATION_FILENAME.matcher(name).matches()) {
                            scheduleReload("file:" + ev.kind() + ":" + name);
                        }
                    }
                }
                if (!key.reset()) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ClosedWatchServiceException ignored) {
        } catch (Exception e) {
            System.err.println("[CockpitGeom] watch: " + e.getMessage());
        }
    }

    private void scheduleReload(String reason) {
        if (debouncer == null) {
            return;
        }
        synchronized (debounceLock) {
            if (pendingReload != null) {
                pendingReload.cancel(false);
            }
            pendingReload = debouncer.schedule(() -> reloadFromDisk(reason), 450, TimeUnit.MILLISECONDS);
        }
    }

    private void reloadFromDisk(String reason) {
        synchronized (reloadLock) {
            if (!ensureOpenCvLoaded()) {
                current = NormQuad.fromStaticFallback();
                return;
            }
            Mat bgr = loadCalibrationMat();
            if (bgr == null || bgr.empty()) {
                System.err.println("[CockpitGeom] Aucune image calibration, repli constants (" + reason + ").");
                current = NormQuad.fromStaticFallback();
                return;
            }
            try {
                int iw = bgr.cols();
                int ih = bgr.rows();
                Optional<PanelCorners> pix = BlueLeftPanelPositionDetector.detectPixelCorners(bgr);
                if (pix.isEmpty()) {
                    System.err.println("[CockpitGeom] Détection quad bleu échouée (" + reason + "), repli constants.");
                    current = NormQuad.fromStaticFallback();
                    return;
                }
                PanelCorners p = pix.get();
                current = NormQuad.fromPixels(p, iw, ih);
                System.out.println("[CockpitGeom] OK " + iw + "×" + ih + " (" + reason + ")");
            } finally {
                bgr.release();
            }
        }
    }

    private Mat loadCalibrationMat() {
        Optional<Path> user = findNewestUserCalibrationFile();
        if (user.isPresent()) {
            Mat m = Imgcodecs.imread(user.get().toString(), Imgcodecs.IMREAD_COLOR);
            if (m != null && !m.empty()) {
                return m;
            }
            if (m != null) {
                m.release();
            }
        }
        return loadBundledMat();
    }

    private static Mat loadBundledMat() {
        try (InputStream in = CockpitLeftPanelGeometryService.class.getResourceAsStream(BUNDLED_CALIBRATION_RESOURCE)) {
            if (in == null) {
                return new Mat();
            }
            byte[] data = in.readAllBytes();
            if (data.length == 0) {
                return new Mat();
            }
            Mat buf = new Mat(1, data.length, org.opencv.core.CvType.CV_8UC1);
            buf.put(0, 0, data);
            Mat decoded = Imgcodecs.imdecode(buf, Imgcodecs.IMREAD_COLOR);
            buf.release();
            return decoded;
        } catch (IOException e) {
            System.err.println("[CockpitGeom] Lecture ressource: " + e.getMessage());
            return new Mat();
        }
    }

    private static Optional<Path> findNewestUserCalibrationFile() {
        if (!Files.isDirectory(CALIBRATION_DIR)) {
            return Optional.empty();
        }
        try (Stream<Path> stream = Files.list(CALIBRATION_DIR)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> CALIBRATION_FILENAME.matcher(p.getFileName().toString()).matches())
                    .max(Comparator.comparingLong(CockpitLeftPanelGeometryService::lastModifiedMillis));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private static long lastModifiedMillis(Path p) {
        try {
            return Files.getLastModifiedTime(p).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    private boolean ensureOpenCvLoaded() {
        if (openCvFailed.get()) {
            return false;
        }
        if (openCvReady.get()) {
            return true;
        }
        synchronized (CockpitLeftPanelGeometryService.class) {
            if (openCvReady.get()) {
                return true;
            }
            if (openCvFailed.get()) {
                return false;
            }
            try {
                OpenCV.loadLocally();
                openCvReady.set(true);
                return true;
            } catch (Throwable t) {
                System.err.println("[CockpitGeom] OpenCV indisponible: " + t.getMessage());
                openCvFailed.set(true);
                return false;
            }
        }
    }

    private record NormQuad(double tlX, double tlY, double trX, double trY, double brX, double brY, double blX,
                            double blY) {
        static NormQuad fromStaticFallback() {
            return new NormQuad(
                    CockpitLeftPanelGeometry.NORM_TOP_LEFT_X,
                    CockpitLeftPanelGeometry.NORM_TOP_LEFT_Y,
                    CockpitLeftPanelGeometry.NORM_TOP_RIGHT_X,
                    CockpitLeftPanelGeometry.NORM_TOP_RIGHT_Y,
                    CockpitLeftPanelGeometry.NORM_BOTTOM_RIGHT_X,
                    CockpitLeftPanelGeometry.NORM_BOTTOM_RIGHT_Y,
                    CockpitLeftPanelGeometry.NORM_BOTTOM_LEFT_X,
                    CockpitLeftPanelGeometry.NORM_BOTTOM_LEFT_Y
            );
        }

        static NormQuad fromPixels(PanelCorners p, int imageW, int imageH) {
            double w = Math.max(1, imageW);
            double h = Math.max(1, imageH);
            return new NormQuad(
                    p.topLeft().getX() / w,
                    p.topLeft().getY() / h,
                    p.topRight().getX() / w,
                    p.topRight().getY() / h,
                    p.bottomRight().getX() / w,
                    p.bottomRight().getY() / h,
                    p.bottomLeft().getX() / w,
                    p.bottomLeft().getY() / h
            );
        }
    }
}
