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
import java.util.*;
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
    
    // Cache des items pour éviter de les recréer
    private final Map<ExplorationDataSale, VBox> saleItemCache = new HashMap<>();

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
            // Obtenir toutes les ventes triées
            List<ExplorationDataSale> allSales = new ArrayList<>();
            if (registry.getCurrentSale() != null) {
                allSales.add(registry.getCurrentSale());
            }
            
            var sortedSales = registry.getAllSales().stream()
                    .filter(sale -> sale != registry.getCurrentSale())
                    .sorted(Comparator.comparing((ExplorationDataSale sale) -> {
                        String ts = sale.getEndTimestamp() != null ? sale.getEndTimestamp() : sale.getTimestamp();
                        return ts != null ? ts : "";
                    }).reversed())
                    .collect(Collectors.toList());
            allSales.addAll(sortedSales);
            
            // Nettoyer le cache des items qui n'existent plus
            Set<ExplorationDataSale> currentSales = new HashSet<>(allSales);
            saleItemCache.entrySet().removeIf(entry -> !currentSales.contains(entry.getKey()));
            
            // Réorganiser la liste pour correspondre à l'ordre trié
            explorationHistoryList.getChildren().clear();
            
            for (ExplorationDataSale sale : allSales) {
                VBox item = saleItemCache.get(sale);
                if (item == null) {
                    // Créer un nouvel item seulement s'il n'existe pas
                    item = createSaleItem(sale, sale == registry.getCurrentSale());
                    saleItemCache.put(sale, item);
                } else {
                    // Mettre à jour l'état "current" si nécessaire
                    ExplorationHistoryItemController controller = (ExplorationHistoryItemController) item.getUserData();
                    if (controller != null) {
                        controller.setSale(sale, sale == registry.getCurrentSale());
                    }
                }
                explorationHistoryList.getChildren().add(item);
            }
        });
    }

    private VBox createSaleItem(ExplorationDataSale sale, boolean isCurrent) {
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
            
            return item;
        } catch (Exception e) {
            System.err.println("Erreur lors de la création d'un élément d'historique: " + e.getMessage());
            e.printStackTrace();
            return null;
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

