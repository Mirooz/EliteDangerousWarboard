package be.mirooz.elitedangerous.dashboard.controller;

import be.mirooz.elitedangerous.dashboard.model.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.service.EdToolsService;
import be.mirooz.elitedangerous.dashboard.controller.ui.manager.PopupManager;
import be.mirooz.elitedangerous.dashboard.controller.ui.component.GenericListView;
import be.mirooz.elitedangerous.dashboard.controller.ui.component.SystemCardComponent;
import be.mirooz.elitedangerous.dashboard.controller.ui.component.ConflictCardComponent;
import be.mirooz.elitedangerous.dashboard.service.InaraService;
import be.mirooz.elitedangerous.lib.edtools.model.MassacreSystem;
import be.mirooz.elitedangerous.lib.inara.model.ConflictSystem;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Contrôleur pour la fenêtre de recherche de systèmes massacre
 */
public class MassacreSearchDialogController implements Initializable {

    @FXML
    private CheckBox LPADonly;
    @FXML
    private TextField referenceSystemField;
    @FXML
    private Spinner<Integer> maxDistanceSpinner;
    @FXML
    private Spinner<Integer> minSourcesSpinner;

    // Onglet Conflit
    @FXML
    private TabPane searchTabPane;
    @FXML
    private TextField conflictReferenceSystemField;
    @FXML
    private Button conflictSearchButton;

    @FXML
    private GenericListView<MassacreSystem> systemList;
    @FXML
    private GenericListView<ConflictSystem> conflictList;
    @FXML
    private Button searchButton;
    @FXML
    private Button closeButton;
    @FXML
    private ImageView fedHeaderIcon;
    @FXML
    private ImageView impHeaderIcon;
    @FXML
    private ImageView allHeaderIcon;
    @FXML
    private ImageView indHeaderIcon;
    @FXML
    private StackPane popupContainer;
    @FXML
    private ProgressIndicator loadingIndicator;

    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();
    private final EdToolsService edToolsService = EdToolsService.getInstance();
    private final InaraService inaraService = InaraService.getInstance();

    private PopupManager popupManager = PopupManager.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Configurer le système de référence par défaut
        String currentSystem = commanderStatus.getCurrentStarSystem();
        if (currentSystem != null && !currentSystem.trim().isEmpty()) {
            referenceSystemField.setText(currentSystem);
        }
        // Charger les icônes des factions dans l'en-tête
        loadFactionHeaderIcons();
        systemList.setComponentFactory(system -> new SystemCardComponent(system, this));
        conflictList.setComponentFactory(conflict -> new ConflictCardComponent(conflict));
        popupManager.attachToContainer(popupContainer);

