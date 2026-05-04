package be.mirooz.elitedangerous.dashboard.service;

import be.mirooz.elitedangerous.dashboard.service.webservice.AnalyticsService;
import be.mirooz.elitedangerous.dashboard.view.exploration.SystemVisualViewComponent;
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
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;

import java.awt.*;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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
    private Point lastMousePos = null;
    private Thread hotasThread;
    private NativeKeyListener keyboardListener = null;
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

        // Ajouter des listeners JavaFX sur la scène pour capturer les touches même quand l'app a le focus
        Platform.runLater(() -> {
            if (stage.getScene() != null) {
                Scene scene = stage.getScene();
                scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressFromScene);
                scene.addEventFilter(KeyEvent.KEY_RELEASED, this::handleWindowToggleKeyReleasedFromScene);
                // Filtre pour désactiver la navigation au clavier (empêcher la sélection des éléments)
                //scene.addEventFilter(KeyEvent.KEY_PRESSED, this::disableKeyboardNavigation);
            }
        });
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
        boolean windowToggleEnabled = preferencesService.isWindowToggleEnabled();
        boolean tabSwitchEnabled = preferencesService.isTabSwitchEnabled();

        if (!windowToggleEnabled && !tabSwitchEnabled) {
            return;
        }

        startGlobalKeyboardListener();
        startHotasListener();
        System.out.println("✅ Service de bind démarré (clavier + HOTAS)");
    }

    /**
     * Arrête les listeners
     */
    public void stop() {
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

        if (hotasThread != null && hotasThread.isAlive()) {
            hotasThread.interrupt();
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
                    int keyCode = e.getKeyCode();
                    if (keyCode == -1) {
                        return;
                    }
                    if (keyCode == tabLeftKeyCode && preferencesService.isTabSwitchEnabled()) {
                        Platform.runLater(() -> switchToPreviousTab());
                    } else if (keyCode == tabRightKeyCode && preferencesService.isTabSwitchEnabled()) {
                        Platform.runLater(() -> switchToNextTab());
                    }
                }

                @Override
                public void nativeKeyReleased(NativeKeyEvent e) {
                    int keyCode = e.getKeyCode();
                    if (keyCode == -1) {
                        return;
                    }
                    if (keyCode == windowToggleKeyCode && preferencesService.isWindowToggleEnabled()) {
                        Platform.runLater(WindowToggleService.this::toggleWindowAndOpenCombo);
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
     * Toggle fenêtre depuis la scène : {@code KEY_RELEASED} seulement (évite la répétition auto de
     * {@code KEY_PRESSED}). Le hook global utilise aussi {@code nativeKeyReleased}.
     */
    private void handleWindowToggleKeyReleasedFromScene(KeyEvent event) {
        if (isPaused) {
            return;
        }
        int eventKeyCode = convertJavaFXKeyCodeToNative(event.getCode());
        if (eventKeyCode == -1) {
            return;
        }
        int windowToggleKeyCode = preferencesService.getWindowToggleKeyboardKey();
        if (eventKeyCode == windowToggleKeyCode && preferencesService.isWindowToggleEnabled()) {
            event.consume();
            toggleWindowAndOpenCombo();
        }
    }

    /**
     * Onglets depuis la scène (toujours sur {@code KEY_PRESSED}).
     */
    private void handleKeyPressFromScene(KeyEvent event) {
        if (isPaused) {
            return;
        }

        int eventKeyCode = convertJavaFXKeyCodeToNative(event.getCode());
        if (eventKeyCode == -1) {
            return;
        }

        if (preferencesService.isTabSwitchEnabled()) {
            int tabLeftKeyCode = preferencesService.getTabSwitchLeftKeyboardKey();
            int tabRightKeyCode = preferencesService.getTabSwitchRightKeyboardKey();

            if (eventKeyCode == tabLeftKeyCode && tabLeftKeyCode > 0) {
                switchToPreviousTab();
                event.consume();
                return;
            } else if (eventKeyCode == tabRightKeyCode && tabRightKeyCode > 0) {
                switchToNextTab();
                event.consume();
                return;
            }
        }
    }

    /**
     * Désactive la navigation au clavier pour empêcher la sélection des éléments
     */
    private void disableKeyboardNavigation(KeyEvent event) {
        // Ne pas bloquer si l'événement est déjà consommé (par exemple par handleKeyPressFromScene)
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

        hotasThread = new Thread(() -> {
            try {
                System.out.println("🔍 Recherche des contrôleurs HOTAS...");
                Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();

                if (controllers.length == 0) {
                    System.out.println("❌ Aucun contrôleur détecté !");
                    return;
                }

                var activeControllers = Arrays.stream(controllers)
                        .filter(c -> c.getType() == Controller.Type.STICK
                                || c.getType() == Controller.Type.GAMEPAD
                                || c.getType() == Controller.Type.WHEEL)
                        .toList();

                if (activeControllers.isEmpty()) {
                    System.out.println("⚠️ Aucun HOTAS, manette ou volant détecté");
                    return;
                }

                System.out.println("✅ HOTAS actif : " +
                        activeControllers.stream().map(Controller::getName).collect(java.util.stream.Collectors.joining(", ")));

                Map<Controller, float[]> lastStates = new HashMap<>();
                for (Controller ctrl : activeControllers) {
                    lastStates.put(ctrl, new float[ctrl.getComponents().length]);
                }

                while (!Thread.currentThread().isInterrupted()) {
                    for (Controller ctrl : activeControllers) {
                        if (!ctrl.poll()) {
                            //System.out.println("⚠️ " + ctrl.getName() + " déconnecté !");
                            continue;
                        }

                        var components = ctrl.getComponents();
                        float[] prevValues = lastStates.get(ctrl);

                        for (int i = 0; i < components.length; i++) {
                            var comp = components[i];
                            float value = comp.getPollData();
                            String name = comp.getName();

                            if (Math.abs(value) < 0.05f)
                                value = 0.0f;

                            if (prevValues[i] != value) {
                                prevValues[i] = value;

                                //System.out.println(ctrl.getName() + " - " + name + ": " + value + " ( analogique : " + comp.isAnalog() + ")" + comp.getDeadZone() + " " + comp.isRelative());

                                // Vérifier window toggle
                                if (hasWindowToggle && preferencesService.isWindowToggleEnabled() &&
                                        windowToggleController.equalsIgnoreCase(ctrl.getName()) &&
                                        windowToggleComponent.equalsIgnoreCase(name) &&
                                        Math.abs(value - windowToggleValue) < 0.01f) {
                                    System.out.println(ctrl.getName() + " - " + name + ": " + value + " ( analogique : " + comp.isAnalog() + ")" + comp.getDeadZone() + " " + comp.isRelative());
                                    Platform.runLater(() -> toggleWindowAndOpenCombo());
                                }

                                // Vérifier tab left
                                if (hasTabLeft && preferencesService.isTabSwitchEnabled() &&
                                        tabLeftController.equalsIgnoreCase(ctrl.getName()) &&
                                        tabLeftComponent.equalsIgnoreCase(name) &&
                                        Math.abs(value - tabLeftValue) < 0.01f) {
                                    Platform.runLater(() -> switchToPreviousTab());
                                }

                                // Vérifier tab right
                                if (hasTabRight && preferencesService.isTabSwitchEnabled() &&
                                        tabRightController.equalsIgnoreCase(ctrl.getName()) &&
                                        tabRightComponent.equalsIgnoreCase(name) &&
                                        Math.abs(value - tabRightValue) < 0.01f) {
                                    Platform.runLater(() -> switchToNextTab());
                                }
                            }
                        }
                    }

                    Thread.sleep(100);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "HotasUnifiedListenerThread");

        hotasThread.start();
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

        mainStage.setMinWidth(savedMinWidth);
        mainStage.setMinHeight(savedMinHeight);

        if (savedWasMaximized) {
            // Sur stage undecorated, appliquer la taille « restaurée » puis setMaximized(true) laisse
            // souvent maximized=true avec une zone client trop petite. On remplit d'abord le
            // visualBounds de l'écran concerné, puis on synchronise l'état maximize au pulse suivant.
            var screens = Screen.getScreensForRectangle(
                    savedX, savedY, Math.max(1.0, savedWidth), Math.max(1.0, savedHeight));
            Screen targetScreen =
                    (screens != null && !screens.isEmpty()) ? screens.get(0) : Screen.getPrimary();
            Rectangle2D vb = targetScreen.getVisualBounds();
            mainStage.setMaximized(false);
            mainStage.setX(vb.getMinX());
            mainStage.setY(vb.getMinY());
            mainStage.setWidth(vb.getWidth());
            mainStage.setHeight(vb.getHeight());
            Platform.runLater(() -> mainStage.setMaximized(true));
        } else {
            double startWidth = VR_COLLAPSED_SIZE;
            double startHeight = VR_COLLAPSED_SIZE;
            double centerX = savedX + savedWidth / 2;
            double centerY = savedY + savedHeight / 2;
            double startX = centerX - startWidth / 2;
            double startY = centerY - startHeight / 2;

            mainStage.setX(startX);
            mainStage.setY(startY);
            mainStage.setWidth(startWidth);
            mainStage.setHeight(startHeight);

            mainStage.setWidth(savedWidth);
            mainStage.setHeight(savedHeight);
            mainStage.setX(savedX);
            mainStage.setY(savedY);
        }

        scheduleRestoreComboAndZoom();
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

        hidden = true;

        double centerX = savedX + savedWidth / 2;
        double centerY = savedY + savedHeight / 2;
        double endWidth = VR_COLLAPSED_SIZE;
        double endHeight = VR_COLLAPSED_SIZE;
        double endX = centerX - endWidth / 2;
        double endY = centerY - endHeight / 2;

        mainStage.setMinWidth(VR_COLLAPSED_SIZE);
        mainStage.setMinHeight(VR_COLLAPSED_SIZE);
        mainStage.setWidth(endWidth);
        mainStage.setHeight(endHeight);
        mainStage.setX(endX);
        mainStage.setY(endY);
        mainStage.setOpacity(0);
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

