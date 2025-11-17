package be.mirooz.elitedangerous.dashboard.controller.ui.component.exploration;

import be.mirooz.elitedangerous.dashboard.controller.IRefreshable;
import be.mirooz.elitedangerous.dashboard.model.exploration.ExplorationDataSale;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.ExplorationDataSaleRegistry;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.Comparator;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.function.Consumer;

/**
 * Composant pour afficher l'historique des groupes d'exploration
 */
public class ExplorationHistoryComponent implements Initializable, IRefreshable {

    @FXML
    private ScrollPane explorationHistoryScrollPane;
    @FXML
    private VBox explorationHistoryList;

    private final ExplorationDataSaleRegistry registry = ExplorationDataSaleRegistry.getInstance();
    private Consumer<ExplorationDataSale> onSaleSelected;
    private ExplorationDataSale selectedSale;

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
            explorationHistoryList.getChildren().clear();
            
            // Ajouter la vente en cours en premier (si elle existe)
            if (registry.getCurrentSale() != null) {
                addSaleItem(registry.getCurrentSale(), true);
            }
            
            // Ajouter les ventes finalisées triées par timestamp décroissant (plus récentes en premier)
            var sortedSales = registry.getAllSales().stream()
                    .filter(sale -> sale != registry.getCurrentSale())
                    .sorted(Comparator.comparing((ExplorationDataSale sale) -> {
                        // Utiliser endTimestamp si disponible, sinon timestamp
                        String ts = sale.getEndTimestamp() != null ? sale.getEndTimestamp() : sale.getTimestamp();
                        return ts != null ? ts : "";
                    }).reversed()) // Ordre décroissant
                    .collect(Collectors.toList());
            
            for (ExplorationDataSale sale : sortedSales) {
                addSaleItem(sale, false);
            }
        });
    }

    private void addSaleItem(ExplorationDataSale sale, boolean isCurrent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/exploration/exploration-history-item.fxml"));
            VBox item = loader.load();
            ExplorationHistoryItemController controller = loader.getController();
            controller.setSale(sale, isCurrent);
            
            // Stocker le contrôleur dans le userData pour pouvoir le récupérer
            item.setUserData(controller);
            
            // Sélectionner la vente en cours par défaut
            if (isCurrent && selectedSale == null) {
                setItemSelected(item, true);
                selectedSale = sale;
                if (onSaleSelected != null) {
                    onSaleSelected.accept(sale);
                }
            }
            
            // Gérer le clic
            item.setOnMouseClicked((MouseEvent e) -> {
                // Désélectionner tous les éléments
                for (var child : explorationHistoryList.getChildren()) {
                    if (child instanceof VBox) {
                        setItemSelected((VBox) child, false);
                    }
                }
                
                setItemSelected(item, true);
                selectedSale = sale;
                if (onSaleSelected != null) {
                    onSaleSelected.accept(sale);
                }
            });
            
            explorationHistoryList.getChildren().add(item);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'ajout d'un élément d'historique: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Met à jour le style de sélection d'un élément d'historique
     */
    private void setItemSelected(VBox item, boolean selected) {
        if (item == null) return;
        
        if (selected) {
            if (!item.getStyleClass().contains("exploration-history-item-selected")) {
                item.getStyleClass().add("exploration-history-item-selected");
            }
        } else {
            item.getStyleClass().remove("exploration-history-item-selected");
        }
    }

    public void setOnSaleSelected(Consumer<ExplorationDataSale> callback) {
        this.onSaleSelected = callback;
    }
}

