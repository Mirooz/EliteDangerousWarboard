package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.dashboard.service.webservice.AnalyticsService;
import be.mirooz.elitedangerous.dashboard.view.exploration.SystemVisualViewComponent;
import be.mirooz.elitedangerous.dashboard.window.StageVisualBounds;
import be.mirooz.elitedangerous.dashboard.window.WindowFramePreferences;
import be.mirooz.elitedangerous.dashboard.window.win32.WindowsBringStageToFrontNoActivate;
import be.mirooz.elitedangerous.dashboard.window.win32.WindowsUndecoratedVrFrameCompat;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import javafx.util.Duration;
import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service pour masquer/afficher le dashboard en VR (bind clavier ou HOTAS).
 */
public class WindowToggleService {
    private static WindowToggleService instance;
    private PreferencesService preferencesService;
    private Stage mainStage;
    private ComboBox<String> comboBox;
    private StackPane rootPane;
    private TabPane tabPane;
    private Tab missionsTab;
    private Tab miningTab;
    private Tab explorationTab;
    private Tab colonisationTab;
    private boolean hidden = false;
    private double savedWidth = 1200;
    private double savedHeight = 800;
    private double savedX = 0;
    private double savedY = 0;
    /** Minima applicatifs du stage avant hidden (réappliqués à la restauration). */
    private double savedMinWidth = 1;
    private double savedMinHeight = 1;
    private static final Duration RESTORE_MAXIMIZE_DELAY = Duration.millis(10);
    /** Incrémenté à chaque hide / restauration : annule un {@code setMaximized(true)} différé obsolète. */
    private volatile int vrLayoutEpoch = 0;
    /** {@code true} si la fenêtre était maximisée avant hidden (restauré avec {@code setMaximized(true)}). */
    private boolean savedWasMaximized = false;
    /** Anti-rebond : répétition OS sur {@code KEY_PRESSED} + double événement Global/JavaFX. */
    private volatile long lastWindowToggleNanoTime = 0L;
    private static final long WINDOW_TOGGLE_DEBOUNCE_NS = 120_000_000L;
    /** HOTAS : ne pas enfiler de {@code Platform.runLater} plus souvent (sinon file FX + RAM si l’analogique « tremble »). */
    private static final long HOTAS_SCHEDULE_DEBOUNCE_NS = 350_000_000L;
    private volatile long lastHotasWindowScheduleNs = 0L;
    private static final float HOTAS_MATCH_EPS = 0.01f;
    /** Ignore le bruit analogique entre deux polls (sinon front « entrée bande » en rafale). */
    private static final float HOTAS_POLL_UNCHANGED_EPS = 0.008f;
    private Point lastMousePos = null;
    /** Poll HOTAS : une tâche planifiée (pas de {@code while} actif) pour limiter CPU et arrêt propre. */
    private ScheduledExecutorService hotasScheduler;
    private volatile HotasPollSession hotasPollSession;
    private NativeKeyListener keyboardListener = null;
    /** Filtre scène : installé une fois sur la scène du stage (pas d’échange de scène en mode VR). */
    private boolean sceneVrKeyFilterRegistered;
    private boolean isPaused = false; // Pour désactiver temporairement le toggle
    private Paint savedSceneFill = null; // Pour sauvegarder la couleur originale de la scène
    private Map<javafx.scene.Node, Double> savedNodeOpacities = new HashMap<>(); // Pour sauvegarder les opacités des nœuds
    private Map<javafx.scene.Node, String> savedNodeStyles = new HashMap<>(); // Pour sauvegarder les styles des nœuds
    private String savedRootPaneStyle = null; // Pour sauvegarder le style original du rootPane
    private String currentPanel = null; // Panel actuellement actif

    private WindowToggleService() {
        this.preferencesService = PreferencesService.getInstance();
    }

    public static WindowToggleService getInstance() {
        if (instance == null) {
            instance = new WindowToggleService();
        }
        return instance;
    }

    /**
     * {@code true} tant que le bind VR a réduit le stage à 1×1 : ne pas persister cette géométrie dans les préférences.
     */
    public boolean isVrWindowGeometryHidden() {
        return hidden;
    }

