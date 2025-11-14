package be.mirooz.elitedangerous.dashboard.controller;

import be.mirooz.elitedangerous.dashboard.controller.ui.manager.UIManager;
import be.mirooz.elitedangerous.dashboard.model.ships.DestroyedShip;
import be.mirooz.elitedangerous.dashboard.model.registries.combat.DestroyedShipsRegistery;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.ResourceBundle;

import static be.mirooz.elitedangerous.dashboard.util.NumberUtil.getFormattedNumber;
import static java.time.format.DateTimeFormatter.ofLocalizedDateTime;

/**
 * Contrôleur pour le panneau des vaisseaux détruits
 */
public class DestroyedShipsController implements Initializable, IRefreshable, IBatchListener {

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
    private TableColumn<DestroyedShip, Integer> bountyColumn;

    @FXML
    private TableColumn<DestroyedShip, String> timeColumn;


    @FXML
    private VBox factionBountyStats;

    @FXML
    private Label destroyedShipsTitleLabel;

    @FXML
    private Label totalShipsTextLabel;

    @FXML
    private Label totalBountyTextLabel;

    private DestroyedShipsRegistery destroyedShipsRegistery = DestroyedShipsRegistery.getInstance();
    private final LocalizationService localizationService = LocalizationService.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        destroyedShipsRegistery = DestroyedShipsRegistery.getInstance();
        initializeTable();
        UIManager.getInstance().register(this);
        updateTranslations();
        
        // Écouter les changements de langue
        localizationService.addLanguageChangeListener(locale -> updateTranslations());
    }

    private void updateTranslations() {
        destroyedShipsTitleLabel.setText(localizationService.getString("destroyed_ships.title"));
        totalShipsTextLabel.setText(localizationService.getString("destroyed_ships.ships"));
        totalBountyTextLabel.setText(localizationService.getString("destroyed_ships.credits_pending"));
        
        // Mettre à jour les en-têtes de colonnes
        timeColumn.setText(localizationService.getString("destroyed_ships.time"));
        shipNameColumn.setText(localizationService.getString("destroyed_ships.ship"));
        bountyColumn.setText(localizationService.getString("destroyed_ships.bounty"));
    }

    private void initializeTable() {
        shipNameColumn.setCellValueFactory(cellData -> {
            String name = cellData.getValue().getShipName();
            return new SimpleStringProperty(name != null ? name.toUpperCase() : "");
        });

        bountyColumn.setCellValueFactory(new PropertyValueFactory<>("totalBountyReward"));

        bountyColumn.setCellFactory(col -> new TableCell<DestroyedShip, Integer>() {
            @Override
            protected void updateItem(Integer value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : getFormattedNumber(value));
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

        destroyedShipsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        destroyedShipsTable.setSelectionModel(null);

    }

    @Override
    public void onBatchStart(){
        destroyedShipsTable.setItems(null);
    }
    @Override
    public void onBatchEnd() {
        destroyedShipsTable.setItems(destroyedShipsRegistery.getDestroyedShips());
        updateStatistics();
    }


    private void updateStatistics() {
        int shipsSinceReset = destroyedShipsRegistery.getDestroyedShips().size();
        int totalBounty = destroyedShipsRegistery.getTotalBountyEarned();
        int totalCombatBond = destroyedShipsRegistery.getTotalConflictBounty();

        totalShipsLabel.setText(String.valueOf(shipsSinceReset));
        totalBountyLabel.setText(getFormattedNumber(totalBounty+totalCombatBond) + " Cr");

        updateFactionBountyStats();
    }

    private void updateFactionBountyStats() {
        // Vider les statistiques existantes
        factionBountyStats.getChildren().clear();

        Map<String, Integer> bountyPerFaction = destroyedShipsRegistery.getBountyPerFaction();

        if (!bountyPerFaction.isEmpty()) {
            // Ajouter un titre
            Label titleLabel = new Label("BOUNTY");
            titleLabel.getStyleClass().add("faction-bounty-title");
            setAmountPerFaction(bountyPerFaction, titleLabel);
        }
        Map<String, Integer> combatBondPerFaction = destroyedShipsRegistery.getCombatBondPerFaction();

        if (!combatBondPerFaction.isEmpty()) {
            Label titleLabel = new Label("COMBAT BONDS");
            titleLabel.getStyleClass().add("faction-bond-title");
            setAmountPerFaction(combatBondPerFaction, titleLabel);
        }
    }

    private void setAmountPerFaction(Map<String, Integer> bountyPerFaction, Label titleLabel) {
        factionBountyStats.getChildren().add(titleLabel);

        // Ajouter les statistiques par faction
        bountyPerFaction.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue())) // Tri décroissant
                .forEach(entry -> {
                    HBox factionRow = new HBox(10);
                    factionRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                    Label factionLabel = new Label(entry.getKey() + ":");
                    factionLabel.getStyleClass().add("faction-bounty-name");

                    Label bountyLabel = new Label(getFormattedNumber(entry.getValue()) + " Cr");
                    bountyLabel.getStyleClass().add("faction-bounty-amount");

                    factionRow.getChildren().addAll(factionLabel, bountyLabel);
                    factionBountyStats.getChildren().add(factionRow);
                });
    }

    @Override
    public void refreshUI() {
        updateStatistics();
    }
}
