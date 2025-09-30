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
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
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
    private Button clearButton;

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

    private void initializeTable() {
        destroyedShipsData = FXCollections.observableArrayList();
        destroyedShipsTable.setItems(destroyedShipsData);

        // Configuration des colonnes
        shipNameColumn.setCellValueFactory(new PropertyValueFactory<>("shipName"));
        pilotNameColumn.setCellValueFactory(new PropertyValueFactory<>("pilotName"));
        bountyColumn.setCellValueFactory(new PropertyValueFactory<>("totalBountyReward"));
        timeColumn.setCellValueFactory(cellData -> {
            DestroyedShip ship = cellData.getValue();
            if (ship.getDestroyedTime() != null) {
                return new javafx.beans.property.SimpleStringProperty(
                    ship.getDestroyedTime().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss"))
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
        timeColumn.setPrefWidth(140);
        shipNameColumn.setPrefWidth(120);
        pilotNameColumn.setPrefWidth(100);
        bountyColumn.setPrefWidth(80);
        
        // Ajuster automatiquement les colonnes pour éviter le défilement horizontal
        destroyedShipsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Configuration du style de la table
        destroyedShipsTable.setStyle("-fx-background-color: transparent;");
        destroyedShipsTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    }

    private void initializeButtons() {
        clearButton.setOnAction(event -> clearDestroyedShips());
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
        int totalShips = destroyedShipsList.getDestroyedShipsCount();
        int totalBounty = destroyedShipsList.getTotalBountyEarned();

        totalShipsLabel.setText(String.valueOf(totalShips));
        totalBountyLabel.setText(String.format("%,d Cr", totalBounty));
    }

    private void clearDestroyedShips() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Effacer les vaisseaux détruits");
        alert.setContentText("Êtes-vous sûr de vouloir effacer tous les vaisseaux détruits de la liste ?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                destroyedShipsList.clearDestroyedShips();
                refresh();
            }
        });
    }

    public void addDestroyedShip(DestroyedShip ship) {
        Platform.runLater(() -> {
            destroyedShipsData.add(ship);
            updateStatistics();
        });
    }
}