        // Initialiser les champs de conflit avec le système actuel
        conflictReferenceSystemField.setText(currentSystem);

    }

    List<MassacreSystem> massacreSystems;
    private final Set<String> activeFilters = new HashSet<>();

    @FXML
    private void toggleHighlight(MouseEvent event) {
        ImageView icon = (ImageView) event.getSource();
        String iconName = (String) icon.getUserData();

        if (icon.getStyleClass().contains("icon-selected")) {
            // Désactivation du filtre
            icon.getStyleClass().remove("icon-selected");
            icon.getStyleClass().add("icon-cursor");
            activeFilters.remove(iconName);
        } else {
            // Activation du filtre
            icon.getStyleClass().add("icon-selected");
            icon.getStyleClass().remove("icon-cursor");
            activeFilters.add(iconName);
        }

        displayResults();
    }

    private List<MassacreSystem> applyFilters() {
        if (massacreSystems == null)
            return List.of();

        return massacreSystems.stream()
                .filter(system -> {
                    if (activeFilters.contains("federation") && system.getFed().isEmpty()) return false;
                    if (activeFilters.contains("empire") && system.getImp().isEmpty()) return false;
                    if (activeFilters.contains("alliance") && system.getAll().isEmpty()) return false;
                    if (activeFilters.contains("independent") && system.getInd().isEmpty()) return false;
                    return true;
                })
                .sorted((s1, s2) -> compareByActiveFilters(s1, s2))
                .toList();
    }

    private void loadFactionHeaderIcons() {
        fedHeaderIcon.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/fed.png"))));
        impHeaderIcon.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/empire.png"))));
        allHeaderIcon.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/alliance.png"))));
        indHeaderIcon.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/independant.png"))));
    }

    @FXML
    private void searchMassacreSystems() {
        String referenceSystem = referenceSystemField.getText();
        if (referenceSystem == null || referenceSystem.trim().isEmpty()) {
            referenceSystem = commanderStatus.getCurrentStarSystem();
            referenceSystemField.setText(referenceSystem);
        }
        executeMassacreSearch(referenceSystem, maxDistanceSpinner.getValue(), minSourcesSpinner.getValue(), LPADonly.isSelected());
    }

    private void handleMassacreSearch(CompletableFuture<List<MassacreSystem>> future) {
        searching(true);
        future.thenAccept(systems -> Platform.runLater(() -> {
                    massacreSystems = systems;
                    displayResults();
                    searching(false);
                }))
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    Platform.runLater(() -> searching(false));
                    return null;
                });
    }

    public void searchFromTarget(String referenceSystem) {
        handleMassacreSearch(edToolsService.findSourcesForTargetSystem(referenceSystem));
    }

    private void executeMassacreSearch(String referenceSystem, int maxDistance, int minSources, boolean largePad) {
        handleMassacreSearch(edToolsService.findMassacreSystems(referenceSystem, maxDistance, minSources, largePad));
    }

    private void searching(boolean isSearching) {
        if (isSearching) {
            searchButton.setDisable(true);
            searchButton.setText("RECHERCHE...");
            systemList.getItems().clear();
            loadingIndicator.setVisible(true);
        } else {
            loadingIndicator.setVisible(false);
            searchButton.setDisable(false);
            searchButton.setText("RECHERCHER");
        }
    }

    private void displayResults() {
        systemList.getItems().setAll(applyFilters());
    }


    @FXML
    private void closeDialog() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    private int compareByActiveFilters(MassacreSystem s1, MassacreSystem s2) {
        // Ordre de priorité fixe
        String[] order = {"federation", "empire", "alliance", "independent"};

        for (String filter : order) {
            if (activeFilters.contains(filter)) {
                int v1 = getValueForFilter(s1, filter);
                int v2 = getValueForFilter(s2, filter);
                int cmp = Integer.compare(v2, v1); // tri décroissant
                if (cmp != 0) return cmp;
            }
        }
        return 0;
    }

    private int getValueForFilter(MassacreSystem system, String filter) {
        return switch (filter) {
            case "federation" -> parseOrZero(system.getFed());
            case "empire" -> parseOrZero(system.getImp());
            case "alliance" -> parseOrZero(system.getAll());
            case "independent" -> parseOrZero(system.getInd());
            default -> 0;
        };
    }

    private int parseOrZero(String value) {
        try {
            return (value != null && !value.isEmpty()) ? Integer.parseInt(value) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @FXML
    private void searchConflictSystems() {
        String referenceSystem = conflictReferenceSystemField.getText();
        if (referenceSystem == null || referenceSystem.trim().isEmpty()) {
            referenceSystem = commanderStatus.getCurrentStarSystem();
            conflictReferenceSystemField.setText(referenceSystem);
        }

        executeConflictSearch(referenceSystem);
    }

    private void executeConflictSearch(String referenceSystem) {
        handleConflictSearch(inaraService.findConflictZoneSystems(referenceSystem));
    }

    private void handleConflictSearch(CompletableFuture<List<ConflictSystem>> future) {
        searchingConflicts(true);
        future.thenAccept(conflicts -> Platform.runLater(() -> {
                    conflictList.getItems().setAll(conflicts);
                    searchingConflicts(false);
                }))
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    Platform.runLater(() -> searchingConflicts(false));
                    return null;
                });
    }

    private void searchingConflicts(boolean isSearching) {
        if (isSearching) {
            conflictSearchButton.setDisable(true);
            conflictSearchButton.setText("RECHERCHE...");
            conflictList.getItems().clear();
            loadingIndicator.setVisible(true);
        } else {
            loadingIndicator.setVisible(false);
            conflictSearchButton.setDisable(false);
            conflictSearchButton.setText("RECHERCHER");
        }
    }

}
