package be.mirooz.elitedangerous.dashboard.controller.ui.component.exploration;

import be.mirooz.elitedangerous.dashboard.model.exploration.ExplorationDataSale;
import be.mirooz.elitedangerous.dashboard.util.DateUtil;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * Contrôleur pour un élément de l'historique d'exploration
 */
public class ExplorationHistoryItemController implements Initializable {

    @FXML
    private VBox root;
    @FXML
    private Label timestampLabel;
    @FXML
    private Label totalEarningsLabel;
    @FXML
    private Label systemsCountLabel;
    @FXML
    private Label currentLabel;
    @FXML
    private Label arrowLabel;

    private ExplorationDataSale sale;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialisation
    }

    public void setSale(ExplorationDataSale sale, boolean isCurrent) {
        this.sale = sale;
        if (sale != null) {
            // Formater la date correctement
            String timestamp = sale.getTimestamp() != null ? sale.getTimestamp() : null;
            if (timestamp != null) {
                try {
                    var dateTime = DateUtil.parseTimestamp(timestamp);
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                    timestampLabel.setText(dateTime.format(formatter));
                } catch (Exception e) {
                    timestampLabel.setText(timestamp);
                }
            } else {
                timestampLabel.setText("N/A");
            }
            totalEarningsLabel.setText(String.format("%,d Cr", sale.getTotalEarnings()));
            systemsCountLabel.setText(sale.getSystemsVisited().size() + " systèmes");
            currentLabel.setVisible(isCurrent);
        }
    }

    public ExplorationDataSale getSale() {
        return sale;
    }

    public void setSelected(boolean selected) {
        if (root == null) {
            return; // Le FXML n'est pas encore chargé
        }
        if (selected) {
            if (!root.getStyleClass().contains("exploration-history-item-selected")) {
                root.getStyleClass().add("exploration-history-item-selected");
            }
            if (arrowLabel != null) {
                arrowLabel.setVisible(true);
            }
        } else {
            root.getStyleClass().remove("exploration-history-item-selected");
            if (arrowLabel != null) {
                arrowLabel.setVisible(false);
            }
        }
    }
    
    public VBox getRoot() {
        return root;
    }
}

