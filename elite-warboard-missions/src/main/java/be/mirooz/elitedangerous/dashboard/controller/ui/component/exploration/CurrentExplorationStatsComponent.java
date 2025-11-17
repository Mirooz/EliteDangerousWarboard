package be.mirooz.elitedangerous.dashboard.controller.ui.component.exploration;

import be.mirooz.elitedangerous.dashboard.controller.IRefreshable;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.ExplorationDataSaleRegistry;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.OrganicDataSaleRegistry;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Composant pour afficher les statistiques d'exploration/biologie courante
 */
public class CurrentExplorationStatsComponent implements Initializable, IRefreshable {

    @FXML
    private Label currentExplorationCreditsLabel;
    @FXML
    private Label currentBiologyCreditsLabel;
    @FXML
    private Label totalCreditsLabel;

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
        // Calculer les crédits de la vente en cours (si elle existe)
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

        currentExplorationCreditsLabel.setText(formatCredits(currentExplorationCredits));
        currentBiologyCreditsLabel.setText(formatCredits(currentBiologyCredits));
        totalCreditsLabel.setText(formatCredits(totalCredits));
    }

    private String formatCredits(long credits) {
        return String.format("%,d Cr", credits);
    }
}

