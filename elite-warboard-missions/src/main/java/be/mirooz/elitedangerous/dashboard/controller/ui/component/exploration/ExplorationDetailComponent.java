package be.mirooz.elitedangerous.dashboard.controller.ui.component.exploration;

import be.mirooz.elitedangerous.dashboard.controller.IRefreshable;
import be.mirooz.elitedangerous.dashboard.model.exploration.ExplorationDataSale;
import be.mirooz.elitedangerous.dashboard.model.exploration.SystemVisited;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Composant pour afficher les détails d'un groupe d'exploration
 */
public class ExplorationDetailComponent implements Initializable, IRefreshable {

    @FXML
    private VBox generalInfoContainer;
    @FXML
    private Label detailTotalEarningsLabel;
    @FXML
    private Label detailSystemsCountLabel;
    @FXML
    private ScrollPane systemsScrollPane;
    @FXML
    private VBox systemsList;

    private ExplorationDataSale currentSale;
    private java.util.function.Consumer<SystemVisited> onSystemSelected;
    private java.util.List<SystemCardController> systemCardControllers = new java.util.ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialisation
    }

    @Override
    public void refreshUI() {
        refresh();
    }

    public void refresh() {
        Platform.runLater(() -> {
            if (currentSale == null) {
                generalInfoContainer.setVisible(false);
                systemsList.getChildren().clear();
                return;
            }

            generalInfoContainer.setVisible(true);
            detailTotalEarningsLabel.setText("Total: " + String.format("%,d Cr", currentSale.getTotalEarnings()));
            detailSystemsCountLabel.setText("Systèmes: " + currentSale.getSystemsVisited().size());

            // Afficher les systèmes visités sous forme de cartes
            systemsList.getChildren().clear();
            systemCardControllers.clear();
            for (SystemVisited system : currentSale.getSystemsVisited()) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/exploration/system-card.fxml"));
                    VBox card = loader.load();
                    SystemCardController controller = loader.getController();
                    controller.setSystem(system);
                    // Connecter le callback pour afficher dans la vue visuelle
                    if (onSystemSelected != null) {
                        controller.setOnSystemClicked(systemVisited -> {
                            // Fermer toutes les autres cartes avant d'ouvrir celle-ci
                            closeAllCardsExcept(controller);
                            onSystemSelected.accept(systemVisited);
                        });
                    }
                    // Ajouter un callback pour fermer les autres quand cette carte est cliquée
                    controller.setOnCardExpanded(() -> closeAllCardsExcept(controller));
                    systemCardControllers.add(controller);
                    systemsList.getChildren().add(card);
                } catch (Exception e) {
                    System.err.println("Erreur lors du chargement d'une carte système: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    public void setExplorationDataSale(ExplorationDataSale sale) {
        this.currentSale = sale;
        refresh();
    }
    
    public void setOnSystemSelected(java.util.function.Consumer<SystemVisited> callback) {
        this.onSystemSelected = callback;
    }
    
    /**
     * Ferme toutes les cartes sauf celle spécifiée
     */
    private void closeAllCardsExcept(SystemCardController exceptController) {
        for (SystemCardController controller : systemCardControllers) {
            if (controller != exceptController) {
                controller.setExpanded(false);
            }
        }
    }
}

