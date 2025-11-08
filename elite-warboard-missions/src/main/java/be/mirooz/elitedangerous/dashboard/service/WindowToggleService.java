package be.mirooz.elitedangerous.dashboard.service;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.effect.Glow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.stage.Stage;
import javafx.util.Duration;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;

import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
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
    private boolean hidden = false;
    private boolean isAnimating = false;
    private double savedWidth = 1200;
    private double savedHeight = 800;
    private double savedX = 0;
    private double savedY = 0;
    private Point lastMousePos = null;
    private Thread hotasThread;
    private NativeKeyListener keyboardListener = null;
    private boolean isPaused = false; // Pour désactiver temporairement le toggle

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
                // Listener pour la touche de bind
                scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressFromScene);
                // Filtre pour désactiver la navigation au clavier (empêcher la sélection des éléments)
                scene.addEventFilter(KeyEvent.KEY_PRESSED, this::disableKeyboardNavigation);
            }
        });
    }
    
    /**
     * Initialise le TabPane pour le changement d'onglet
     */
    public void initializeTabPane(TabPane tabPane, Tab missionsTab, Tab miningTab) {
        this.tabPane = tabPane;
        this.missionsTab = missionsTab;
        this.miningTab = miningTab;
    }

    /**
     * Démarre les listeners si le toggle est activé dans les préférences
     */
    public void start() {
        boolean windowToggleEnabled = preferencesService.isWindowToggleEnabled();
        boolean tabSwitchEnabled = preferencesService.isTabSwitchEnabled();
        
        if (!windowToggleEnabled && !tabSwitchEnabled) {
            System.out.println("⚠️ Aucun service activé dans les préférences");
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
            } catch (Exception ignored) {}
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
            
            // Enregistrer le hook seulement s'il n'est pas déjà enregistré
            if (!GlobalScreen.isNativeHookRegistered()) {
                GlobalScreen.registerNativeHook();
            }

            // Créer et ajouter le listener
            keyboardListener = new NativeKeyListener() {
                @Override
                public void nativeKeyPressed(NativeKeyEvent e) {
                    int keyCode = e.getKeyCode();
                    boolean isFocused = mainStage.isFocused();
                    
                    System.out.println("🔑 Touche pressée (GlobalScreen): " + keyCode + " (app focused: " + isFocused + ")");
                    
                    // Ne traiter que si l'app n'a pas le focus (sinon c'est le listener JavaFX qui gère)
                    if (isFocused) {
                        System.out.println("⏭️ Ignoré car l'app a le focus (JavaFX listener gère)");
                        return;
                    }
                    
                    if (keyCode == windowToggleKeyCode && preferencesService.isWindowToggleEnabled()) {
                        System.out.println("✅ Touche window toggle détectée (GlobalScreen)! (code: " + windowToggleKeyCode + ")");
                        Platform.runLater(() -> toggleWindowAndOpenCombo());
                    } else if (keyCode == tabLeftKeyCode && preferencesService.isTabSwitchEnabled()) {
                        System.out.println("✅ Touche tab left détectée (GlobalScreen)! (code: " + tabLeftKeyCode + ")");
                        Platform.runLater(() -> switchToPreviousTab());
                    } else if (keyCode == tabRightKeyCode && preferencesService.isTabSwitchEnabled()) {
                        System.out.println("✅ Touche tab right détectée (GlobalScreen)! (code: " + tabRightKeyCode + ")");
                        Platform.runLater(() -> switchToNextTab());
                    } else {
                        System.out.println("ℹ️ Touche non bindée (code: " + keyCode + ")");
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
     * Gère les pressions de touches depuis la scène JavaFX (quand l'app a le focus)
     */
    private void handleKeyPressFromScene(KeyEvent event) {
        if (isPaused) {
            return;
        }
        
        // Convertir KeyCode JavaFX en code NativeKeyEvent pour comparaison
        int eventKeyCode = convertJavaFXKeyCodeToNative(event.getCode());
        KeyCode keyCode = event.getCode();
        
        System.out.println("🔑 Touche pressée (JavaFX): " + keyCode.getName() + " (code: " + eventKeyCode + ")");
        
        // Vérifier le bind window toggle
        int windowToggleKeyCode = preferencesService.getWindowToggleKeyboardKey();
        if (eventKeyCode == windowToggleKeyCode && windowToggleKeyCode > 0 && preferencesService.isWindowToggleEnabled()) {
            System.out.println("✅ Touche window toggle détectée! (code: " + windowToggleKeyCode + ")");
            toggleWindowAndOpenCombo();
            event.consume();
            return;
        }
        
        // Vérifier les binds de changement d'onglet
        if (preferencesService.isTabSwitchEnabled()) {
            int tabLeftKeyCode = preferencesService.getTabSwitchLeftKeyboardKey();
            int tabRightKeyCode = preferencesService.getTabSwitchRightKeyboardKey();
            
            System.out.println("📋 Binds configurés - Left: " + tabLeftKeyCode + ", Right: " + tabRightKeyCode);
            
            if (eventKeyCode == tabLeftKeyCode && tabLeftKeyCode > 0) {
                System.out.println("✅ Touche tab left détectée! (code: " + tabLeftKeyCode + ")");
                switchToPreviousTab();
                event.consume();
                return;
            } else if (eventKeyCode == tabRightKeyCode && tabRightKeyCode > 0) {
                System.out.println("✅ Touche tab right détectée! (code: " + tabRightKeyCode + ")");
                switchToNextTab();
                event.consume();
                return;
            }
        } else {
            System.out.println("⚠️ Changement d'onglet désactivé dans les préférences");
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
            case SPACE: return NativeKeyEvent.VC_SPACE;
            case ENTER: return NativeKeyEvent.VC_ENTER;
            case ESCAPE: return NativeKeyEvent.VC_ESCAPE;
            case TAB: return NativeKeyEvent.VC_TAB;
            case BACK_SPACE: return NativeKeyEvent.VC_BACKSPACE;
            case DELETE: return NativeKeyEvent.VC_DELETE;
            case UP: return NativeKeyEvent.VC_UP;
            case DOWN: return NativeKeyEvent.VC_DOWN;
            case LEFT: return NativeKeyEvent.VC_LEFT;
            case RIGHT: return NativeKeyEvent.VC_RIGHT;
            case F1: return NativeKeyEvent.VC_F1;
            case F2: return NativeKeyEvent.VC_F2;
            case F3: return NativeKeyEvent.VC_F3;
            case F4: return NativeKeyEvent.VC_F4;
            case F5: return NativeKeyEvent.VC_F5;
            case F6: return NativeKeyEvent.VC_F6;
            case F7: return NativeKeyEvent.VC_F7;
            case F8: return NativeKeyEvent.VC_F8;
            case F9: return NativeKeyEvent.VC_F9;
            case F10: return NativeKeyEvent.VC_F10;
            case F11: return NativeKeyEvent.VC_F11;
            case F12: return NativeKeyEvent.VC_F12;
            default:
                // Pour les lettres (A-Z)
                if (keyCode.isLetterKey()) {
                    char c = Character.toUpperCase(keyCode.getName().charAt(0));
                    return NativeKeyEvent.VC_A + (c - 'A');
                }
                // Pour les chiffres (0-9)
                if (keyCode.isDigitKey()) {
                    int digit = Character.getNumericValue(keyCode.getName().charAt(0));
                    return NativeKeyEvent.VC_0 + digit;
                }
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
                            System.out.println("⚠️ " + ctrl.getName() + " déconnecté !");
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

                                // Vérifier window toggle
                                if (hasWindowToggle && preferencesService.isWindowToggleEnabled() &&
                                    windowToggleController.equalsIgnoreCase(ctrl.getName()) &&
                                    windowToggleComponent.equalsIgnoreCase(name) &&
                                    Math.abs(value - windowToggleValue) < 0.01f) {
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
        // Ne pas toggle si le service est en pause (fenêtre de config ouverte)
        if (isPaused) {
            return;
        }
        
        if (isAnimating) {
            return;
        }

        try {
            Robot robot = new Robot();

            if (hidden) {
                restoreWindowWithAnimation(robot);
            } else {
                minimizeWindowWithAnimation(robot);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Restaure la fenêtre avec animation
     */
    private void restoreWindowWithAnimation(Robot robot) {
        if (mainStage.isMaximized()) {
            mainStage.setMaximized(false);
        }

        mainStage.toFront();

        isAnimating = true;
        hidden = false;

        double startWidth = 1;
        double startHeight = 1;
        double centerX = savedX + savedWidth / 2;
        double centerY = savedY + savedHeight / 2;
        double startX = centerX - startWidth / 2;
        double startY = centerY - startHeight / 2;

        double endWidth = savedWidth;
        double endHeight = savedHeight;
        double endX = savedX;
        double endY = savedY;

        mainStage.setX(startX);
        mainStage.setY(startY);
        mainStage.setWidth(startWidth);
        mainStage.setHeight(startHeight);
        mainStage.setOpacity(1.0);

        Interpolator interpolator = Interpolator.SPLINE(0.4, 0.0, 0.2, 1.0);
        Duration windowDuration = Duration.millis(100);
        Duration triangleDuration = Duration.millis(200);

        Timeline windowTimeline = new Timeline();
        windowTimeline.setCycleCount(1);

        windowTimeline.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            double progress = Math.min(1.0, newTime.toMillis() / windowDuration.toMillis());
            double interpolatedProgress = interpolator.interpolate(0.0, 1.0, progress);
            
            double currentWidth = startWidth + (endWidth - startWidth) * interpolatedProgress;
            double currentHeight = startHeight + (endHeight - startHeight) * interpolatedProgress;
            double currentX = startX + (endX - startX) * interpolatedProgress;
            double currentY = startY + (endY - startY) * interpolatedProgress;
            
            mainStage.setWidth(currentWidth);
            mainStage.setHeight(currentHeight);
            mainStage.setX(currentX);
            mainStage.setY(currentY);
        });

        windowTimeline.setOnFinished(event -> {
            mainStage.setWidth(endWidth);
            mainStage.setHeight(endHeight);
            mainStage.setX(endX);
            mainStage.setY(endY);
            PauseTransition delay = new PauseTransition(Duration.millis(200));
            delay.setOnFinished(e -> comboBox.show());
            delay.play();
        });

        Pane triangleOverlay = createTriangleAnimationOverlay();
        rootPane.getChildren().add(triangleOverlay);

        windowTimeline.play();

        Timeline triangleTimeline = new Timeline();
        triangleTimeline.setCycleCount(1);
        
        KeyValue overlayOpacityStart = new KeyValue(triangleOverlay.opacityProperty(), 1.0);
        KeyValue overlayOpacityEnd = new KeyValue(triangleOverlay.opacityProperty(), 0.0, Interpolator.EASE_OUT);
        KeyFrame overlayFrameStart = new KeyFrame(Duration.ZERO, overlayOpacityStart);
        KeyFrame overlayFrameEnd = new KeyFrame(Duration.millis(300), overlayOpacityEnd);
        
        triangleTimeline.getKeyFrames().addAll(overlayFrameStart, overlayFrameEnd);
        
        triangleTimeline.setOnFinished(event -> {
            rootPane.getChildren().remove(triangleOverlay);
            isAnimating = false;
            System.out.println("🔼 Fenêtre restaurée");
            comboBox.hide();
            moveMouseToCenter(robot);
        });

        PauseTransition delay = new PauseTransition(Duration.millis(100));
        delay.setOnFinished(e -> {
            animateTriangles(triangleOverlay, triangleDuration);
            PauseTransition endDelay = new PauseTransition(triangleDuration);
            endDelay.setOnFinished(ev -> triangleTimeline.play());
            endDelay.play();
        });
        delay.play();
    }

    /**
     * Réduit la fenêtre avec animation
     */
    private void minimizeWindowWithAnimation(Robot robot) {
        savedWidth = mainStage.getWidth();
        savedHeight = mainStage.getHeight();
        savedX = mainStage.getX();
        savedY = mainStage.getY();
        
        isAnimating = true;
        hidden = true;

        double startWidth = savedWidth;
        double startHeight = savedHeight;
        double startX = savedX;
        double startY = savedY;

        double centerX = startX + startWidth / 2;
        double centerY = startY + startHeight / 2;
        double endWidth = 1;
        double endHeight = 1;
        double endX = centerX - endWidth / 2;
        double endY = centerY - endHeight / 2;

        Interpolator interpolator = Interpolator.SPLINE(0.4, 0.0, 0.2, 1.0);
        Duration windowDuration = Duration.millis(200);
        Duration triangleDuration = Duration.millis(400);

        Pane triangleOverlay = createTriangleAnimationOverlay();
        rootPane.getChildren().add(triangleOverlay);
        
        animateTrianglesDisappear(triangleOverlay, triangleDuration);

        Timeline windowTimeline = new Timeline();
        windowTimeline.setCycleCount(1);

        windowTimeline.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            double progress = Math.min(1.0, newTime.toMillis() / windowDuration.toMillis());
            double interpolatedProgress = interpolator.interpolate(0.0, 1.0, progress);
            
            double currentWidth = startWidth + (endWidth - startWidth) * interpolatedProgress;
            double currentHeight = startHeight + (endHeight - startHeight) * interpolatedProgress;
            double currentX = startX + (endX - startX) * interpolatedProgress;
            double currentY = startY + (endY - startY) * interpolatedProgress;
            
            mainStage.setWidth(currentWidth);
            mainStage.setHeight(currentHeight);
            mainStage.setX(currentX);
            mainStage.setY(currentY);
        });

        windowTimeline.setOnFinished(event -> {
            mainStage.setWidth(endWidth);
            mainStage.setHeight(endHeight);
            mainStage.setX(endX);
            mainStage.setY(endY);
        });

        Timeline triangleTimeline = new Timeline();
        triangleTimeline.setCycleCount(1);
        
        KeyValue overlayOpacityStart = new KeyValue(triangleOverlay.opacityProperty(), 1.0);
        KeyValue overlayOpacityEnd = new KeyValue(triangleOverlay.opacityProperty(), 0.0, Interpolator.EASE_OUT);
        KeyFrame overlayFrameStart = new KeyFrame(Duration.ZERO, overlayOpacityStart);
        KeyFrame overlayFrameEnd = new KeyFrame(Duration.millis(100), overlayOpacityEnd);
        
        triangleTimeline.getKeyFrames().addAll(overlayFrameStart, overlayFrameEnd);
        
        triangleTimeline.setOnFinished(event -> {
            rootPane.getChildren().remove(triangleOverlay);
            isAnimating = false;
            System.out.println("🔽 Fenêtre réduite");
            if (lastMousePos != null) {
                robot.mouseMove(lastMousePos.x, lastMousePos.y);
                System.out.println("🖱️ Souris restaurée à (" + lastMousePos.x + ", " + lastMousePos.y + ")");
            }
            PauseTransition delay = new PauseTransition(Duration.millis(50));
            delay.setOnFinished(e -> {
                comboBox.show();
                PauseTransition delay2 = new PauseTransition(Duration.millis(50));
                delay2.setOnFinished(ev -> comboBox.hide());
                delay2.play();
            });
            delay.play();
        });

        PauseTransition waitForTriangles = new PauseTransition(triangleDuration.add(Duration.millis(200)));
        waitForTriangles.setOnFinished(e -> {
            windowTimeline.play();
            PauseTransition endDelay = new PauseTransition(windowDuration);
            endDelay.setOnFinished(ev -> triangleTimeline.play());
            endDelay.play();
        });
        waitForTriangles.play();
    }

    /**
     * Crée un overlay avec des triangles pour l'animation
     */
    private Pane createTriangleAnimationOverlay() {
        Pane overlay = new Pane();
        overlay.setStyle("-fx-background-color: #000000;");
        overlay.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
        
        if (mainStage.getScene() != null) {
            overlay.prefWidthProperty().bind(mainStage.getScene().widthProperty());
            overlay.prefHeightProperty().bind(mainStage.getScene().heightProperty());
            overlay.maxWidthProperty().bind(mainStage.getScene().widthProperty());
            overlay.maxHeightProperty().bind(mainStage.getScene().heightProperty());
        }
        
        return overlay;
    }

    /**
     * Anime les triangles qui apparaissent
     */
    private void animateTriangles(Pane overlay, Duration duration) {
        Platform.runLater(() -> {
            double width = mainStage.getWidth() > 0 ? mainStage.getWidth() : savedWidth;
            double height = mainStage.getHeight() > 0 ? mainStage.getHeight() : savedHeight;
            
            double triangleSize = 35.0;
            double spacing = triangleSize * 1.2;
            
            int cols = (int) Math.ceil(width / spacing) + 2;
            int rows = (int) Math.ceil(height / spacing) + 2;
            
            java.util.List<Polygon> triangles = new java.util.ArrayList<>();
            Random random = new Random();
            
            Color whiteStroke = Color.WHITE;
            
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    double x = col * spacing + (random.nextDouble() - 0.5) * spacing * 0.3;
                    double y = row * spacing + (random.nextDouble() - 0.5) * spacing * 0.3;
                    
                    boolean pointingUp = (row + col) % 2 == 0;
                    Polygon triangle;
                    
                    if (pointingUp) {
                        triangle = new Polygon();
                        triangle.getPoints().addAll(
                            x, y + triangleSize,
                            x + triangleSize / 2, y,
                            x + triangleSize, y + triangleSize
                        );
                    } else {
                        triangle = new Polygon();
                        triangle.getPoints().addAll(
                            x, y,
                            x + triangleSize / 2, y + triangleSize,
                            x + triangleSize, y
                        );
                    }
                    
                    triangle.setFill(Color.BLACK);
                    triangle.setStroke(whiteStroke);
                    triangle.setStrokeWidth(2.0);
                    
                    Glow glow = new Glow(0.6);
                    triangle.setEffect(glow);
                    
                    triangle.setOpacity(0.0);
                    triangles.add(triangle);
                    overlay.getChildren().add(triangle);
                }
            }
            
            Collections.shuffle(triangles, random);
            
            java.util.List<FadeTransition> transitions = new java.util.ArrayList<>();
            
            for (int i = 0; i < triangles.size(); i++) {
                Polygon triangle = triangles.get(i);
                
                double randomProgress = random.nextDouble();
                Duration appearDelay = duration.multiply(randomProgress);
                Duration fadeDuration = Duration.millis(80);
                
                FadeTransition fadeIn = new FadeTransition(fadeDuration, triangle);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.setDelay(appearDelay);
                transitions.add(fadeIn);
            }
            
            for (FadeTransition transition : transitions) {
                transition.play();
            }
            
            PauseTransition finalCheck = new PauseTransition(duration.add(Duration.millis(50)));
            finalCheck.setOnFinished(e -> {
                for (Polygon triangle : triangles) {
                    triangle.setOpacity(1.0);
                }
            });
            finalCheck.play();
        });
    }

    /**
     * Anime la disparition des triangles
     */
    private void animateTrianglesDisappear(Pane overlay, Duration duration) {
        Platform.runLater(() -> {
            double width = mainStage.getWidth() > 0 ? mainStage.getWidth() : savedWidth;
            double height = mainStage.getHeight() > 0 ? mainStage.getHeight() : savedHeight;
            
            double triangleSize = 35.0;
            double spacing = triangleSize * 1.2;
            
            int cols = (int) Math.ceil(width / spacing) + 2;
            int rows = (int) Math.ceil(height / spacing) + 2;
            
            java.util.List<Polygon> triangles = new java.util.ArrayList<>();
            Random random = new Random();
            
            Color whiteStroke = Color.WHITE;
            
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    double x = col * spacing + (random.nextDouble() - 0.5) * spacing * 0.3;
                    double y = row * spacing + (random.nextDouble() - 0.5) * spacing * 0.3;
                    
                    boolean pointingUp = (row + col) % 2 == 0;
                    Polygon triangle;
                    
                    if (pointingUp) {
                        triangle = new Polygon();
                        triangle.getPoints().addAll(
                            x, y + triangleSize,
                            x + triangleSize / 2, y,
                            x + triangleSize, y + triangleSize
                        );
                    } else {
                        triangle = new Polygon();
                        triangle.getPoints().addAll(
                            x, y,
                            x + triangleSize / 2, y + triangleSize,
                            x + triangleSize, y
                        );
                    }
                    
                    triangle.setFill(Color.BLACK);
                    triangle.setStroke(whiteStroke);
                    triangle.setStrokeWidth(2.0);
                    
                    Glow glow = new Glow(0.6);
                    triangle.setEffect(glow);
                    
                    triangle.setOpacity(1.0);
                    triangles.add(triangle);
                    overlay.getChildren().add(triangle);
                }
            }
            
            Collections.shuffle(triangles, random);
            
            java.util.List<FadeTransition> transitions = new java.util.ArrayList<>();
            
            for (int i = 0; i < triangles.size(); i++) {
                Polygon triangle = triangles.get(i);
                
                double randomProgress = random.nextDouble();
                Duration disappearDelay = duration.multiply(randomProgress);
                Duration fadeDuration = Duration.millis(80);
                
                FadeTransition fadeOut = new FadeTransition(fadeDuration, triangle);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setDelay(disappearDelay);
                transitions.add(fadeOut);
            }
            
            for (FadeTransition transition : transitions) {
                transition.play();
            }
            
            PauseTransition finalCheck = new PauseTransition(duration.add(Duration.millis(50)));
            finalCheck.setOnFinished(e -> {
                for (Polygon triangle : triangles) {
                    triangle.setOpacity(0.0);
                }
            });
            finalCheck.play();
        });
    }

    /**
     * Déplace la souris au centre
     */
    private void moveMouseToCenter(Robot robot) {
        lastMousePos = MouseInfo.getPointerInfo().getLocation();
        double x = mainStage.getX();
        double y = mainStage.getY();
        double width = mainStage.getWidth();
        int centerX = (int) x + (int) width / 2;
        int centerY = (int) y + 15;
        robot.mouseMove(centerX, centerY);
        System.out.println("🎯 Souris déplacée au centre (" + centerX + ", " + centerY + ")");
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
        
        if (missionsTab == null || miningTab == null) {
            System.out.println("⚠️ Changement d'onglet ignoré (onglets non initialisés)");
            return;
        }
        
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab == missionsTab) {
            tabPane.getSelectionModel().select(miningTab);
            System.out.println("📑 Changement vers onglet Mining");
        } else if (selectedTab == miningTab) {
            tabPane.getSelectionModel().select(missionsTab);
            System.out.println("📑 Changement vers onglet Missions");
        } else {
            tabPane.getSelectionModel().select(missionsTab);
            System.out.println("📑 Sélection de l'onglet Missions (aucun onglet sélectionné)");
        }
    }

    /**
     * Change vers l'onglet suivant (cycle: Missions -> Mining -> Missions)
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
        
        if (missionsTab == null || miningTab == null) {
            System.out.println("⚠️ Changement d'onglet ignoré (onglets non initialisés)");
            return;
        }
        
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab == missionsTab) {
            tabPane.getSelectionModel().select(miningTab);
            System.out.println("📑 Changement vers onglet Mining");
        } else if (selectedTab == miningTab) {
            tabPane.getSelectionModel().select(missionsTab);
            System.out.println("📑 Changement vers onglet Missions");
        } else {
            tabPane.getSelectionModel().select(missionsTab);
            System.out.println("📑 Sélection de l'onglet Missions (aucun onglet sélectionné)");
        }
    }
}

