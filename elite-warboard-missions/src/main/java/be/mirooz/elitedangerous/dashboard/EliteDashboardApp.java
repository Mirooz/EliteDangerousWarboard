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
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.*;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EliteDashboardApp extends Application {

    private LocalizationService localizationService = LocalizationService.getInstance();
    private Stage mainStage;
    private ComboBox<String> comboBox;
    private boolean hidden = false;

    private double savedWidth = 1200;
    private double savedHeight = 800;
    private double savedX = 0;
    private double savedY = 0;

    @Override
    public void start(Stage stage) {
        this.mainStage = stage;

        try {
            // --- Charge le FXML principal ---
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
            BorderPane dashboardRoot = loader.load();

            // --- Crée un conteneur global pour y superposer la ComboBox ---
            StackPane root = new StackPane();
            root.getChildren().add(dashboardRoot);

            // --- Ajoute la ComboBox ---
            comboBox = new ComboBox<>(FXCollections.observableArrayList(" "));
            comboBox.setPromptText(".");
            comboBox.setPrefWidth(1);
            comboBox.setPrefHeight(1);
            comboBox.setVisible(false);
            comboBox.setManaged(false);
            root.getChildren().add(comboBox);

            Scene scene = new Scene(root, savedWidth, savedHeight);

            // --- Feuille de style ---
            scene.getStylesheets().add(
                    Objects.requireNonNull(getClass().getResource("/css/elite-theme.css"))
                            .toExternalForm()
            );

            // --- Icône ---
            Image icon = new Image(
                    Objects.requireNonNull(getClass().getResource("/images/elite_dashboard_icon.png"))
                            .toExternalForm()
            );
            stage.getIcons().add(icon);

            // --- Configuration de la fenêtre ---
            String title = localizationService.getString("app.title");
            stage.setTitle(title);
            stage.setScene(scene);
            stage.setResizable(true);
            stage.setMinWidth(1000);
            stage.setMinHeight(600);
            // Lance en mode maximisé, mais pas plein écran
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

            // --- Active le hook clavier global (même hors focus) ---
            startGlobalKeyboardListener();

            System.out.println("✅ Application démarrée, touche Espace active même hors focus.");

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du chargement du Dashboard", e);
        }
    }

    /** Force la fenêtre à occuper tout l'écran sans passer en "plein écran" natif */
    private void maximizeWindow(Stage stage) {
        Screen screen = Screen.getPrimary(); // récupère l'écran principal (ou celui courant)
        var bounds = screen.getVisualBounds();

        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());

        System.out.println("🖥️ Fenêtre maximisée sans plein écran");
    }

    /** Active un hook clavier global pour la touche Espace */
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
    private Point lastMousePos = null; // position sauvegardée

    private void toggleWindowAndOpenCombo() {
        try {
            Robot robot = new Robot();

            if (hidden) {
                // --- Restaurer ---
                if (mainStage.isMaximized()) {
                    System.out.println("⚠️ Fenêtre était maximisée → on la restaure d'abord");
                    mainStage.setMaximized(false);
                }

                mainStage.setWidth(savedWidth);
                mainStage.setHeight(savedHeight);
                mainStage.setX(savedX);
                mainStage.setY(savedY);
                hidden = false;
                System.out.println("🔼 Fenêtre restaurée");
                // 🧭 Sauvegarde la position de la souris
                lastMousePos = MouseInfo.getPointerInfo().getLocation();
                System.out.println("💾 Souris sauvegardée à (" + lastMousePos.x + ", " + lastMousePos.y + ")");
                // 🎯 Déplace la souris au centre de l’écran principal
                moveMouseToCenter(robot);


            } else {
                // --- Réduit ---
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

            // --- Refresh Meta Link ---
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
        double x = mainStage.getX();
        double y = mainStage.getY();
        double width = mainStage.getWidth();
        int centerX=  (int) x+(int)width/2;
        int centerY= (int) y+100;
        robot.mouseMove(centerX, centerY);
        System.out.println("🎯 Souris déplacée au centre (" + centerX + ", " + centerY + ")");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
