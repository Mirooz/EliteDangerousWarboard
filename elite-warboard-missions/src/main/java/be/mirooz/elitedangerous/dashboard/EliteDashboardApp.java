package be.mirooz.elitedangerous.dashboard;

import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.service.journal.watcher.JournalTailService;
import be.mirooz.elitedangerous.dashboard.service.journal.watcher.JournalWatcherService;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.effect.Glow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;

import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class   EliteDashboardApp extends Application {

    private LocalizationService localizationService = LocalizationService.getInstance();
    private Stage mainStage;
    private ComboBox<String> comboBox;
    private boolean hidden = false;
    private StackPane rootPane; // Référence au root pour l'animation

    private double savedWidth = 1200;
    private double savedHeight = 800;
    private double savedX = 0;
    private double savedY = 0;

    private Point lastMousePos = null; // position sauvegardée
    private boolean isAnimating = false; // pour éviter les animations multiples simultanées

    @Override
    public void start(Stage stage) {
        this.mainStage = stage;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
            BorderPane dashboardRoot = loader.load();

            StackPane root = new StackPane(dashboardRoot);
            this.rootPane = root; // Sauvegarder la référence

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
            stage.setOpacity(1.0); // Initialiser l'opacité pour les animations
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
                // Initialisation du ControllerEnvironment peut être lente, on le fait dans un thread séparé
                System.out.println("🔍 Recherche des contrôleurs HOTAS...");
                Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();

                if (controllers.length == 0) {
                    System.out.println("❌ Aucun contrôleur détecté !");
                    return;
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

                System.out.println("✅ HOTAS actif : " +
                        activeControllers.stream().map(Controller::getName).collect(java.util.stream.Collectors.joining(", ")));

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

                            // État modifié → on log uniquement les actions importantes
                            if (prevValues[i] != value) {
                                prevValues[i] = value;

                                // 🎯 Hat switch vers le haut sur TWCS Throttle
                                if ("Commande de pouce".equalsIgnoreCase(name) && value == 0.25f && ctrl.getName().equals("TWCS Throttle")) {
                                    Platform.runLater(() -> toggleWindowAndOpenCombo());
                                }
                                // Note: Les logs verbeux ont été supprimés pour améliorer les performances
                            }
                        }
                    }

                    Thread.sleep(100); // 20 Hz → très léger, quasi zéro CPU
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
        // Éviter les animations multiples simultanées
        if (isAnimating) {
            return;
        }

        try {
            Robot robot = new Robot();

            if (hidden) {
                // Restaurer la fenêtre avec animation
                restoreWindowWithAnimation(robot);
            } else {
                // Réduire la fenêtre avec animation
                minimizeWindowWithAnimation(robot);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Restaure la fenêtre avec une animation de transition élégante
     * similaire aux dashboards Elite Dangerous
     */
    private void restoreWindowWithAnimation(Robot robot) {
        if (mainStage.isMaximized()) {
            mainStage.setMaximized(false);
        }

        // Mettre la fenêtre au premier plan
        mainStage.toFront();

        isAnimating = true;
        hidden = false;

        // Position et taille de départ (réduite au centre)
        double startWidth = 1;
        double startHeight = 1;
        double centerX = savedX + savedWidth / 2;
        double centerY = savedY + savedHeight / 2;
        double startX = centerX - startWidth / 2;
        double startY = centerY - startHeight / 2;

        // Position et taille de fin
        double endWidth = savedWidth;
        double endHeight = savedHeight;
        double endX = savedX;
        double endY = savedY;

        // Définir la position initiale
        mainStage.setX(startX);
        mainStage.setY(startY);
        mainStage.setWidth(startWidth);
        mainStage.setHeight(startHeight);
        mainStage.setOpacity(1.0); // Garder l'opacité à 1 pour voir l'animation

        // Créer l'interpolateur pour un mouvement fluide
        Interpolator interpolator = Interpolator.SPLINE(0.4, 0.0, 0.2, 1.0);
        
        // Durée de l'animation de taille/position (rapide)
        Duration windowDuration = Duration.millis(100);
        // Durée totale de l'animation des triangles (très rapide - 0.4 seconde)
        Duration triangleDuration = Duration.millis(200);

        // Créer l'animation de transition avec mise à jour manuelle pour la fenêtre
        Timeline windowTimeline = new Timeline();
        windowTimeline.setCycleCount(1);

        // Mettre à jour manuellement la taille et la position à chaque frame
        windowTimeline.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            double progress = Math.min(1.0, newTime.toMillis() / windowDuration.toMillis());
            double interpolatedProgress = interpolator.interpolate(0.0, 1.0, progress);
            
            // Calculer les valeurs interpolées
            double currentWidth = startWidth + (endWidth - startWidth) * interpolatedProgress;
            double currentHeight = startHeight + (endHeight - startHeight) * interpolatedProgress;
            double currentX = startX + (endX - startX) * interpolatedProgress;
            double currentY = startY + (endY - startY) * interpolatedProgress;
            
            // Mettre à jour la fenêtre
            mainStage.setWidth(currentWidth);
            mainStage.setHeight(currentHeight);
            mainStage.setX(currentX);
            mainStage.setY(currentY);
        });

        windowTimeline.setOnFinished(event -> {
            // S'assurer que les valeurs finales sont exactes
            mainStage.setWidth(endWidth);
            mainStage.setHeight(endHeight);
            mainStage.setX(endX);
            mainStage.setY(endY);
            // Afficher le comboBox 0.2 seconde après que la fenêtre a été redimensionnée
            PauseTransition delay = new PauseTransition(Duration.millis(200));
            delay.setOnFinished(e -> comboBox.show());
            delay.play();
        });

        // Créer et afficher l'animation des triangles
        Pane triangleOverlay = createTriangleAnimationOverlay();
        // Ajouter l'overlay au-dessus de tout (dernier élément = au-dessus dans StackPane)
        rootPane.getChildren().add(triangleOverlay);

        // Démarrer l'animation de la fenêtre immédiatement
        windowTimeline.play();

        // Démarrer l'animation des triangles (2 secondes)
        Timeline triangleTimeline = new Timeline();
        triangleTimeline.setCycleCount(1);
        
        // Animation pour faire disparaître l'overlay à la fin
        KeyValue overlayOpacityStart = new KeyValue(triangleOverlay.opacityProperty(), 1.0);
        KeyValue overlayOpacityEnd = new KeyValue(triangleOverlay.opacityProperty(), 0.0, Interpolator.EASE_OUT);
        KeyFrame overlayFrameStart = new KeyFrame(Duration.ZERO, overlayOpacityStart);
        KeyFrame overlayFrameEnd = new KeyFrame(Duration.millis(300), overlayOpacityEnd);
        
        triangleTimeline.getKeyFrames().addAll(overlayFrameStart, overlayFrameEnd);
        
        triangleTimeline.setOnFinished(event -> {
            rootPane.getChildren().remove(triangleOverlay);
            isAnimating = false;
            System.out.println("🔼 Fenêtre restaurée");
            // Cacher le comboBox quand l'animation complète est finie
            comboBox.hide();
            // Déplacer la souris au centre uniquement quand l'animation est complètement terminée
            moveMouseToCenter(robot);
        });

        // Démarrer l'animation des triangles après un court délai pour laisser la fenêtre s'agrandir
        PauseTransition delay = new PauseTransition(Duration.millis(100));
        delay.setOnFinished(e -> {
            animateTriangles(triangleOverlay, triangleDuration);
            // Démarrer la disparition de l'overlay à la fin de l'animation des triangles
            PauseTransition endDelay = new PauseTransition(triangleDuration);
            endDelay.setOnFinished(ev -> triangleTimeline.play());
            endDelay.play();
        });
        delay.play();
    }

    /**
     * Crée un overlay avec des triangles pour l'animation de chargement
     */
    private Pane createTriangleAnimationOverlay() {
        Pane overlay = new Pane();
        overlay.setStyle("-fx-background-color: #000000;"); // Fond noir
        overlay.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
        
        // Lier la taille de l'overlay à la taille de la scène
        if (mainStage.getScene() != null) {
            overlay.prefWidthProperty().bind(mainStage.getScene().widthProperty());
            overlay.prefHeightProperty().bind(mainStage.getScene().heightProperty());
            overlay.maxWidthProperty().bind(mainStage.getScene().widthProperty());
            overlay.maxHeightProperty().bind(mainStage.getScene().heightProperty());
        }
        
        return overlay;
    }

    /**
     * Anime les triangles qui apparaissent progressivement de manière chaotique
     */
    private void animateTriangles(Pane overlay, Duration duration) {
        // Attendre que l'overlay ait une taille avant de créer les triangles
        Platform.runLater(() -> {
            double width = mainStage.getWidth() > 0 ? mainStage.getWidth() : savedWidth;
            double height = mainStage.getHeight() > 0 ? mainStage.getHeight() : savedHeight;
            
            // Taille des triangles (plus grands)
            double triangleSize = 35.0;
            double spacing = triangleSize * 1.2; // Espacement entre les triangles
            
            // Calculer le nombre de triangles nécessaires pour couvrir l'écran
            int cols = (int) Math.ceil(width / spacing) + 2;
            int rows = (int) Math.ceil(height / spacing) + 2;
            int totalTriangles = cols * rows;
            
            // Créer tous les triangles
            java.util.List<Polygon> triangles = new java.util.ArrayList<>();
            Random random = new Random();
            
            // Couleur blanche pour les contours
            Color whiteStroke = Color.WHITE;
            
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    // Ajouter un peu de variation aléatoire dans la position
                    double x = col * spacing + (random.nextDouble() - 0.5) * spacing * 0.3;
                    double y = row * spacing + (random.nextDouble() - 0.5) * spacing * 0.3;
                    
                    // Alterner entre triangles pointant vers le haut et vers le bas
                    boolean pointingUp = (row + col) % 2 == 0;
                    Polygon triangle;
                    
                    if (pointingUp) {
                        // Triangle pointant vers le haut
                        triangle = new Polygon();
                        triangle.getPoints().addAll(
                            x, y + triangleSize,                    // Bas gauche
                            x + triangleSize / 2, y,                 // Haut (pointe)
                            x + triangleSize, y + triangleSize      // Bas droit
                        );
                    } else {
                        // Triangle pointant vers le bas
                        triangle = new Polygon();
                        triangle.getPoints().addAll(
                            x, y,                                    // Haut gauche
                            x + triangleSize / 2, y + triangleSize, // Bas (pointe)
                            x + triangleSize, y                      // Haut droit
                        );
                    }
                    
                    // Configurer le triangle avec intérieur noir et bord blanc
                    triangle.setFill(Color.BLACK); // Intérieur noir
                    triangle.setStroke(whiteStroke); // Bord blanc
                    triangle.setStrokeWidth(2.0); // Épaisseur du contour
                    
                    // Ajouter un effet de lueur pour le bord blanc
                    Glow glow = new Glow(0.6);
                    triangle.setEffect(glow);
                    
                    triangle.setOpacity(0.0);
                    triangles.add(triangle);
                    overlay.getChildren().add(triangle);
                }
            }
            
            // Mélanger l'ordre des triangles pour un effet chaotique
            Collections.shuffle(triangles, random);
            
            // Animer l'apparition progressive des triangles de manière chaotique
            // Utiliser des FadeTransition pour chaque triangle avec des délais aléatoires
            java.util.List<FadeTransition> transitions = new java.util.ArrayList<>();
            
            for (int i = 0; i < triangles.size(); i++) {
                Polygon triangle = triangles.get(i);
                
                // Délai aléatoire réparti sur toute la durée (mais plus rapide)
                double randomProgress = random.nextDouble(); // 0.0 à 1.0 de manière aléatoire
                Duration appearDelay = duration.multiply(randomProgress);
                
                // Durée de fade pour l'effet de moins en moins transparent (plus rapide)
                Duration fadeDuration = Duration.millis(80); // Fade rapide pour un effet dynamique
                
                // Créer une transition pour faire apparaître ce triangle
                FadeTransition fadeIn = new FadeTransition(fadeDuration, triangle);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.setDelay(appearDelay);
                transitions.add(fadeIn);
            }
            
            // Démarrer toutes les transitions
            for (FadeTransition transition : transitions) {
                transition.play();
            }
            
            // S'assurer que tous les triangles sont visibles à la fin
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
     * Réduit la fenêtre avec une animation de transition élégante
     * similaire aux dashboards Elite Dangerous
     */
    private void minimizeWindowWithAnimation(Robot robot) {
        // Sauvegarder les valeurs actuelles
        savedWidth = mainStage.getWidth();
        savedHeight = mainStage.getHeight();
        savedX = mainStage.getX();
        savedY = mainStage.getY();
        
        isAnimating = true;
        hidden = true;

        // Position et taille de départ
        double startWidth = savedWidth;
        double startHeight = savedHeight;
        double startX = savedX;
        double startY = savedY;

        // Position et taille de fin (réduite au centre)
        double centerX = startX + startWidth / 2;
        double centerY = startY + startHeight / 2;
        double endWidth = 1;
        double endHeight = 1;
        double endX = centerX - endWidth / 2;
        double endY = centerY - endHeight / 2;

        // Créer l'interpolateur pour un mouvement fluide
        Interpolator interpolator = Interpolator.SPLINE(0.4, 0.0, 0.2, 1.0);
        
        // Durée de l'animation de taille/position (rapide)
        Duration windowDuration = Duration.millis(200);
        // Durée totale de l'animation des triangles (très rapide - 0.2 seconde)
        Duration triangleDuration = Duration.millis(400);

        // Créer et afficher l'animation des triangles qui disparaissent
        Pane triangleOverlay = createTriangleAnimationOverlay();
        // Ajouter l'overlay au-dessus de tout
        rootPane.getChildren().add(triangleOverlay);
        
        // Créer les triangles et les faire disparaître
        animateTrianglesDisappear(triangleOverlay, triangleDuration);

        // Créer l'animation de transition avec mise à jour manuelle pour la fenêtre
        Timeline windowTimeline = new Timeline();
        windowTimeline.setCycleCount(1);

        // Mettre à jour manuellement la taille et la position à chaque frame
        windowTimeline.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            double progress = Math.min(1.0, newTime.toMillis() / windowDuration.toMillis());
            double interpolatedProgress = interpolator.interpolate(0.0, 1.0, progress);
            
            // Calculer les valeurs interpolées
            double currentWidth = startWidth + (endWidth - startWidth) * interpolatedProgress;
            double currentHeight = startHeight + (endHeight - startHeight) * interpolatedProgress;
            double currentX = startX + (endX - startX) * interpolatedProgress;
            double currentY = startY + (endY - startY) * interpolatedProgress;
            
            // Mettre à jour la fenêtre
            mainStage.setWidth(currentWidth);
            mainStage.setHeight(currentHeight);
            mainStage.setX(currentX);
            mainStage.setY(currentY);
        });

        windowTimeline.setOnFinished(event -> {
            // S'assurer que les valeurs finales sont exactes
            mainStage.setWidth(endWidth);
            mainStage.setHeight(endHeight);
            mainStage.setX(endX);
            mainStage.setY(endY);
        });

        // Animation pour faire disparaître l'overlay à la fin
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
            // 🔁 Remet la souris à sa position d'origine
            if (lastMousePos != null) {
                robot.mouseMove(lastMousePos.x, lastMousePos.y);
                System.out.println("🖱️ Souris restaurée à (" + lastMousePos.x + ", " + lastMousePos.y + ")");
            }
            // Animation du comboBox après que la fenêtre soit disparue (pour le focus)
            PauseTransition delay = new PauseTransition(Duration.millis(50));
            delay.setOnFinished(e -> {
                comboBox.show();
                PauseTransition delay2 = new PauseTransition(Duration.millis(50));
                delay2.setOnFinished(ev -> comboBox.hide());
                delay2.play();
            });
            delay.play();
        });

        // Attendre que l'animation des triangles soit complètement terminée avant de réduire la fenêtre
        // Ajouter un petit délai supplémentaire pour s'assurer que tout est visible
        PauseTransition waitForTriangles = new PauseTransition(triangleDuration.add(Duration.millis(200)));
        waitForTriangles.setOnFinished(e -> {
            // Maintenant réduire la fenêtre
            windowTimeline.play();
            // Démarrer la disparition de l'overlay après la réduction de la fenêtre
            PauseTransition endDelay = new PauseTransition(windowDuration);
            endDelay.setOnFinished(ev -> triangleTimeline.play());
            endDelay.play();
        });
        waitForTriangles.play();
    }

    /**
     * Anime la disparition progressive des triangles de manière chaotique
     */
    private void animateTrianglesDisappear(Pane overlay, Duration duration) {
        // Attendre que l'overlay ait une taille avant de créer les triangles
        Platform.runLater(() -> {
            double width = mainStage.getWidth() > 0 ? mainStage.getWidth() : savedWidth;
            double height = mainStage.getHeight() > 0 ? mainStage.getHeight() : savedHeight;
            
            // Taille des triangles (plus grands)
            double triangleSize = 35.0;
            double spacing = triangleSize * 1.2; // Espacement entre les triangles
            
            // Calculer le nombre de triangles nécessaires pour couvrir l'écran
            int cols = (int) Math.ceil(width / spacing) + 2;
            int rows = (int) Math.ceil(height / spacing) + 2;
            int totalTriangles = cols * rows;
            
            // Créer tous les triangles
            java.util.List<Polygon> triangles = new java.util.ArrayList<>();
            Random random = new Random();
            
            // Couleur blanche pour les contours
            Color whiteStroke = Color.WHITE;
            
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < cols; col++) {
                    // Ajouter un peu de variation aléatoire dans la position
                    double x = col * spacing + (random.nextDouble() - 0.5) * spacing * 0.3;
                    double y = row * spacing + (random.nextDouble() - 0.5) * spacing * 0.3;
                    
                    // Alterner entre triangles pointant vers le haut et vers le bas
                    boolean pointingUp = (row + col) % 2 == 0;
                    Polygon triangle;
                    
                    if (pointingUp) {
                        // Triangle pointant vers le haut
                        triangle = new Polygon();
                        triangle.getPoints().addAll(
                            x, y + triangleSize,                    // Bas gauche
                            x + triangleSize / 2, y,                 // Haut (pointe)
                            x + triangleSize, y + triangleSize      // Bas droit
                        );
                    } else {
                        // Triangle pointant vers le bas
                        triangle = new Polygon();
                        triangle.getPoints().addAll(
                            x, y,                                    // Haut gauche
                            x + triangleSize / 2, y + triangleSize, // Bas (pointe)
                            x + triangleSize, y                      // Haut droit
                        );
                    }
                    
                    // Configurer le triangle avec intérieur noir et bord blanc
                    triangle.setFill(Color.BLACK); // Intérieur noir
                    triangle.setStroke(whiteStroke); // Bord blanc
                    triangle.setStrokeWidth(2.0); // Épaisseur du contour
                    
                    // Ajouter un effet de lueur pour le bord blanc
                    Glow glow = new Glow(0.6);
                    triangle.setEffect(glow);
                    
                    // Les triangles commencent visibles (opacité 1.0)
                    triangle.setOpacity(1.0);
                    triangles.add(triangle);
                    overlay.getChildren().add(triangle);
                }
            }
            
            // Mélanger l'ordre des triangles pour un effet chaotique
            Collections.shuffle(triangles, random);
            
            // Animer la disparition progressive des triangles de manière chaotique
            // Utiliser des FadeTransition pour chaque triangle avec des délais aléatoires
            java.util.List<FadeTransition> transitions = new java.util.ArrayList<>();
            
            for (int i = 0; i < triangles.size(); i++) {
                Polygon triangle = triangles.get(i);
                
                // Délai aléatoire réparti sur toute la durée
                double randomProgress = random.nextDouble(); // 0.0 à 1.0 de manière aléatoire
                Duration disappearDelay = duration.multiply(randomProgress);
                
                // Durée de fade pour l'effet de plus en plus transparent (plus rapide)
                Duration fadeDuration = Duration.millis(80); // Fade rapide pour un effet dynamique
                
                // Créer une transition pour faire disparaître ce triangle
                FadeTransition fadeOut = new FadeTransition(fadeDuration, triangle);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setDelay(disappearDelay);
                transitions.add(fadeOut);
            }
            
            // Démarrer toutes les transitions
            for (FadeTransition transition : transitions) {
                transition.play();
            }
            
            // S'assurer que tous les triangles sont invisibles à la fin
            PauseTransition finalCheck = new PauseTransition(duration.add(Duration.millis(50)));
            finalCheck.setOnFinished(e -> {
                for (Polygon triangle : triangles) {
                    triangle.setOpacity(0.0);
                }
            });
            finalCheck.play();
        });
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
