package be.mirooz.elitedangerous.dashboard.controller.ui.component.mining;

import be.mirooz.elitedangerous.dashboard.model.events.ProspectedAsteroid;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.service.MiningService;
import be.mirooz.elitedangerous.dashboard.service.PreferencesService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.URL;
import java.util.*;

/**
 * Composant pour l'affichage et la navigation des prospecteurs actuels
 * <p>
 * Ce composant gère :
 * - L'affichage du prospecteur actuel
 * - La navigation entre les prospecteurs (précédent/suivant)
 * - Le compteur de prospecteurs
 * - L'affichage du message "aucun prospecteur"
 */
public class CurrentProspectorComponent implements Initializable {

    // Services
    private final MiningService miningService = MiningService.getInstance();
    private final LocalizationService localizationService = LocalizationService.getInstance();
    private final PreferencesService preferencesService = PreferencesService.getInstance();
    
    // Clés pour les préférences de l'overlay
    private static final String OVERLAY_WIDTH_KEY = "overlay.width";
    private static final String OVERLAY_HEIGHT_KEY = "overlay.height";
    private static final String OVERLAY_OPACITY_KEY = "overlay.opacity";

    // Composants FXML
    @FXML
    private VBox currentProspectorContent;
    @FXML
    private VBox noProspectorContainer;
    @FXML
    private Button lastProspectorButton;
    @FXML
    private Button previousProspectorButton;
    @FXML
    private Button nextProspectorButton;
    @FXML
    private Label prospectorCounterLabel;
    @FXML
    private Label currentProspectorLabel;
    @FXML
    private Button overlayButton;

    private Stage overlayStage;
    private double overlayOpacity;

    // Variables pour la navigation
    private List<ProspectedAsteroid> allProspectors = new ArrayList<>();
    private int currentProspectorIndex = 0;

    // Callback pour notifier le parent des changements
    private Runnable onProspectorChanged;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateProspectors();
        updateTranslations();
        
        // S'assurer que le bouton overlay est initialisé avec le bon texte
        Platform.runLater(() -> {
            updateOverlayButtonText();
        });
        
