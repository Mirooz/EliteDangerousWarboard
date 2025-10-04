package be.mirooz.elitedangerous.dashboard.controller;

import be.mirooz.elitedangerous.dashboard.controller.ui.manager.UIManager;
import be.mirooz.elitedangerous.dashboard.model.DestroyedShip;
import be.mirooz.elitedangerous.dashboard.model.DestroyedShipsList;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.ResourceBundle;

import static java.time.format.DateTimeFormatter.ofLocalizedDateTime;

/**
 * Contrôleur pour le panneau des vaisseaux détruits
 */
public class DestroyedShipsController implements Initializable, Refreshable {

    @FXML
    private VBox destroyedShipsPanel;

    @FXML
    private Label totalShipsLabel;

    @FXML
    private Label totalBountyLabel;

    @FXML
    private TableView<DestroyedShip> destroyedShipsTable;

    @FXML
    private TableColumn<DestroyedShip, String> shipNameColumn;

    @FXML
    private TableColumn<DestroyedShip, String> pilotNameColumn;


    @FXML
    private TableColumn<DestroyedShip, Integer> bountyColumn;

    @FXML
    private TableColumn<DestroyedShip, String> timeColumn;


    @FXML
    private VBox factionBountyStats;

    private DestroyedShipsList destroyedShipsList = DestroyedShipsList.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        destroyedShipsList = DestroyedShipsList.getInstance();
        initializeTable();
        UIManager.getInstance().register(this);
    }

    private static final DecimalFormat DF;

    static {
        var sym = new DecimalFormatSymbols();
        sym.setGroupingSeparator('.');
        DF = new DecimalFormat("#,##0", sym);
    }

    private void initializeTable() {
        // Colonne Bounty : format avec séparateur de milliers
        shipNameColumn.setCellValueFactory(new PropertyValueFactory<>("shipName"));
        pilotNameColumn.setCellValueFactory(new PropertyValueFactory<>("pilotName"));
        bountyColumn.setCellValueFactory(new PropertyValueFactory<>("totalBountyReward"));

        bountyColumn.setCellFactory(col -> new TableCell<DestroyedShip, Integer>() {
            @Override
            protected void updateItem(Integer value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : DF.format(value));
            }
        });

        // Colonne Heure : format HH:mm
        timeColumn.setCellValueFactory(cellData -> {
            DestroyedShip ship = cellData.getValue();
            if (ship.getDestroyedTime() != null) {
                return new SimpleStringProperty(
                        ship.getDestroyedTime().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                );
            }
            return new SimpleStringProperty("N/A");
        });

        destroyedShipsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        destroyedShipsTable.setSelectionModel(null);

    }

    public void postBatch() {
        destroyedShipsTable.setItems(destroyedShipsList.getDestroyedShips());
        updateStatistics();
    }


    private void updateStatistics() {
        int shipsSinceReset = destroyedShipsList.getShipsSinceLastReset();
        int totalBounty = destroyedShipsList.getTotalBountyEarned();

        totalShipsLabel.setText(String.valueOf(shipsSinceReset));
        totalBountyLabel.setText(DF.format(totalBounty) + " Cr");

        updateFactionBountyStats();
    }

    private void updateFactionBountyStats() {
        // Vider les statistiques existantes
        factionBountyStats.getChildren().clear();

        Map<String, Integer> bountyPerFaction = destroyedShipsList.getBountyPerFaction();

        if (!bountyPerFaction.isEmpty()) {
            // Ajouter un titre
            Label titleLabel = new Label("BOUNTY PAR FACTION");
            titleLabel.getStyleClass().add("faction-bounty-title");
            factionBountyStats.getChildren().add(titleLabel);

            // Ajouter les statistiques par faction
            bountyPerFaction.entrySet().stream()
                    .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue())) // Tri décroissant
                    .forEach(entry -> {
                        HBox factionRow = new HBox(10);
                        factionRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                        Label factionLabel = new Label(entry.getKey() + ":");
                        factionLabel.getStyleClass().add("faction-bounty-name");

                        Label bountyLabel = new Label(DF.format(entry.getValue()) + " Cr");
                        bountyLabel.getStyleClass().add("faction-bounty-amount");

                        factionRow.getChildren().addAll(factionLabel, bountyLabel);
                        factionBountyStats.getChildren().add(factionRow);
                    });
        }
    }

    @Override
    public void refreshUI() {
        updateStatistics();
    }
}