    /**
     * Initialise le service avec le stage et les composants nécessaires
     */
    public void initialize(Stage stage, ComboBox<String> comboBox, StackPane rootPane) {
        this.mainStage = stage;
        this.comboBox = comboBox;
        this.rootPane = rootPane;

        // Sauvegarder les dimensions initiales
        this.savedWidth = stage.getWidth();
        this.savedHeight = stage.getHeight();
        this.savedX = stage.getX();
        this.savedY = stage.getY();
    }

    /** Mode VR activé dans les préférences (masquer/afficher le dashboard). */
    private boolean isVrModeEnabled() {
        return preferencesService.isWindowToggleEnabled();
    }

    private void registerSceneVrKeyFilterOnCurrentSceneIfNeeded() {
        if (!isVrModeEnabled() || mainStage == null) {
            return;
        }
        Scene scene = mainStage.getScene();
        if (scene == null || sceneVrKeyFilterRegistered) {
            return;
        }
        scene.addEventFilter(KeyEvent.KEY_RELEASED, this::handleVrKeyReleasedFromScene);
        sceneVrKeyFilterRegistered = true;
    }

    /** À appeler depuis {@link #start()} lorsque le mode VR est actif (scène parfois encore nulle). */
    private void attachSceneVrKeyFilterForVrMode() {
        Platform.runLater(this::registerSceneVrKeyFilterOnCurrentSceneIfNeeded);
    }

    private void detachSceneVrKeyFilter() {
        Runnable detach = () -> {
            if (mainStage != null && mainStage.getScene() != null && sceneVrKeyFilterRegistered) {
                mainStage.getScene().removeEventFilter(KeyEvent.KEY_RELEASED, this::handleVrKeyReleasedFromScene);
                sceneVrKeyFilterRegistered = false;
            }
        };
        if (Platform.isFxApplicationThread()) {
            detach.run();
        } else {
            Platform.runLater(detach);
        }
    }

    /**
     * Rattache un {@link Stage} flottant (overlay) au stage principal : sous Windows, la fenêtre
     * n’apparaît plus comme une « tâche » distincte (aperçu barre des tâches, Alt+Tab).
     * À appeler avant {@link Stage#show()}.
     */
    public void bindOverlayOwner(Stage overlayStage) {
        if (overlayStage == null || mainStage == null) {
            return;
        }
        overlayStage.initOwner(mainStage);
    }

    /**
     * Stage principal après {@link #initialize(Stage, ComboBox, StackPane)} (sinon {@code null}).
     */
    public Stage getMainStage() {
        return mainStage;
    }

    /**
     * Initialise le TabPane pour le changement d'onglet
     */
    public void initializeTabPane(TabPane tabPane, Tab missionsTab, Tab miningTab, Tab explorationTab, Tab colonisationTab) {
        this.tabPane = tabPane;
        this.missionsTab = missionsTab;
        this.miningTab = miningTab;
        this.explorationTab = explorationTab;
        this.colonisationTab = colonisationTab;


        // Ajouter un listener pour tracker les changements d'onglet
        tabPane.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Tab>() {
            @Override
            public void changed(ObservableValue<? extends Tab> observable, Tab oldTab, Tab newTab) {
                if (AnalyticsService.getInstance() == null) {
                    return;
                }
                
                // Arrêter le tracking du panel précédent
                if (oldTab != null && currentPanel != null) {
                    AnalyticsService.getInstance().endPanelTime(currentPanel);
                }

                // Démarrer le tracking du nouveau panel
                if (newTab == missionsTab) {
                    currentPanel = "Missions";
                } else if (newTab == miningTab) {
                    currentPanel = "Mining";
                } else if (newTab == explorationTab) {
                    currentPanel = "Exploration";
                } else if (newTab == colonisationTab) {
                    currentPanel = "Colonisation";
                } else {
                    currentPanel = null;
                }

                if (currentPanel != null) {
                    AnalyticsService.getInstance().startPanelTime(currentPanel);
                }
            }
        });

