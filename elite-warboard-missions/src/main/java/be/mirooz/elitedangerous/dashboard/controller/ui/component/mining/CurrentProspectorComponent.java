package be.mirooz.elitedangerous.dashboard.controller.ui.component.mining;

import be.mirooz.elitedangerous.dashboard.model.events.ProspectedAsteroid;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.service.MiningService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

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

    private OverlayComponent overlayComponent;

    // Variables pour la navigation
    private List<ProspectedAsteroid> allProspectors = new ArrayList<>();
    private int currentProspectorIndex = 0;

    // Callback pour notifier le parent des changements
    private Runnable onProspectorChanged;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialiser le composant overlay
        overlayComponent = new OverlayComponent();
        
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
        
        // Mettre à jour l'overlay si il est ouvert
        updateOverlayContent();
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
            
            if (overlayComponent != null && overlayComponent.isShowing()) {
                text = getTranslation("mining.overlay_close");
                icon = "✖"; // Croix pour fermer
            } else {
                text = getTranslation("mining.overlay_open");
                icon = "🗔"; // Icône de fenêtre pour ouvrir
            }
            
            // Vérifier si la traduction a fonctionné, sinon utiliser un texte par défaut
            if (text == null || text.startsWith("mining.")) {
                if (overlayComponent != null && overlayComponent.isShowing()) {
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
        if (overlayComponent != null) {
            overlayComponent.showOverlay(getCurrentProspector());
            updateOverlayButtonText();
        }
    }

    /**
     * Met à jour le contenu de l'overlay lors du changement de prospecteur
     */
    private void updateOverlayContent() {
        if (overlayComponent != null && overlayComponent.isShowing()) {
            overlayComponent.updateContent(getCurrentProspector());
        }
    }

}
