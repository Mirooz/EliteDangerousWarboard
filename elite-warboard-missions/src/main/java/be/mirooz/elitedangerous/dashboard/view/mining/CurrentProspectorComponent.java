package be.mirooz.elitedangerous.dashboard.view.mining;

import be.mirooz.elitedangerous.dashboard.view.common.IBatchListener;
import be.mirooz.elitedangerous.dashboard.view.common.context.DashboardContext;
import be.mirooz.elitedangerous.dashboard.model.events.ProspectedAsteroid;
import be.mirooz.elitedangerous.dashboard.model.registries.mining.ProspectedAsteroidListener;
import be.mirooz.elitedangerous.dashboard.service.DashboardService;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.service.listeners.MiningEventNotificationService;
import be.mirooz.elitedangerous.dashboard.service.MiningService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import be.mirooz.elitedangerous.dashboard.view.common.overlay.OverlayUi;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

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
public class CurrentProspectorComponent implements Initializable, ProspectedAsteroidListener, MiningEventNotificationService.MiningEventListener, IBatchListener {

    // Services
    private final MiningService miningService = MiningService.getInstance();
    private final LocalizationService localizationService = LocalizationService.getInstance();
    private final MiningEventNotificationService miningEventNotificationService = MiningEventNotificationService.getInstance();

    // Composants FXML
    @FXML
    private VBox currentProspectorContent;
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
    @FXML
    private ToggleButton overlayPassThroughLockButton;

    private ProspectorOverlayComponent prospectorOverlayComponent;

    // Variables pour la navigation
    private List<ProspectedAsteroid> allProspectors = new ArrayList<>();
    private int currentProspectorIndex = 0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialiser le composant overlay
        prospectorOverlayComponent = new ProspectorOverlayComponent();
        prospectorOverlayComponent.setOnOverlayClosed(() -> {
            if (overlayPassThroughLockButton != null) {
                overlayPassThroughLockButton.setSelected(false);
            }
        });
        if (overlayPassThroughLockButton != null) {
            OverlayUi.applyOverlayLockToggleStyle(overlayPassThroughLockButton);
            overlayPassThroughLockButton.setSelected(false);
            Tooltip lt = new Tooltip();
            lt.setWrapText(true);
            lt.setMaxWidth(340);
            lt.setShowDelay(Duration.millis(200));
            overlayPassThroughLockButton.setTooltip(lt);
            OverlayUi.updateLockToggleGlyph(overlayPassThroughLockButton);
            OverlayUi.refreshLockTooltip(overlayPassThroughLockButton, localizationService);
            overlayPassThroughLockButton.selectedProperty().addListener((obs, o, n) -> {
                OverlayUi.updateLockToggleGlyph(overlayPassThroughLockButton);
                OverlayUi.refreshLockTooltip(overlayPassThroughLockButton, localizationService);
                if (prospectorOverlayComponent != null && prospectorOverlayComponent.isShowing()) {
                    prospectorOverlayComponent.setClickThroughLocked(Boolean.TRUE.equals(n));
                }
            });
        }
        DashboardService.getInstance().addBatchListener(this);
        updateProspectors();
        updateTranslations();

        // S'assurer que le bouton overlay est initialisé avec le bon texte
        Platform.runLater(() -> {
            updateOverlayButtonText();
        });

