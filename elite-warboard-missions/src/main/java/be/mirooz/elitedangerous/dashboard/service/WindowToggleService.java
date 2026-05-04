package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.dashboard.service.webservice.AnalyticsService;
import be.mirooz.elitedangerous.dashboard.view.exploration.SystemVisualViewComponent;
import be.mirooz.elitedangerous.dashboard.window.WindowFramePreferences;
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
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service pour gérer le toggle de fenêtre avec bind clavier et HOTAS
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
    private boolean isAnimating = false;
    private double savedWidth = 1200;
    private double savedHeight = 800;
    private double savedX = 0;
    private double savedY = 0;
    /** Minima du stage avant réduction VR (réappliqués à la restauration). */
    private double savedMinWidth = 1;
    private double savedMinHeight = 1;
    /** Taille cible une fois les minima contournés (quasi invisible en VR). */
    private static final double VR_COLLAPSED_SIZE = 1;
    /** État plein écran au moment de la réduction (restauré après le toggle). */
    private boolean savedWasMaximized = false;
    /** Incrémenté pour ignorer un {@code runLater} de réduction si l’utilisateur a déjà restauré. */
    private int vrCollapseGeneration = 0;
    /** Réduction VR en cours (entre clic et {@link #captureBoundsAndApplyVrCollapse}). */
    private volatile boolean vrCollapsePending = false;
    /** Anti-rebond : répétition OS sur {@code KEY_PRESSED} + double événement Global/JavaFX. */
    private volatile long lastWindowToggleNanoTime = 0L;
    private static final long WINDOW_TOGGLE_DEBOUNCE_NS = 120_000_000L;
    /** HOTAS : ne pas enfiler de {@code Platform.runLater} plus souvent (sinon file FX + RAM si l’analogique « tremble »). */
    private static final long HOTAS_SCHEDULE_DEBOUNCE_NS = 350_000_000L;
    private volatile long lastHotasWindowScheduleNs = 0L;
    private volatile long lastHotasTabLeftScheduleNs = 0L;
    private volatile long lastHotasTabRightScheduleNs = 0L;
    private static final float HOTAS_MATCH_EPS = 0.01f;
    /** Ignore le bruit analogique entre deux polls (sinon front « entrée bande » en rafale). */
    private static final float HOTAS_POLL_UNCHANGED_EPS = 0.008f;
    private Point lastMousePos = null;
    /** Poll HOTAS : une tâche planifiée (pas de {@code while} actif) pour limiter CPU et arrêt propre. */
    private ScheduledExecutorService hotasScheduler;
    private volatile HotasPollSession hotasPollSession;
    private NativeKeyListener keyboardListener = null;
    /** Filtre scène : installé seulement quand le mode VR (prefs) est actif, retiré dans {@link #stop()}. */
    private boolean sceneVrKeyFilterRegistered;
    private boolean sceneVrKeyFilterSceneListenerInstalled;
    private final ChangeListener<Scene> sceneForVrKeyFilterListener = (obs, oldScene, newScene) -> {
        if (newScene != null && isVrModeEnabled()) {
            Platform.runLater(this::registerSceneVrKeyFilterOnCurrentSceneIfNeeded);
        }
    };
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

    /** Mode VR activé dans les préférences (toggle fenêtre et/ou changement d’onglets). */
    private boolean isVrModeEnabled() {
        return preferencesService.isWindowToggleEnabled() || preferencesService.isTabSwitchEnabled();
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
        Platform.runLater(() -> {
            registerSceneVrKeyFilterOnCurrentSceneIfNeeded();
            if (mainStage == null || mainStage.getScene() != null || sceneVrKeyFilterSceneListenerInstalled) {
                return;
            }
            mainStage.sceneProperty().addListener(sceneForVrKeyFilterListener);
            sceneVrKeyFilterSceneListenerInstalled = true;
        });
    }

    private void detachSceneVrKeyFilter() {
        Runnable detach = () -> {
            if (mainStage != null && sceneVrKeyFilterSceneListenerInstalled) {
                mainStage.sceneProperty().removeListener(sceneForVrKeyFilterListener);
                sceneVrKeyFilterSceneListenerInstalled = false;
            }
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
            int tabLeftKeyCode = preferencesService.getTabSwitchLeftKeyboardKey();
            int tabRightKeyCode = preferencesService.getTabSwitchRightKeyboardKey();

            // Ne pas démarrer si aucun keyCode n'est configuré
            if (windowToggleKeyCode <= 0 && tabLeftKeyCode <= 0 && tabRightKeyCode <= 0) {
                System.out.println("⚠️ Pas de bind clavier configuré");
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
                        Platform.runLater(WindowToggleService.this::toggleWindowAndOpenCombo);
                    } else if (tabLeftKeyCode > 0
                            && keyCode == tabLeftKeyCode
                            && preferencesService.isTabSwitchEnabled()) {
                        Platform.runLater(WindowToggleService.this::switchToPreviousTab);
                    } else if (tabRightKeyCode > 0
                            && keyCode == tabRightKeyCode
                            && preferencesService.isTabSwitchEnabled()) {
                        Platform.runLater(WindowToggleService.this::switchToNextTab);
                    }
                }
            };

            GlobalScreen.addNativeKeyListener(keyboardListener);

            System.out.println("🎧 Hook clavier global actif (window: " + windowToggleKeyCode +
                    ", tab left: " + tabLeftKeyCode + ", tab right: " + tabRightKeyCode + ").");
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
                return;
            }
        }
        if (preferencesService.isTabSwitchEnabled()) {
            int tabLeftKeyCode = preferencesService.getTabSwitchLeftKeyboardKey();
            int tabRightKeyCode = preferencesService.getTabSwitchRightKeyboardKey();
            if (tabLeftKeyCode > 0 && eventKeyCode == tabLeftKeyCode) {
                switchToPreviousTab();
                event.consume();
            } else if (tabRightKeyCode > 0 && eventKeyCode == tabRightKeyCode) {
                switchToNextTab();
                event.consume();
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

        // Vérifier si c'est une touche de bind configurée
        int windowToggleKeyCode = preferencesService.getWindowToggleKeyboardKey();
        int tabLeftKeyCode = preferencesService.getTabSwitchLeftKeyboardKey();
        int tabRightKeyCode = preferencesService.getTabSwitchRightKeyboardKey();

        // Ne pas bloquer les touches de bind
        if (eventKeyCode == windowToggleKeyCode ||
                eventKeyCode == tabLeftKeyCode ||
                eventKeyCode == tabRightKeyCode) {
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

        String tabLeftController = preferencesService.getTabSwitchLeftHotasController();
        String tabLeftComponent = preferencesService.getTabSwitchLeftHotasComponent();
        float tabLeftValue = preferencesService.getTabSwitchLeftHotasValue();

        String tabRightController = preferencesService.getTabSwitchRightHotasController();
        String tabRightComponent = preferencesService.getTabSwitchRightHotasComponent();
        float tabRightValue = preferencesService.getTabSwitchRightHotasValue();

        // Si aucune configuration HOTAS, ne pas démarrer
        boolean hasWindowToggle = windowToggleController != null && !windowToggleController.isEmpty() &&
                windowToggleComponent != null && !windowToggleComponent.isEmpty();
        boolean hasTabLeft = tabLeftController != null && !tabLeftController.isEmpty() &&
                tabLeftComponent != null && !tabLeftComponent.isEmpty();
        boolean hasTabRight = tabRightController != null && !tabRightController.isEmpty() &&
                tabRightComponent != null && !tabRightComponent.isEmpty();

        if (!hasWindowToggle && !hasTabLeft && !hasTabRight) {
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
                windowToggleValue,
                hasTabLeft,
                tabLeftController,
                tabLeftComponent,
                tabLeftValue,
                hasTabRight,
                tabRightController,
                tabRightComponent,
                tabRightValue);

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

                    if (session.hasTabLeft
                            && preferencesService.isTabSwitchEnabled()
                            && session.tabLeftController.equalsIgnoreCase(ctrl.getName())
                            && session.tabLeftComponent.equalsIgnoreCase(name)
                            && crossedIntoFloatBand(prev, value, session.tabLeftValue, HOTAS_MATCH_EPS)) {
                        scheduleHotasTabLeftOnFxThread();
                    }

                    if (session.hasTabRight
                            && preferencesService.isTabSwitchEnabled()
                            && session.tabRightController.equalsIgnoreCase(ctrl.getName())
                            && session.tabRightComponent.equalsIgnoreCase(name)
                            && crossedIntoFloatBand(prev, value, session.tabRightValue, HOTAS_MATCH_EPS)) {
                        scheduleHotasTabRightOnFxThread();
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
        private final boolean hasTabLeft;
        private final String tabLeftController;
        private final String tabLeftComponent;
        private final float tabLeftValue;
        private final boolean hasTabRight;
        private final String tabRightController;
        private final String tabRightComponent;
        private final float tabRightValue;

        private HotasPollSession(
                List<Controller> controllers,
                Map<Controller, float[]> lastStates,
                boolean hasWindowToggle,
                String windowToggleController,
                String windowToggleComponent,
                float windowToggleValue,
                boolean hasTabLeft,
                String tabLeftController,
                String tabLeftComponent,
                float tabLeftValue,
                boolean hasTabRight,
                String tabRightController,
                String tabRightComponent,
                float tabRightValue) {
            this.controllers = List.copyOf(controllers);
            this.lastStates = lastStates;
            this.hasWindowToggle = hasWindowToggle;
            this.windowToggleController = windowToggleController;
            this.windowToggleComponent = windowToggleComponent;
            this.windowToggleValue = windowToggleValue;
            this.hasTabLeft = hasTabLeft;
            this.tabLeftController = tabLeftController;
            this.tabLeftComponent = tabLeftComponent;
            this.tabLeftValue = tabLeftValue;
            this.hasTabRight = hasTabRight;
            this.tabRightController = tabRightController;
            this.tabRightComponent = tabRightComponent;
            this.tabRightValue = tabRightValue;
        }
    }

    /** Appelé depuis le thread HOTAS uniquement : limite les {@code Platform.runLater} (bug RAM si bind HOTAS). */
    private void scheduleHotasWindowToggleOnFxThread() {
        long now = System.nanoTime();
        if (now - lastHotasWindowScheduleNs < HOTAS_SCHEDULE_DEBOUNCE_NS) {
            return;
        }
        lastHotasWindowScheduleNs = now;
        Platform.runLater(WindowToggleService.this::toggleWindowAndOpenCombo);
    }

    private void scheduleHotasTabLeftOnFxThread() {
        long now = System.nanoTime();
        if (now - lastHotasTabLeftScheduleNs < HOTAS_SCHEDULE_DEBOUNCE_NS) {
            return;
        }
        lastHotasTabLeftScheduleNs = now;
        Platform.runLater(WindowToggleService.this::switchToPreviousTab);
    }

    private void scheduleHotasTabRightOnFxThread() {
        long now = System.nanoTime();
        if (now - lastHotasTabRightScheduleNs < HOTAS_SCHEDULE_DEBOUNCE_NS) {
            return;
        }
        lastHotasTabRightScheduleNs = now;
        Platform.runLater(WindowToggleService.this::switchToNextTab);
    }

    /**
     * Toggle la fenêtre
     */
    private void toggleWindowAndOpenCombo() {
        if (isPaused) {
            return;
        }
        if (isAnimating) {
            return;
        }
        long now = System.nanoTime();
        if (now - lastWindowToggleNanoTime < WINDOW_TOGGLE_DEBOUNCE_NS) {
            return;
        }
        lastWindowToggleNanoTime = now;

        try {
            if (hidden) {
                restoreWindowWithAnimation();
            } else {
                if (vrCollapsePending) {
                    return;
                }
                minimizeWindowWithAnimation();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Restaure la fenêtre avec animation
     */
    private void restoreWindowWithAnimation() {
        vrCollapseGeneration++;
        vrCollapsePending = false;
        mainStage.toFront();
        hidden = false;
        mainStage.setOpacity(1.0);

        // Minima applicatifs (valeurs sauvegardées avant la réduction), puis géométrie / maximize sauvegardés.
        mainStage.setMinWidth(savedMinWidth);
        mainStage.setMinHeight(savedMinHeight);

        if (savedWasMaximized) {
            // Plein écran sur l’écran qui portait la fenêtre (pas l’écran primaire par défaut).
            mainStage.setMaximized(false);
            Screen target = screenBestForRestore();
            Rectangle2D vb = target.getVisualBounds();
            mainStage.setX(vb.getMinX());
            mainStage.setY(vb.getMinY());
            mainStage.setWidth(vb.getWidth());
            mainStage.setHeight(vb.getHeight());
            Platform.runLater(
                    () -> {
                        mainStage.setMaximized(true);
                        Platform.runLater(WindowToggleService.this::correctMaximizedClientIfNeeded);
                    });
            scheduleWin32UndecoratedFrameRefresh();
        } else {
            mainStage.setMaximized(false);
            double w = Math.max(savedMinWidth, Math.max(1.0, savedWidth));
            double h = Math.max(savedMinHeight, Math.max(1.0, savedHeight));
            Screen target = screenBestForRestore();
            Rectangle2D safe = clampToVisualBounds(target, savedX, savedY, w, h);
            mainStage.setX(safe.getMinX());
            mainStage.setY(safe.getMinY());
            mainStage.setWidth(safe.getWidth());
            mainStage.setHeight(safe.getHeight());
        }

        scheduleRestoreComboAndZoom();
    }

    /**
     * Choisit l’écran le plus cohérent avec la géométrie sauvegardée (intersection de surface, puis centre).
     */
    private Screen screenBestForRestore() {
        double sw = Math.max(1.0, savedWidth);
        double sh = Math.max(1.0, savedHeight);
        Rectangle2D saved = new Rectangle2D(savedX, savedY, sw, sh);
        var byRect = Screen.getScreensForRectangle(savedX, savedY, sw, sh);
        if (byRect != null && !byRect.isEmpty()) {
            if (byRect.size() == 1) {
                return byRect.get(0);
            }
            return byRect.stream()
                    .max(Comparator.comparingDouble(s -> intersectionArea(saved, s.getVisualBounds())))
                    .orElse(byRect.get(0));
        }
        double cx = savedX + sw / 2.0;
        double cy = savedY + sh / 2.0;
        for (Screen s : Screen.getScreens()) {
            if (s.getVisualBounds().contains(cx, cy)) {
                return s;
            }
        }
        // Fenêtre à cheval / centre hors visual : garder l’écran avec la plus grande intersection (≠ primary aveugle).
        Screen bestOverlap = null;
        double maxA = 0.0;
        for (Screen s : Screen.getScreens()) {
            double a = intersectionArea(saved, s.getVisualBounds());
            if (a > maxA) {
                maxA = a;
                bestOverlap = s;
            }
        }
        if (bestOverlap != null && maxA > 0.0) {
            return bestOverlap;
        }
        Screen closest = Screen.getPrimary();
        double bestD = Double.MAX_VALUE;
        for (Screen s : Screen.getScreens()) {
            Rectangle2D vb = s.getVisualBounds();
            double scx = vb.getMinX() + vb.getWidth() / 2.0;
            double scy = vb.getMinY() + vb.getHeight() / 2.0;
            double d = (cx - scx) * (cx - scx) + (cy - scy) * (cy - scy);
            if (d < bestD) {
                bestD = d;
                closest = s;
            }
        }
        return closest;
    }

    /**
     * {@code setMaximized(true)} sur stage undecorated : la fenêtre OS peut dépasser légèrement le
     * {@code visualBounds} JavaFX — ne pas traiter ça comme une erreur à « corriger » en boucle.
     */
    private static boolean isUndecoratedMaximizedSlopMatch(Stage stage, Rectangle2D vb) {
        double x = stage.getX();
        double y = stage.getY();
        double w = stage.getWidth();
        double h = stage.getHeight();
        boolean xOk = x >= vb.getMinX() - 20.0 && x <= vb.getMinX() + 15.0;
        boolean yOk = y >= vb.getMinY() - 20.0 && y <= vb.getMinY() + 15.0;
        boolean wOk = w >= vb.getWidth() - 15.0 && w <= vb.getWidth() + 50.0;
        boolean hOk = h >= vb.getHeight() - 15.0 && h <= vb.getHeight() + 100.0;
        return xOk && yOk && wOk && hOk;
    }

    private static Rectangle2D rectIntersection(Rectangle2D a, Rectangle2D b) {
        double x1 = Math.max(a.getMinX(), b.getMinX());
        double y1 = Math.max(a.getMinY(), b.getMinY());
        double x2 = Math.min(a.getMaxX(), b.getMaxX());
        double y2 = Math.min(a.getMaxY(), b.getMaxY());
        double w = Math.max(0.0, x2 - x1);
        double h = Math.max(0.0, y2 - y1);
        return new Rectangle2D(x1, y1, w, h);
    }

    private static double intersectionArea(Rectangle2D a, Rectangle2D b) {
        Rectangle2D inter = rectIntersection(a, b);
        return inter.getWidth() * inter.getHeight();
    }

    /** Ramène la fenêtre dans {@code screen#getVisualBounds()} sans basculer sur l’écran primaire par erreur. */
    private static Rectangle2D clampToVisualBounds(Screen screen, double x, double y, double w, double h) {
        Rectangle2D vb = screen.getVisualBounds();
        Rectangle2D candidate = new Rectangle2D(x, y, w, h);
        if (candidate.intersects(vb)) {
            double maxX = Math.max(vb.getMinX(), vb.getMaxX() - w);
            double maxY = Math.max(vb.getMinY(), vb.getMaxY() - h);
            double nx = Math.min(Math.max(x, vb.getMinX()), maxX);
            double ny = Math.min(Math.max(y, vb.getMinY()), maxY);
            return new Rectangle2D(nx, ny, w, h);
        }
        double nw = Math.min(w, vb.getWidth());
        double nh = Math.min(h, vb.getHeight());
        double nx = vb.getMinX() + (vb.getWidth() - nw) / 2.0;
        double ny = vb.getMinY() + (vb.getHeight() - nh) / 2.0;
        return new Rectangle2D(nx, ny, nw, nh);
    }

    /** Écran qui contient le centre du stage (après coup WM), plus fiable que la seule géométrie sauvegardée. */
    private Screen screenForCurrentStageCenter() {
        if (mainStage == null) {
            return Screen.getPrimary();
        }
        double w = Math.max(1.0, mainStage.getWidth());
        double h = Math.max(1.0, mainStage.getHeight());
        double cx = mainStage.getX() + w / 2.0;
        double cy = mainStage.getY() + h / 2.0;
        var list = Screen.getScreensForRectangle(cx - 1, cy - 1, 2, 2);
        if (list != null && !list.isEmpty()) {
            return list.get(0);
        }
        return screenBestForRestore();
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
     * Après {@code setMaximized(true)} (undecorated / Windows), corrige si la taille client ne suit pas le
     * {@code visualBounds} de l’écran réellement occupé.
     */
    private void correctMaximizedClientIfNeeded() {
        if (hidden || mainStage == null) {
            return;
        }
        if (!savedWasMaximized || !mainStage.isMaximized()) {
            return;
        }
        Screen target = screenForCurrentStageCenter();
        Rectangle2D vb = target.getVisualBounds();
        // Sous Windows / undecorated, setMaximized(true) donne souvent ~±7 px et +14/+62 : « corriger » repasse en boucle au même état.
        if (isUndecoratedMaximizedSlopMatch(mainStage, vb)) {
            return;
        }
        mainStage.setMaximized(false);
        mainStage.setX(vb.getMinX());
        mainStage.setY(vb.getMinY());
        mainStage.setWidth(vb.getWidth());
        mainStage.setHeight(vb.getHeight());
        Platform.runLater(() -> {
            if (!hidden) {
                mainStage.setMaximized(true);
                scheduleWin32UndecoratedFrameRefresh();
            }
        });
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
     * Réduit la fenêtre au strict minimum (VR) : minima, taille, position centrée sur la zone sauvegardée.
     */
    private void applyVrHiddenBounds() {
        if (mainStage == null) {
            return;
        }
        mainStage.setMaximized(false);
        double cx = savedX + savedWidth / 2.0;
        double cy = savedY + savedHeight / 2.0;
        mainStage.setMinWidth(VR_COLLAPSED_SIZE);
        mainStage.setMinHeight(VR_COLLAPSED_SIZE);
        mainStage.setWidth(VR_COLLAPSED_SIZE);
        mainStage.setHeight(VR_COLLAPSED_SIZE);
        mainStage.setX(cx - VR_COLLAPSED_SIZE / 2.0);
        mainStage.setY(cy - VR_COLLAPSED_SIZE / 2.0);
        mainStage.setOpacity(0);
    }

    private void scheduleRestoreComboAndZoom() {
        PauseTransition delay = new PauseTransition(Duration.millis(50));
        delay.setOnFinished(e -> {
            comboBox.show();
            PauseTransition delay2 = new PauseTransition(Duration.millis(50));
            delay2.setOnFinished(ev -> {
                comboBox.hide();
                recalculateSystemVisualZoom();
            });
            delay2.play();
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

    /**
     * Réduit la fenêtre avec animation
     */
    private void minimizeWindowWithAnimation() {
        vrCollapseGeneration++;
        final int collapseGen = vrCollapseGeneration;
        vrCollapsePending = true;
        savedWasMaximized = mainStage.isMaximized();
        if (savedWasMaximized) {
            mainStage.setMaximized(false);
        }
        Platform.runLater(() -> {
            if (collapseGen != vrCollapseGeneration) {
                vrCollapsePending = false;
                return;
            }
            captureBoundsAndApplyVrCollapse();
        });
    }

    private void captureBoundsAndApplyVrCollapse() {
        savedWidth = mainStage.getWidth();
        savedHeight = mainStage.getHeight();
        savedX = mainStage.getX();
        savedY = mainStage.getY();
        savedMinWidth = mainStage.getMinWidth();
        savedMinHeight = mainStage.getMinHeight();
        // wasMaximized + démax avant capture ⇒ JavaFX donne souvent 1200×800 (restauration), pas le plein écran :
        // on aligne la sauvegarde sur le visualBounds de l’écran concerné pour restauration cohérente.
        if (savedWasMaximized) {
            Screen maxScreen = screenBestForRestore();
            Rectangle2D vbMax = maxScreen.getVisualBounds();
            savedX = vbMax.getMinX();
            savedY = vbMax.getMinY();
            savedWidth = vbMax.getWidth();
            savedHeight = vbMax.getHeight();
        }

        applyVrHiddenBounds();
        hidden = true;
        // Un seul rappel au pulse suivant (sans écouteurs permanents : évite boucles / dérives avec le WM).
        Platform.runLater(() -> {
            if (hidden) {
                applyVrHiddenBounds();
            }
        });

        vrCollapsePending = false;
        PauseTransition delay = new PauseTransition(Duration.millis(50));
        delay.setOnFinished(e -> {
            comboBox.show();
            PauseTransition delay2 = new PauseTransition(Duration.millis(50));
            delay2.setOnFinished(ev -> comboBox.hide());
            delay2.play();
        });
        delay.play();
    }

    /**
     * Change vers l'onglet précédent (cycle: Mining -> Missions -> Mining)
     */
    private void switchToPreviousTab() {
        if (isPaused) {
            System.out.println("⚠️ Changement d'onglet ignoré (service en pause)");
            return;
        }

        // Vérifier que la fenêtre est visible (pas cachée)
        if (hidden) {
            System.out.println("⚠️ Changement d'onglet ignoré (fenêtre cachée)");
            return;
        }

        if (tabPane == null) {
            System.out.println("⚠️ Changement d'onglet ignoré (TabPane non initialisé)");
            return;
        }

        if (missionsTab == null || miningTab == null || explorationTab == null || colonisationTab == null) {
            System.out.println("⚠️ Changement d'onglet ignoré (onglets non initialisés)");
            return;
        }

        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab == missionsTab) {
            tabPane.getSelectionModel().select(colonisationTab);
            System.out.println("📑 Changement vers onglet Colonisation");
        } else if (selectedTab == miningTab) {
            tabPane.getSelectionModel().select(missionsTab);
            System.out.println("📑 Changement vers onglet Missions");
        } else if (selectedTab == explorationTab) {
            tabPane.getSelectionModel().select(miningTab);
            System.out.println("📑 Changement vers onglet Mining");
        } else if (selectedTab == colonisationTab) {
            tabPane.getSelectionModel().select(explorationTab);
            System.out.println("📑 Changement vers onglet Exploration");
        } else {
            tabPane.getSelectionModel().select(missionsTab);
            System.out.println("📑 Sélection de l'onglet Missions (aucun onglet sélectionné)");
        }
    }

    /**
     * Change vers l'onglet suivant (cycle: Missions -> Mining -> Exploration -> Missions)
     */
    private void switchToNextTab() {
        if (isPaused) {
            System.out.println("⚠️ Changement d'onglet ignoré (service en pause)");
            return;
        }

        // Vérifier que la fenêtre est visible (pas cachée)
        if (hidden) {
            System.out.println("⚠️ Changement d'onglet ignoré (fenêtre cachée)");
            return;
        }

        if (tabPane == null) {
            System.out.println("⚠️ Changement d'onglet ignoré (TabPane non initialisé)");
            return;
        }

        if (missionsTab == null || miningTab == null || explorationTab == null || colonisationTab == null) {
            System.out.println("⚠️ Changement d'onglet ignoré (onglets non initialisés)");
            return;
        }

        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab == missionsTab) {
            tabPane.getSelectionModel().select(miningTab);
            System.out.println("📑 Changement vers onglet Mining");
        } else if (selectedTab == miningTab) {
            tabPane.getSelectionModel().select(explorationTab);
            System.out.println("📑 Changement vers onglet Exploration");
        } else if (selectedTab == explorationTab) {
            tabPane.getSelectionModel().select(colonisationTab);
            System.out.println("📑 Changement vers onglet Colonisation");
        } else if (selectedTab == colonisationTab) {
            tabPane.getSelectionModel().select(missionsTab);
            System.out.println("📑 Changement vers onglet Missions");
        } else {
            tabPane.getSelectionModel().select(missionsTab);
            System.out.println("📑 Sélection de l'onglet Missions (aucun onglet sélectionné)");
        }
    }
}