        // Initialiser le panel actuel
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab == missionsTab) {
            currentPanel = "Missions";
        } else if (selectedTab == miningTab) {
            currentPanel = "Mining";
        } else if (selectedTab == explorationTab) {
            currentPanel = "Exploration";
        } else if (selectedTab == colonisationTab) {
            currentPanel = "Colonisation";
        }

        if (AnalyticsService.getInstance() != null && currentPanel != null) {
            AnalyticsService.getInstance().startPanelTime(currentPanel);
        }
    }

    /**
     * Démarre les listeners si le toggle est activé dans les préférences
     */
    public void start() {
        if (!isVrModeEnabled()) {
            return;
        }

        startGlobalKeyboardListener();
        startHotasListener();
        attachSceneVrKeyFilterForVrMode();
        System.out.println("✅ Service de bind démarré (clavier + HOTAS)");
    }

    /**
     * Arrête les listeners
     */
    public void stop() {
        detachSceneVrKeyFilter();
        // Retirer le listener clavier si présent
        if (keyboardListener != null) {
            try {
                GlobalScreen.removeNativeKeyListener(keyboardListener);
            } catch (Exception ignored) {
            }
            keyboardListener = null;
        }

        // Ne pas dé-enregistrer le hook car d'autres composants peuvent l'utiliser
        // (comme ConfigDialogController pour la capture de touche)

        stopHotasPolling();
    }

    private void stopHotasPolling() {
        hotasPollSession = null;
        ScheduledExecutorService ex = hotasScheduler;
        hotasScheduler = null;
        if (ex != null) {
            ex.shutdownNow();
            try {
                ex.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Redémarre le service pour appliquer les nouvelles configurations
     */
    public void restart() {
        stop();
        start();
    }

    /**
     * Met en pause le service (désactive le toggle temporairement)
     */
    public void pause() {
        isPaused = true;
    }

    /**
     * Reprend le service (réactive le toggle)
     */
    public void resume() {
        isPaused = false;
    }

    /**
     * Listener clavier global (GlobalScreen - fonctionne quand l'app n'a pas le focus)
     */
    private void startGlobalKeyboardListener() {
        try {
            int windowToggleKeyCode = preferencesService.getWindowToggleKeyboardKey();

            if (windowToggleKeyCode <= 0) {
                System.out.println("⚠️ Pas de bind clavier configuré pour le toggle fenêtre");
                return;
            }

            Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
            logger.setLevel(Level.OFF);

            String nativeDir = System.getenv("LOCALAPPDATA") + "\\EliteWarboard\\native";
            new File(nativeDir).mkdirs();

            // Dire à JNativeHook d'utiliser ce dossier
            System.setProperty("jnativehook.lib.path", nativeDir);
            // Enregistrer le hook seulement s'il n'est pas déjà enregistré
            if (!GlobalScreen.isNativeHookRegistered()) {
                try {
                    GlobalScreen.registerNativeHook();
                    System.out.println("Native hook OK, DLL dans : " + nativeDir);
                } catch (Exception e) {
                    System.err.println("Échec du chargement natif : " + e.getMessage());
                }
            }

            keyboardListener = new NativeKeyListener() {
                @Override
                public void nativeKeyPressed(NativeKeyEvent e) {
                    // Rien : KEY_PRESSED + autorepeat saturait Platform.runLater (RAM / gel).
                    // Toggle fenêtre + onglets : uniquement sur nativeKeyReleased.
                }

                @Override
                public void nativeKeyReleased(NativeKeyEvent e) {
                    if (!isVrModeEnabled()) {
                        return;
                    }
                    int keyCode = e.getKeyCode();
                    if (keyCode == -1) {
                        return;
                    }
                    if (windowToggleKeyCode > 0
                            && keyCode == windowToggleKeyCode
                            && preferencesService.isWindowToggleEnabled()) {
                        WindowToggleService.this.runToggleWindowOnFxThread();
                    }
                }
            };

            GlobalScreen.addNativeKeyListener(keyboardListener);

            System.out.println("🎧 Hook clavier global actif (toggle fenêtre: " + windowToggleKeyCode + ").");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * VR depuis la scène : tout sur {@code KEY_RELEASED} (évite autorepeat KEY_PRESSED → file FX infinie).
     * Cohérent avec le hook global JNativeHook ({@code nativeKeyReleased}).
     */
    private void handleVrKeyReleasedFromScene(KeyEvent event) {
        if (!isVrModeEnabled()) {
            return;
        }
        if (isPaused) {
            return;
        }
        int eventKeyCode = convertJavaFXKeyCodeToNative(event.getCode());
        if (eventKeyCode == -1) {
            return;
        }
        if (preferencesService.isWindowToggleEnabled()) {
            int windowToggleKeyCode = preferencesService.getWindowToggleKeyboardKey();
            if (windowToggleKeyCode > 0 && eventKeyCode == windowToggleKeyCode) {
                event.consume();
                toggleWindowAndOpenCombo();
            }
        }
    }

    /**
     * Désactive la navigation au clavier pour empêcher la sélection des éléments
     */
    private void disableKeyboardNavigation(KeyEvent event) {
        // Ne pas bloquer si l'événement est déjà consommé (par exemple par handleVrKeyReleasedFromScene)
        if (event.isConsumed()) {
            return;
        }

        // Ne pas bloquer si le service est en pause (fenêtre de config ouverte)
        if (isPaused) {
            return;
        }

        KeyCode keyCode = event.getCode();
        int eventKeyCode = convertJavaFXKeyCodeToNative(keyCode);

        int windowToggleKeyCode = preferencesService.getWindowToggleKeyboardKey();

        if (eventKeyCode == windowToggleKeyCode) {
            return;
        }

        // Bloquer toutes les touches de navigation au clavier
        if (keyCode == KeyCode.TAB ||
                keyCode == KeyCode.UP ||
                keyCode == KeyCode.DOWN ||
                keyCode == KeyCode.LEFT ||
                keyCode == KeyCode.RIGHT ||
                keyCode == KeyCode.HOME ||
                keyCode == KeyCode.END ||
                keyCode == KeyCode.PAGE_UP ||
                keyCode == KeyCode.PAGE_DOWN ||
                keyCode == KeyCode.ENTER ||
                keyCode == KeyCode.SPACE) {
            event.consume(); // Consommer l'événement pour empêcher la navigation
        }
    }

    /**
     * Convertit KeyCode JavaFX en code NativeKeyEvent
     */
    private int convertJavaFXKeyCodeToNative(KeyCode keyCode) {
        switch (keyCode) {
            case SPACE:
                return NativeKeyEvent.VC_SPACE;
            case ENTER:
                return NativeKeyEvent.VC_ENTER;
            case ESCAPE:
                return NativeKeyEvent.VC_ESCAPE;
            case TAB:
                return NativeKeyEvent.VC_TAB;
            case BACK_SPACE:
                return NativeKeyEvent.VC_BACKSPACE;
            case DELETE:
                return NativeKeyEvent.VC_DELETE;
            case UP:
                return NativeKeyEvent.VC_UP;
            case DOWN:
                return NativeKeyEvent.VC_DOWN;
            case LEFT:
                return NativeKeyEvent.VC_LEFT;
            case RIGHT:
                return NativeKeyEvent.VC_RIGHT;
            case F1:
                return NativeKeyEvent.VC_F1;
            case F2:
                return NativeKeyEvent.VC_F2;
            case F3:
                return NativeKeyEvent.VC_F3;
            case F4:
                return NativeKeyEvent.VC_F4;
            case F5:
                return NativeKeyEvent.VC_F5;
            case F6:
                return NativeKeyEvent.VC_F6;
            case F7:
                return NativeKeyEvent.VC_F7;
            case F8:
                return NativeKeyEvent.VC_F8;
            case F9:
                return NativeKeyEvent.VC_F9;
            case F10:
                return NativeKeyEvent.VC_F10;
            case F11:
                return NativeKeyEvent.VC_F11;
            case F12:
                return NativeKeyEvent.VC_F12;
            // Mapping explicite pour toutes les lettres
            case A: return NativeKeyEvent.VC_A;
            case B: return NativeKeyEvent.VC_B;
            case C: return NativeKeyEvent.VC_C;
            case D: return NativeKeyEvent.VC_D;
            case E: return NativeKeyEvent.VC_E;
            case F: return NativeKeyEvent.VC_F;
            case G: return NativeKeyEvent.VC_G;
            case H: return NativeKeyEvent.VC_H;
            case I: return NativeKeyEvent.VC_I;
            case J: return NativeKeyEvent.VC_J;
            case K: return NativeKeyEvent.VC_K;
            case L: return NativeKeyEvent.VC_L;
            case M: return NativeKeyEvent.VC_M;
            case N: return NativeKeyEvent.VC_N;
            case O: return NativeKeyEvent.VC_O;
            case P: return NativeKeyEvent.VC_P;
            case Q: return NativeKeyEvent.VC_Q;
            case R: return NativeKeyEvent.VC_R;
            case S: return NativeKeyEvent.VC_S;
            case T: return NativeKeyEvent.VC_T;
            case U: return NativeKeyEvent.VC_U;
            case V: return NativeKeyEvent.VC_V;
            case W: return NativeKeyEvent.VC_W;
            case X: return NativeKeyEvent.VC_X;
            case Y: return NativeKeyEvent.VC_Y;
            case Z: return NativeKeyEvent.VC_Z;
            // Mapping explicite pour les chiffres
            case DIGIT0: return NativeKeyEvent.VC_0;
            case DIGIT1: return NativeKeyEvent.VC_1;
            case DIGIT2: return NativeKeyEvent.VC_2;
            case DIGIT3: return NativeKeyEvent.VC_3;
            case DIGIT4: return NativeKeyEvent.VC_4;
            case DIGIT5: return NativeKeyEvent.VC_5;
            case DIGIT6: return NativeKeyEvent.VC_6;
            case DIGIT7: return NativeKeyEvent.VC_7;
            case DIGIT8: return NativeKeyEvent.VC_8;
            case DIGIT9: return NativeKeyEvent.VC_9;
            default:
                return -1;
        }
    }

    /**
     * Listener HOTAS unifié pour tous les binds
     */
    private void startHotasListener() {
        // Récupérer toutes les configurations HOTAS
        String windowToggleController = preferencesService.getWindowToggleHotasController();
        String windowToggleComponent = preferencesService.getWindowToggleHotasComponent();
        float windowToggleValue = preferencesService.getWindowToggleHotasValue();

        boolean hasWindowToggle = windowToggleController != null && !windowToggleController.isEmpty() &&
                windowToggleComponent != null && !windowToggleComponent.isEmpty();

        if (!hasWindowToggle) {
            System.out.println("⚠️ Aucune configuration HOTAS");
            return;
        }

        stopHotasPolling();

        System.out.println("🔍 Recherche des contrôleurs HOTAS...");
        Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
        if (controllers.length == 0) {
            System.out.println("❌ Aucun contrôleur détecté !");
            return;
        }

        List<Controller> activeControllers = new ArrayList<>();
        for (Controller c : controllers) {
            if (c.getType() == Controller.Type.STICK
                    || c.getType() == Controller.Type.GAMEPAD
                    || c.getType() == Controller.Type.WHEEL) {
                activeControllers.add(c);
            }
        }
        if (activeControllers.isEmpty()) {
            System.out.println("⚠️ Aucun HOTAS, manette ou volant détecté");
            return;
        }

        System.out.println("✅ HOTAS actif : "
                + activeControllers.stream().map(Controller::getName).collect(java.util.stream.Collectors.joining(", ")));

        Map<Controller, float[]> lastStates = new HashMap<>();
        for (Controller ctrl : activeControllers) {
            lastStates.put(ctrl, new float[ctrl.getComponents().length]);
        }

        hotasPollSession = new HotasPollSession(
                activeControllers,
                lastStates,
                hasWindowToggle,
                windowToggleController,
                windowToggleComponent,
                windowToggleValue);

        hotasScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "HotasPoll");
            t.setDaemon(true);
            return t;
        });
        hotasScheduler.scheduleWithFixedDelay(this::runHotasPollTick, 0, 100, TimeUnit.MILLISECONDS);
    }

    private void runHotasPollTick() {
        if (!isVrModeEnabled()) {
            return;
        }
        HotasPollSession session = hotasPollSession;
        if (session == null) {
            return;
        }
        try {
            for (Controller ctrl : session.controllers) {
                if (!ctrl.poll()) {
                    continue;
                }
                Component[] components = ctrl.getComponents();
                float[] prevValues = session.lastStates.get(ctrl);
                if (prevValues == null || prevValues.length != components.length) {
                    continue;
                }

                for (int i = 0; i < components.length; i++) {
                    Component comp = components[i];
                    float value = comp.getPollData();
                    String name = comp.getName();
                    if (name == null) {
                        continue;
                    }

                    if (Math.abs(value) < 0.05f) {
                        value = 0.0f;
                    }

                    float prev = prevValues[i];
                    if (Math.abs(prev - value) < HOTAS_POLL_UNCHANGED_EPS) {
                        continue;
                    }
                    prevValues[i] = value;

                    if (session.hasWindowToggle
                            && preferencesService.isWindowToggleEnabled()
                            && session.windowToggleController.equalsIgnoreCase(ctrl.getName())
                            && session.windowToggleComponent.equalsIgnoreCase(name)
                            && crossedIntoFloatBand(prev, value, session.windowToggleValue, HOTAS_MATCH_EPS)) {
                        scheduleHotasWindowToggleOnFxThread();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * État immutable d’une session de poll HOTAS (remplacée au {@link #restart()}).
     */
    private static final class HotasPollSession {
        private final List<Controller> controllers;
        private final Map<Controller, float[]> lastStates;
        private final boolean hasWindowToggle;
        private final String windowToggleController;
        private final String windowToggleComponent;
        private final float windowToggleValue;

        private HotasPollSession(
                List<Controller> controllers,
                Map<Controller, float[]> lastStates,
                boolean hasWindowToggle,
                String windowToggleController,
                String windowToggleComponent,
                float windowToggleValue) {
            this.controllers = List.copyOf(controllers);
            this.lastStates = lastStates;
            this.hasWindowToggle = hasWindowToggle;
            this.windowToggleController = windowToggleController;
            this.windowToggleComponent = windowToggleComponent;
            this.windowToggleValue = windowToggleValue;
        }
    }

    /** Appelé depuis le thread HOTAS uniquement : limite les {@code Platform.runLater} (bug RAM si bind HOTAS). */
    private void scheduleHotasWindowToggleOnFxThread() {
        long now = System.nanoTime();
        if (now - lastHotasWindowScheduleNs < HOTAS_SCHEDULE_DEBOUNCE_NS) {
            return;
        }
        lastHotasWindowScheduleNs = now;
        Platform.runLater(WindowToggleService.this::runToggleWindowOnFxThread);
    }

    /**
     * Entrées depuis JNativeHook (thread natif) : public pour éviter {@code access$n} depuis les classes internes.
     */
    public void runToggleWindowOnFxThread() {
        if (Platform.isFxApplicationThread()) {
            toggleWindowAndOpenCombo();
        } else {
            Platform.runLater(this::toggleWindowAndOpenCombo);
        }
    }

    /**
     * Toggle la fenêtre
     */
    private void toggleWindowAndOpenCombo() {
        if (isPaused) {
            return;
        }
        long now = System.nanoTime();
        if (now - lastWindowToggleNanoTime < WINDOW_TOGGLE_DEBOUNCE_NS) {
            return;
        }
        lastWindowToggleNanoTime = now;

        try {
            if (hidden) {
                restoreFromHidden();
            } else {
                hideForVr();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Reprend la géométrie sauvegardée avant hidden ; si la fenêtre était maximisée, réapplique le maximize écran.
     */
    private void restoreFromHidden() {
        if (mainStage == null) {
            return;
        }
        vrLayoutEpoch++;
        final int epoch = vrLayoutEpoch;
        hidden = false;
        mainStage.setOpacity(1.0);

        mainStage.setMinWidth(savedMinWidth);
        mainStage.setMinHeight(savedMinHeight);

        if (savedWasMaximized) {
            mainStage.setMaximized(false);
            mainStage.setX(savedX);
            mainStage.setY(Math.max(0.0, savedY));
            mainStage.setWidth(savedWidth);
            mainStage.setHeight(savedHeight);
            PauseTransition maximizeDelay = new PauseTransition(RESTORE_MAXIMIZE_DELAY);
            maximizeDelay.setOnFinished(e -> {
                if (epoch != vrLayoutEpoch || hidden || mainStage == null) {
                    return;
                }
                if (WindowFramePreferences.useNativeOsWindowFrame()) {
                    mainStage.setMaximized(true);
                } else {
                    StageVisualBounds.fitStageToVisualBounds(mainStage);
                }
                scheduleWin32UndecoratedFrameRefresh();
                finishRestoreZOrder();
            });
            maximizeDelay.play();
        } else {
            mainStage.setMaximized(false);
            mainStage.setX(savedX);
            mainStage.setY(Math.max(0.0, savedY));
            mainStage.setWidth(savedWidth);
            mainStage.setHeight(savedHeight);
        }

        finishRestoreZOrder();
        scheduleRestoreComboAndZoom();
    }

    /**
     * Sous Windows : premier plan sans activer le HWND (évite de capter la souris au jeu au bind VR).
     * Sinon ou si HWND introuvable : repli {@link Stage#toFront()}.
     */
    private void finishRestoreZOrder() {
        if (mainStage == null) {
            return;
        }
        if (WindowsBringStageToFrontNoActivate.isSupported()) {
            if (!WindowsBringStageToFrontNoActivate.apply(mainStage)) {
                mainStage.toFront();
            }
            Platform.runLater(() -> WindowsBringStageToFrontNoActivate.apply(mainStage));
        } else {
            mainStage.toFront();
        }
    }

    /**
     * Ré-applique les styles Win32 (WS_THICKFRAME…) après un changement de taille, comme au premier
     * {@link be.mirooz.elitedangerous.dashboard.EliteDashboardApp#start}.
     */
    private void scheduleWin32UndecoratedFrameRefresh() {
        if (mainStage == null
                || WindowFramePreferences.useNativeOsWindowFrame()
                || !WindowsUndecoratedVrFrameCompat.isSupportedOs()
                || !WindowsUndecoratedVrFrameCompat.isEnabledByProperty()) {
            return;
        }
        PauseTransition pause = new PauseTransition(Duration.millis(100));
        pause.setOnFinished(e -> WindowsUndecoratedVrFrameCompat.applyAfterShown(mainStage));
        pause.play();
    }

    /**
     * Passe dans la bande {@code |v - target| < halfBand} en venant de l’extérieur (évite les déclenchements
     * répétés si la valeur analogique oscille autour de la cible).
     */
    private static boolean crossedIntoFloatBand(float previous, float current, float target, float halfBand) {
        boolean wasIn = Math.abs(previous - target) < halfBand;
        boolean nowIn = Math.abs(current - target) < halfBand;
        return nowIn && !wasIn;
    }

    /**
     * Réduit la fenêtre pour la VR : sauve l’état, enlève les minima du stage, impose tout de suite 1×1 px
     * (le contenu impose sinon ~1000×600 tant que les minima applicatifs sont actifs).
     */
    private void hideForVr() {
        if (mainStage == null || hidden) {
            return;
        }
        vrLayoutEpoch++;
        savedWasMaximized = mainStage.isMaximized()
                || (!WindowFramePreferences.useNativeOsWindowFrame()
                        && StageVisualBounds.isStageFillingWorkArea(mainStage, 4.0));
        if (mainStage.isMaximized()) {
            mainStage.setMaximized(false);
        }
        savedMinWidth = mainStage.getMinWidth();
        savedMinHeight = mainStage.getMinHeight();
        savedX = mainStage.getX();
        savedY = mainStage.getY();
        savedWidth = mainStage.getWidth();
        savedHeight = mainStage.getHeight();

        hidden = true;
        mainStage.setMaximized(false);

        mainStage.setMinWidth(0);
        mainStage.setMinHeight(0);
        mainStage.setX(savedX);
        mainStage.setY(savedY);
        mainStage.setWidth(1);
        mainStage.setHeight(1);
    }

    private void scheduleRestoreComboAndZoom() {
        PauseTransition delay = new PauseTransition(Duration.millis(50));
        delay.setOnFinished(e -> {
            if (comboBox != null
                    && comboBox.getScene() != null
                    && comboBox.getScene().getWindow() != null) {
                comboBox.show();
                PauseTransition delay2 = new PauseTransition(Duration.millis(50));
                delay2.setOnFinished(ev -> {
                    comboBox.hide();
                    recalculateSystemVisualZoom();
                });
                delay2.play();
            } else {
                recalculateSystemVisualZoom();
            }
        });
        delay.play();
    }

    /**
     * Recalcule le zoom optimal de la vue visuelle du système
     */
    private void recalculateSystemVisualZoom() {
        Platform.runLater(() -> {
            try {
                SystemVisualViewComponent systemVisualView = SystemVisualViewComponent.getInstance();
                if (systemVisualView != null) {
                    systemVisualView.recalculateOptimalZoom();
                }
            } catch (Exception ex) {
                // Ignorer les erreurs silencieusement si le composant n'est pas encore initialisé
            }
        });
    }

}