        // Écouter les changements de langue
        localizationService.addLanguageChangeListener(locale -> updateTranslations());

        
        // S'enregistrer comme listener des événements de minage
    }
    @Override
    public void onBatchEnd(){
        miningEventNotificationService.addListener(this);
    }
    @Override
    public void onBatchStart(){
        miningEventNotificationService.removeListeners();
    }

    /**
     * Met à jour l'affichage des prospecteurs avec navigation
     */
    public void updateProspectors() {
        if (!DashboardContext.getInstance().isBatchLoading()) {
            Platform.runLater(() -> {
                List<ProspectedAsteroid> prospectors = miningService.getAllProspectors();

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
    }

    /**
     * Affiche le prospecteur actuel
     */
    private void showCurrentProspector() {
        if (allProspectors.isEmpty() || currentProspectorIndex >= allProspectors.size()) {
            showNoProspector();
            return;
        }

        // Vider le contenu actuel
        currentProspectorContent.getChildren().clear();

        // Afficher le prospecteur actuel (toujours en grand format)
        ProspectedAsteroid currentProspector = allProspectors.get(currentProspectorIndex);
        VBox card = ProspectorCardComponent.createProspectorCard(currentProspector);


        currentProspectorContent.getChildren().add(card);

        // Mettre à jour le compteur
        updateProspectorCounter();

        // Mettre à jour l'overlay si il est ouvert
        updateOverlayContent();
    }

    /**
     * Affiche le message "aucun prospecteur"
     */
    private void showNoProspector() {
        // Vider le contenu actuel
        currentProspectorContent.getChildren().clear();

        // Réinitialiser l'index
        currentProspectorIndex = 0;

        // Mettre à jour les boutons de navigation
        updateNavigationButtons();

        // Vider l'overlay si il est ouvert
        updateOverlayContent();
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

            if (prospectorOverlayComponent != null && prospectorOverlayComponent.isShowing()) {
                text = localizationService.getString("overlay.action.close");
                if (text == null || text.startsWith("overlay.")) {
                    text = getTranslation("mining.overlay_close");
                }
                icon = "✖"; // Croix pour fermer
            } else {
                text = localizationService.getString("overlay.action.open");
                if (text == null || text.startsWith("overlay.")) {
                    text = getTranslation("mining.overlay_open");
                }
                icon = OverlayUi.GLYPH_WINDOW_STACK;
            }

            // Vérifier si la traduction a fonctionné, sinon utiliser un texte par défaut
            if (text == null || text.startsWith("mining.")) {
                if (prospectorOverlayComponent != null && prospectorOverlayComponent.isShowing()) {
                    text = "Fermer Overlay";
                } else {
                    text = "Ouvrir Overlay";
                }
            }

            // Combiner l'icône et le texte
            overlayButton.setText(icon + " " + text);
        }
        if (overlayPassThroughLockButton != null) {
            OverlayUi.updateLockToggleGlyph(overlayPassThroughLockButton);
            OverlayUi.refreshLockTooltip(overlayPassThroughLockButton, localizationService);
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

    
    // Implémentation de ProspectedAsteroidListener
    
    @Override
    public void onProspectorAdded(ProspectedAsteroid prospector) {
        Platform.runLater(() -> {
            System.out.println("🔄 CurrentProspectorComponent: Nouveau prospecteur ajouté - " + 
                (prospector != null ? prospector.toString() : "null"));
            // Afficher automatiquement le dernier prospecteur (qui est maintenant le premier dans la liste inversée)
            refresh();
            showLastProspector();
        });
    }
    
    @Override
    public void onRegistryCleared() {
        Platform.runLater(() -> {
            System.out.println("🔄 CurrentProspectorComponent: Registre des prospecteurs vidé");
            showNoProspector();
        });
    }
    
    // Implémentation de MiningEventNotificationService.MiningEventListener
    
    @Override
    public void onAsteroidCracked(ProspectedAsteroid prospector) {
        Platform.runLater(() -> {
            System.out.println("💥 CurrentProspectorComponent: Astéroïde craqué détecté");
            
            // Vérifier si l'overlay est ouvert et affiche un prospecteur avec core
            if (prospectorOverlayComponent != null && prospectorOverlayComponent.isShowing()) {
                ProspectedAsteroid currentOverlayProspector = getCurrentProspector();
                
                // Vérifier que l'overlay affiche bien un prospecteur avec core material
                if (currentOverlayProspector != null && currentOverlayProspector.getCoreMineral() != null) {
                    System.out.printf("🔍 Overlay affiche un prospecteur avec core: %s - vidage du contenu%n", 
                        currentOverlayProspector.getCoreMineral().getVisibleName());
                    currentOverlayProspector.setCracked(true);
                    prospectorOverlayComponent.clearContent();
                    System.out.println("🗑️ Contenu de l'overlay vidé après craquage d'astéroïde");
                } else {
                    System.out.println("ℹ️ Overlay ouvert mais aucun prospecteur avec core affiché - pas de vidage");
                }
            } else {
                System.out.println("ℹ️ Overlay fermé - aucune action nécessaire");
            }
        });
    }

    @FXML
    public void showProspectorOverlay() {
        if (prospectorOverlayComponent != null) {
            prospectorOverlayComponent.showOverlay(getCurrentProspector());
            updateOverlayButtonText();
            if (prospectorOverlayComponent.isShowing() && overlayPassThroughLockButton != null) {
                prospectorOverlayComponent.setClickThroughLocked(overlayPassThroughLockButton.isSelected());
            }
        }
    }

    /**
     * Met à jour le contenu de l'overlay lors du changement de prospecteur
     */
    private void updateOverlayContent() {
        if (prospectorOverlayComponent != null && prospectorOverlayComponent.isShowing()) {
            ProspectedAsteroid currentProspector = getCurrentProspector();
            if (currentProspector != null && !currentProspector.isCracked()) {
                prospectorOverlayComponent.updateContent(currentProspector);
            } else {
                // Si aucun prospecteur, vider l'overlay
                prospectorOverlayComponent.clearContent();
            }
        }
    }

}