        // Écouter les changements de langue
        localizationService.addLanguageChangeListener(locale -> updateTranslations());
    }

    /**
     * Met à jour l'affichage des prospecteurs avec navigation
     */
    public void updateProspectors() {
        Platform.runLater(() -> {
            Deque<ProspectedAsteroid> prospectors = miningService.getAllProspectors();

            // Convertir en liste pour faciliter la navigation et inverser l'ordre
            allProspectors.clear();
            allProspectors.addAll(prospectors);
            
            // Inverser la liste pour que le dernier soit en premier
            java.util.Collections.reverse(allProspectors);

            if (allProspectors.isEmpty()) {
                showNoProspector();
                return;
            }

            // S'assurer que l'index est valide
            if (currentProspectorIndex >= allProspectors.size()) {
                currentProspectorIndex = 0;
            }

            showCurrentProspector();
            updateNavigationButtons();
        });
    }

    /**
     * Affiche le prospecteur actuel
     */
    private void showCurrentProspector() {
        if (allProspectors.isEmpty() || currentProspectorIndex >= allProspectors.size()) {
            showNoProspector();
            return;
        }

        noProspectorContainer.setVisible(false);
        noProspectorContainer.setManaged(false);


        // Vider le contenu actuel
        currentProspectorContent.getChildren().clear();

        // Afficher le prospecteur actuel (toujours en grand format)
        ProspectedAsteroid currentProspector = allProspectors.get(currentProspectorIndex);
        VBox card = ProspectorCardComponent.createProspectorCard(currentProspector, true);

        // Fixer la taille pour éviter les changements de layout
        card.setMinSize(500, 450);
        card.setPrefSize(500, 450);

        currentProspectorContent.getChildren().add(card);

        // Mettre à jour le compteur
        updateProspectorCounter();

        // Notifier le parent du changement
        if (onProspectorChanged != null) {
            onProspectorChanged.run();
        }
    }

    /**
     * Affiche le message "aucun prospecteur"
     */
    private void showNoProspector() {
        noProspectorContainer.setVisible(false);
        noProspectorContainer.setManaged(false);
        updateNavigationButtons();
    }

    /**
     * Met à jour les boutons de navigation
     */
    private void updateNavigationButtons() {
        boolean hasProspectors = !allProspectors.isEmpty();
        boolean canGoPrevious = hasProspectors && currentProspectorIndex > 0;
        boolean canGoNext = hasProspectors && currentProspectorIndex < allProspectors.size() - 1;
        boolean canGoLast = hasProspectors && currentProspectorIndex > 0; // Désactivé quand on est déjà au premier (dernier prospecteur)

        lastProspectorButton.setDisable(!canGoLast);
        previousProspectorButton.setDisable(!canGoPrevious);
        nextProspectorButton.setDisable(!canGoNext);

        if (hasProspectors) {
            updateProspectorCounter();
        } else {
            prospectorCounterLabel.setText("0/0");
        }
    }

    /**
     * Met à jour le compteur de prospecteurs
     */
    private void updateProspectorCounter() {
        if (!allProspectors.isEmpty()) {
            prospectorCounterLabel.setText(String.format("%d/%d", currentProspectorIndex + 1, allProspectors.size()));
        }
    }

    /**
     * Affiche le dernier prospecteur (qui est maintenant le premier de la liste inversée)
     */
    @FXML
    public void showLastProspector() {
        if (!allProspectors.isEmpty()) {
            currentProspectorIndex = 0; // Le premier élément est maintenant le dernier prospecteur
            showCurrentProspector();
            updateNavigationButtons();
        }
    }

    /**
     * Affiche le prospecteur précédent
     */
    @FXML
    public void showPreviousProspector() {
        if (currentProspectorIndex > 0) {
            currentProspectorIndex--;
            showCurrentProspector();
            updateNavigationButtons();
        }
    }

    /**
     * Affiche le prospecteur suivant
     */
    @FXML
    public void showNextProspector() {
        if (currentProspectorIndex < allProspectors.size() - 1) {
            currentProspectorIndex++;
            showCurrentProspector();
            updateNavigationButtons();
        }
    }

    /**
     * Met à jour toutes les traductions de l'interface
     */
    private void updateTranslations() {
        if (currentProspectorLabel != null) {
            currentProspectorLabel.setText(getTranslation("mining.current_prospector"));
        }
        if (lastProspectorButton != null) {
            lastProspectorButton.setText(getTranslation("mining.last"));
        }
        updateOverlayButtonText();
    }

    /**
     * Met à jour le texte du bouton overlay selon l'état de la fenêtre
     */
    private void updateOverlayButtonText() {
        if (overlayButton != null) {
            String text;
            String icon;
            
            if (overlayStage != null && overlayStage.isShowing()) {
                text = getTranslation("mining.overlay_close");
                icon = "✖"; // Croix pour fermer
            } else {
                text = getTranslation("mining.overlay_open");
                icon = "🗔"; // Icône de fenêtre pour ouvrir
            }
            
            // Vérifier si la traduction a fonctionné, sinon utiliser un texte par défaut
            if (text == null || text.startsWith("mining.")) {
                if (overlayStage != null && overlayStage.isShowing()) {
                    text = "Fermer Overlay";
                } else {
                    text = "Ouvrir Overlay";
                }
            }
            
            // Combiner l'icône et le texte
            overlayButton.setText(icon + " " + text);
        }
    }

    /**
     * Crée une carte de prospecteur avec le style d'overlay et la transparence appropriée
     */
    private VBox createOverlayCard(ProspectedAsteroid prospector) {
        VBox card = ProspectorCardComponent.createProspectorCard(prospector, true);
        card.getStyleClass().add("mirror-overlay");

        
        return card;
    }

    /**
     * Sauvegarde la taille et la transparence du background de l'overlay dans les préférences
     */
    private void saveOverlaySize() {
        if (overlayStage != null && overlayStage.isShowing()) {
            preferencesService.setPreference(OVERLAY_WIDTH_KEY, String.valueOf((int)overlayStage.getWidth()));
            preferencesService.setPreference(OVERLAY_HEIGHT_KEY, String.valueOf((int)overlayStage.getHeight()));
            preferencesService.setPreference(OVERLAY_OPACITY_KEY, String.valueOf(overlayOpacity));
            System.out.println("💾 Taille et transparence de l'overlay sauvegardées: " + 
                (int)overlayStage.getWidth() + "x" + (int)overlayStage.getHeight() + 
                " (opacité overlay: " + String.format("%.2f", overlayOpacity) + ")");
        }
    }

    /**
     * Récupère une traduction depuis le LocalizationService
     */
    private String getTranslation(String key) {
        String translation = localizationService.getString(key);
        if (translation == null || translation.equals(key)) {
            System.out.println("⚠️ Traduction manquante pour la clé: " + key);
            return key; // Retourner la clé si la traduction n'est pas trouvée
        }
        return translation;
    }

    // Getters et setters
    public void setOnProspectorChanged(Runnable onProspectorChanged) {
        this.onProspectorChanged = onProspectorChanged;
    }

    /**
     * Retourne le prospecteur actuellement affiché
     */
    public ProspectedAsteroid getCurrentProspector() {
        if (allProspectors.isEmpty() || currentProspectorIndex >= allProspectors.size()) {
            return null;
        }
        return allProspectors.get(currentProspectorIndex);
    }

    /**
     * Retourne la liste de tous les prospecteurs
     */
    public List<ProspectedAsteroid> getAllProspectors() {
        return new ArrayList<>(allProspectors);
    }

    /**
     * Retourne l'index du prospecteur actuel
     */
    public int getCurrentProspectorIndex() {
        return currentProspectorIndex;
    }

    /**
     * Retourne le nombre total de prospecteurs
     */
    public int getTotalProspectorsCount() {
        return allProspectors.size();
    }

    /**
     * Force le rafraîchissement de l'affichage
     */
    public void refresh() {
        updateProspectors();
    }

    @FXML
    public void showProspectorOverlay() {
        // Rien à afficher si pas de prospecteur
        if (getCurrentProspector() == null) {
            System.out.println("⚠️ Aucun prospecteur à afficher dans l'overlay.");
            return;
        }

        // Si la fenêtre est déjà ouverte, on la ferme
        if (overlayStage != null && overlayStage.isShowing()) {
            // Sauvegarder la taille actuelle avant de fermer
            saveOverlaySize();
            overlayStage.close();
            overlayStage = null;
            updateOverlayButtonText();
            return;
        }

        // --- Création de la fenêtre overlay ---
        overlayStage = new Stage();
        overlayStage.initStyle(StageStyle.TRANSPARENT);
        overlayStage.setAlwaysOnTop(true);
        overlayStage.setTitle("Prospector Overlay");
        overlayStage.setResizable(true);

        // Définir la taille par défaut et minimale
        overlayStage.setMinWidth(200);
        overlayStage.setMinHeight(150);
        
        // Restaurer la taille sauvegardée ou utiliser les valeurs par défaut
        String savedWidthStr = preferencesService.getPreference(OVERLAY_WIDTH_KEY, "600");
        String savedHeightStr = preferencesService.getPreference(OVERLAY_HEIGHT_KEY, "500");
        String savedOpacityStr = preferencesService.getPreference(OVERLAY_OPACITY_KEY, "0.92");
        
        double savedWidth = Double.parseDouble(savedWidthStr);
        double savedHeight = Double.parseDouble(savedHeightStr);
        double savedBackgroundOpacity = Double.parseDouble(savedOpacityStr);
        
        // S'assurer que la taille sauvegardée respecte les tailles minimales
        overlayStage.setWidth(Math.max(savedWidth, overlayStage.getMinWidth()));
        overlayStage.setHeight(Math.max(savedHeight, overlayStage.getMinHeight()));
        overlayStage.setOpacity(1.0); // Overlay toujours opaque

        // On crée une carte identique au panel principal (clone visuel)
        VBox mirrorCard = createOverlayCard(getCurrentProspector());
        
        // Ajouter un indicateur de redimensionnement dans le coin inférieur droit
        javafx.scene.control.Label resizeHandle = new javafx.scene.control.Label("⤡");
        resizeHandle.getStyleClass().add("resize-handle");
        resizeHandle.setStyle("-fx-text-fill: gold; -fx-font-size: 16px; -fx-font-weight: bold; -fx-alignment: center;");
        resizeHandle.setOpacity(0.0); // Masquer par défaut
        
        // Ajouter un curseur de transparence en bas à droite
        Slider opacitySlider = new Slider(0.05, 1.0, savedBackgroundOpacity);
        opacitySlider.setOrientation(javafx.geometry.Orientation.VERTICAL);
        opacitySlider.setPrefWidth(20);
        opacitySlider.setPrefHeight(80);
        opacitySlider.setOpacity(0.0); // Masquer par défaut
        opacitySlider.getStyleClass().add("opacity-slider");
        
        // Configuration pour des valeurs plus précises
        opacitySlider.setMajorTickUnit(0.2);
        opacitySlider.setMinorTickCount(1);
        opacitySlider.setShowTickLabels(false);
        opacitySlider.setShowTickMarks(false);
        opacitySlider.setSnapToTicks(false); // Permettre des valeurs intermédiaires

        // Positionner les éléments dans le coin inférieur droit
        javafx.scene.layout.StackPane stackPane = new javafx.scene.layout.StackPane();
        stackPane.getChildren().addAll(mirrorCard, resizeHandle, opacitySlider);
        StackPane.setAlignment(resizeHandle, javafx.geometry.Pos.BOTTOM_RIGHT);
        StackPane.setAlignment(opacitySlider, javafx.geometry.Pos.BOTTOM_RIGHT);
        StackPane.setMargin(opacitySlider, new javafx.geometry.Insets(0, 30, 0, 0)); // Décaler vers la gauche
        stackPane.setPickOnBounds(true);
        
        // Appliquer la transparence initiale du StackPane
        updatePaneStyle(savedBackgroundOpacity, stackPane);

        // Écouter les changements de transparence
        opacitySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double opacity = Math.max(newVal.doubleValue(), 0.01);
            updatePaneStyle(opacity, stackPane);
            this.overlayOpacity = opacity;
        });
        Scene scene = new Scene(stackPane);
        overlayStage.setScene(scene);
        scene.setFill(Color.TRANSPARENT);
        overlayStage.setOpacity(1.0); // Overlay toujours opaque

        // Appliquer les mêmes styles CSS que le reste de l’app
        scene.getStylesheets().add(getClass().getResource("/css/elite-theme.css").toExternalForm());

        // Permettre le déplacement et le redimensionnement de la fenêtre
        final double[] offset = new double[2];
        final double[] resizeOffset = new double[2];
        final boolean[] isResizing = {false};
        
        scene.setOnMousePressed(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                offset[0] = e.getScreenX() - overlayStage.getX();
                offset[1] = e.getScreenY() - overlayStage.getY();
                
                // Vérifier si on est dans une zone de redimensionnement (coin inférieur droit)
                double sceneWidth = scene.getWidth();
                double sceneHeight = scene.getHeight();
                double mouseX = e.getSceneX();
                double mouseY = e.getSceneY();
                
                // Zone de redimensionnement : coin inférieur droit (25x25 pixels)
                if (mouseX >= sceneWidth - 25 && mouseY >= sceneHeight - 25) {
                    isResizing[0] = true;
                    resizeOffset[0] = e.getScreenX();
                    resizeOffset[1] = e.getScreenY();
                }
            }
        });
        
        scene.setOnMouseDragged(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                if (isResizing[0]) {
                    // Redimensionnement
                    double deltaX = e.getScreenX() - resizeOffset[0];
                    double deltaY = e.getScreenY() - resizeOffset[1];
                    
                    double newWidth = overlayStage.getWidth() + deltaX;
                    double newHeight = overlayStage.getHeight() + deltaY;
                    
                    // Respecter les tailles minimales
                    if (newWidth >= overlayStage.getMinWidth()) {
                        overlayStage.setWidth(newWidth);
                    }
                    if (newHeight >= overlayStage.getMinHeight()) {
                        overlayStage.setHeight(newHeight);
                    }
                    
                    resizeOffset[0] = e.getScreenX();
                    resizeOffset[1] = e.getScreenY();
                } else {
                    // Déplacement
                    overlayStage.setX(e.getScreenX() - offset[0]);
                    overlayStage.setY(e.getScreenY() - offset[1]);
                }
            }
        });
        
        scene.setOnMouseReleased(e -> {
            isResizing[0] = false;
        });
        
        // Gestion du curseur et de la visibilité des contrôles
        scene.setOnMouseMoved(e -> {
            double sceneWidth = scene.getWidth();
            double sceneHeight = scene.getHeight();
            double mouseX = e.getSceneX();
            double mouseY = e.getSceneY();
            
            // Zone de redimensionnement : coin inférieur droit (25x25 pixels)
            if (mouseX >= sceneWidth - 25 && mouseY >= sceneHeight - 25) {
                scene.setCursor(javafx.scene.Cursor.SE_RESIZE);
                resizeHandle.setOpacity(1.0); // Afficher l'icône de redimensionnement
                opacitySlider.setOpacity(0.8); // Afficher le curseur de transparence
            } else {
                scene.setCursor(javafx.scene.Cursor.DEFAULT);
                resizeHandle.setOpacity(0.8); // Afficher l'icône partout dans l'overlay
                opacitySlider.setOpacity(0.8); // Afficher le curseur de transparence
            }
        });
        
        // Masquer les contrôles quand la souris quitte la scène
        scene.setOnMouseExited(e -> {
            resizeHandle.setOpacity(0.0);
            opacitySlider.setOpacity(0.0);
        });
        
        // Afficher les contrôles quand la souris entre dans la scène
        scene.setOnMouseEntered(e -> {
            resizeHandle.setOpacity(0.8); // Afficher l'icône dès l'entrée
            opacitySlider.setOpacity(0.8); // Afficher le curseur dès l'entrée
        });

        // Ajouter un listener pour détecter la fermeture de la fenêtre
        overlayStage.setOnCloseRequest(event -> {
            // Sauvegarder la taille actuelle de la fenêtre
            saveOverlaySize();
            overlayStage = null;
            updateOverlayButtonText();
        });

        overlayStage.show();
        updateOverlayButtonText();

        // --- Synchronisation automatique ---
        // À chaque changement de prospecteur → mise à jour de l'overlay
        setOnProspectorChanged(() -> {
            if (overlayStage != null && overlayStage.isShowing()) {
                Platform.runLater(() -> {
                    // Utiliser la même méthode que l'initialisation
                    VBox newCard = createOverlayCard(getCurrentProspector());
                    StackPane rootStackPane = (StackPane) overlayStage.getScene().getRoot();
                    rootStackPane.getChildren().set(0, newCard);
                });
            }
        });
    }

    private void updatePaneStyle(double savedBackgroundOpacity, StackPane stackPane) {
        double initialStackPaneOpacity = Math.max(0.05, savedBackgroundOpacity);
        overlayOpacity = initialStackPaneOpacity;
        String initialStackPaneStyle = String.format(
                Locale.US,
                "-fx-background-color: rgba(0, 0, 0, %.2f);",
                initialStackPaneOpacity
        );
        stackPane.setStyle(initialStackPaneStyle);
    }

}
