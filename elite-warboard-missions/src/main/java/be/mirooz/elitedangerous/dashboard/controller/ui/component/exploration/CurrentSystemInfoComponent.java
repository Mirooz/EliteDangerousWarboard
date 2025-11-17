package be.mirooz.elitedangerous.dashboard.controller.ui.component.exploration;

import be.mirooz.elitedangerous.dashboard.controller.IRefreshable;
import be.mirooz.elitedangerous.dashboard.model.exploration.ACelesteBody;
import be.mirooz.elitedangerous.dashboard.model.exploration.PlaneteDetail;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.ExplorationDataSaleRegistry;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.OrganicDataSaleRegistry;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.PlaneteRegistry;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Composant pour afficher les informations du système actuel et les statistiques courantes
 */
public class CurrentSystemInfoComponent implements Initializable, IRefreshable {

    @FXML
    private Label systemNameLabel;
    @FXML
    private ScrollPane mappablePlanetsScrollPane;
    @FXML
    private VBox mappablePlanetsList;
    @FXML
    private ScrollPane exobiologyScrollPane;
    @FXML
    private VBox exobiologyList;
    @FXML
    private Region radarPlaceholder;
    
    // Labels pour les statistiques courantes
    @FXML
    private Label currentExplorationCreditsLabel;
    @FXML
    private Label currentBiologyCreditsLabel;
    @FXML
    private Label totalCreditsLabel;

    private final PlaneteRegistry planeteRegistry = PlaneteRegistry.getInstance();
    private final ExplorationDataSaleRegistry explorationRegistry = ExplorationDataSaleRegistry.getInstance();
    private final OrganicDataSaleRegistry organicRegistry = OrganicDataSaleRegistry.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        refresh();
    }

    @Override
    public void refreshUI() {
        refresh();
    }

    public void refresh() {
        Platform.runLater(() -> {
            String currentSystem = planeteRegistry.getCurrentStarSystem();
            if (currentSystem == null && !planeteRegistry.getAllPlanetes().isEmpty()) {
                // Si pas de système courant défini, prendre le premier système trouvé
                currentSystem = planeteRegistry.getAllPlanetes().iterator().next().getStarSystem();
            }
            systemNameLabel.setText(currentSystem != null ? currentSystem : "Aucun système");

            // Afficher les planètes mappables (triées par valeur décroissante, simplifié)
            mappablePlanetsList.getChildren().clear();
            if (currentSystem != null) {
                List<PlaneteDetail> mappablePlanets = planeteRegistry.getPlanetesBySystem(currentSystem).stream()
                        .filter(body -> body instanceof PlaneteDetail)
                        .map(body -> (PlaneteDetail) body)
                        .filter(planet -> !planet.isMapped() && planet.computeValue() > 1000) // Planètes intéressantes
                        .sorted(Comparator.comparingInt(PlaneteDetail::computeValue).reversed())
                        .limit(5) // Top 5 seulement
                        .collect(Collectors.toList());

                for (PlaneteDetail planet : mappablePlanets) {
                    Label planetLabel = new Label(String.format("%s - %,d Cr", 
                            getBodyNameWithoutSystem(planet), planet.computeValue()));
                    planetLabel.getStyleClass().add("exploration-planet-item");
                    mappablePlanetsList.getChildren().add(planetLabel);
                }
            }

            // Afficher l'exobiologie (simplifié, juste les planètes avec exobio)
            exobiologyList.getChildren().clear();
            if (currentSystem != null) {
                List<PlaneteDetail> planetsWithBio = planeteRegistry.getPlanetesBySystem(currentSystem).stream()
                        .filter(body -> body instanceof PlaneteDetail)
                        .map(body -> (PlaneteDetail) body)
                        .filter(planet -> planet.getBioSpecies() != null && !planet.getBioSpecies().isEmpty())
                        .limit(5) // Limiter à 5 planètes
                        .collect(Collectors.toList());

                for (PlaneteDetail planet : planetsWithBio) {
                    Label planetLabel = new Label(getBodyNameWithoutSystem(planet));
                    planetLabel.getStyleClass().add("exploration-exobiology-planet-name");
                    exobiologyList.getChildren().add(planetLabel);
                }
            }

            // TODO: Implémenter le radar pour la position actuelle et les samples
            
            // Calculer et afficher les statistiques courantes
            long currentExplorationCredits = 0;
            if (explorationRegistry.getCurrentSale() != null) {
                currentExplorationCredits = explorationRegistry.getCurrentSale().getTotalEarnings();
            }
            
            // Calculer les crédits biologiques en cours (accumulés mais pas encore vendus)
            long currentBiologyCredits = 0;
            if (organicRegistry.getCurrentOrganicDataOnHold() != null) {
                var onHold = organicRegistry.getCurrentOrganicDataOnHold();
                currentBiologyCredits = onHold.getTotalValue() + onHold.getTotalBonus();
            }
            
            long totalCredits = currentExplorationCredits + currentBiologyCredits;
            
            currentExplorationCreditsLabel.setText(String.format("%,d Cr", currentExplorationCredits));
            currentBiologyCreditsLabel.setText(String.format("%,d Cr", currentBiologyCredits));
            totalCreditsLabel.setText(String.format("%,d Cr", totalCredits));
        });
    }
    
    /**
     * Retire le nom du système du début du bodyName
     */
    private String getBodyNameWithoutSystem(ACelesteBody body) {
        String bodyName = body.getBodyName();
        String systemName = body.getStarSystem();
        
        if (systemName != null && bodyName != null && bodyName.startsWith(systemName)) {
            // Retirer le nom du système et l'espace qui suit
            String nameWithoutSystem = bodyName.substring(systemName.length()).trim();
            // Si le nom commence par un espace, le retirer
            if (nameWithoutSystem.startsWith(" ")) {
                nameWithoutSystem = nameWithoutSystem.substring(1);
            }
            return nameWithoutSystem.isEmpty() ? bodyName : nameWithoutSystem;
        }
        
        return bodyName;
    }
}

