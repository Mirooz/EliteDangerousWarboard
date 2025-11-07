package be.mirooz.elitedangerous.dashboard;

import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.service.journal.watcher.JournalTailService;
import be.mirooz.elitedangerous.dashboard.service.journal.watcher.JournalWatcherService;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;

import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class   EliteDashboardApp extends Application {

    private LocalizationService localizationService = LocalizationService.getInstance();
    private Stage mainStage;
    private ComboBox<String> comboBox;
    private boolean hidden = false;

    private double savedWidth = 1200;
    private double savedHeight = 800;
    private double savedX = 0;
    private double savedY = 0;

    private Point lastMousePos = null; // position sauvegardée

    @Override
    public void start(Stage stage) {
        this.mainStage = stage;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
            BorderPane dashboardRoot = loader.load();

            StackPane root = new StackPane(dashboardRoot);

            comboBox = new ComboBox<>(FXCollections.observableArrayList(" "));
            comboBox.setPromptText(".");
            comboBox.setPrefWidth(1);
            comboBox.setPrefHeight(1);
            comboBox.setVisible(false);
            comboBox.setManaged(false);
            root.getChildren().add(comboBox);

            Scene scene = new Scene(root, savedWidth, savedHeight);

            scene.getStylesheets().add(
                    Objects.requireNonNull(getClass().getResource("/css/elite-theme.css"))
                            .toExternalForm()
            );

            Image icon = new Image(
                    Objects.requireNonNull(getClass().getResource("/images/elite_dashboard_icon.png"))
                            .toExternalForm()
            );
            stage.getIcons().add(icon);

            String title = localizationService.getString("app.title");
            stage.setTitle(title);
            stage.setScene(scene);
            stage.setResizable(true);
            stage.setMinWidth(1000);
            stage.setMinHeight(600);
            maximizeWindow(stage);

            stage.setOnCloseRequest(event -> {
                System.out.println("Arrêt des services de journal...");
                JournalTailService.getInstance().stop();
                JournalWatcherService.getInstance().stop();
                try {
                    GlobalScreen.unregisterNativeHook();
                } catch (Exception ignored) {}
                Platform.exit();
                System.exit(0);
            });

            stage.show();

            startGlobalKeyboardListener(); // toujours utile
            startHotasListener(); // 👈 nouveau

            System.out.println("✅ Application démarrée (clavier + HOTAS actifs)");

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du chargement du Dashboard", e);
        }
    }

    private void maximizeWindow(Stage stage) {
        var bounds = Screen.getPrimary().getVisualBounds();
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
    }

    /** --- HOTAS listener avec JInput --- */
    private void startHotasListener() {
        new Thread(() -> {
            try {
                Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();

                if (controllers.length == 0) {
                    System.out.println("❌ Aucun contrôleur détecté !");
                    return;
                }

                System.out.println("🎮 Contrôleurs détectés :");
                for (Controller c : controllers) {
                    System.out.println("   ➜ " + c.getName() + " (" + c.getType() + ")");
                }

                // On garde uniquement les périphériques pertinents
                var activeControllers = Arrays.stream(controllers)
                        .filter(c -> c.getType() == Controller.Type.STICK
                                || c.getType() == Controller.Type.GAMEPAD
                                || c.getType() == Controller.Type.WHEEL)
                        .toList();

                if (activeControllers.isEmpty()) {
                    System.out.println("⚠️ Aucun HOTAS, manette ou volant détecté");
                    return;
                }

                System.out.println("✅ Suivi des contrôleurs actifs : " +
                        activeControllers.stream().map(Controller::getName).toList());

                // État précédent de chaque composant pour chaque contrôleur
                Map<Controller, float[]> lastStates = new HashMap<>();

                for (Controller ctrl : activeControllers) {
                    lastStates.put(ctrl, new float[ctrl.getComponents().length]);
                }

                while (true) {
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

                            // Ignore le bruit analogique
                            if (Math.abs(value) < 0.05f) value = 0.0f;

                            // État modifié → on log
                            if (prevValues[i] != value) {
                                prevValues[i] = value;

                                if (!name.isBlank()) {
                                    System.out.printf("🎛️ [%s] %s → %.2f%n", ctrl.getName(), name, value);
                                }

                                // 🎯 Exemple : hat switch vers le haut sur n'importe quel contrôleur
                                if ("Commande de pouce".equalsIgnoreCase(name) && value == 0.25f && ctrl.getName().equals("TWCS Throttle")) {
                                    Platform.runLater(() -> toggleWindowAndOpenCombo());
                                }

                                // Exemple : bouton pressé / relâché
                                if (value == 1.0f) {
                                    System.out.printf("🟢 [%s] Bouton pressé: %s%n", ctrl.getName(), name);
                                } else if (value == 0.0f) {
                                    System.out.printf("⚫ [%s] Bouton relâché: %s%n", ctrl.getName(), name);
                                }
                            }
                        }
                    }

                    Thread.sleep(50); // 20 Hz → très léger, quasi zéro CPU
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "MultiControllerListenerThread").start();
    }


    /** --- Clavier global --- */
    private void startGlobalKeyboardListener() {
        try {
            Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
            logger.setLevel(Level.OFF);
            GlobalScreen.registerNativeHook();

            GlobalScreen.addNativeKeyListener(new NativeKeyListener() {
                @Override
                public void nativeKeyPressed(NativeKeyEvent e) {
                    if (e.getKeyCode() == NativeKeyEvent.VC_SPACE) {
                        Platform.runLater(() -> toggleWindowAndOpenCombo());
                    }
                }
            });

            System.out.println("🎧 Hook clavier global actif (touche Espace).");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /** --- Toggle fenetre --- */
    private void toggleWindowAndOpenCombo() {
        try {
            Robot robot = new Robot();

            if (hidden) {
                if (mainStage.isMaximized()) mainStage.setMaximized(false);

                mainStage.setWidth(savedWidth);
                mainStage.setHeight(savedHeight);
                mainStage.setX(savedX);
                mainStage.setY(savedY);
                hidden = false;
                System.out.println("🔼 Fenêtre restaurée");
                moveMouseToCenter(robot);

            } else {
                savedWidth = mainStage.getWidth();
                savedHeight = mainStage.getHeight();
                savedX = mainStage.getX();
                savedY = mainStage.getY();
                hidden = true;
                // 🔁 Remet la souris à sa position d'origine
                if (lastMousePos != null) {
                    robot.mouseMove(lastMousePos.x, lastMousePos.y);
                    System.out.println("🖱️ Souris restaurée à (" + lastMousePos.x + ", " + lastMousePos.y + ")");
                }

                mainStage.setWidth(1);
                mainStage.setHeight(1);
                System.out.println("🔽 Fenêtre réduite");
            }

            PauseTransition delay = new PauseTransition(Duration.seconds(0.1));
            delay.setOnFinished(event -> comboBox.show());
            delay.play();

            PauseTransition delay2 = new PauseTransition(Duration.seconds(0.1));
            delay2.setOnFinished(event -> comboBox.hide());
            delay2.play();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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

    public static void main(String[] args) {
        launch(args);
    }
}
