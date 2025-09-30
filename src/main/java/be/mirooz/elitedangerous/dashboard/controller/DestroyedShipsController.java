package be.mirooz.elitedangerous.dashboard.controller;

import be.mirooz.elitedangerous.dashboard.model.DestroyedShip;
import be.mirooz.elitedangerous.dashboard.model.DestroyedShipsList;
import be.mirooz.elitedangerous.dashboard.ui.UIRefreshManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static java.time.format.DateTimeFormatter.ofLocalizedDateTime;

/**
 * Contrôleur pour le panneau des vaisseaux détruits
 */
public class DestroyedShipsController implements Initializable {

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

    private DestroyedShipsList destroyedShipsList;
    private ObservableList<DestroyedShip> destroyedShipsData;
    private UIRefreshManager uiRefreshManager;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        destroyedShipsList = DestroyedShipsList.getInstance();
        uiRefreshManager = UIRefreshManager.getInstance();
        
        initializeTable();
        initializeButtons();
        loadDestroyedShips();
        
        // S'enregistrer pour les mises à jour UI
        uiRefreshManager.registerDestroyedShipsController(this);
    }
    private static final DecimalFormat DF;
    static {
        var sym = new DecimalFormatSymbols();
        sym.setGroupingSeparator('.');
        DF = new DecimalFormat("#,##0", sym);
    }
    private void initializeTable() {
        destroyedShipsData = FXCollections.observableArrayList();
        destroyedShipsTable.setItems(destroyedShipsData);

        // Configuration des colonnes
        shipNameColumn.setCellValueFactory(new PropertyValueFactory<>("shipName"));
        pilotNameColumn.setCellValueFactory(new PropertyValueFactory<>("pilotName")); bountyColumn.setCellValueFactory(new PropertyValueFactory<>("totalBountyReward"));

        bountyColumn.setCellFactory(col -> new TableCell<DestroyedShip, Integer>() {
            @Override
            protected void updateItem(Integer value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : DF.format(value) + " Cr");
            }
        });
        timeColumn.setCellValueFactory(cellData -> {
            DestroyedShip ship = cellData.getValue();
            if (ship.getDestroyedTime() != null) {
                return new javafx.beans.property.SimpleStringProperty(
                    ship.getDestroyedTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                );
            }
            return new javafx.beans.property.SimpleStringProperty("N/A");
        });

        // Configuration du style des colonnes
        shipNameColumn.setStyle("-fx-alignment: CENTER-LEFT;");
        pilotNameColumn.setStyle("-fx-alignment: CENTER-LEFT;");
        bountyColumn.setStyle("-fx-alignment: CENTER;");
        timeColumn.setStyle("-fx-alignment: CENTER;");

        // Configuration de la largeur des colonnes pour s'adapter à la largeur du tableau
        timeColumn.setPrefWidth(60);
        shipNameColumn.setPrefWidth(120);
        pilotNameColumn.setPrefWidth(100);
        bountyColumn.setPrefWidth(100);
        
        // Ajuster automatiquement les colonnes pour éviter le défilement horizontal
        destroyedShipsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Configuration du style de la table
        destroyedShipsTable.setStyle("-fx-background-color: transparent;");
        destroyedShipsTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    }

    private void initializeButtons() {
        // Plus de boutons à initialiser
    }

    public void refresh() {
        Platform.runLater(() -> {
            loadDestroyedShips();
            updateStatistics();
        });
    }

    private void loadDestroyedShips() {
        List<DestroyedShip> ships = destroyedShipsList.getDestroyedShips();
        destroyedShipsData.clear();
        destroyedShipsData.addAll(ships);
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


    public void addDestroyedShip(DestroyedShip ship) {
        Platform.runLater(() -> {
            destroyedShipsData.add(ship);
            updateStatistics();
        });
    }
}
