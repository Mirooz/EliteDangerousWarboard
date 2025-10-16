package be.mirooz.elitedangerous.dashboard.controller;

import be.mirooz.elitedangerous.dashboard.model.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.service.EdToolsService;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.controller.ui.manager.PopupManager;
import be.mirooz.elitedangerous.dashboard.controller.ui.component.NotSelectableListView;
import be.mirooz.elitedangerous.dashboard.controller.ui.component.SystemCardComponent;
import be.mirooz.elitedangerous.dashboard.controller.ui.component.ConflictCardComponent;
import be.mirooz.elitedangerous.dashboard.service.InaraService;
import be.mirooz.elitedangerous.lib.edtools.model.MassacreSystem;
import be.mirooz.elitedangerous.lib.inara.model.conflictsearch.ConflictSystem;
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
    private NotSelectableListView<MassacreSystem> systemList;
    @FXML
    private NotSelectableListView<ConflictSystem> conflictList;
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

    @FXML
    private ProgressIndicator conflictLoadingIndicator;

    @FXML
    private Label dialogTitleLabel;
    @FXML
    private Label dialogSubtitleLabel;
    @FXML
    private Tab massacreTab;
    @FXML
    private Tab conflictTab;

    @FXML
    private Label systemLabel;
    @FXML
    private Label distanceLabel;
    @FXML
    private Label distanceUnitLabel;
    @FXML
    private Label sourcesLabel;
    @FXML
    private Label sourcesUnitLabel;
    @FXML
    private Tooltip largePadTooltip;

    @FXML
    private Label sourceHeaderLabel;

    @FXML
    private Label targetHeaderLabel;

    @FXML
    private Label countHeaderLabel;

    @FXML
    private Label distanceHeaderLabel;

    @FXML
    private Label padsHeaderLabel;

    @FXML
    private Label resHeaderLabel;

    @FXML
    private Tooltip countTooltip;

    @FXML
    private Label conflictSystemHeaderLabel;

    @FXML
    private Label conflictDistanceHeaderLabel;

    @FXML
    private Label conflictFactionHeaderLabel;

    @FXML
    private Label conflictOpponentHeaderLabel;

    @FXML
    private Label conflictSurfaceHeaderLabel;

    @FXML
    private Label conflictSystemLabel;

    @FXML
    private Label massacreDescriptionLabel;

    @FXML
    private Label conflictDescriptionLabel;

    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();
    private final EdToolsService edToolsService = EdToolsService.getInstance();
    private final LocalizationService localizationService = LocalizationService.getInstance();
    private final InaraService inaraService = InaraService.getInstance();

    private PopupManager popupManager = PopupManager.getInstance();
    private List<MassacreSystem> massacreSystems;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Configurer le système de référence par défaut
        getReferenceSystem(referenceSystemField);
        getReferenceSystem(conflictReferenceSystemField);
        // Charger les icônes des factions dans l'en-tête
        loadFactionHeaderIcons();
        systemList.setComponentFactory(system -> new SystemCardComponent(system, this));
        conflictList.setComponentFactory(ConflictCardComponent::new);
        popupManager.attachToContainer(popupContainer);


        // Mettre à jour les traductions
        updateTranslations();
        
        // Écouter les changements de langue
        localizationService.addLanguageChangeListener(locale -> updateTranslations());
    }

    private void updateTranslations() {
        // Mettre à jour les titres
        dialogTitleLabel.setText(localizationService.getString("search.systems.title"));
        dialogSubtitleLabel.setText(localizationService.getString("search.systems.subtitle"));
        
        // Mettre à jour les onglets
        massacreTab.setText(localizationService.getString("search.massacre.title"));
        conflictTab.setText(localizationService.getString("search.conflict.title"));
        
        // Mettre à jour les labels de configuration
        systemLabel.setText(localizationService.getString("search.system.label"));
        distanceLabel.setText(localizationService.getString("search.distance.label"));
        distanceUnitLabel.setText(localizationService.getString("search.distance.unit"));
        sourcesLabel.setText(localizationService.getString("search.sources.label"));
        sourcesUnitLabel.setText(localizationService.getString("search.sources.unit"));
        
        // Mettre à jour les boutons
        searchButton.setText(localizationService.getString("search.button"));
        conflictSearchButton.setText(localizationService.getString("search.button"));
        closeButton.setText(localizationService.getString("search.close"));
        
        // Mettre à jour les tooltips
        largePadTooltip.setText(localizationService.getString("search.large_pads"));
        countTooltip.setText(localizationService.getString("search.count_tooltip"));
        
        // Mettre à jour les en-têtes de massacre
        sourceHeaderLabel.setText(localizationService.getString("search.massacre.source"));
        targetHeaderLabel.setText(localizationService.getString("search.massacre.target"));
        countHeaderLabel.setText(localizationService.getString("search.massacre.count"));
        distanceHeaderLabel.setText(localizationService.getString("search.massacre.distance"));
        padsHeaderLabel.setText(localizationService.getString("search.massacre.pads"));
        resHeaderLabel.setText(localizationService.getString("search.massacre.res"));
        
        // Mettre à jour les en-têtes de conflit
        conflictSystemLabel.setText(localizationService.getString("search.system.label"));
        conflictSystemHeaderLabel.setText(localizationService.getString("search.conflict.system"));
        conflictDistanceHeaderLabel.setText(localizationService.getString("search.conflict.distance"));
        conflictFactionHeaderLabel.setText(localizationService.getString("search.conflict.faction"));
        conflictOpponentHeaderLabel.setText(localizationService.getString("search.conflict.opponent"));
        conflictSurfaceHeaderLabel.setText(localizationService.getString("search.conflict.surface"));
        
        // Mettre à jour les descriptions des onglets
        massacreDescriptionLabel.setText(localizationService.getString("search.massacre.description"));
        conflictDescriptionLabel.setText(localizationService.getString("search.conflict.description"));
    }

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
        executeMassacreSearch(getReferenceSystem(referenceSystemField), maxDistanceSpinner.getValue(), minSourcesSpinner.getValue(), LPADonly.isSelected());
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
                    Platform.runLater(() -> {
                        searching(false);
                        // Afficher un popup d'erreur réseau à la position du loading indicator
                        String errorMessage = localizationService.getString("error.network");
                        // Calculer la position du loading indicator
                        double x = loadingIndicator.getLayoutX() + loadingIndicator.getBoundsInLocal().getWidth() / 2;
                        double y = loadingIndicator.getLayoutY() + loadingIndicator.getBoundsInLocal().getHeight() / 2;
                        popupManager.showWarningPopup(errorMessage, x, y, 
                            (Stage) searchButton.getScene().getWindow());
                    });
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
            searchButton.setText(localizationService.getString("search.searching"));
            systemList.getItems().clear();
            loadingIndicator.setVisible(true);
        } else {
            loadingIndicator.setVisible(false);
            searchButton.setDisable(false);
            searchButton.setText(localizationService.getString("search.button"));
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
        executeConflictSearch(getReferenceSystem(conflictReferenceSystemField));
    }

    private String getReferenceSystem(TextField referenceSystemField) {
        String referenceSystem = referenceSystemField.getText();
        if (referenceSystem == null || referenceSystem.trim().isEmpty()) {
            referenceSystem = commanderStatus.getCurrentStarSystem();
            referenceSystemField.setText(referenceSystem);
            if (referenceSystem == null || referenceSystem.isEmpty()) {
                referenceSystemField.setText("Sol");
                referenceSystem = "Sol";
            }
        }
        return referenceSystem;
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
                    Platform.runLater(() -> {
                        searchingConflicts(false);
                        // Afficher un popup d'erreur réseau à la position du loading indicator
                        String errorMessage = localizationService.getString("error.network");
                        // Calculer la position du loading indicator
                        double x = loadingIndicator.getLayoutX() + loadingIndicator.getBoundsInLocal().getWidth() / 2;
                        double y = loadingIndicator.getLayoutY() + loadingIndicator.getBoundsInLocal().getHeight() / 2;
                        popupManager.showWarningPopup(errorMessage, x, y, 
                            (Stage) conflictSearchButton.getScene().getWindow());
                    });
                    return null;
                });
    }

    private void searchingConflicts(boolean isSearching) {
        if (isSearching) {
            conflictSearchButton.setDisable(true);
            conflictSearchButton.setText(localizationService.getString("search.searching"));
            conflictList.getItems().clear();
            conflictLoadingIndicator.setVisible(true);
        } else {
            conflictLoadingIndicator.setVisible(false);
            conflictSearchButton.setDisable(false);
            conflictSearchButton.setText(localizationService.getString("search.button"));
        }
    }

}
