package be.mirooz.elitedangerous.dashboard.view.colonisation;

import be.mirooz.elitedangerous.backend.edcolonise.EdColoniseStarSystemSearchQuery;
import be.mirooz.elitedangerous.backend.generated.model.EdColoniseColonisedSystemRef;
import be.mirooz.elitedangerous.backend.generated.model.EdColoniseStarSystemSearchResult;
import be.mirooz.elitedangerous.backend.generated.model.EdColoniseSystemCounts;
import be.mirooz.elitedangerous.commons.lib.models.commodities.CarrierCommodityResolver;
import be.mirooz.elitedangerous.commons.lib.models.commodities.ColonisationCommodityKeys;
import be.mirooz.elitedangerous.commons.lib.models.commodities.CommodityCategory;
import be.mirooz.elitedangerous.commons.lib.models.commodities.ICommodity;
import be.mirooz.elitedangerous.backend.generated.model.CommodityRequest;
import be.mirooz.elitedangerous.backend.generated.model.MatchedCommodityNearbyExport;
import be.mirooz.elitedangerous.backend.generated.model.NearbyExportsBestStationResult;
import be.mirooz.elitedangerous.dashboard.model.colonisation.CarrierTradeOrderEntry;
import be.mirooz.elitedangerous.dashboard.model.colonisation.ColonisationArchitectMapCaptionLine;
import be.mirooz.elitedangerous.dashboard.model.colonisation.ColonisationArchitectSystem;
import be.mirooz.elitedangerous.dashboard.model.colonisation.ColonisationConstruction;
import be.mirooz.elitedangerous.dashboard.model.colonisation.construction.Colony;
import be.mirooz.elitedangerous.dashboard.model.colonisation.construction.Structure;
import be.mirooz.elitedangerous.dashboard.model.colonisation.ColonisationDockEntry;
import be.mirooz.elitedangerous.dashboard.model.colonisation.ConstructionResource;
import be.mirooz.elitedangerous.dashboard.model.colonisation.ConstructionStatus;
import be.mirooz.elitedangerous.dashboard.model.registries.commander.CommanderShip;
import be.mirooz.elitedangerous.dashboard.model.registries.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.model.registries.fleetcarrier.CarrierStatus;
import be.mirooz.elitedangerous.dashboard.service.CarrierTradeService;
import be.mirooz.elitedangerous.dashboard.service.ColonisationService;
import be.mirooz.elitedangerous.dashboard.service.EdColoniseService;
import be.mirooz.elitedangerous.dashboard.service.SpanshSystemVisitedService;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.service.MiningService;
import be.mirooz.elitedangerous.dashboard.service.PreferencesService;
import be.mirooz.elitedangerous.dashboard.service.listeners.CargoEventNotificationService;
import be.mirooz.elitedangerous.dashboard.service.listeners.ColonisationNotificationService;
import be.mirooz.elitedangerous.dashboard.view.exploration.SystemVisualViewComponent;
import be.mirooz.elitedangerous.dashboard.view.common.managers.CopyClipboardManager;
import be.mirooz.elitedangerous.dashboard.view.common.managers.PopupManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.TextAlignment;
import javafx.scene.layout.Background;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.io.IOException;
import java.net.URL;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Onglet colonisation + fleet carrier : chantiers architecte, chantier courant, ressources, suggestions d’achat dans le détail.
 */
public class ColonisationPanelController implements Initializable {

    @FXML
    private Button updateTradeStationButton;
    @FXML
    private Label statusLabel;
    @FXML
    private Label constructionsTitleLabel;
    @FXML
    private ComboBox<ColonisationArchitectSystem> architectSystemComboBox;
    @FXML
    private Label architectSystemStatsLabel;
    @FXML
    private Label fleetTitleLabel;
    @FXML
    private Button fleetCollapseButton;
    @FXML
    private VBox fleetCollapsibleContent;
    @FXML
    private VBox fleetSummaryBox;
    @FXML
    private VBox fleetCargoBarBox;
    @FXML
    private GridPane fleetMarketGrid;
    @FXML
    private VBox fleetOptimalMarketPanel;
    @FXML
    private CheckBox fleetAvoidPlanetaryLandingCheckBox;
    @FXML
    private CheckBox fleetLargePadOnlyCheckBox;
    @FXML
    private Button fleetFindOptimalMarketButton;
    @FXML
    private Button fleetOptimalMarketHelpButton;
    @FXML
    private ProgressIndicator fleetOptimalMarketProgress;
    @FXML
    private VBox fleetOptimalMarketResultsBox;
    @FXML
    private Label commanderCurrentCargoTitleLabel;
    @FXML
    private VBox fleetRightColumn;
    @FXML
    private Button commanderCollapseButton;
    @FXML
    private VBox commanderCollapsibleContent;
    @FXML
    private ProgressBar commanderCargoProgressBar;
    @FXML
    private Label commanderCargoUsedLabel;
    @FXML
    private VBox commanderColonyVBox;
    @FXML
    private Label constructionDetailTitleLabel;
    @FXML
    private VBox architectMapContainer;
    @FXML
    private VBox constructionDetailContent;
    @FXML
    private VBox architectViewRoot;
    @FXML
    private TabPane architectCenterTabPane;
    @FXML
    private Tab architectSystemViewTab;
    @FXML
    private Tab architectCargoFleetTab;
    @FXML
    private Tab architectColonisableSearchTab;
    @FXML
    private Label searchMinLandablesLabel;
    @FXML
    private Spinner<Integer> searchMinLandablesSpinner;
    @FXML
    private Label searchMinRingsLabel;
    @FXML
    private Spinner<Integer> searchMinRingsSpinner;
    @FXML
    private Button searchMoreFiltersButton;
    @FXML
    private Label searchMaxDistanceSolLabel;
    @FXML
    private Spinner<Integer> searchMaxDistanceSolSpinner;
    @FXML
    private Button searchColonisableButton;
    @FXML
    private Button searchHelpButton;
    @FXML
    private ProgressIndicator searchLoadingIndicator;
    @FXML
    private Label searchErrorLabel;
    @FXML
    private VBox searchResultsBox;
    @FXML
    private VBox searchMapContainer;
    private final ColonisationService colonisationService = ColonisationService.getInstance();
    private final EdColoniseService edColoniseService = EdColoniseService.getInstance();
    private final SpanshSystemVisitedService spanshSystemVisitedService = SpanshSystemVisitedService.getInstance();
    private final PreferencesService preferencesService = PreferencesService.getInstance();
    private final CarrierTradeService carrierTradeService = CarrierTradeService.getInstance();
    private final LocalizationService localizationService = LocalizationService.getInstance();
    private final CopyClipboardManager copyClipboardManager = CopyClipboardManager.getInstance();
    private final PopupManager popupManager = PopupManager.getInstance();
    private final MiningService miningService = MiningService.getInstance();
    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();
    private final CargoEventNotificationService cargoEventNotificationService = CargoEventNotificationService.getInstance();
    private final CargoEventNotificationService.CargoEventInterface commanderCargoListener =
            () -> Platform.runLater(this::refreshFleetPanel);
    private final ColonisationNotificationService.ColonisationDataListener colonisationDataListener = this::onColonisationDataChanged;

    /** Dernières stations d’achat affichées dans le détail (vidée au changement de sélection / actualisation). */
    private List<NearbyExportsBestStationResult> suggestedBuyStations = List.of();
    private final Set<Long> constructionListMarketIds = new LinkedHashSet<>();

    private ConstructionSiteRow selectedConstructionRow;

    /** Système sélectionné dans la liste déroulante architecte. */
    private ColonisationArchitectSystem selectedArchitectArch;
    private String selectedArchitectStarSystem;
    /** Planète / lune choisie sur la carte (détail chantiers en cours). */
    private Integer selectedArchitectPlanetBodyId;
    private String selectedArchitectPlanetDisplayName;
    private final Map<Integer, String> architectBodyNamesById = new HashMap<>();

    private boolean fleetPanelCollapsed;
    private boolean commanderPanelCollapsed;

    /** Requête API « stations d’achat » en cours (build ou mise à jour). */
    private boolean suggestBuyStationsRequestInProgress;
    private boolean constructionListLoadedAfterJournalRead;

    /** Recherche « marché optimal » Fleet Carrier (onglet Cargo & Fleet). */
    private volatile boolean fleetOptimalMarketSearchInProgress;
    private final Map<String, Color> fleetCargoRowHighlightByMergeKey = new HashMap<>();
    private Tooltip fleetOptimalMarketHelpTooltip;
    private Tooltip searchHelpTooltip;

    private boolean suppressArchitectComboListener;

    private SystemVisualViewComponent architectSystemVisualView;
    private SystemVisualViewComponent searchSystemVisualView;
    private StackPane architectSystemMapRoot;
    private VBox architectSystemMapDetailOverlay;
    /** Marché du détail carte ouvert (pour resynchroniser le bouton « Ajouter à la liste » si la liste change). */
    private Long architectMapOverlayDockMarketId;
    private final List<BorderPane> searchResultCards = new ArrayList<>();
    private BorderPane selectedSearchResultCard;
    private EdColoniseSearchAdvancedSnapshot coloniseSearchAdvancedSnapshot = EdColoniseSearchAdvancedSnapshot.defaults();

    private static final int MAX_DISTANCE_SOL_LY = 2770;
    private static final int MAX_NEIGHBORS_SHOWN = 3;
    /** Max colonised neighbor system names shown on the right of each ED Colonise search result card. */
    private static final int MAX_NEIGHBOR_SYSTEMS_ON_SEARCH_CARD = 3;
    private static final int MAX_FLEET_MARKET_ROWS_COMPACT = 8;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (updateTradeStationButton != null) {
            updateTradeStationButton.setVisible(false);
            updateTradeStationButton.setManaged(false);
        }
        initArchitectCenterTabs();
        initArchitectSystemCombo();
        initArchitectVisualPanel();
        initColonisationSearchTab();

        localizationService.addLanguageChangeListener(locale -> applyLocalizedTexts());
        cargoEventNotificationService.addListener(commanderCargoListener);
        ColonisationNotificationService.getInstance().addListener(colonisationDataListener);
        applyLocalizedTexts();
        setupFoldableFleetAndCargoPanels();
        showArchitectView();
        refreshAll();
        hideFleetRightColumn();
        initFleetOptimalMarketPanel();
    }

    private void initFleetOptimalMarketPanel() {
        if (fleetFindOptimalMarketButton != null) {
            fleetFindOptimalMarketButton.setOnAction(e -> onFleetFindOptimalMarket());
        }
        if (fleetAvoidPlanetaryLandingCheckBox != null) {
            fleetAvoidPlanetaryLandingCheckBox.setSelected(true);
        }
        if (fleetLargePadOnlyCheckBox != null) {
            fleetLargePadOnlyCheckBox.setSelected(true);
        }
        if (fleetOptimalMarketHelpButton != null) {
            fleetOptimalMarketHelpTooltip = new Tooltip();
            fleetOptimalMarketHelpTooltip.setWrapText(true);
            fleetOptimalMarketHelpTooltip.setMaxWidth(320);
            fleetOptimalMarketHelpTooltip.setShowDelay(Duration.millis(200));
            fleetOptimalMarketHelpTooltip.setShowDuration(Duration.minutes(3));
            fleetOptimalMarketHelpTooltip.setHideDelay(Duration.millis(800));
            fleetOptimalMarketHelpTooltip.setText(localizationService.getString("colonisation.fleet.optimalMarket.helpTooltip"));
            fleetOptimalMarketHelpButton.setTooltip(fleetOptimalMarketHelpTooltip);
        }
        if (fleetOptimalMarketResultsBox != null) {
            fleetOptimalMarketResultsBox.getChildren().clear();
        }
        fleetCargoRowHighlightByMergeKey.clear();
    }

    /** Onglet carte architecte : sélectionne la vue système (carte + détail chantiers). */
    private void showArchitectView() {
        if (architectViewRoot != null) {
            architectViewRoot.setVisible(true);
            architectViewRoot.setManaged(true);
        }
        if (architectCenterTabPane != null && architectSystemViewTab != null) {
            architectCenterTabPane.getSelectionModel().select(architectSystemViewTab);
        }
    }

    private void hideFleetRightColumn() {
        if (fleetRightColumn != null) {
            fleetRightColumn.setManaged(false);
            fleetRightColumn.setVisible(false);
        }
    }

    private void onColonisationDataChanged() {
        ensureConstructionListLoadedAfterBatch();
        refreshAll();
    }

    private void ensureConstructionListLoadedAfterBatch() {
        if (constructionListLoadedAfterJournalRead) {
            return;
        }
        if (!colonisationService.isPersistedUiStateLoadedAfterBatch()) {
            return;
        }
        reloadPersistedConstructionList();
        constructionListLoadedAfterJournalRead = true;
    }

    private void initColonisationSearchTab() {
        if (searchColonisableButton != null) {
            searchColonisableButton.setOnAction(e -> onSearchColonisableSystems());
        }
        if (searchMoreFiltersButton != null) {
            searchMoreFiltersButton.setOnAction(e -> onSearchMoreFilters());
        }
        if (searchHelpButton != null) {
            searchHelpTooltip = new Tooltip();
            searchHelpTooltip.setWrapText(true);
            searchHelpTooltip.setMaxWidth(360);
            searchHelpTooltip.setShowDelay(Duration.millis(200));
            searchHelpTooltip.setShowDuration(Duration.minutes(3));
            searchHelpTooltip.setHideDelay(Duration.millis(800));
            searchHelpTooltip.setText(localizationService.getString("colonisation.edcolonise.search.helpTooltip"));
            searchHelpButton.setTooltip(searchHelpTooltip);
        }
        if (searchMapContainer != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/exploration/system-visual-view.fxml"));
                Parent content = loader.load();
                searchSystemVisualView = loader.getController();
                searchSystemVisualView.setBodiesListPanelVisible(false);
                searchSystemVisualView.setExplorationValueIndicatorsSuppressed(true);
                searchMapContainer.getChildren().setAll(content);
                VBox.setVgrow(content, Priority.ALWAYS);
            } catch (Exception e) {
                Label error = new Label("Unable to load map view: " + e.getMessage());
                error.getStyleClass().add("colonisation-detail-placeholder");
                searchMapContainer.getChildren().setAll(error);
            }
        }
    }

    private void initArchitectVisualPanel() {
        if (architectMapContainer == null) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/exploration/system-visual-view.fxml"));
            Parent content = loader.load();
            architectSystemVisualView = loader.getController();
            architectSystemVisualView.setBodiesListPanelVisible(false);
            architectSystemVisualView.setExplorationValueIndicatorsSuppressed(true);
            architectSystemVisualView.setColonisationArchitectBodyCaptions(Map.of());
            architectSystemVisualView.setColonisationArchitectStationClickHandler(null);
            architectSystemMapRoot = new StackPane(content);
            architectSystemMapRoot.setMaxWidth(Double.MAX_VALUE);
            architectSystemMapRoot.setMaxHeight(Double.MAX_VALUE);
            StackPane.setAlignment(content, Pos.CENTER);
            initArchitectMapDetailOverlay();
            architectMapContainer.getChildren().setAll(architectSystemMapRoot);
            VBox.setVgrow(architectSystemMapRoot, Priority.ALWAYS);
        } catch (Exception e) {
            Label error = new Label("Unable to load map view: " + e.getMessage());
            error.getStyleClass().add("colonisation-detail-placeholder");
            architectMapContainer.getChildren().setAll(error);
        }
    }

    private void initArchitectMapDetailOverlay() {
        architectSystemMapDetailOverlay = new VBox(8);
        architectSystemMapDetailOverlay.setManaged(false);
        architectSystemMapDetailOverlay.setVisible(false);
        architectSystemMapDetailOverlay.setFillWidth(true);
        architectSystemMapDetailOverlay.setMaxWidth(430);
        architectSystemMapDetailOverlay.getStyleClass().addAll("colonisation-column", "colonisation-map-detail-overlay");
        StackPane.setAlignment(architectSystemMapDetailOverlay, Pos.TOP_RIGHT);
        StackPane.setMargin(architectSystemMapDetailOverlay, new Insets(12));
        architectSystemMapRoot.getChildren().add(architectSystemMapDetailOverlay);
    }

    private void initArchitectSystemCombo() {
        if (architectSystemComboBox == null) {
            return;
        }
        architectSystemComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(ColonisationArchitectSystem a) {
                if (a == null) {
                    return "";
                }
                String s = a.getStarSystem();
                return s != null && !s.isBlank() ? s : "—";
            }

            @Override
            public ColonisationArchitectSystem fromString(String string) {
                return null;
            }
        });
        architectSystemComboBox.valueProperty().addListener((obs, oldV, newV) -> {
            if (suppressArchitectComboListener || newV == null) {
                return;
            }
            selectArchitectSystem(newV);
        });
    }

    private void initArchitectCenterTabs() {
        if (architectCenterTabPane == null || architectCargoFleetTab == null) {
            return;
        }
        architectCenterTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == architectCargoFleetTab) {
                // En mode Cargo/Fleet, les deux panneaux doivent rester affichés.
                fleetPanelCollapsed = false;
                commanderPanelCollapsed = false;
                applyFleetPanelFoldState();
                applyCommanderPanelFoldState();
                if (fleetCollapseButton != null) {
                    fleetCollapseButton.setDisable(true);
                }
                if (commanderCollapseButton != null) {
                    commanderCollapseButton.setDisable(true);
                }
            } else {
                if (fleetCollapseButton != null) {
                    fleetCollapseButton.setDisable(false);
                }
                if (commanderCollapseButton != null) {
                    commanderCollapseButton.setDisable(false);
                }
            }
        });
    }

    private void setupFoldableFleetAndCargoPanels() {
        fleetCollapseButton.setOnAction(e -> {
            fleetPanelCollapsed = !fleetPanelCollapsed;
            applyFleetPanelFoldState();
            updateFoldButtonTooltips();
        });
        commanderCollapseButton.setOnAction(e -> {
            commanderPanelCollapsed = !commanderPanelCollapsed;
            applyCommanderPanelFoldState();
            updateFoldButtonTooltips();
        });
        applyFleetPanelFoldState();
        applyCommanderPanelFoldState();
        updateFoldButtonTooltips();
    }

    private void applyFleetPanelFoldState() {
        fleetCollapsibleContent.setManaged(!fleetPanelCollapsed);
        fleetCollapsibleContent.setVisible(!fleetPanelCollapsed);
        fleetCollapseButton.setText(fleetPanelCollapsed ? "\u25B6" : "\u25BC");
    }

    private void applyCommanderPanelFoldState() {
        commanderCollapsibleContent.setManaged(!commanderPanelCollapsed);
        commanderCollapsibleContent.setVisible(!commanderPanelCollapsed);
        commanderCollapseButton.setText(commanderPanelCollapsed ? "\u25B6" : "\u25BC");
    }

    private void updateFoldButtonTooltips() {
        if (fleetCollapseButton == null || commanderCollapseButton == null) {
            return;
        }
        String collapseHint = localizationService.getString("colonisation.panel.foldCollapse");
        String expandHint = localizationService.getString("colonisation.panel.foldExpand");
        fleetCollapseButton.setTooltip(new Tooltip(fleetPanelCollapsed ? expandHint : collapseHint));
        commanderCollapseButton.setTooltip(new Tooltip(commanderPanelCollapsed ? expandHint : collapseHint));
    }

    private void applyLocalizedTexts() {
        if (architectSystemViewTab != null) {
            architectSystemViewTab.setText(localizationService.getString("colonisation.tab.architectView"));
        }
        if (architectCargoFleetTab != null) {
            architectCargoFleetTab.setText(localizationService.getString("colonisation.tab.fleetCargo"));
        }
        if (architectColonisableSearchTab != null) {
            architectColonisableSearchTab.setText(localizationService.getString("colonisation.tab.findColonisable"));
        }
        if (searchMinLandablesLabel != null) {
            searchMinLandablesLabel.setText(localizationService.getString("colonisation.edcolonise.metric.landables"));
        }
        if (searchMinRingsLabel != null) {
            searchMinRingsLabel.setText(localizationService.getString("colonisation.edcolonise.metric.rings"));
        }
        if (searchMaxDistanceSolLabel != null) {
            searchMaxDistanceSolLabel.setText(localizationService.getString("colonisation.edcolonise.field.maxDistanceSolShort"));
        }
        if (searchMoreFiltersButton != null) {
            searchMoreFiltersButton.setText(localizationService.getString("colonisation.edcolonise.moreFilters"));
        }
        if (searchColonisableButton != null) {
            searchColonisableButton.setText(localizationService.getString("colonisation.edcolonise.search"));
        }
        if (searchHelpTooltip != null) {
            searchHelpTooltip.setText(localizationService.getString("colonisation.edcolonise.search.helpTooltip"));
        }
        if (updateTradeStationButton != null) {
            updateTradeStationButton.setText(localizationService.getString("colonisation.updateTradeStation"));
        }
        constructionsTitleLabel.setText(localizationService.getString("colonisation.constructions.title"));
        fleetTitleLabel.setText(localizationService.getString("colonisation.fleet.title"));
        if (fleetAvoidPlanetaryLandingCheckBox != null) {
            fleetAvoidPlanetaryLandingCheckBox.setText(localizationService.getString("colonisation.fleet.optimalMarket.avoidPlanetary"));
        }
        if (fleetLargePadOnlyCheckBox != null) {
            fleetLargePadOnlyCheckBox.setText("Large pads");
        }
        if (fleetFindOptimalMarketButton != null) {
            fleetFindOptimalMarketButton.setText(localizationService.getString("colonisation.fleet.optimalMarket.findButton"));
        }
        if (fleetOptimalMarketHelpTooltip != null) {
            fleetOptimalMarketHelpTooltip.setText(localizationService.getString("colonisation.fleet.optimalMarket.helpTooltip"));
        }
        commanderCurrentCargoTitleLabel.setText(localizationService.getString("colonisation.list.title"));
        updateFoldButtonTooltips();
        rebuildArchitectSystemCards();
        refreshFleetPanel();
        refreshConstructionDetailPanel();
    }

    /** Appelé quand l’onglet devient visible (depuis le dashboard). */
    public void refreshAll() {
        clearSuggestedBuyStations();
        rebuildArchitectSystemCards();
        refreshFleetPanel();
        refreshConstructionDetailPanel();
        updateButtonStates();
        statusLabel.setText("");
    }

    private String localizedConstructionStatus(ConstructionStatus status) {
        if (status == null) {
            return "—";
        }
        String key = "colonisation.status." + status.name();
        String t = localizationService.getString(key);
        if (t == null || t.isBlank() || key.equals(t)) {
            return status.name();
        }
        return t;
    }

    private void rebuildArchitectSystemCards() {
        selectedArchitectPlanetBodyId = null;
        selectedArchitectPlanetDisplayName = null;
        long preferredMarketId = 0;
        ColonisationDockEntry cur = colonisationService.getCurrentConstructionSite();
        if (cur != null) {
            preferredMarketId = cur.getMarketId();
        } else if (selectedConstructionRow != null) {
            preferredMarketId = selectedConstructionRow.getMarketId();
        }
        if (preferredMarketId == 0) {
            preferredMarketId = parseLongOrZero(preferencesService.getPreference(
                    PreferencesService.PREF_COLONISATION_LAST_SELECTED_COLONY_MARKET_ID, ""));
        }

        ConstructionSiteRow globalMatch = null;
        List<ColonisationArchitectSystem> systemsWithSites = new ArrayList<>();

        for (ColonisationArchitectSystem arch : colonisationService.getArchitectSystems()) {
            List<ConstructionSiteRow> items = buildConstructionItems(arch);
            if (items.isEmpty()) {
                continue;
            }
            systemsWithSites.add(arch);
            if (preferredMarketId != 0) {
                for (ConstructionSiteRow r : items) {
                    if (r.getMarketId() == preferredMarketId) {
                        globalMatch = r;
                        break;
                    }
                }
            }
        }

        if (architectSystemComboBox != null) {
            suppressArchitectComboListener = true;
            try {
                architectSystemComboBox.getItems().setAll(systemsWithSites);
                architectSystemComboBox.getSelectionModel().clearSelection();
            } finally {
                suppressArchitectComboListener = false;
            }
        }

        if (systemsWithSites.isEmpty()) {
            if (architectSystemComboBox != null) {
                architectSystemComboBox.setDisable(true);
            }
            selectedConstructionRow = null;
            selectedArchitectArch = null;
            selectedArchitectStarSystem = null;
            selectedArchitectPlanetBodyId = null;
            selectedArchitectPlanetDisplayName = null;
            applyArchitectColonisationOverlayToMapView();
            clearArchitectVisualPanel();
            updateArchitectSystemStatsLabel();
            refreshConstructionDetailPanel();
            updateButtonStates();
        } else {
            if (architectSystemComboBox != null) {
                architectSystemComboBox.setDisable(false);
            }
            selectedConstructionRow = null;
            ColonisationArchitectSystem toSelect = resolveArchitectSystemSelection(systemsWithSites, cur, globalMatch);
            if (architectSystemComboBox != null) {
                suppressArchitectComboListener = true;
                try {
                    architectSystemComboBox.getSelectionModel().select(toSelect);
                } finally {
                    suppressArchitectComboListener = false;
                }
            }
            selectArchitectSystem(toSelect);
            if (globalMatch != null && Objects.equals(globalMatch.getStarSystem(), toSelect.getStarSystem())) {
                selectConstructionRow(globalMatch);
            }
        }
    }

    private static long parseLongOrZero(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private ColonisationArchitectSystem resolveArchitectSystemSelection(
            List<ColonisationArchitectSystem> built,
            ColonisationDockEntry currentSite,
            ConstructionSiteRow globalMatch) {
        if (selectedArchitectArch != null) {
            String keep = selectedArchitectArch.getStarSystem();
            for (ColonisationArchitectSystem a : built) {
                if (Objects.equals(keep, a.getStarSystem())) {
                    return a;
                }
            }
        }
        String persistedArchitectSystem = preferencesService.getPreference(
                PreferencesService.PREF_COLONISATION_LAST_SELECTED_ARCHITECT_SYSTEM, "");
        String persistedFromColonisationService = colonisationService.getPersistedSelectedArchitectSystem();
        if (persistedFromColonisationService != null && !persistedFromColonisationService.isBlank()) {
            persistedArchitectSystem = persistedFromColonisationService;
        }
        String persistedFromConstructionListFile = preferencesService.loadColonisationConstructionListSelectedArchitectSystem();
        if (persistedFromConstructionListFile != null && !persistedFromConstructionListFile.isBlank()) {
            persistedArchitectSystem = persistedFromConstructionListFile;
        }
        if (persistedArchitectSystem != null && !persistedArchitectSystem.isBlank()) {
            String keep = persistedArchitectSystem.trim();
            for (ColonisationArchitectSystem a : built) {
                if (Objects.equals(keep, a.getStarSystem())) {
                    return a;
                }
            }
        }
        if (currentSite != null && currentSite.getStarSystem() != null) {
            for (ColonisationArchitectSystem a : built) {
                if (currentSite.getStarSystem().equals(a.getStarSystem())) {
                    return a;
                }
            }
        }
        if (globalMatch != null) {
            for (ColonisationArchitectSystem a : built) {
                if (globalMatch.getStarSystem().equals(a.getStarSystem())) {
                    return a;
                }
            }
        }
        return built.get(0);
    }

    private void selectArchitectSystem(ColonisationArchitectSystem arch) {
        boolean systemChanged = selectedArchitectArch == null
                || !Objects.equals(selectedArchitectArch.getStarSystem(), arch.getStarSystem());
        selectedArchitectArch = arch;
        selectedArchitectStarSystem = arch.getStarSystem();
        if (selectedArchitectStarSystem != null && !selectedArchitectStarSystem.isBlank()) {
            preferencesService.setPreference(
                    PreferencesService.PREF_COLONISATION_LAST_SELECTED_ARCHITECT_SYSTEM,
                    selectedArchitectStarSystem);
            preferencesService.persistColonisationConstructionListSelectedArchitectSystem(selectedArchitectStarSystem);
        }
        if (systemChanged) {
            selectedArchitectPlanetBodyId = null;
            selectedArchitectPlanetDisplayName = null;
            architectBodyNamesById.clear();
            clearSuggestedBuyStations();
            hideArchitectMapConstructionOverlay();
        }
        if (selectedConstructionRow != null && selectedConstructionRow.getStarSystem() != null
                && selectedArchitectStarSystem != null
                && !selectedConstructionRow.getStarSystem().equals(selectedArchitectStarSystem)) {
            selectedConstructionRow = null;
        }
        syncArchitectSystemComboSelection(arch);
        // Ne pas changer l’onglet (ex. Fleet & Cargo) lors d’un simple refresh si le système est inchangé.
        if (systemChanged && architectCenterTabPane != null && architectSystemViewTab != null) {
            architectCenterTabPane.getSelectionModel().select(architectSystemViewTab);
        }
        applyArchitectColonisationOverlayToMapView();
        loadArchitectVisualForSelectedSystem(selectedArchitectStarSystem);
        resetSuggestedStationsForSelection();
        updateArchitectSystemStatsLabel();
        refreshConstructionDetailPanel();
        updateButtonStates();
    }

    private void updateArchitectSystemStatsLabel() {
        if (architectSystemStatsLabel == null) {
            return;
        }
        if (selectedArchitectArch == null) {
            architectSystemStatsLabel.setText("");
            architectSystemStatsLabel.setManaged(false);
            architectSystemStatsLabel.setVisible(false);
            return;
        }
        architectSystemStatsLabel.setManaged(true);
        architectSystemStatsLabel.setVisible(true);
        int completed = 0;
        int total = 0;
        for (ColonisationDockEntry site : selectedArchitectArch.getSites()) {
            ColonisationConstruction c = site.getConstruction();
            if (c == null) {
                continue;
            }
            total++;
            if (c.getStatus() == ConstructionStatus.COMPLETE) {
                completed++;
            }
        }
        int remainingTons = remainingTonsToDeliverAcrossSystem(selectedArchitectArch);
        architectSystemStatsLabel.setText(localizationService.getString(
                "colonisation.architect.systemStats",
                completed,
                total,
                remainingTons));
    }

    private static int remainingTonsToDeliverAcrossSystem(ColonisationArchitectSystem arch) {
        if (arch == null) {
            return 0;
        }
        int sum = 0;
        for (ColonisationDockEntry site : arch.getSites()) {
            ColonisationConstruction c = site.getConstruction();
            if (c == null || c.getResourcesRequired() == null) {
                continue;
            }
            for (ConstructionResource r : c.getResourcesRequired()) {
                sum += Math.max(0, r.getRequiredAmount() - r.getProvidedAmount());
            }
        }
        return sum;
    }

    private void syncArchitectSystemComboSelection(ColonisationArchitectSystem arch) {
        if (architectSystemComboBox == null || arch == null) {
            return;
        }
        suppressArchitectComboListener = true;
        try {
            ColonisationArchitectSystem match = null;
            for (ColonisationArchitectSystem a : architectSystemComboBox.getItems()) {
                if (Objects.equals(a.getStarSystem(), arch.getStarSystem())) {
                    match = a;
                    break;
                }
            }
            if (match != null) {
                architectSystemComboBox.getSelectionModel().select(match);
            }
        } finally {
            suppressArchitectComboListener = false;
        }
    }

    private Map<Integer, List<ColonisationArchitectMapCaptionLine>> buildColonisationCaptionsByBody(ColonisationArchitectSystem arch) {
        Map<Integer, List<ColonisationArchitectMapCaptionLine>> acc = new LinkedHashMap<>();
        if (arch == null) {
            return Map.of();
        }
        for (ColonisationDockEntry site : arch.getSites()) {
            if (site.getBodyId() == null) {
                continue;
            }
            ColonisationConstruction c = site.getConstruction();
            if (c == null) {
                continue;
            }
            int bid = site.getBodyId().intValue();
            String siteName = truncateCaptionSite(
                    firstNonBlank(site.getSiteNameLocalised(), site.getStationNameRaw(), "?"), 20);
            boolean surface = isColonisationSurfaceStationType(site.getStationType());
            acc.computeIfAbsent(bid, k -> new ArrayList<>())
                    .add(new ColonisationArchitectMapCaptionLine(siteName, c.getStatus(), site.getMarketId(), surface));
        }
        if (acc.isEmpty()) {
            return Map.of();
        }
        Map<Integer, List<ColonisationArchitectMapCaptionLine>> out = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<ColonisationArchitectMapCaptionLine>> e : acc.entrySet()) {
            out.put(e.getKey(), List.copyOf(e.getValue()));
        }
        return out;
    }

    /** Types journal « au sol » (chantier planétaire) ; défaut = orbital ({@code port.png}). */
    private static boolean isColonisationSurfaceStationType(String stationType) {
        if (stationType == null) {
            return false;
        }
        String t = stationType.trim();
        if (t.isEmpty()) {
            return false;
        }
        return "CraterPort".equalsIgnoreCase(t) || "PlanetaryPort".equalsIgnoreCase(t) || "PlanetaryConstructionDepot".equalsIgnoreCase(t);
    }

    private static String truncateCaptionSite(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.strip();
        if (t.length() <= max) {
            return t;
        }
        return t.substring(0, max - 1) + "…";
    }

    private void applyArchitectColonisationOverlayToMapView() {
        if (architectSystemVisualView == null) {
            return;
        }
        if (selectedArchitectArch == null) {
            architectSystemVisualView.setColonisationArchitectBodyCaptions(Map.of());
            architectSystemVisualView.setColonisationArchitectStationClickHandler(null);
            return;
        }
        architectSystemVisualView.setColonisationArchitectBodyCaptions(buildColonisationCaptionsByBody(selectedArchitectArch));
        architectSystemVisualView.setColonisationArchitectStationClickHandler(this::onArchitectStationChipClicked);
    }

    private void onArchitectStationChipClicked(long marketId) {
        if (selectedArchitectArch == null) {
            return;
        }
        for (ColonisationDockEntry site : selectedArchitectArch.getSites()) {
            if (site.getMarketId() != marketId || site.getConstruction() == null) {
                continue;
            }
            if (site.getBodyId() != null) {
                int bid = site.getBodyId().intValue();
                selectedArchitectPlanetBodyId = bid;
                String bodyFromMap = architectBodyNamesById.get(bid);
                selectedArchitectPlanetDisplayName =
                        (bodyFromMap != null && !bodyFromMap.isBlank()) ? bodyFromMap : null;
            } else {
                selectedArchitectPlanetBodyId = null;
                selectedArchitectPlanetDisplayName = null;
            }
            clearSuggestedBuyStations();
            selectedConstructionRow = null;
            colonisationService.designateBuildingSite(site.getMarketId());
            preferencesService.setPreference(
                    PreferencesService.PREF_COLONISATION_LAST_SELECTED_COLONY_MARKET_ID,
                    String.valueOf(site.getMarketId()));
            refreshFleetPanel();
            refreshPlanetInProgressSiteSelectionStyles();
            resetSuggestedStationsForSelection();
            updateButtonStates();
            showArchitectMapConstructionOverlay(site);
            return;
        }
    }

    private ConstructionSiteRow constructionSiteRowFromDock(ColonisationDockEntry site) {
        String sys = site.getStarSystem() != null ? site.getStarSystem() : selectedArchitectStarSystem;
        ColonisationConstruction c = site.getConstruction();
        String siteLabel = site.getSiteNameLocalised();
        if (siteLabel == null || siteLabel.isBlank()) {
            siteLabel = site.getStationNameRaw() != null ? site.getStationNameRaw() : "";
        }
        return new ConstructionSiteRow(
                sys != null ? sys : "",
                site.getMarketId(),
                siteLabel,
                c != null ? c.getConstructionProgress() : 0,
                c != null ? c.getStatus() : ConstructionStatus.IN_PROGRESS);
    }

    private void selectConstructionRow(ConstructionSiteRow row) {
        clearSuggestedBuyStations();
        selectedConstructionRow = row;
        if (row != null) {
            colonisationService.designateBuildingSite(row.getMarketId());
            preferencesService.setPreference(
                    PreferencesService.PREF_COLONISATION_LAST_SELECTED_COLONY_MARKET_ID,
                    String.valueOf(row.getMarketId()));
        }
        refreshFleetPanel();
        refreshPlanetInProgressSiteSelectionStyles();
        resetSuggestedStationsForSelection();
        updateButtonStates();
        refreshConstructionDetailPanel();
    }

    private void refreshPlanetInProgressSiteSelectionStyles() {
        if (constructionDetailContent == null || selectedConstructionRow == null) {
            return;
        }
        long mid = selectedConstructionRow.getMarketId();
        for (Node n : constructionDetailContent.getChildren()) {
            if (n instanceof VBox card) {
                Object ud = card.getUserData();
                card.getStyleClass().remove("colonisation-construction-card-selected");
                if (ud instanceof Long lid && lid == mid) {
                    card.getStyleClass().add("colonisation-construction-card-selected");
                }
            }
        }
    }

    private void refreshConstructionDetailPanel() {
        constructionDetailContent.getChildren().clear();
        constructionDetailTitleLabel.setText(localizationService.getString("colonisation.list.title"));

        if (selectedArchitectStarSystem == null || selectedArchitectArch == null) {
            if (architectSystemVisualView != null) {
                architectSystemVisualView.setColonisationArchitectBodyCaptions(Map.of());
                architectSystemVisualView.setColonisationArchitectStationClickHandler(null);
            }
            clearArchitectVisualPanel();
            constructionDetailContent.getChildren().add(placeholderLabel("colonisation.list.empty"));
            return;
        }
        renderConstructionListDetailPanel();
    }

    private void renderConstructionListDetailPanel() {
        constructionDetailTitleLabel.setText(localizationService.getString("colonisation.list.title"));
        List<ColonisationDockEntry> docks = selectedConstructionListDocks();
        if (docks.isEmpty()) {
            constructionDetailContent.getChildren().add(buildConstructionListEmptyPlaceholder());
            return;
        }
        for (ColonisationDockEntry dock : docks) {
            VBox card = new VBox(4);
            card.getStyleClass().addAll("colonisation-buy-station-wrap", "colonisation-construction-list-card");
            String headline = firstNonBlank(dock.getSiteNameLocalised(), dock.getStationNameRaw(), "—");
            BorderPane titleRow = new BorderPane();
            Label h = new Label(headline);
            h.getStyleClass().add("colonisation-construction-card-title");
            h.setWrapText(true);
            h.setMaxWidth(Double.MAX_VALUE);
            Button removeButton = new Button("X");
            removeButton.setMnemonicParsing(false);
            removeButton.setFocusTraversable(false);
            removeButton.getStyleClass().add("colonisation-panel-fold-button");
            removeButton.setOnAction(e -> {
                removeFromConstructionList(dock.getMarketId());
                refreshConstructionDetailPanel();
                refreshFleetPanel();
            });
            titleRow.setCenter(h);
            titleRow.setRight(removeButton);
            card.getChildren().add(titleRow);
            Label line = new Label(constructionListSubtitleBodyAndType(dock));
            line.getStyleClass().addAll("colonisation-buy-station-meta-dim", "colonisation-construction-list-subtitle");
            line.setWrapText(true);
            card.getChildren().add(line);
            if (dock.getConstruction() != null) {
                card.getChildren().add(buildConstructionListProgressNode(dock.getConstruction()));
            }
            constructionDetailContent.getChildren().add(card);
        }
        List<ConstructionResource> aggregated = aggregateConstructionListResources(docks);
        if (!aggregated.isEmpty()) {
            addConstructionResourcesSection(constructionDetailContent, aggregated);
        }
    }

    private void populateConstructionDetailBody(VBox parent, ColonisationDockEntry dock, boolean mapOverlayMode) {
        addUserConstructionStructureTypeRow(parent, dock);
        appendKv(parent, "colonisation.detail.stationType", localizedConstructionSiteClass(dock));
        String bodyLabel = resolveDockBodyDisplayName(dock);
        if (bodyLabel != null && !bodyLabel.isBlank()) {
            appendKv(parent, "colonisation.detail.body", bodyLabel);
        }
        if (dock.getDistFromStarLs() > 0 && Double.isFinite(dock.getDistFromStarLs())) {
            appendKv(parent, "colonisation.detail.distLs", String.format("%.0f", dock.getDistFromStarLs()));
        }
        ColonisationConstruction c = dock.getConstruction();
        if (c != null) {
            addConstructionProgressSection(parent, c, mapOverlayMode);
            if (c.getStatus() != ConstructionStatus.COMPLETE
                    && c.getResourcesRequired() != null
                    && !c.getResourcesRequired().isEmpty()) {
                if (mapOverlayMode) {
                    addCollapsibleConstructionResourcesSection(parent, c);
                } else {
                    addConstructionResourcesSection(parent, c);
                }
            }
            appendColonyImpactPointsPanel(parent, dock.getMarketId());
        } else {
            clearSuggestedBuyStations();
        }
    }

    private VBox buildConstructionDetailWindow(String headline, ColonisationDockEntry dock, Runnable onClose) {
        VBox window = new VBox(8);
        window.setMaxWidth(Double.MAX_VALUE);
        window.getStyleClass().addAll("colonisation-detail-window", "colonisation-map-detail-window");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("colonisation-detail-window-header");

        Label title = new Label(headline);
        title.setMaxWidth(Double.MAX_VALUE);
        title.getStyleClass().add("colonisation-section-subtitle");
        HBox.setHgrow(title, Priority.ALWAYS);

        Button closeButton = new Button("X");
        closeButton.setMnemonicParsing(false);
        closeButton.setFocusTraversable(false);
        closeButton.getStyleClass().add("colonisation-panel-fold-button");
        closeButton.setTooltip(new Tooltip(localizationService.getString("colonisation.detail.close")));
        closeButton.setOnAction(e -> onClose.run());

        header.getChildren().addAll(title, closeButton);
        window.getChildren().add(header);
        return window;
    }

    private void showArchitectMapConstructionOverlay(ColonisationDockEntry dock) {
        if (architectSystemMapDetailOverlay == null || dock == null) {
            return;
        }
        String headline = firstNonBlank(dock.getSiteNameLocalised(), dock.getStationNameRaw(), "—");
        VBox window = buildConstructionDetailWindow(headline, dock, this::hideArchitectMapConstructionOverlay);
        VBox body = new VBox(10);
        body.setMaxWidth(Double.MAX_VALUE);
        body.getStyleClass().add("colonisation-detail-window-body");
        ColonisationConstruction c = dock.getConstruction();
        if (c != null && c.getStatus() != ConstructionStatus.COMPLETE) {
            Button addButton = new Button(localizationService.getString("colonisation.list.add"));
            addButton.setMnemonicParsing(false);
            addButton.setFocusTraversable(false);
            addButton.getStyleClass().add("elite-nav-button");
            addButton.setDisable(constructionListMarketIds.contains(dock.getMarketId()));
            addButton.setOnAction(e -> {
                addToConstructionList(dock.getMarketId());
                addButton.setDisable(true);
                refreshConstructionDetailPanel();
                refreshFleetPanel();
                statusLabel.setText(localizationService.getString("colonisation.list.added"));
            });
            body.getChildren().add(addButton);
        }
        populateConstructionDetailBody(body, dock, true);
        javafx.scene.control.ScrollPane bodyScroll = new javafx.scene.control.ScrollPane(body);
        bodyScroll.setFitToWidth(true);
        bodyScroll.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        bodyScroll.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
        bodyScroll.getStyleClass().add("colonisation-map-detail-scroll");
        window.getChildren().add(bodyScroll);
        architectSystemMapDetailOverlay.getChildren().setAll(window);
        architectSystemMapDetailOverlay.setManaged(true);
        architectSystemMapDetailOverlay.setVisible(true);
        architectMapOverlayDockMarketId = dock.getMarketId();
    }

    private void hideArchitectMapConstructionOverlay() {
        if (architectSystemMapDetailOverlay == null) {
            return;
        }
        architectSystemMapDetailOverlay.getChildren().clear();
        architectSystemMapDetailOverlay.setManaged(false);
        architectSystemMapDetailOverlay.setVisible(false);
        architectMapOverlayDockMarketId = null;
    }

    /** Reconstruit l’overlay carte si ouvert, p.ex. pour réactiver « Ajouter à la liste » après retrait depuis la liste. */
    private void refreshArchitectMapConstructionOverlayIfOpen() {
        if (architectSystemMapDetailOverlay == null
                || !architectSystemMapDetailOverlay.isVisible()
                || architectMapOverlayDockMarketId == null
                || selectedArchitectArch == null) {
            return;
        }
        long mid = architectMapOverlayDockMarketId;
        for (ColonisationDockEntry site : selectedArchitectArch.getSites()) {
            if (site.getMarketId() == mid && site.getConstruction() != null) {
                showArchitectMapConstructionOverlay(site);
                return;
            }
        }
    }

    private void reloadPersistedConstructionList() {
        constructionListMarketIds.clear();
        constructionListMarketIds.addAll(colonisationService.getPersistedConstructionListMarketIds());
    }

    private void addToConstructionList(long marketId) {
        if (marketId <= 0) {
            return;
        }
        if (constructionListMarketIds.add(marketId)) {
            persistConstructionList();
        }
    }

    private void removeFromConstructionList(long marketId) {
        if (marketId <= 0) {
            return;
        }
        if (constructionListMarketIds.remove(marketId)) {
            persistConstructionList();
            refreshArchitectMapConstructionOverlayIfOpen();
        }
    }

    private void persistConstructionList() {
        clearSuggestedBuyStations();
        preferencesService.persistColonisationConstructionListMarketIds(constructionListMarketIds);
        resetFleetOptimalMarketPanelAfterConstructionListChanged();
    }

    private void resetFleetOptimalMarketPanelAfterConstructionListChanged() {
        fleetCargoRowHighlightByMergeKey.clear();
        if (fleetOptimalMarketResultsBox != null) {
            fleetOptimalMarketResultsBox.getChildren().clear();
        }
        CarrierStatus cs = carrierTradeService.getCarrierStatus();
        if (fleetMarketGrid != null && cs.isCarrierStatsInitialized()) {
            refreshFleetMarketGrid(cs);
        }
    }

    private void addConstructionProgressSection(VBox parent, ColonisationConstruction c) {
        addConstructionProgressSection(parent, c, false);
    }

    private void addConstructionProgressSection(VBox parent, ColonisationConstruction c, boolean showCompletedText) {
        appendSectionTitle(parent, "colonisation.col.progress");
        if (showCompletedText && c.getStatus() == ConstructionStatus.COMPLETE) {
            Label completed = new Label(localizationService.getString("colonisation.status.COMPLETED_CAPS"));
            completed.getStyleClass().add("colonisation-progress-completed");
            parent.getChildren().add(completed);
            return;
        }
        double raw = c.getConstructionProgress();
        double percent = (raw >= 0 && raw <= 1.0) ? raw * 100.0 : raw;
        double clampedPercent = Math.max(0.0, Math.min(100.0, percent));

        StackPane wrap = new StackPane();
        wrap.getStyleClass().add("colonisation-detail-progress-wrap");
        wrap.setMaxWidth(Double.MAX_VALUE);

        ProgressBar pb = new ProgressBar(clampedPercent / 100.0);
        pb.getStyleClass().addAll("progress-bar", "colonisation-detail-progress-bar", "colonisation-progress-black");
        pb.setMaxWidth(Double.MAX_VALUE);
        pb.setPrefHeight(18);

        Label pct = new Label(String.format(Locale.ROOT, "%.1f %%", clampedPercent));
        pct.getStyleClass().add("colonisation-detail-progress-label");

        wrap.getChildren().addAll(pb, pct);
        parent.getChildren().add(wrap);
    }

    private Node buildConstructionListProgressNode(ColonisationConstruction c) {
        if (c == null) {
            return new Label("—");
        }
        if (c.getStatus() == ConstructionStatus.COMPLETE) {
            Label completed = new Label(localizationService.getString("colonisation.status.COMPLETED_CAPS"));
            completed.getStyleClass().add("colonisation-progress-completed");
            return completed;
        }
        double raw = c.getConstructionProgress();
        double percent = (raw >= 0 && raw <= 1.0) ? raw * 100.0 : raw;
        double clampedPercent = Math.max(0.0, Math.min(100.0, percent));
        StackPane wrap = new StackPane();
        wrap.getStyleClass().add("colonisation-detail-progress-wrap");
        ProgressBar pb = new ProgressBar(clampedPercent / 100.0);
        pb.getStyleClass().addAll("progress-bar", "colonisation-detail-progress-bar", "colonisation-progress-black", "colonisation-list-progress-bar");
        pb.setMaxWidth(Double.MAX_VALUE);
        Label pct = new Label(String.format(Locale.ROOT, "%.1f %%", clampedPercent));
        pct.getStyleClass().add("colonisation-detail-progress-label");
        wrap.getChildren().addAll(pb, pct);
        return wrap;
    }

    private void addCollapsibleConstructionResourcesSection(VBox parent, ColonisationConstruction c) {
        VBox block = new VBox(6);
        block.setMaxWidth(Double.MAX_VALUE);
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(localizationService.getString("colonisation.detail.section.resources"));
        title.getStyleClass().add("colonisation-section-subtitle");
        HBox.setHgrow(title, Priority.ALWAYS);
        Button toggle = new Button("▼");
        toggle.setMnemonicParsing(false);
        toggle.setFocusTraversable(false);
        toggle.getStyleClass().add("colonisation-panel-fold-button");
        VBox body = new VBox(6);
        body.setMaxWidth(Double.MAX_VALUE);
        List<ConstructionResource> req = sortResourcesRequiredIncompleteFirst(c.getResourcesRequired());
        addConstructionResourcesSection(body, req, false);
        toggle.setOnAction(e -> {
            boolean collapse = body.isManaged();
            body.setManaged(!collapse);
            body.setVisible(!collapse);
            toggle.setText(collapse ? "▶" : "▼");
        });
        header.getChildren().addAll(title, toggle);
        block.getChildren().addAll(header, body);
        parent.getChildren().add(block);
    }

    private void appendColonyImpactPointsPanel(VBox parent, long marketId) {
        Optional<Structure> chosen = preferencesService.getColonisationUserConstructionStructure(marketId);
        if (chosen.isEmpty()) {
            return;
        }
        Structure s = chosen.get();
        Label title = new Label("Impact");
        title.getStyleClass().add("colonisation-section-subtitle");
        title.setMaxWidth(Double.MAX_VALUE);
        parent.getChildren().add(title);
        int tier = Colony.getInstance().getTier();
        parent.getChildren().add(buildColonyTierPointsRow(tier, s));
        if (s.population != null && s.population.initialIncrease != null) {
            parent.getChildren().add(buildColonyChevronStatRow("colonisation.colony.population", s.population.initialIncrease));
        }
        if (s.population != null && s.population.maxIncrease != null) {
            parent.getChildren().add(buildColonyChevronStatRow("colonisation.colony.popMax", s.population.maxIncrease));
        }
        if (s.stats != null && s.stats.security != null) {
            parent.getChildren().add(buildColonyChevronStatRow("colonisation.colony.security", s.stats.security));
        }
        if (s.stats != null && s.stats.techLevel != null) {
            parent.getChildren().add(buildColonyChevronStatRow("colonisation.colony.techLevel", s.stats.techLevel));
        }
        if (s.stats != null && s.stats.wealth != null) {
            parent.getChildren().add(buildColonyChevronStatRow("colonisation.colony.wealth", s.stats.wealth));
        }
        if (s.stats != null && s.stats.standardOfLiving != null) {
            parent.getChildren().add(buildColonyChevronStatRow("colonisation.colony.standardOfLiving", s.stats.standardOfLiving));
        }
        if (s.stats != null && s.stats.developmentLevel != null) {
            parent.getChildren().add(buildColonyChevronStatRow("colonisation.colony.developmentLevel", s.stats.developmentLevel));
        }
    }

    private void clearArchitectVisualPanel() {
        hideArchitectMapConstructionOverlay();
        if (architectSystemVisualView != null) {
            architectSystemVisualView.displaySystem(null);
        }
    }

    private void loadArchitectVisualForSelectedSystem(String starSystem) {
        if (architectSystemVisualView == null || starSystem == null || starSystem.isBlank()) {
            architectBodyNamesById.clear();
            clearArchitectVisualPanel();
            return;
        }
        final String requestedSystem = starSystem.trim();
        final Long requestedSystemId64 = (selectedArchitectArch != null
                && Objects.equals(selectedArchitectArch.getStarSystem(), requestedSystem)
                && selectedArchitectArch.getSystemAddress() != 0L)
                        ? selectedArchitectArch.getSystemAddress()
                        : null;
        architectSystemVisualView.setPendingSystemTitle(requestedSystem);
        architectSystemVisualView.showSpanshLoadingPlaceholder();
        Thread t = new Thread(() -> {
            try {
                var visited = spanshSystemVisitedService.fetchSystemVisited(requestedSystem, requestedSystemId64);
                Map<Integer, String> bodyNames = new HashMap<>();
                if (visited != null && visited.getCelesteBodies() != null) {
                    for (var body : visited.getCelesteBodies()) {
                        if (body == null) {
                            continue;
                        }
                        bodyNames.put(body.getBodyID(), body.getBodyName());
                    }
                }
                Platform.runLater(() -> {
                    if (!Objects.equals(requestedSystem, selectedArchitectStarSystem)) {
                        return;
                    }
                    if (architectSystemVisualView != null) {
                        architectSystemVisualView.displaySystem(visited, true);
                    }
                    architectBodyNamesById.clear();
                    architectBodyNamesById.putAll(bodyNames);
                    if (selectedArchitectPlanetBodyId != null) {
                        String n = architectBodyNamesById.get(selectedArchitectPlanetBodyId);
                        if (n != null && !n.isBlank()) {
                            selectedArchitectPlanetDisplayName = n;
                        }
                    }
                    refreshConstructionDetailPanel();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (!Objects.equals(requestedSystem, selectedArchitectStarSystem)) {
                        return;
                    }
                    architectBodyNamesById.clear();
                    clearArchitectVisualPanel();
                    refreshConstructionDetailPanel();
                });
            }
        }, "spansh-colonise-architect-map-load");
        t.setDaemon(true);
        t.start();
    }

    private String resolveDockBodyDisplayName(ColonisationDockEntry dock) {
        if (dock == null || dock.getBodyId() == null || dock.getBodyId() < 0) {
            return "";
        }
        int bodyId = dock.getBodyId().intValue();
        String bodyName = architectBodyNamesById.get(bodyId);
        if (bodyName != null && !bodyName.isBlank()) {
            return bodyName;
        }
        if (selectedArchitectPlanetBodyId != null
                && selectedArchitectPlanetBodyId == bodyId
                && selectedArchitectPlanetDisplayName != null
                && !selectedArchitectPlanetDisplayName.isBlank()) {
            return selectedArchitectPlanetDisplayName;
        }
        return "#" + bodyId;
    }

    private void clearSuggestedBuyStations() {
        suggestedBuyStations = List.of();
    }

    private void onSearchMoreFilters() {
        Window owner = searchMoreFiltersButton != null && searchMoreFiltersButton.getScene() != null
                ? searchMoreFiltersButton.getScene().getWindow()
                : null;
        EdColoniseAdvancedFiltersPopup.show(owner, localizationService, coloniseSearchAdvancedSnapshot,
                snap -> coloniseSearchAdvancedSnapshot = snap);
    }

    private void onSearchColonisableSystems() {
        if (searchErrorLabel != null) {
            searchErrorLabel.setVisible(false);
            searchErrorLabel.setManaged(false);
        }
        int minLand = searchMinLandablesSpinner != null ? searchMinLandablesSpinner.getValue() : 0;
        int minRing = searchMinRingsSpinner != null ? searchMinRingsSpinner.getValue() : 0;
        int distLy = searchMaxDistanceSolSpinner != null
                ? Math.min(MAX_DISTANCE_SOL_LY, Math.max(1, searchMaxDistanceSolSpinner.getValue()))
                : Math.min(MAX_DISTANCE_SOL_LY, 800);
        EdColoniseStarSystemSearchQuery params = EdColoniseSearchFilterForm.mergeMainAndAdvanced(
                minLand, minRing, distLy, coloniseSearchAdvancedSnapshot);
        setSearchBusy(true);
        Thread t = new Thread(() -> {
            try {
                var response = edColoniseService.searchColonisableStarSystems(params);
                List<EdColoniseStarSystemSearchResult> results = response.getResults() != null
                        ? response.getResults()
                        : List.of();
                Platform.runLater(() -> {
                    renderSearchResults(results);
                    setSearchBusy(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setSearchBusy(false);
                    searchErrorLabel.setText(localizationService.getString("colonisation.edcolonise.error") + " " + e.getMessage());
                    searchErrorLabel.setVisible(true);
                    searchErrorLabel.setManaged(true);
                });
            }
        }, "ed-colonise-tab-search");
        t.setDaemon(true);
        t.start();
    }

    private void setSearchBusy(boolean busy) {
        if (searchColonisableButton != null) {
            searchColonisableButton.setDisable(busy);
        }
        if (searchLoadingIndicator != null) {
            searchLoadingIndicator.setVisible(busy);
        }
        if (busy && searchResultsBox != null) {
            searchResultsBox.getChildren().clear();
        }
    }

    private void renderSearchResults(List<EdColoniseStarSystemSearchResult> results) {
        if (searchResultsBox == null) {
            return;
        }
        searchResultsBox.getChildren().clear();
        searchResultCards.clear();
        selectedSearchResultCard = null;
        if (results == null || results.isEmpty()) {
            Label empty = new Label(localizationService.getString("colonisation.edcolonise.noResults"));
            empty.getStyleClass().add("colonisation-detail-placeholder");
            empty.setWrapText(true);
            searchResultsBox.getChildren().add(empty);
            return;
        }
        for (EdColoniseStarSystemSearchResult r : results) {
            BorderPane card = buildSearchResultCard(r);
            searchResultCards.add(card);
            searchResultsBox.getChildren().add(card);
        }
        onSearchCandidateSelected(results.get(0), searchResultCards.get(0));
    }

    private BorderPane buildSearchResultCard(EdColoniseStarSystemSearchResult r) {
        BorderPane card = new BorderPane();
        card.getStyleClass().addAll("colonisation-buy-station-wrap", "colonisation-search-result-card");
        card.setMaxWidth(Double.MAX_VALUE);
        card.setOnMouseClicked(e -> onSearchCandidateSelected(r, card));

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setMaxWidth(Double.MAX_VALUE);
        ColumnConstraints leftCol = new ColumnConstraints();
        leftCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().add(leftCol);

        VBox left = buildSearchLeftColumn(r);
        GridPane.setVgrow(left, Priority.ALWAYS);
        grid.add(left, 0, 0);
        card.setCenter(grid);

        VBox neighborsCol = buildSearchCardNeighborsColumn(r);
        if (neighborsCol != null) {
            card.setRight(neighborsCol);
            BorderPane.setAlignment(neighborsCol, Pos.TOP_RIGHT);
        }

        String lastUp = formatColonisationLastUpdate(r.getLastUpdate());
        if (!lastUp.isBlank()) {
            HBox bottom = new HBox();
            bottom.setAlignment(Pos.CENTER_RIGHT);
            bottom.setPadding(new javafx.geometry.Insets(6, 4, 2, 4));
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label last = new Label(localizationService.getString("colonisation.edcolonise.result.lastUpdate", lastUp));
            last.getStyleClass().add("colonisation-buy-station-meta-dim");
            last.setWrapText(false);
            bottom.getChildren().addAll(spacer, last);
            card.setBottom(bottom);
        }

        return card;
    }

    private VBox buildSearchLeftColumn(EdColoniseStarSystemSearchResult r) {
        VBox col = new VBox(8);
        col.setMaxWidth(Double.MAX_VALUE);

        String name = r.getSystemName() != null ? r.getSystemName() : "—";
        Label l = createCopyableNameLabel(name, false);
        l.getStyleClass().remove("colonisation-buy-station-system");
        l.getStyleClass().add("colonisation-search-system-name");
        l.setWrapText(true);
        col.getChildren().add(l);

        VBox stats = new VBox(4);
        if (r.getDistanceToSol() != null) {
            Label d = new Label("Dist Sol: " + r.getDistanceToSol() + " ly");
            d.getStyleClass().addAll("colonisation-detail-label", "colonisation-search-distance-value");
            d.setWrapText(true);
            stats.getChildren().add(d);
        }
        Integer bodies = sumPlanetaryBodiesFromSearch(r.getSystemCounts());
        if (bodies != null) {
            Label b = new Label("Bodies: " + bodies);
            b.getStyleClass().addAll("colonisation-detail-label", "colonisation-search-bodies-value");
            b.setWrapText(true);
            stats.getChildren().add(b);
        }
        Integer ringBodies = ringBodyCountFromSearch(r);
        if (ringBodies != null) {
            Label rg = new Label("Ring bodies: " + ringBodies);
            rg.getStyleClass().addAll("colonisation-detail-label", "colonisation-search-ring-bodies-value");
            rg.setWrapText(true);
            stats.getChildren().add(rg);
        }
        if (!stats.getChildren().isEmpty()) {
            col.getChildren().add(stats);
        }
        return col;
    }

    /**
     * Right column: up to {@value #MAX_NEIGHBOR_SYSTEMS_ON_SEARCH_CARD} colonised neighbor system names only (compact, copy on click).
     */
    private VBox buildSearchCardNeighborsColumn(EdColoniseStarSystemSearchResult r) {
        if (r.getColonisedSystems() == null || r.getColonisedSystems().isEmpty()) {
            return null;
        }
        VBox col = new VBox(4);
        col.setFillWidth(false);
        col.setAlignment(Pos.TOP_RIGHT);
        col.getStyleClass().add("colonisation-search-neighbors-column");
        col.setMaxWidth(188);
        col.setMinWidth(Region.USE_PREF_SIZE);
        Label title = new Label(localizationService.getString("colonisation.edcolonise.neighbors.cardTitle"));
        title.getStyleClass().add("colonisation-search-neighbors-caption");
        title.setWrapText(true);
        title.setMaxWidth(188);
        title.setAlignment(Pos.CENTER_RIGHT);
        title.setTextAlignment(TextAlignment.RIGHT);
        col.getChildren().add(title);
        int added = 0;
        for (EdColoniseColonisedSystemRef ref : r.getColonisedSystems()) {
            if (added >= MAX_NEIGHBOR_SYSTEMS_ON_SEARCH_CARD) {
                break;
            }
            String sysName = ref.getSystemName() != null ? ref.getSystemName() : "—";
            Label lbl = createCopyableNameLabel(sysName, false);
            lbl.getStyleClass().add("colonisation-search-neighbor-system-compact");
            lbl.setWrapText(true);
            lbl.setMaxWidth(188);
            lbl.setAlignment(Pos.CENTER_RIGHT);
            lbl.setTextAlignment(TextAlignment.RIGHT);
            col.getChildren().add(lbl);
            added++;
        }
        return col.getChildren().isEmpty() ? null : col;
    }

    private void onSearchCandidateSelected(EdColoniseStarSystemSearchResult candidate, BorderPane selectedCard) {
        if (candidate == null || candidate.getSystemName() == null || candidate.getSystemName().isBlank()) {
            return;
        }
        setSelectedSearchResultCard(selectedCard);
        if (searchSystemVisualView != null) {
            searchSystemVisualView.showSpanshLoadingPlaceholder();
        }
        final Long candidateSystemId64 = candidate.getSystemID();
        Thread t = new Thread(() -> {
            try {
                var visited = spanshSystemVisitedService.fetchSystemVisited(candidate.getSystemName(), candidateSystemId64);
                Platform.runLater(() -> {
                    if (searchSystemVisualView != null) {
                        searchSystemVisualView.displaySystem(visited, true);
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    if (searchSystemVisualView != null) {
                        searchSystemVisualView.displaySystem(null);
                    }
                    if (searchErrorLabel != null) {
                        searchErrorLabel.setText(localizationService.getString("colonisation.edcolonise.error") + " " + ex.getMessage());
                        searchErrorLabel.setVisible(true);
                        searchErrorLabel.setManaged(true);
                    }
                });
            }
        }, "spansh-colonise-map-load");
        t.setDaemon(true);
        t.start();
    }

    private void setSelectedSearchResultCard(BorderPane selectedCard) {
        if (selectedSearchResultCard != null) {
            selectedSearchResultCard.getStyleClass().remove("colonisation-search-result-card-selected");
        }
        selectedSearchResultCard = selectedCard;
        if (selectedSearchResultCard != null && !selectedSearchResultCard.getStyleClass().contains("colonisation-search-result-card-selected")) {
            selectedSearchResultCard.getStyleClass().add("colonisation-search-result-card-selected");
        }
    }

    private String formatColonisationLastUpdate(OffsetDateTime t) {
        if (t == null) {
            return "";
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withLocale(localizationService.getCurrentLocale());
        return t.atZoneSameInstant(ZoneId.systemDefault()).format(fmt);
    }

    private static Integer sumPlanetaryBodiesFromSearch(EdColoniseSystemCounts c) {
        if (c == null) {
            return null;
        }
        int s = 0;
        boolean any = false;
        Integer[] parts = {
                c.getIcyBodyCount(),
                c.getGasGiantCount(),
                c.getRockBodyCount(),
                c.getEarthLikeCount(),
                c.getMetalRichCount(),
                c.getWaterWorldCount(),
                c.getAmmoniaWorldCount(),
                c.getRockyIceBodyCount(),
                c.getHighMetalContentCount()
        };
        for (Integer v : parts) {
            if (v != null) {
                any = true;
                s += v;
            }
        }
        return any ? s : null;
    }

    private static Integer ringBodyCountFromSearch(EdColoniseStarSystemSearchResult r) {
        EdColoniseSystemCounts c = r.getSystemCounts();
        if (c != null && c.getRingCount() != null) {
            return c.getRingCount();
        }
        if (r.getRings() != null) {
            return r.getRings().size();
        }
        return null;
    }

    private boolean isSelectedRowCurrentBuildingSite() {
        if (selectedConstructionRow == null) {
            return false;
        }
        ColonisationDockEntry cur = colonisationService.getCurrentConstructionSite();
        return cur != null && selectedConstructionRow.getMarketId() == cur.getMarketId();
    }

    private void resetSuggestedStationsForSelection() {
        suggestedBuyStations = List.of();
    }

    /**
     * Ressources encore incomplètes (fourni &lt; requis) en tête, puis par libellé d’affichage.
     */
    private static List<ConstructionResource> sortResourcesRequiredIncompleteFirst(List<ConstructionResource> resources) {
        if (resources == null || resources.isEmpty()) {
            return List.of();
        }
        List<ConstructionResource> out = new ArrayList<>(resources);
        out.sort(Comparator
                .comparing((ConstructionResource r) -> r.getProvidedAmount() >= r.getRequiredAmount())
                .thenComparing(ConstructionResource::displayLabel, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    /**
     * Liste « ressources à fournir » : mêmes lignes x/y que d’habitude ; si des stations d’achat sont connues,
     * les commodités encore requises correspondant à un marché sont regroupées dans un encadré par station.
     */
    private void addConstructionResourcesSection(VBox parent, ColonisationConstruction c) {
        List<ConstructionResource> req = sortResourcesRequiredIncompleteFirst(c.getResourcesRequired());
        addConstructionResourcesSection(parent, req, true);
    }

    private void addConstructionResourcesSection(VBox parent, List<ConstructionResource> req) {
        addConstructionResourcesSection(parent, req, true);
    }

    private void addConstructionResourcesSection(VBox parent, List<ConstructionResource> req, boolean includeTitle) {
        if (req.isEmpty()) {
            return;
        }
        if (includeTitle) {
            appendSectionTitle(parent, "colonisation.detail.section.resources");
        }
        int[] w = maxProvidedRequiredDigits(req);
        int wProv = w[0];
        int wReq = w[1];
        if (suggestedBuyStations.isEmpty()) {
            VBox allWrap = new VBox(4);
            allWrap.getStyleClass().add("colonisation-buy-station-wrap");
            addColoredResourceRows(allWrap, req, wProv, wReq);
            parent.getChildren().add(allWrap);
            return;
        }

        boolean[] placed = new boolean[req.size()];
        boolean anyStationWrap = false;
        for (NearbyExportsBestStationResult st : suggestedBuyStations) {
            List<Integer> idxInGroup = new ArrayList<>();
            for (int i = 0; i < req.size(); i++) {
                if (placed[i]) {
                    continue;
                }
                ConstructionResource r = req.get(i);
                if (r.getProvidedAmount() >= r.getRequiredAmount()) {
                    continue;
                }
                if (findMatchForResource(st, r) != null) {
                    idxInGroup.add(i);
                }
            }
            if (idxInGroup.isEmpty()) {
                continue;
            }
            anyStationWrap = true;
            VBox wrap = new VBox(4);
            wrap.getStyleClass().add("colonisation-buy-station-wrap");
            wrap.getChildren().add(buildBuyStationHeaderCompact(st));
            int wPriceChars = maxBuyPriceLabelChars(idxInGroup, req, st);
            for (int i : idxInGroup) {
                placed[i] = true;
                ConstructionResource r = req.get(i);
                MatchedCommodityNearbyExport m = Objects.requireNonNull(findMatchForResource(st, r));
                wrap.getChildren().add(buildCompactResourceRowWithMarket(r, m, wProv, wReq, wPriceChars));
            }
            parent.getChildren().add(wrap);
        }

        boolean anyUngrouped = false;
        for (int i = 0; i < req.size(); i++) {
            if (!placed[i]) {
                anyUngrouped = true;
                break;
            }
        }
        if (anyUngrouped && anyStationWrap) {
            Label hint = new Label(localizationService.getString("colonisation.buy.ungroupedHint"));
            hint.getStyleClass().add("colonisation-buy-ungrouped-hint");
            hint.setWrapText(true);
            hint.setMaxWidth(Double.MAX_VALUE);
            parent.getChildren().add(hint);
        }
        for (int i = 0; i < req.size(); i++) {
            if (!placed[i]) {
                parent.getChildren().add(buildResourceRatioRow(req.get(i), wProv, wReq));
            }
        }
    }

    private List<ColonisationDockEntry> selectedConstructionListDocks() {
        List<ColonisationDockEntry> out = new ArrayList<>();
        boolean changed = false;
        boolean hasAnyDockEntryLoaded = !colonisationService.getDockEntries().isEmpty();
        for (Long marketId : constructionListMarketIds) {
            ColonisationDockEntry dock = findDockEntry(marketId);
            if (dock != null && dock.getConstruction() != null && dock.getConstruction().getStatus() != ConstructionStatus.COMPLETE) {
                out.add(dock);
            } else {
                changed = true;
            }
        }
        // Important: au démarrage les docks peuvent ne pas être encore chargés ; ne pas vider la liste persistée trop tôt.
        if (changed && hasAnyDockEntryLoaded) {
            constructionListMarketIds.clear();
            for (ColonisationDockEntry dock : out) {
                constructionListMarketIds.add(dock.getMarketId());
            }
            persistConstructionList();
            refreshArchitectMapConstructionOverlayIfOpen();
        }
        return out;
    }

    private static final class ResourceAccumulator {
        ConstructionResource resource;
        int required;
        int provided;
        long payment;
    }

    private List<ConstructionResource> aggregateConstructionListResources(List<ColonisationDockEntry> docks) {
        Map<String, ResourceAccumulator> byCommodity = new LinkedHashMap<>();
        for (ColonisationDockEntry dock : docks) {
            ColonisationConstruction c = dock.getConstruction();
            if (c == null || c.getResourcesRequired() == null) {
                continue;
            }
            for (ConstructionResource r : c.getResourcesRequired()) {
                if (r == null || r.getCommodity() == null) {
                    continue;
                }
                String key = firstNonBlank(r.getCommodity().getCargoJsonName(), r.displayLabel(), "?");
                ResourceAccumulator acc = byCommodity.computeIfAbsent(key, k -> {
                    ResourceAccumulator a = new ResourceAccumulator();
                    a.resource = r;
                    return a;
                });
                acc.required += r.getRequiredAmount();
                acc.provided += r.getProvidedAmount();
                acc.payment = Math.max(acc.payment, r.getPayment());
            }
        }
        List<ConstructionResource> out = new ArrayList<>();
        for (ResourceAccumulator acc : byCommodity.values()) {
            out.add(new ConstructionResource(acc.resource.getCommodity(), acc.required, acc.provided, acc.payment));
        }
        return sortResourcesRequiredIncompleteFirst(out);
    }

    private static int[] maxProvidedRequiredDigits(List<ConstructionResource> resources) {
        int wProv = 1;
        int wReq = 1;
        for (ConstructionResource r : resources) {
            wProv = Math.max(wProv, String.valueOf(r.getProvidedAmount()).length());
            wReq = Math.max(wReq, String.valueOf(r.getRequiredAmount()).length());
        }
        return new int[]{wProv, wReq};
    }

    /** Même comportement que combat / minage : presse-papiers + popup « system-copied » dans le conteneur du dashboard. */
    private void copyToClipboardWithStandardPopup(Label sourceLabel, String text, boolean stationName, MouseEvent click) {
        if (text == null || text.isBlank() || sourceLabel.getScene() == null) {
            return;
        }
        Window win = sourceLabel.getScene().getWindow();
        if (win == null) {
            return;
        }
        copyClipboardManager.copyToClipboard(text);
        String messageKey = stationName ? "colonisation.buy.copiedStation" : "system.copied";
        popupManager.showPopup(localizationService.getString(messageKey), click.getSceneX(), click.getSceneY(), win);
        click.consume();
    }

    private Label createCopyableNameLabel(String text, boolean stationName) {
        Label l = new Label(text);
        l.getStyleClass().addAll(
                stationName ? "colonisation-buy-station-name" : "colonisation-buy-station-system",
                "colonisation-buy-copy-target");
        l.setCursor(Cursor.HAND);
        String tipKey = stationName ? "colonisation.buy.tooltipCopyStation" : "colonisation.buy.tooltipCopySystem";
        Tooltip tip = new Tooltip(localizationService.getString(tipKey));
        tip.setShowDelay(Duration.millis(350));
        Tooltip.install(l, tip);
        l.setOnMouseClicked(e -> copyToClipboardWithStandardPopup(l, text, stationName, e));
        return l;
    }

    /** Station colorée + copie (panneau marché Fleet), sans le style orange générique des achats chantier. */
    private Label createCopyableFleetOptimalStationLabel(String station, Color c) {
        Label l = new Label(station);
        l.getStyleClass().addAll("colonisation-fleet-optimal-station-copy", "colonisation-buy-copy-target");
        l.setTextFill(c);
        l.setCursor(Cursor.HAND);
        Tooltip tip = new Tooltip(localizationService.getString("colonisation.buy.tooltipCopyStation"));
        tip.setShowDelay(Duration.millis(350));
        Tooltip.install(l, tip);
        l.setOnMouseClicked(e -> copyToClipboardWithStandardPopup(l, station, true, e));
        return l;
    }

    private Label createCopyableFleetOptimalSystemLabel(String system) {
        Label l = new Label(system);
        l.getStyleClass().addAll("colonisation-fleet-optimal-system-copy", "colonisation-buy-copy-target");
        l.setCursor(Cursor.HAND);
        Tooltip tip = new Tooltip(localizationService.getString("colonisation.buy.tooltipCopySystem"));
        tip.setShowDelay(Duration.millis(350));
        Tooltip.install(l, tip);
        l.setOnMouseClicked(e -> copyToClipboardWithStandardPopup(l, system, false, e));
        return l;
    }

    private static Label metaDot() {
        Label s = new Label("·");
        s.getStyleClass().add("colonisation-buy-station-dot");
        return s;
    }

    /**
     * Ligne 1 : station et système (plus grands, couleurs distinctes, clic = copier).
     * Ligne 2 : distance, type (sans nombre de correspondances).
     */
    private Node buildBuyStationHeaderCompact(NearbyExportsBestStationResult st) {
        String station = firstNonBlank(st.getStationName(), "—");
        String sys = firstNonBlank(st.getSystemName(), "");

        HBox names = new HBox(6);
        names.setAlignment(Pos.CENTER_LEFT);
        names.getStyleClass().add("colonisation-buy-station-names-row");
        names.getChildren().add(createCopyableNameLabel(station, true));
        if (!sys.isBlank()) {
            names.getChildren().addAll(metaDot(), createCopyableNameLabel(sys, false));
        }

        boolean hasMeta = st.getMinDistanceLy() != null
                || (st.getStationType() != null && !st.getStationType().isBlank());
        if (!hasMeta) {
            return names;
        }

        HBox meta = new HBox(6);
        meta.setAlignment(Pos.CENTER_LEFT);
        meta.getStyleClass().add("colonisation-buy-station-meta-row");
        boolean needSep = false;
        if (st.getMinDistanceLy() != null) {
            if (needSep) {
                meta.getChildren().add(metaDot());
            }
            Label d = new Label("~" + st.getMinDistanceLy() + " AL");
            d.getStyleClass().add("colonisation-buy-station-meta-dim");
            meta.getChildren().add(d);
            needSep = true;
        }
        if (st.getStationType() != null && !st.getStationType().isBlank()) {
            if (needSep) {
                meta.getChildren().add(metaDot());
            }
            Label t = new Label(st.getStationType());
            t.getStyleClass().add("colonisation-buy-station-meta-dim");
            meta.getChildren().add(t);
        }

        VBox v = new VBox(4);
        v.getChildren().addAll(names, meta);
        return v;
    }

    /** Largeur max (caractères) du libellé prix « … Cr » pour une colonne prix alignée entre les lignes d’un même encadré station. */
    private int maxBuyPriceLabelChars(List<Integer> idxInGroup, List<ConstructionResource> req, NearbyExportsBestStationResult st) {
        int max = 5;
        for (int idx : idxInGroup) {
            MatchedCommodityNearbyExport m = findMatchForResource(st, req.get(idx));
            String s = formatBuyPriceOnly(m);
            if (!s.isEmpty()) {
                max = Math.max(max, s.length());
            }
        }
        return max;
    }

    private static String formatBuyPriceOnly(MatchedCommodityNearbyExport match) {
        if (match == null || match.getExport() == null || match.getExport().getBuyPrice() == null) {
            return "";
        }
        return match.getExport().getBuyPrice() + " Cr";
    }

    /**
     * Ligne ressource + prix marché : une seule {@link GridPane} (mêmes colonnes quantités que le détail,
     * colonne prix à droite, sans affichage du stock).
     */
    private GridPane buildCompactResourceRowWithMarket(
            ConstructionResource r,
            MatchedCommodityNearbyExport m,
            int wProv,
            int wReq,
            int wPriceChars) {
        GridPane grid = new GridPane();
        grid.getStyleClass().addAll("colonisation-buy-res-market-line", "colonisation-detail-res-row");
        grid.setMaxWidth(Double.MAX_VALUE);
        double digitPxBuy = 9.9;
        configureResourceRatioGridColumns(grid, wProv, wReq, digitPxBuy);

        double priceColW = digitPxBuy * Math.max(5, wPriceChars);
        ColumnConstraints cPrice = new ColumnConstraints(priceColW, priceColW, priceColW);
        cPrice.setHgrow(Priority.NEVER);
        cPrice.setHalignment(HPos.RIGHT);
        grid.getColumnConstraints().add(cPrice);

        fillResourceRatioRow(grid, 0, r, wProv, wReq);

        String priceText = formatBuyPriceOnly(m);
        Label price = new Label(priceText);
        price.getStyleClass().add("colonisation-buy-market-inline");
        if (!priceText.isEmpty()) {
            price.getStyleClass().add("colonisation-detail-res-num");
        }
        price.setAlignment(Pos.CENTER_RIGHT);
        price.setMaxWidth(Double.MAX_VALUE);
        grid.add(price, 5, 0);
        GridPane.setHalignment(price, HPos.RIGHT);
        return grid;
    }

    private void configureResourceRatioGridColumns(GridPane grid, int wProv, int wReq) {
        configureResourceRatioGridColumns(grid, wProv, wReq, 9.0);
    }

    private void configureResourceRatioGridColumns(GridPane grid, int wProv, int wReq, double digitPx) {
        grid.setHgap(6);
        grid.setAlignment(Pos.CENTER_LEFT);
        grid.getColumnConstraints().clear();

        double provColW = digitPx * wProv;
        double reqColW = digitPx * wReq;

        ColumnConstraints cDot = new ColumnConstraints();
        cDot.setHgrow(Priority.NEVER);
        ColumnConstraints cName = new ColumnConstraints();
        cName.setHgrow(Priority.ALWAYS);
        cName.setMinWidth(0);
        ColumnConstraints cProv = new ColumnConstraints(provColW, provColW, provColW);
        cProv.setHgrow(Priority.NEVER);
        cProv.setHalignment(HPos.RIGHT);
        ColumnConstraints cSlash = new ColumnConstraints(8, 10, 14);
        cSlash.setHgrow(Priority.NEVER);
        cSlash.setHalignment(HPos.CENTER);
        ColumnConstraints cReq = new ColumnConstraints(reqColW, reqColW, reqColW);
        cReq.setHgrow(Priority.NEVER);
        cReq.setHalignment(HPos.RIGHT);
        grid.getColumnConstraints().addAll(cDot, cName, cProv, cSlash, cReq);
    }

    private void fillResourceRatioRow(GridPane grid, int row, ConstructionResource r, int wProv, int wReq) {
        Label dot = new Label("•");
        dot.getStyleClass().add("colonisation-detail-res-bullet");
        String n = r.displayLabel();
        Label name = new Label(n);
        name.getStyleClass().add("colonisation-detail-res-name");
        name.setMaxWidth(Double.MAX_VALUE);
        name.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);
        int prov = r.getProvidedAmount();
        int req = r.getRequiredAmount();
        Label x = new Label(String.format("%" + wProv + "d", prov));
        x.getStyleClass().addAll(
                prov >= req ? "colonisation-detail-res-x-complete" : "colonisation-detail-res-x-partial",
                "colonisation-detail-res-num");
        x.setAlignment(Pos.CENTER_RIGHT);
        x.setMaxWidth(Double.MAX_VALUE);
        Label slash = new Label("/");
        slash.getStyleClass().add("colonisation-detail-res-slash");
        Label y = new Label(String.format("%" + wReq + "d", req));
        y.getStyleClass().addAll("colonisation-detail-res-y", "colonisation-detail-res-num");
        y.setAlignment(Pos.CENTER_RIGHT);
        y.setMaxWidth(Double.MAX_VALUE);

        grid.add(dot, 0, row);
        grid.add(name, 1, row);
        grid.add(x, 2, row);
        grid.add(slash, 3, row);
        grid.add(y, 4, row);
        GridPane.setHgrow(name, Priority.ALWAYS);
        GridPane.setHalignment(x, HPos.RIGHT);
        GridPane.setHalignment(slash, HPos.CENTER);
        GridPane.setHalignment(y, HPos.RIGHT);
    }

    private MatchedCommodityNearbyExport findMatchForResource(NearbyExportsBestStationResult st, ConstructionResource r) {
        if (st.getMatches() == null) {
            return null;
        }
        for (MatchedCommodityNearbyExport m : st.getMatches()) {
            if (colonisationService.resourceMatchesNearbyBuyRequest(r, m.getRequestedCommodityName())) {
                return m;
            }
        }
        return null;
    }

    private void addColoredResourceRows(VBox parent, List<ConstructionResource> resources, int wProv, int wReq) {
        for (ConstructionResource r : resources) {
            parent.getChildren().add(buildResourceRatioRow(r, wProv, wReq));
        }
    }

    private GridPane buildResourceRatioRow(ConstructionResource r, int wProv, int wReq) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("colonisation-detail-res-row");
        grid.setMaxWidth(Double.MAX_VALUE);
        configureResourceRatioGridColumns(grid, wProv, wReq);
        fillResourceRatioRow(grid, 0, r, wProv, wReq);
        return grid;
    }

    private void appendSectionTitle(VBox parent, String messageKey) {
        Label t = new Label(localizationService.getString(messageKey));
        t.getStyleClass().add("colonisation-section-subtitle");
        t.setMaxWidth(Double.MAX_VALUE);
        parent.getChildren().add(t);
    }

    private void appendKv(VBox parent, String labelKey, String value) {
        HBox row = new HBox(8);
        row.getStyleClass().add("colonisation-detail-row");
        row.setAlignment(Pos.TOP_LEFT);
        Label k = new Label(localizationService.getString(labelKey) + ":");
        k.getStyleClass().add("colonisation-detail-k");
        String v = value != null && !value.isBlank() ? value : "—";
        Label vl = new Label(v);
        vl.getStyleClass().add("colonisation-detail-v");
        vl.setWrapText(true);
        HBox.setHgrow(vl, Priority.ALWAYS);
        row.getChildren().addAll(k, vl);
        parent.getChildren().add(row);
    }

    /**
     * Bloc « effets colonie » (données {@link Structure} / {@code construction_class.json}) pour la structure choisie.
     */
    private void appendColonyStructureStatsPanel(VBox parent, long marketId) {
        VBox panel = new VBox(8);
        panel.getStyleClass().add("colonisation-colony-stat-panel");
        panel.setMaxWidth(Double.MAX_VALUE);

        Optional<Structure> chosen = preferencesService.getColonisationUserConstructionStructure(marketId);
        if (chosen.isEmpty()) {
            panel.getStyleClass().add("colonisation-colony-stat-panel--hint");
            Label h = new Label(localizationService.getString("colonisation.colony.hintNoStructure"));
            h.getStyleClass().add("colonisation-colony-stat-hint");
            h.setWrapText(true);
            h.setMaxWidth(Double.MAX_VALUE);
            panel.getChildren().add(h);
            parent.getChildren().add(panel);
            return;
        }
        panel.getStyleClass().add("colonisation-colony-stat-panel--filled");
        Structure s = chosen.get();
        Label head = new Label(localizationService.getString(
                "colonisation.colony.sectionTitle",
                firstNonBlank(s.name, s.type, "?")));
        head.getStyleClass().add("colonisation-colony-stat-section-title");
        head.setMaxWidth(Double.MAX_VALUE);
        panel.getChildren().add(head);

        int tier = Colony.getInstance().getTier();
        panel.getChildren().add(buildColonyTierPointsRow(tier, s));

        if (s.population != null) {
            if (s.population.initialIncrease != null) {
                panel.getChildren().add(buildColonyChevronStatRow("colonisation.colony.population", s.population.initialIncrease));
            }
            if (s.population.maxIncrease != null) {
                panel.getChildren().add(buildColonyChevronStatRow("colonisation.colony.popMax", s.population.maxIncrease));
            }
        }
        if (s.stats != null) {
            if (s.stats.security != null) {
                panel.getChildren().add(buildColonyChevronStatRow("colonisation.colony.security", s.stats.security));
            }
            if (s.stats.techLevel != null) {
                panel.getChildren().add(buildColonyChevronStatRow("colonisation.colony.techLevel", s.stats.techLevel));
            }
            if (s.stats.wealth != null) {
                panel.getChildren().add(buildColonyChevronStatRow("colonisation.colony.wealth", s.stats.wealth));
            }
            if (s.stats.standardOfLiving != null) {
                panel.getChildren().add(buildColonyChevronStatRow("colonisation.colony.standardOfLiving", s.stats.standardOfLiving));
            }
            if (s.stats.developmentLevel != null) {
                panel.getChildren().add(buildColonyChevronStatRow("colonisation.colony.developmentLevel", s.stats.developmentLevel));
            }
        }
        parent.getChildren().add(panel);
    }

    private HBox buildColonyTierPointsRow(int tier, Structure s) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        row.getStyleClass().add("colonisation-colony-stat-row");
        Label name = new Label(localizationService.getString("colonisation.colony.tierPoints", tier));
        name.getStyleClass().add("colonisation-colony-stat-label");
        name.setMinWidth(210);
        HBox right = new HBox(10);
        right.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(right, Priority.ALWAYS);

        String key = "t" + tier;
        Integer earning = s.earning != null ? s.earning.get(key) : null;
        Integer cost = s.cost != null ? s.cost.get(key) : null;
        int earnVal = earning != null ? earning : 0;
        int costVal = cost != null ? cost : 0;
        if (earnVal != 0) {
            appendColonyTierValueWithGlyph(right, earnVal, true);
        }
        if (costVal != 0) {
            appendColonyTierValueWithGlyph(right, costVal, false);
        }
        if (right.getChildren().isEmpty()) {
            Label dash = new Label("—");
            dash.getStyleClass().add("colonisation-colony-stat-dim");
            right.getChildren().add(dash);
        }
        row.getChildren().addAll(name, right);
        return row;
    }

    private HBox buildColonyChevronStatRow(String messageKey, int value) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        row.getStyleClass().add("colonisation-colony-stat-row");
        Label name = new Label(localizationService.getString(messageKey));
        name.getStyleClass().add("colonisation-colony-stat-label");
        name.setMinWidth(210);
        HBox right = new HBox(8);
        right.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(right, Priority.ALWAYS);
        Label v = new Label(fmtSignedColonyStat(value));
        v.getStyleClass().add(value < 0 ? "colonisation-colony-stat-value-negative" : "colonisation-colony-stat-value");
        int n = Math.min(16, Math.max(0, Math.abs(value)));
        String glyphs = value < 0 ? "<".repeat(n) : ">".repeat(n);
        Label chev = new Label(glyphs);
        chev.getStyleClass().add(value < 0
                ? "colonisation-colony-chevron-negative"
                : "colonisation-colony-chevron-positive");
        right.getChildren().addAll(v, chev);
        row.getChildren().addAll(name, right);
        return row;
    }

    private String fmtSignedColonyStat(int v) {
        return String.format(localizationService.getCurrentLocale(), "%+d", v);
    }

    /** Points de palier : cube si contribution ≥ 0, flèches rouges vers la gauche si &lt; 0. */
    private void appendColonyTierValueWithGlyph(HBox right, int val, boolean isEarning) {
        Label v = new Label(fmtSignedColonyStat(val));
        v.getStyleClass().add(val < 0 ? "colonisation-colony-stat-value-negative" : "colonisation-colony-stat-value");
        right.getChildren().add(v);
        int n = Math.min(16, Math.max(0, Math.abs(val)));
        if (val < 0) {
            Label ar = new Label("<".repeat(Math.max(1, n)));
            ar.getStyleClass().add("colonisation-colony-chevron-negative");
            right.getChildren().add(ar);
        } else {
            Label cube = new Label("▣");
            cube.getStyleClass().add(isEarning ? "colonisation-colony-cube-earn" : "colonisation-colony-cube-cost");
            right.getChildren().add(cube);
        }
    }

    /**
     * Choix utilisateur de la structure ({@link Colony} / {@code construction_class.json}),
     * persisté sous {@code ~/.elite-warboard/}. Menu en cascade : catégorie → type → (settlements : famille) → modèle.
     */
    private void addUserConstructionStructureTypeRow(VBox parent, ColonisationDockEntry dock) {
        if (dock == null) {
            return;
        }
        long marketId = dock.getMarketId();
        VBox block = new VBox(8);
        block.getStyleClass().add("colonisation-structure-type-picker");
        block.setMaxWidth(Double.MAX_VALUE);

        List<Structure> catalog = Colony.getInstance().getConstructionClassCatalog();
        Optional<String> preferredCategory = inferDefaultConstructionCategory(dock);

        Label section = new Label(localizationService.getString("colonisation.structureType.user") + ":");
        section.getStyleClass().add("colonisation-detail-k");
        section.setTooltip(new Tooltip(localizationService.getString("colonisation.structureType.cascadeHint")));

        final Structure[] selection = {null};

        MenuButton cascade = new MenuButton();
        cascade.setMaxWidth(Double.MAX_VALUE);
        cascade.setMnemonicParsing(false);
        cascade.getStyleClass().add("colonisation-colony-cascade");
        cascade.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                Platform.runLater(() -> ensureColonisationCascadePopupStyle(cascade));
            }
        });

        Runnable updateCascadeLabel = () -> {
            Structure s = selection[0];
            if (s == null) {
                cascade.setText(localizationService.getString("colonisation.structureType.none"));
            } else {
                boolean hideCategory = preferredCategory.isPresent();
                cascade.setText(formatColonyCascadeLabel(s, hideCategory));
            }
        };

        Tooltip tip = new Tooltip();
        tip.setWrapText(true);
        tip.setMaxWidth(380);
        Tooltip.install(block, tip);
        Runnable refreshTip = () -> tip.setText(preferencesService.getColonisationUserConstructionStructure(marketId)
                .map(Colony::structureSummaryText)
                .orElse(localizationService.getString("colonisation.structureType.noStats")));

        Runnable refreshColonyStatsIfDetailForSite = () -> {
            if (selectedConstructionRow != null && selectedConstructionRow.getMarketId() == marketId) {
                Platform.runLater(this::refreshConstructionDetailPanel);
            }
        };

        Runnable rebuildCascadeMenu = () -> {
            cascade.getItems().clear();
            if (preferredCategory.isPresent()) {
                String cat = preferredCategory.get();
                addTypeMenusForCategory(cascade.getItems(), catalog, cat, selection, updateCascadeLabel,
                        marketId, refreshTip, refreshColonyStatsIfDetailForSite);
            } else {
                for (String cat : Colony.distinctCategories(catalog)) {
                    Menu catMenu = new Menu(cat);
                    addTypeMenusForCategory(catMenu.getItems(), catalog, cat, selection, updateCascadeLabel,
                            marketId, refreshTip, refreshColonyStatsIfDetailForSite);
                    cascade.getItems().add(catMenu);
                }
            }
            for (MenuItem mi : cascade.getItems()) {
                applyColonisationCascadeMenuItemStyle(mi);
            }
            Platform.runLater(() -> ensureColonisationCascadePopupStyle(cascade));
        };

        rebuildCascadeMenu.run();
        selection[0] = preferencesService.getColonisationUserConstructionStructure(marketId).orElse(null);
        if (selection[0] != null && preferredCategory.isPresent()
                && !preferredCategory.get().equalsIgnoreCase(selection[0].category)) {
            selection[0] = null;
        }
        if (selection[0] == null && preferredCategory.isPresent()) {
            selection[0] = firstStructureForCategory(catalog, preferredCategory.get()).orElse(null);
            if (selection[0] != null) {
                preferencesService.setColonisationUserConstructionStructure(marketId, selection[0]);
            }
        }
        updateCascadeLabel.run();
        refreshTip.run();

        block.getChildren().addAll(section, cascade);
        parent.getChildren().add(block);
        Platform.runLater(() -> ensureColonisationCascadePopupStyle(cascade));
    }

    /** Le {@link ContextMenu} du {@link MenuButton} n’existe qu’après attachement au graphe de scène / skin. */
    private static void ensureColonisationCascadePopupStyle(MenuButton cascade) {
        ContextMenu cm = cascade.getContextMenu();
        if (cm == null) {
            return;
        }
        if (!cm.getStyleClass().contains("colonisation-colony-cascade-popup")) {
            cm.getStyleClass().add("colonisation-colony-cascade-popup");
        }
    }

    /** Même palette que {@code elite-nav-button} (sous-menus : pas de classe sur le popup, seulement sur les lignes). */
    private static void applyColonisationCascadeMenuItemStyle(MenuItem node) {
        node.getStyleClass().add("colonisation-colony-cascade-menu-item");
        if (node instanceof Menu m) {
            for (MenuItem c : m.getItems()) {
                applyColonisationCascadeMenuItemStyle(c);
            }
        }
    }

    private static final Pattern SETTLEMENT_NAME_PARTS = Pattern.compile(
            "^(?<fam>.+?)\\s+T(?<tier>[12])\\s+(?<pad>[SML]|SL|ML|SM)$");
    private static final Pattern ORBITAL_SITE_NAME_PATTERN = Pattern.compile(
            "^orbital\\s+construction\\s+site\\s*:\\s*.+$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PLANETARY_SITE_NAME_PATTERN = Pattern.compile(
            "^planetary\\s+construction\\s+site\\s*:\\s*.+$",
            Pattern.CASE_INSENSITIVE);

    private static Optional<String> inferDefaultConstructionCategory(ColonisationDockEntry dock) {
        if (dock == null) {
            return Optional.of("Orbital");
        }
        if (isColonisationSurfaceStationType(dock.getStationType())) {
            return Optional.of("Surface");
        }
        if (dock.getStationType() != null && !dock.getStationType().isBlank()) {
            return Optional.of("Orbital");
        }
        String siteLabel = firstNonBlank(dock.getSiteNameLocalised(), dock.getStationNameRaw(), "").trim();
        if (siteLabel.isEmpty()) {
            return Optional.of("Orbital");
        }
        if (ORBITAL_SITE_NAME_PATTERN.matcher(siteLabel).matches()) {
            return Optional.of("Orbital");
        }
        if (PLANETARY_SITE_NAME_PATTERN.matcher(siteLabel).matches()) {
            return Optional.of("Surface");
        }
        return Optional.of("Orbital");
    }

    private static Optional<Structure> firstStructureForCategory(List<Structure> catalog, String category) {
        if (catalog == null || category == null || category.isBlank()) {
            return Optional.empty();
        }
        return catalog.stream()
                .filter(s -> s != null && s.category != null && category.equalsIgnoreCase(s.category))
                .min(Comparator
                        .comparing((Structure s) -> s.type != null ? s.type : "", String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(s -> s.name != null ? s.name : "", String.CASE_INSENSITIVE_ORDER));
    }

    private void addTypeMenusForCategory(List<MenuItem> targetItems,
                                         List<Structure> catalog,
                                         String category,
                                         Structure[] selection,
                                         Runnable updateCascadeLabel,
                                         long marketId,
                                         Runnable refreshTip,
                                         Runnable refreshColonyStatsIfDetailForSite) {
        for (String typ : Colony.distinctTypes(catalog, category)) {
            if (isSettlementStructureType(typ)) {
                Menu settlementMenu = new Menu(typ);
                Map<String, ArrayList<Structure>> byFamily = new LinkedHashMap<>();
                for (Structure s : Colony.structuresFor(catalog, category, typ)) {
                    String fam = settlementFamilyKey(s);
                    byFamily.computeIfAbsent(fam, k -> new ArrayList<>()).add(s);
                }
                ArrayList<String> families = new ArrayList<>(byFamily.keySet());
                families.sort(String.CASE_INSENSITIVE_ORDER);
                for (String fam : families) {
                    Menu famMenu = new Menu(fam);
                    ArrayList<Structure> group = byFamily.get(fam);
                    group.sort(Comparator.comparingInt(ColonisationPanelController::settlementTierPadSortKey));
                    for (Structure s : group) {
                        Structure chosen = s;
                        String itemLabel = settlementTierPadLabel(s);
                        MenuItem it = new MenuItem(itemLabel);
                        it.setOnAction(e -> {
                            selection[0] = chosen;
                            updateCascadeLabel.run();
                            preferencesService.setColonisationUserConstructionStructure(marketId, chosen);
                            refreshTip.run();
                            refreshColonyStatsIfDetailForSite.run();
                        });
                        famMenu.getItems().add(it);
                    }
                    settlementMenu.getItems().add(famMenu);
                }
                targetItems.add(settlementMenu);
            } else {
                Menu typMenu = new Menu(typ);
                for (Structure s : Colony.structuresFor(catalog, category, typ)) {
                    Structure chosen = s;
                    String nm = s.name != null ? s.name : "";
                    MenuItem it = new MenuItem(nm);
                    it.setOnAction(e -> {
                        selection[0] = chosen;
                        updateCascadeLabel.run();
                        preferencesService.setColonisationUserConstructionStructure(marketId, chosen);
                        refreshTip.run();
                        refreshColonyStatsIfDetailForSite.run();
                    });
                    typMenu.getItems().add(it);
                }
                targetItems.add(typMenu);
            }
        }
    }

    private static String formatColonyCascadeLabel(Structure s, boolean hideCategory) {
        String c = s.category != null ? s.category : "";
        String t = s.type != null ? s.type : "";
        String n = s.name != null ? s.name : "";
        if (isSettlementStructureType(s.type)) {
            String fam = settlementFamilyKey(s);
            String tail = settlementTierPadLabel(s);
            return hideCategory
                    ? joinCascadeParts(t, fam, tail)
                    : joinCascadeParts(c, t, fam, tail);
        }
        return hideCategory
                ? joinCascadeParts(t, n)
                : joinCascadeParts(c, t, n);
    }

    private String localizedConstructionSiteClass(ColonisationDockEntry dock) {
        if (dock == null) {
            return localizationService.getString("colonisation.detail.stationClassOrbital");
        }
        return isColonisationSurfaceStationType(dock.getStationType())
                ? localizationService.getString("colonisation.detail.stationClassSurface")
                : localizationService.getString("colonisation.detail.stationClassOrbital");
    }

    /**
     * Sous-titre liste construction : corps céleste puis type (StationType journal en mots minuscules ;
     * si type absent : « orbital » / « surface » localisés).
     */
    private String constructionListSubtitleBodyAndType(ColonisationDockEntry dock) {
        String body = resolveDockBodyDisplayName(dock);
        String typePart = formattedJournalStationTypeOrOrbitalSurface(dock);
        if (!body.isBlank() && !typePart.isBlank()) {
            return body + " · " + typePart;
        }
        if (!body.isBlank()) {
            return body;
        }
        return typePart.isBlank() ? "—" : typePart;
    }

    private String formattedJournalStationTypeOrOrbitalSurface(ColonisationDockEntry dock) {
        if (dock == null) {
            return "";
        }
        String raw = dock.getStationType();
        if (raw != null && !raw.isBlank()) {
            String spaced = splitPascalCaseToLowerSpaceSep(raw.trim());
            if (!spaced.isBlank()) {
                return spaced;
            }
        }
        return localizedConstructionSiteClass(dock);
    }

    /** Ex. {@code OrbitalConstructionSite} → {@code orbital construction site}. */
    private static String splitPascalCaseToLowerSpaceSep(String s) {
        if (s == null || s.isBlank()) {
            return "";
        }
        String t = s.replaceAll("([a-z0-9])([A-Z])", "$1 $2")
                .replaceAll("([A-Z]+)([A-Z][a-z])", "$1 $2");
        return t.toLowerCase(Locale.ROOT).trim();
    }

    private static String joinCascadeParts(String... parts) {
        StringBuilder b = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.isBlank()) {
                continue;
            }
            if (!b.isEmpty()) {
                b.append(" — ");
            }
            b.append(p);
        }
        return b.isEmpty() ? "" : b.toString();
    }

    private static boolean isSettlementStructureType(String typ) {
        return typ != null && "Settlement".equals(typ);
    }

    /** Groupe menu (ex. « Agriculture » depuis « Agriculture T1 S »). */
    private static String settlementFamilyKey(Structure s) {
        if (s == null || s.name == null || s.name.isBlank()) {
            return "";
        }
        Matcher m = SETTLEMENT_NAME_PARTS.matcher(s.name.trim());
        if (m.matches()) {
            return m.group("fam").strip();
        }
        return s.name.strip();
    }

    /** Libellé feuille : « T1 S », « T2 L », etc. */
    private static String settlementTierPadLabel(Structure s) {
        if (s == null || s.name == null) {
            return "";
        }
        Matcher m = SETTLEMENT_NAME_PARTS.matcher(s.name.trim());
        if (m.matches()) {
            return "T" + m.group("tier") + " " + m.group("pad");
        }
        return s.name.trim();
    }

    private static int settlementTierPadSortKey(Structure s) {
        if (s == null || s.name == null) {
            return 9999;
        }
        Matcher m = SETTLEMENT_NAME_PARTS.matcher(s.name.trim());
        if (!m.matches()) {
            return 5000 + s.name.hashCode() % 1000;
        }
        int tier = Integer.parseInt(m.group("tier")) * 20;
        String pad = m.group("pad");
        int padOrder = switch (pad) {
            case "S" -> 0;
            case "SM" -> 1;
            case "M" -> 2;
            case "ML" -> 3;
            case "SL" -> 4;
            case "L" -> 5;
            default -> 9;
        };
        return tier + padOrder;
    }

    private static String formatProgress(double progress) {
        double p = progress;
        if (p >= 0 && p <= 1.0) {
            p = p * 100.0;
        }
        return String.format("%.1f %%", p);
    }

    private ColonisationDockEntry findDockEntry(long marketId) {
        for (ColonisationDockEntry e : colonisationService.getDockEntries()) {
            if (e.getMarketId() == marketId) {
                return e;
            }
        }
        return null;
    }

    private static List<ConstructionSiteRow> buildConstructionItems(ColonisationArchitectSystem arch) {
        List<ConstructionSiteRow> out = new ArrayList<>();
        String sysName = arch.getStarSystem();
        for (ColonisationDockEntry site : arch.getSites()) {
            ColonisationConstruction c = site.getConstruction();
            if (c == null) {
                continue;
            }
            String siteLabel = site.getSiteNameLocalised();
            if (siteLabel == null || siteLabel.isBlank()) {
                siteLabel = site.getStationNameRaw() != null ? site.getStationNameRaw() : "";
            }
            out.add(new ConstructionSiteRow(
                    sysName,
                    site.getMarketId(),
                    siteLabel,
                    c.getConstructionProgress(),
                    c.getStatus()));
        }
        return out;
    }

    private void refreshFleetPanel() {
        if (fleetSummaryBox != null) {
            fleetSummaryBox.getChildren().clear();
        }
        if (fleetCargoBarBox != null) {
            fleetCargoBarBox.getChildren().clear();
        }
        if (fleetMarketGrid != null) {
            fleetMarketGrid.getChildren().clear();
        }
        CarrierStatus cs = carrierTradeService.getCarrierStatus();
        if (!cs.isCarrierStatsInitialized()) {
            fleetCargoRowHighlightByMergeKey.clear();
            if (fleetFindOptimalMarketButton != null) {
                fleetFindOptimalMarketButton.setDisable(true);
            }
            Label empty = new Label(localizationService.getString("colonisation.fleet.notInitialized"));
            empty.getStyleClass().add("colonisation-detail-placeholder");
            empty.setWrapText(true);
            empty.setMaxWidth(Double.MAX_VALUE);
            if (fleetSummaryBox != null) {
                fleetSummaryBox.getChildren().add(empty);
            }
            refreshCommanderColonyPanel();
            return;
        }

        FlowPane line1 = new FlowPane(10, 4);
        line1.getStyleClass().add("colonisation-fleet-inline");
        line1.getChildren().addAll(
                fleetInlineLabel("colonisation.fleet.name"),
                fleetInlineValue(cs.getName()),
                fleetInlineSep(),
                fleetInlineLabel("colonisation.fleet.callsign"),
                fleetInlineValue(cs.getCallsign()),
                fleetInlineSep(),
                fleetInlineLabel("colonisation.fleet.system"),
                fleetInlineValue(cs.getPosition() != null ? cs.getPosition().getStarSystem() : ""));
        if (fleetSummaryBox != null) {
            fleetSummaryBox.getChildren().add(line1);
        }

        refreshFleetMarketGrid(cs);
        if (fleetFindOptimalMarketButton != null) {
            fleetFindOptimalMarketButton.setDisable(fleetOptimalMarketSearchInProgress);
        }
        refreshCommanderColonyPanel();
    }

    private void onFleetFindOptimalMarket() {
        if (fleetOptimalMarketSearchInProgress) {
            return;
        }
        CarrierStatus cs = carrierTradeService.getCarrierStatus();
        if (!cs.isCarrierStatsInitialized()) {
            showFleetOptimalMarketMessage(localizationService.getString("colonisation.fleet.notInitialized"));
            return;
        }
        String refSystem = cs.getPosition() != null ? cs.getPosition().getStarSystem() : "";
        if (refSystem == null || refSystem.isBlank()) {
            showFleetOptimalMarketMessage(localizationService.getString("colonisation.fleet.optimalMarket.noCarrierSystem"));
            return;
        }
        List<CommodityRequest> requests = buildFleetOptimalMarketCommodityRequests(cs);
        final boolean avoidPlanetary = fleetAvoidPlanetaryLandingCheckBox != null && fleetAvoidPlanetaryLandingCheckBox.isSelected();
        final boolean largePadOnly = fleetLargePadOnlyCheckBox != null && fleetLargePadOnlyCheckBox.isSelected();
        if (requests.isEmpty()) {
            showFleetOptimalMarketMessage(localizationService.getString("colonisation.fleet.optimalMarket.nothingToSearch"));
            fleetCargoRowHighlightByMergeKey.clear();
            refreshFleetMarketGrid(cs);
            return;
        }
        fleetOptimalMarketSearchInProgress = true;
        if (fleetFindOptimalMarketButton != null) {
            fleetFindOptimalMarketButton.setDisable(true);
        }
        if (fleetOptimalMarketProgress != null) {
            fleetOptimalMarketProgress.setManaged(true);
            fleetOptimalMarketProgress.setVisible(true);
        }
        showFleetOptimalMarketMessage(localizationService.getString("colonisation.fleet.optimalMarket.searching"));
        final String systemArg = refSystem.trim();
        final List<CommodityRequest> requestsArg = List.copyOf(requests);
        Thread t = new Thread(() -> {
            try {
                List<NearbyExportsBestStationResult> stations =
                        colonisationService.suggestBuyStationsForCommodityRequests(
                                systemArg, requestsArg, avoidPlanetary, largePadOnly);
                Platform.runLater(() -> {
                    fleetOptimalMarketSearchInProgress = false;
                    if (fleetOptimalMarketProgress != null) {
                        fleetOptimalMarketProgress.setVisible(false);
                        fleetOptimalMarketProgress.setManaged(false);
                    }
                    if (fleetFindOptimalMarketButton != null) {
                        fleetFindOptimalMarketButton.setDisable(false);
                    }
                    if (stations.isEmpty()) {
                        fleetCargoRowHighlightByMergeKey.clear();
                        showFleetOptimalMarketMessage(localizationService.getString("colonisation.fleet.optimalMarket.noResults"));
                    } else {
                        Map<String, Integer> tonsByKey = buildRequestedTonsByMergeKey(requestsArg);
                        List<NearbyExportsBestStationResult> ordered =
                                sortFleetOptimalStationsByTotalTonsDesc(stations, tonsByKey);
                        rebuildFleetCargoHighlightsFromStations(ordered);
                        populateFleetOptimalMarketResults(ordered, tonsByKey);
                    }
                    refreshFleetMarketGrid(cs);
                });
            } catch (IOException e) {
                Platform.runLater(() -> {
                    fleetOptimalMarketSearchInProgress = false;
                    if (fleetOptimalMarketProgress != null) {
                        fleetOptimalMarketProgress.setVisible(false);
                        fleetOptimalMarketProgress.setManaged(false);
                    }
                    if (fleetFindOptimalMarketButton != null) {
                        fleetFindOptimalMarketButton.setDisable(false);
                    }
                    fleetCargoRowHighlightByMergeKey.clear();
                    showFleetOptimalMarketMessage(localizationService.getString("colonisation.fleet.optimalMarket.error") + " " + e.getMessage());
                    refreshFleetMarketGrid(cs);
                });
            }
        }, "fleet-optimal-market");
        t.setDaemon(true);
        t.start();
    }

    private List<CommodityRequest> buildFleetOptimalMarketCommodityRequests(CarrierStatus cs) {
        List<CommodityRequest> out = new ArrayList<>();
        for (FleetMarketRow r : buildFleetMergedRows(cs)) {
            if (r.getMissing() <= 0) {
                continue;
            }
            if (r.getCommodity() == null) {
                continue;
            }
            String cargo = r.getCommodity().getCargoJsonName();
            if (cargo == null || cargo.isBlank()) {
                continue;
            }
            out.add(new CommodityRequest().name(cargo).volume(r.getMissing()));
        }
        return out;
    }

    private void showFleetOptimalMarketMessage(String text) {
        if (fleetOptimalMarketResultsBox == null) {
            return;
        }
        fleetOptimalMarketResultsBox.getChildren().clear();
        Label l = new Label(text);
        l.setWrapText(true);
        l.setMaxWidth(Double.MAX_VALUE);
        l.getStyleClass().add("colonisation-detail-placeholder");
        fleetOptimalMarketResultsBox.getChildren().add(l);
    }

    private void rebuildFleetCargoHighlightsFromStations(List<NearbyExportsBestStationResult> stations) {
        fleetCargoRowHighlightByMergeKey.clear();
        int idx = 0;
        for (NearbyExportsBestStationResult st : stations) {
            if (st == null) {
                continue;
            }
            Color c = fleetStationColor(idx++);
            if (st.getMatches() == null) {
                continue;
            }
            for (MatchedCommodityNearbyExport m : st.getMatches()) {
                if (m == null || m.getRequestedCommodityName() == null) {
                    continue;
                }
                String mk = ColonisationCommodityKeys.normalizeMergeKey(m.getRequestedCommodityName());
                if (mk.isBlank()) {
                    continue;
                }
                fleetCargoRowHighlightByMergeKey.putIfAbsent(mk, c);
                ICommodity resolved = CarrierCommodityResolver.resolve(
                        m.getRequestedCommodityName() != null ? m.getRequestedCommodityName() : "", "");
                String mkResolved = ColonisationCommodityKeys.mergeKey(resolved);
                if (!mkResolved.isBlank() && !mkResolved.equals(mk)) {
                    fleetCargoRowHighlightByMergeKey.putIfAbsent(mkResolved, c);
                }
            }
        }
    }

    private static Map<String, Integer> buildRequestedTonsByMergeKey(List<CommodityRequest> requests) {
        Map<String, Integer> map = new HashMap<>();
        if (requests == null) {
            return map;
        }
        for (CommodityRequest cr : requests) {
            if (cr == null || cr.getName() == null || cr.getName().isBlank()) {
                continue;
            }
            String mk = ColonisationCommodityKeys.normalizeMergeKey(cr.getName());
            if (mk.isBlank()) {
                continue;
            }
            int v = cr.getVolume() != null ? cr.getVolume() : 0;
            map.merge(mk, Math.max(0, v), Integer::sum);
        }
        return map;
    }

    private static int fleetOptimalStationTotalRequestedTons(
            NearbyExportsBestStationResult st,
            Map<String, Integer> requestedTonsByMergeKey) {
        if (st == null || st.getMatches() == null || requestedTonsByMergeKey == null || requestedTonsByMergeKey.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (MatchedCommodityNearbyExport m : st.getMatches()) {
            if (m == null || m.getRequestedCommodityName() == null) {
                continue;
            }
            String mk = ColonisationCommodityKeys.normalizeMergeKey(m.getRequestedCommodityName());
            total += requestedTonsByMergeKey.getOrDefault(mk, 0);
        }
        return total;
    }

    /** Plus de tonnes demandées en tête (même ordre que les couleurs du tableau fleet). */
    private static List<NearbyExportsBestStationResult> sortFleetOptimalStationsByTotalTonsDesc(
            List<NearbyExportsBestStationResult> stations,
            Map<String, Integer> requestedTonsByMergeKey) {
        if (stations == null || stations.isEmpty()) {
            return List.of();
        }
        List<NearbyExportsBestStationResult> copy = new ArrayList<>(stations);
        copy.sort((a, b) -> {
            int ta = fleetOptimalStationTotalRequestedTons(a, requestedTonsByMergeKey);
            int tb = fleetOptimalStationTotalRequestedTons(b, requestedTonsByMergeKey);
            if (tb != ta) {
                return Integer.compare(tb, ta);
            }
            String la = fleetOptimalStationSortLabel(a);
            String lb = fleetOptimalStationSortLabel(b);
            return la.compareToIgnoreCase(lb);
        });
        return copy;
    }

    private static String fleetOptimalStationSortLabel(NearbyExportsBestStationResult st) {
        if (st == null) {
            return "";
        }
        return firstNonBlank(st.getSystemName(), "") + "\u0000" + firstNonBlank(st.getStationName(), "");
    }

    private void populateFleetOptimalMarketResults(
            List<NearbyExportsBestStationResult> stations,
            Map<String, Integer> requestedTonsByMergeKey) {
        if (fleetOptimalMarketResultsBox == null) {
            return;
        }
        fleetOptimalMarketResultsBox.getChildren().clear();
        int idx = 0;
        for (NearbyExportsBestStationResult st : stations) {
            if (st == null) {
                continue;
            }
            Color c = fleetStationColor(idx++);
            VBox card = new VBox(4);
            card.getStyleClass().add("colonisation-fleet-optimal-result-card");
            card.setMaxWidth(Double.MAX_VALUE);

            HBox head = new HBox(4);
            head.setAlignment(Pos.CENTER_LEFT);
            String sysText = firstNonBlank(st.getSystemName(), "—");
            String stationText = firstNonBlank(st.getStationName(), "—");
            Label sysPart = createCopyableFleetOptimalSystemLabel(sysText);
            Label sep = new Label(" - ");
            sep.getStyleClass().add("colonisation-fleet-optimal-result-sep");
            Label stationPart = createCopyableFleetOptimalStationLabel(stationText, c);
            HBox.setHgrow(stationPart, Priority.ALWAYS);
            head.getChildren().addAll(sysPart, sep, stationPart);

            List<String> names = new ArrayList<>();
            if (st.getMatches() != null) {
                for (MatchedCommodityNearbyExport m : st.getMatches()) {
                    if (m == null) {
                        continue;
                    }
                    String n = fleetOptimalMatchedCommodityVisibleName(m);
                    if (!n.isBlank()) {
                        names.add(n);
                    }
                }
            }
            Label commodities = new Label(String.join(", ", names));
            commodities.setWrapText(true);
            commodities.setMaxWidth(Double.MAX_VALUE);
            commodities.getStyleClass().add("colonisation-fleet-optimal-result-commodities");

            int totalTons = fleetOptimalStationTotalRequestedTons(st, requestedTonsByMergeKey);
            Label totalLabel = new Label(localizationService.getString("colonisation.fleet.optimalMarket.totalTons", totalTons));
            totalLabel.setWrapText(true);
            totalLabel.setMaxWidth(Double.MAX_VALUE);
            totalLabel.getStyleClass().add("colonisation-fleet-optimal-result-total-tons");

            card.getChildren().addAll(head, commodities, totalLabel);
            fleetOptimalMarketResultsBox.getChildren().add(card);
        }
    }

    /** Teintes espacées (ratio d’or) ; phase +215° pour éviter rouge et vert sur la 1re station. */
    private static Color fleetStationColor(int index) {
        double hue = (index * 0.38196601125 * 360.0 + 215.0) % 360.0;
        return Color.hsb(hue, 0.62, 0.92, 1.0);
    }

    private String fleetOptimalMatchedCommodityVisibleName(MatchedCommodityNearbyExport m) {
        if (m == null) {
            return "";
        }
        String req = m.getRequestedCommodityName();
        ICommodity comm = CarrierCommodityResolver.resolve(req != null ? req : "", "");
        if (comm != null) {
            String vis = comm.getVisibleName();
            if (vis != null && !vis.isBlank()) {
                return vis;
            }
        }
        if (m.getExport() != null && m.getExport().getCommodityName() != null && !m.getExport().getCommodityName().isBlank()) {
            return m.getExport().getCommodityName();
        }
        return firstNonBlank(req, "");
    }

    private Color fleetRouteColorForRow(FleetMarketRow r) {
        if (r == null || r.getMissing() <= 0) {
            return null;
        }
        String mk = r.getCommodityKey();
        Color c = fleetCargoRowHighlightByMergeKey.get(mk);
        if (c != null) {
            return c;
        }
        if (r.getCommodity() != null) {
            String cargo = r.getCommodity().getCargoJsonName();
            if (cargo != null && !cargo.isBlank()) {
                c = fleetCargoRowHighlightByMergeKey.get(ColonisationCommodityKeys.normalizeMergeKey(cargo));
            }
        }
        return c;
    }

    private static void clearFleetMarketRowHighlight(Label name, Label shipL, Label stock, Label missing, Label buyOrder, Label price) {
        for (Label cell : List.of(name, shipL, stock, missing, buyOrder, price)) {
            cell.setBackground(Background.EMPTY);
        }
        name.setStyle(null);
    }

    /**
     * Couleur uniquement sur le libellé commodité : le thème impose {@code -fx-text-fill} sur {@code .cargo-mineral-name},
     * donc style inline pour la couleur station.
     */
    private void applyFleetMarketRowRouteHighlight(
            FleetMarketRow r, Label name, Label shipL, Label stock, Label missing, Label buyOrder, Label price) {
        Color c = fleetRouteColorForRow(r);
        if (c == null) {
            clearFleetMarketRowHighlight(name, shipL, stock, missing, buyOrder, price);
            return;
        }
        shipL.setBackground(Background.EMPTY);
        stock.setBackground(Background.EMPTY);
        missing.setBackground(Background.EMPTY);
        buyOrder.setBackground(Background.EMPTY);
        price.setBackground(Background.EMPTY);
        name.setBackground(Background.EMPTY);
        int rr = (int) Math.round(c.getRed() * 255);
        int gg = (int) Math.round(c.getGreen() * 255);
        int bb = (int) Math.round(c.getBlue() * 255);
        name.setStyle(String.format(Locale.ROOT, "-fx-text-fill: rgb(%d,%d,%d); -fx-font-weight: bold;", rr, gg, bb));
    }

    /**
     * Panneau Fleet (colonne droite) basé sur la construction list : cartes colonies + ressources agrégées.
     */
    private void refreshCommanderColonyPanel() {
        Platform.runLater(() -> {
            if (commanderColonyVBox == null) {
                return;
            }
            commanderColonyVBox.getChildren().clear();
            List<ColonisationDockEntry> docks = selectedConstructionListDocks();
            if (docks.isEmpty()) {
                Node ph = buildConstructionListEmptyPlaceholder();
                if (ph instanceof Region r) {
                    r.setMaxWidth(Double.MAX_VALUE);
                }
                commanderColonyVBox.getChildren().add(ph);
                return;
            }
            for (ColonisationDockEntry dock : docks) {
                commanderColonyVBox.getChildren().add(buildFleetColonySiteCard(dock));
                ColonisationConstruction c = dock.getConstruction();
                if (c != null) {
                    commanderColonyVBox.getChildren().add(buildConstructionListProgressNode(c));
                }
            }
            List<ConstructionResource> aggregated = aggregateConstructionListResources(docks);
            if (!aggregated.isEmpty()) {
                addConstructionResourcesSection(commanderColonyVBox, aggregated);
            }
        });
    }

    private VBox buildFleetColonySiteCard(ColonisationDockEntry site) {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("colonisation-buy-station-wrap", "colonisation-fleet-colony-card");
        String headline = firstNonBlank(site.getSiteNameLocalised(), site.getStationNameRaw(), "—");
        Label title = new Label(headline);
        title.getStyleClass().add("colonisation-fleet-colony-title");
        title.setWrapText(true);
        title.setMaxWidth(Double.MAX_VALUE);
        HBox sysRow = new HBox(8);
        sysRow.setAlignment(Pos.CENTER_LEFT);
        Label sysK = new Label(localizationService.getString("colonisation.fleet.system") + ":");
        sysK.getStyleClass().add("colonisation-fleet-colony-k");
        Label sysV = new Label(firstNonBlank(site.getStarSystem(), "—"));
        sysV.getStyleClass().add("colonisation-fleet-colony-v");
        sysV.setWrapText(true);
        HBox.setHgrow(sysV, Priority.ALWAYS);
        sysRow.getChildren().addAll(sysK, sysV);
        card.getChildren().addAll(title, sysRow);
        return card;
    }

    private HBox buildFleetColonyStatusRow(ColonisationConstruction c) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("colonisation-fleet-colony-status-row");
        Label k = new Label(localizationService.getString("colonisation.fleet.currentColony.status") + ":");
        k.getStyleClass().add("colonisation-fleet-colony-k");
        Label badge = new Label(localizedConstructionStatus(c.getStatus()));
        badge.getStyleClass().addAll(
                "colonisation-construction-badge",
                "colonisation-fleet-colony-status-badge");
        badge.setWrapText(true);
        HBox.setHgrow(badge, Priority.ALWAYS);
        row.getChildren().addAll(k, badge);
        return row;
    }

    /**
     * Ressources requises (fourni / requis), sans regroupement par stations d’achat (réservé au panneau détail).
     */
    private void addFleetColonyResourcesSection(VBox parent, ColonisationConstruction c) {
        List<ConstructionResource> req = sortResourcesRequiredIncompleteFirst(c.getResourcesRequired());
        if (req.isEmpty()) {
            return;
        }
        appendSectionTitle(parent, "colonisation.detail.section.resources");
        int[] w = maxProvidedRequiredDigits(req);
        VBox wrap = new VBox(4);
        wrap.getStyleClass().add("colonisation-buy-station-wrap");
        addColoredResourceRows(wrap, req, w[0], w[1]);
        parent.getChildren().add(wrap);
    }

    /** Tonnes par commodité (clé fusion) dans le vaisseau : journal / {@link MiningService#getCargo}. */
    private Map<String, Integer> buildShipStockTonsByMergeKey() {
        Map<String, Integer> out = new HashMap<>();
        CommanderShip ship = commanderStatus.getShip();
        CommanderShip.ShipCargo cargo = miningService.getCargo();
        if (ship == null || cargo == null || cargo.getCommodities() == null) {
            return out;
        }
        for (Map.Entry<ICommodity, Integer> e : cargo.getCommodities().entrySet()) {
            ICommodity comm = e.getKey();
            if (comm == null) {
                continue;
            }
            String k = ColonisationCommodityKeys.mergeKey(comm);
            if (k.isBlank()) {
                continue;
            }
            int q = e.getValue() != null ? e.getValue() : 0;
            out.merge(k, Math.max(0, q), Integer::sum);
        }
        return out;
    }

    private Label placeholderLabel(String messageKey) {
        Label ph = new Label(localizationService.getString(messageKey));
        ph.getStyleClass().add("colonisation-detail-placeholder");
        ph.setWrapText(true);
        ph.setMaxWidth(Double.MAX_VALUE);
        return ph;
    }

    private VBox buildConstructionListEmptyPlaceholder() {
        VBox box = new VBox(10);
        box.setMaxWidth(Double.MAX_VALUE);
        Label title = new Label(localizationService.getString("colonisation.list.empty"));
        title.getStyleClass().add("colonisation-detail-placeholder");
        title.setWrapText(true);
        title.setMaxWidth(Double.MAX_VALUE);
        Label hint = new Label(localizationService.getString("colonisation.list.empty.hint"));
        hint.getStyleClass().addAll("colonisation-detail-placeholder", "colonisation-buy-station-meta-dim");
        hint.setWrapText(true);
        hint.setMaxWidth(Double.MAX_VALUE);
        box.getChildren().addAll(title, hint);
        return box;
    }

    /** Bandeau type « cargo actuel » : total tonnes, détail par commodité, barre / capacité (marché noir seulement en tête de panneau). */
    private void refreshFleetCargoBar(CarrierStatus cs) {
        int totalTons = cs.sumPhysicalStocksTons();
        if (totalTons == 0) {
            for (CarrierTradeOrderEntry e : cs.getActiveTransactions()) {
                if (e != null) {
                    totalTons += Math.max(0, e.getStock());
                }
            }
        }
        int cap = Math.max(cs.getTotalCapacity(), 1);
        double pct = Math.min(1.0, Math.max(0.0, (double) totalTons / (double) cap));

        HBox bar = new HBox(12);
        bar.getStyleClass().addAll("cargo-info", "colonisation-fleet-cargo-info");

        VBox left = new VBox(4);
        left.setAlignment(Pos.CENTER_LEFT);
        left.setPrefWidth(72);
        Label tonsNum = new Label(Integer.toString(totalTons));
        tonsNum.getStyleClass().add("cargo-stat-number");
        Label tonsLbl = new Label(localizationService.getString("colonisation.fleet.stockTons"));
        tonsLbl.getStyleClass().add("cargo-stat-label");
        left.getChildren().addAll(tonsNum, tonsLbl);

        VBox mid = new VBox(4);
        mid.setAlignment(Pos.CENTER);
        mid.setPrefWidth(180);
        HBox.setHgrow(mid, Priority.ALWAYS);
        ProgressBar pb = new ProgressBar(pct);
        pb.setPrefWidth(130);
        pb.setPrefHeight(8);
        pb.getStyleClass().add("progress-bar");
        Label used = new Label(totalTons + "/" + cs.getTotalCapacity());
        used.getStyleClass().addAll("cargo-stat-number", "cargo-stat-number-wide");
        mid.getChildren().addAll(pb, used);

        bar.getChildren().addAll(left, mid);
        fleetCargoBarBox.getChildren().add(bar);
    }

    private void refreshFleetMarketGrid(CarrierStatus cs) {
        fleetMarketGrid.getChildren().clear();
        addFleetMarketHeaderRow();
        List<FleetMarketRow> rows = buildFleetMergedRows(cs);
        Map<String, Integer> shipByKey = buildShipStockTonsByMergeKey();
        if (rows.isEmpty()) {
            Label empty = new Label(localizationService.getString("colonisation.fleet.marketEmpty"));
            empty.getStyleClass().add("cargo-mineral-null-price");
            empty.setWrapText(true);
            fleetMarketGrid.add(empty, 0, 1, 6, 1);
            return;
        }
        int row = 1;
        CommodityCategory lastCategory = null;
        for (FleetMarketRow r : rows) {
            CommodityCategory cat = fleetMarketRowCategory(r);
            if (lastCategory != cat) {
                Label catHead = new Label(fleetCommodityCategoryLabel(cat));
                catHead.getStyleClass().add("colonisation-fleet-commodity-category");
                catHead.setMaxWidth(Double.MAX_VALUE);
                fleetMarketGrid.add(catHead, 0, row, 6, 1);
                row++;
                lastCategory = cat;
            }
            Label name = new Label(r.getDisplayName());
            name.getStyleClass().add("cargo-mineral-name");
            name.setWrapText(true);
            name.setMaxWidth(Double.MAX_VALUE);
            int shipT = shipByKey.getOrDefault(r.getCommodityKey(), 0);
            Label shipL = new Label(shipT > 0 ? Integer.toString(shipT) : "—");
            shipL.getStyleClass().add(shipT > 0 ? "cargo-mineral-quantity" : "cargo-mineral-null-price");
            int po = r.getPurchaseOrder();
            int so = r.getSaleOrder();
            int marketOrder = po > 0 ? po : so;
            Label buyOrder = new Label(marketOrder > 0 ? Integer.toString(marketOrder) : "—");
            if (po > 0) {
                buyOrder.setText("▲ " + marketOrder);
                buyOrder.getStyleClass().add("colonisation-fleet-market-order-buy");
            } else if (so > 0) {
                buyOrder.setText("▼ " + marketOrder);
                buyOrder.getStyleClass().add("colonisation-fleet-market-order-sell");
            } else {
                buyOrder.getStyleClass().add("cargo-mineral-null-price");
            }
            String priceText;
            if (po <= 0) {
                priceText = "—";
            } else {
                priceText = formatCreditsThousandsDots(r.getCarrierPurchaseBidPerTonCr());
            }
            Label price = new Label(priceText);
            price.getStyleClass().add(po > 0 ? "cargo-mineral-total-price" : "cargo-mineral-null-price");
            Label stock = new Label(Integer.toString(r.getStock()));
            stock.getStyleClass().add(r.getStock() > 0 ? "cargo-mineral-quantity" : "cargo-mineral-null-price");
            Label missing = new Label(r.getMissing() > 0 ? Integer.toString(r.getMissing()) : "—");
            missing.getStyleClass().add(r.getMissing() > 0 ? "cargo-mineral-unit-price" : "cargo-mineral-null-price");
            applyFleetMarketRowRouteHighlight(r, name, shipL, stock, missing, buyOrder, price);
            fleetMarketGrid.add(name, 0, row);
            fleetMarketGrid.add(shipL, 1, row);
            fleetMarketGrid.add(stock, 2, row);
            fleetMarketGrid.add(missing, 3, row);
            fleetMarketGrid.add(buyOrder, 4, row);
            fleetMarketGrid.add(price, 5, row);
            row++;
        }
    }

    private void addFleetMarketHeaderRow() {
        Label commodity = fleetMarketHeaderLabel("colonisation.fleet.col.commodity");
        Label ship = fleetMarketHeaderLabel("colonisation.fleet.col.shipStock");
        Label stock = fleetMarketHeaderLabel("colonisation.fleet.col.stock");
        Label missing = fleetMarketHeaderLabel("colonisation.fleet.col.missing");
        Label buyOrder = fleetMarketHeaderLabel("colonisation.fleet.col.buyOrder");
        Label price = fleetMarketHeaderLabel("colonisation.fleet.col.price");

        fleetMarketGrid.add(commodity, 0, 0);
        fleetMarketGrid.add(ship, 1, 0);
        fleetMarketGrid.add(stock, 2, 0);
        fleetMarketGrid.add(missing, 3, 0);
        fleetMarketGrid.add(buyOrder, 4, 0);
        fleetMarketGrid.add(price, 5, 0);

        GridPane.setHalignment(commodity, HPos.LEFT);
        GridPane.setHalignment(ship, HPos.LEFT);
        GridPane.setHalignment(stock, HPos.LEFT);
        GridPane.setHalignment(missing, HPos.LEFT);
        GridPane.setHalignment(buyOrder, HPos.LEFT);
        GridPane.setHalignment(price, HPos.LEFT);
    }

    private Label fleetMarketHeaderLabel(String messageKey) {
        Label l = new Label(localizationService.getString(messageKey).toUpperCase(Locale.ROOT));
        l.getStyleClass().add("cargo-column-header");
        return l;
    }

    /** Séparateur de milliers « . » (ex. 1.234.567) pour les Cr affichés dans le tableau fleet. */
    private static String formatCreditsThousandsDots(long amount) {
        if (amount == 0L) {
            return "0";
        }
        String s = Long.toString(amount);
        int len = s.length();
        int firstGroup = len % 3;
        if (firstGroup == 0) {
            firstGroup = 3;
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < firstGroup; i++) {
            out.append(s.charAt(i));
        }
        for (int i = firstGroup; i < len; i += 3) {
            out.append('.');
            out.append(s, i, i + 3);
        }
        return out.toString();
    }

    private List<FleetMarketRow> buildFleetMarketRows(CarrierStatus cs) {
        Map<ICommodity, Integer> stocks = cs.getStocksByCommodity();
        Map<String, FleetMarketRow> acc = new LinkedHashMap<>();

        for (CarrierTradeOrderEntry e : cs.getActiveTransactions()) {
            if (e == null) {
                continue;
            }
            if (e.getCommodity() == null || CarrierStatus.isFleetStockExcludedDrone(e.getCommodity())) {
                continue;
            }
            ICommodity nk = cs.canonicalCommodity(e);
            if (nk == null || ColonisationCommodityKeys.mergeKey(nk).isBlank()) {
                continue;
            }
            int stockVal = Math.max(e.getStock(), cs.physicalStock(nk));
            if (stockVal <= 0 && e.getPurchaseOrder() == 0 && e.getSaleOrder() == 0) {
                continue;
            }
            String display = firstNonBlank(
                    nk.getTitleName(),
                    nk.getVisibleName(),
                    nk.getCargoJsonName());
            /*
             * Journal CarrierTradeOrder : Price est le prix à la tonne pour l’ordre actif.
             * Pour un ordre d’achat (PurchaseOrder > 0), c’est ce que le carrier propose d’offrir par tonne ;
             * pour un ordre de vente seul, ce serait le prix de vente — on ne le mélange pas dans cette colonne.
             */
            long carrierPurchaseBidPerTonCr = e.getPurchaseOrder() > 0 ? e.getPrice() : 0L;
            String rowKey = ColonisationCommodityKeys.mergeKey(nk);
            acc.put(rowKey, new FleetMarketRow(nk, display, stockVal, e.getPurchaseOrder(), e.getSaleOrder(), carrierPurchaseBidPerTonCr));
        }

        if (stocks != null) {
            for (Map.Entry<ICommodity, Integer> en : stocks.entrySet()) {
                int st = en.getValue() == null ? 0 : en.getValue();
                if (st <= 0) {
                    continue;
                }
                ICommodity comm = en.getKey();
                if (comm == null) {
                    continue;
                }
                if (CarrierStatus.isFleetStockExcludedDrone(comm)) {
                    continue;
                }
                String nk = ColonisationCommodityKeys.mergeKey(comm);
                if (nk.isBlank()) {
                    continue;
                }
                if (acc.containsKey(nk)) {
                    FleetMarketRow old = acc.get(nk);
                    acc.put(nk, old.withStock(Math.max(old.getStock(), st)));
                } else {
                    acc.put(nk, new FleetMarketRow(comm, cs.displayLabel(comm), st, 0, 0, 0L));
                }
            }
        }

        List<FleetMarketRow> out = new ArrayList<>(acc.values());
        out.sort(Comparator
                .comparing(this::fleetMarketRowCategorySortKey, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(FleetMarketRow::getDisplayName, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    private List<FleetMarketRow> buildFleetMergedRows(CarrierStatus cs) {
        List<FleetMarketRow> base = buildFleetMarketRows(cs);
        Map<String, FleetMarketRow> byCommodity = new LinkedHashMap<>();
        for (FleetMarketRow r : base) {
            byCommodity.put(r.getCommodityKey(), r);
        }
        Map<String, Integer> missingByCommodity = buildMissingByCommodity();
        Map<String, String> missingDisplayByCommodity = buildMissingDisplayByCommodity();
        for (Map.Entry<String, Integer> e : missingByCommodity.entrySet()) {
            String k = e.getKey();
            int missing = e.getValue();
            FleetMarketRow existing = byCommodity.get(k);
            if (existing != null) {
                byCommodity.put(k, existing.withMissing(missing));
            } else {
                String display = missingDisplayByCommodity.getOrDefault(k, k);
                ICommodity onlyMissing = CarrierCommodityResolver.resolve(k, display);
                byCommodity.put(k, new FleetMarketRow(onlyMissing, display, 0, 0, 0, 0L, missing));
            }
        }
        List<FleetMarketRow> out = new ArrayList<>(byCommodity.values());
        out.sort(Comparator
                .comparing(this::fleetMarketRowCategorySortKey, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(FleetMarketRow::getDisplayName, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    /** Clé de tri des groupes : libellé {@code colonisation.fleet.commodityCategory.<ENUM>}. */
    private String fleetMarketRowCategorySortKey(FleetMarketRow r) {
        return fleetCommodityCategoryLabel(fleetMarketRowCategory(r));
    }

    private static CommodityCategory fleetMarketRowCategory(FleetMarketRow r) {
        if (r == null || r.getCommodity() == null) {
            return CommodityCategory.UNKNOWN;
        }
        return r.getCommodity().getInaraCommodityCategory();
    }

    private String fleetCommodityCategoryLabel(CommodityCategory cat) {
        return localizationService.getString("colonisation.fleet.commodityCategory." + cat.name());
    }

    private Map<String, Integer> buildMissingByCommodity() {
        Map<String, Integer> out = new HashMap<>();
        for (ColonisationDockEntry dock : selectedConstructionListDocks()) {
            ColonisationConstruction c = dock.getConstruction();
            if (c == null || c.getResourcesRequired() == null) {
                continue;
            }
            for (ConstructionResource r : c.getResourcesRequired()) {
                int missing = Math.max(0, r.getRequiredAmount() - r.getProvidedAmount());
                if (missing <= 0) {
                    continue;
                }
                if (r.getCommodity() == null) {
                    continue;
                }
                String key = ColonisationCommodityKeys.mergeKey(r.getCommodity());
                if (!key.isBlank()) {
                    out.merge(key, missing, Integer::sum);
                }
            }
        }
        return out;
    }

    private Map<String, String> buildMissingDisplayByCommodity() {
        Map<String, String> out = new HashMap<>();
        for (ColonisationDockEntry dock : selectedConstructionListDocks()) {
            ColonisationConstruction c = dock.getConstruction();
            if (c == null || c.getResourcesRequired() == null) {
                continue;
            }
            for (ConstructionResource r : c.getResourcesRequired()) {
                if (r.getCommodity() == null) {
                    continue;
                }
                String key = ColonisationCommodityKeys.mergeKey(r.getCommodity());
                if (key.isBlank()) {
                    continue;
                }
                out.putIfAbsent(key, firstNonBlank(r.displayLabel(), key));
            }
        }
        return out;
    }

    private static String normalizeCommodityKey(String raw) {
        return ColonisationCommodityKeys.normalizeMergeKey(raw);
    }

    private Label fleetInlineLabel(String messageKey) {
        Label l = new Label(localizationService.getString(messageKey));
        l.getStyleClass().add("colonisation-fleet-inline-dim");
        return l;
    }

    private Label fleetInlineValue(String text) {
        Label l = new Label(text != null && !text.isBlank() ? text : "—");
        l.getStyleClass().add("colonisation-fleet-inline-value");
        l.setWrapText(false);
        return l;
    }

    private Label fleetInlineSep() {
        Label s = new Label("·");
        s.getStyleClass().add("colonisation-fleet-inline-sep");
        return s;
    }

    /**
     * Définit le chantier courant (persisté dans {@code ~/.elite-warboard/preferences.properties}) puis lance la recherche de stations d’achat.
     * Les suggestions « optimal stations » ne sont pas persistées.
     */
    private void onBuildStation() {
        if (selectedConstructionRow == null) {
            statusLabel.setText(localizationService.getString("colonisation.buy.needSelection"));
            return;
        }
        ColonisationDockEntry dock = findDockEntry(selectedConstructionRow.getMarketId());
        if (dock == null) {
            statusLabel.setText(localizationService.getString("colonisation.buy.noDock"));
            return;
        }
        clearSuggestedBuyStations();
        colonisationService.designateBuildingSite(selectedConstructionRow.getMarketId());
        rebuildArchitectSystemCards();
        suggestBuyStationsRequestInProgress = true;
        updateButtonStates();
        statusLabel.setText(localizationService.getString("colonisation.buildStationSearching"));
        final long requestMarketId = selectedConstructionRow.getMarketId();
        runSuggestBuyStationsWorker(requestMarketId);
    }

    /**
     * Rafraîchit uniquement les stations d’achat pour le chantier déjà enregistré (sans redésigner le site).
     */
    private void onUpdateTradeStations() {
        if (!isSelectedRowCurrentBuildingSite() || selectedConstructionRow == null) {
            return;
        }
        ColonisationDockEntry dock = findDockEntry(selectedConstructionRow.getMarketId());
        if (dock == null) {
            statusLabel.setText(localizationService.getString("colonisation.buy.noDock"));
            return;
        }
        clearSuggestedBuyStations();
        refreshConstructionDetailPanel();
        suggestBuyStationsRequestInProgress = true;
        updateButtonStates();
        statusLabel.setText(localizationService.getString("colonisation.updateTradeSearching"));
        runSuggestBuyStationsWorker(selectedConstructionRow.getMarketId());
    }

    private void runSuggestBuyStationsWorker(long requestMarketId) {
        final List<ColonisationDockEntry> constructionListSnapshot = selectedConstructionListDocks();
        Thread t = new Thread(() -> {
            try {
                ColonisationDockEntry dockRef = findDockEntry(requestMarketId);
                if (dockRef == null) {
                    Platform.runLater(() -> {
                        suggestBuyStationsRequestInProgress = false;
                        updateButtonStates();
                    });
                    return;
                }
                List<NearbyExportsBestStationResult> list = constructionListSnapshot.isEmpty()
                        ? colonisationService.suggestBuyStationsForDock(dockRef, false)
                        : colonisationService.suggestBuyStationsForConstructionDocks(constructionListSnapshot, false);
                int n = list.size();
                Platform.runLater(() -> {
                    if (selectedConstructionRow == null || selectedConstructionRow.getMarketId() != requestMarketId) {
                        suggestBuyStationsRequestInProgress = false;
                        updateButtonStates();
                        return;
                    }
                    suggestedBuyStations = List.copyOf(list);
                    refreshConstructionDetailPanel();
                    if (n == 0) {
                        statusLabel.setText(localizationService.getString("colonisation.buy.noStations"));
                    } else {
                        statusLabel.setText("");
                    }
                    suggestBuyStationsRequestInProgress = false;
                    updateButtonStates();
                });
            } catch (IOException e) {
                Platform.runLater(() -> {
                    if (selectedConstructionRow != null && selectedConstructionRow.getMarketId() == requestMarketId) {
                        suggestedBuyStations = List.of();
                        refreshConstructionDetailPanel();
                        statusLabel.setText(localizationService.getString("colonisation.stationsError") + " " + e.getMessage());
                    }
                    suggestBuyStationsRequestInProgress = false;
                    updateButtonStates();
                });
            }
        }, "colonisation-suggest-buy");
        t.setDaemon(true);
        t.start();
    }

    private void updateButtonStates() {
        if (updateTradeStationButton != null) {
            updateTradeStationButton.setVisible(false);
            updateTradeStationButton.setManaged(false);
        }
    }

    private static String firstNonBlank(String... parts) {
        if (parts == null) {
            return "";
        }
        for (String p : parts) {
            if (p != null && !p.isBlank()) {
                return p;
            }
        }
        return "";
    }

    public static final class FleetMarketRow {
        private final ICommodity commodity;
        private final String displayName;
        private final int stock;
        private final int purchaseOrder;
        private final int saleOrder;
        /** Crédits par tonne offerts par le carrier pour un ordre d’achat (journal), 0 si pas d’ordre d’achat. */
        private final long price;
        private final int missing;

        public FleetMarketRow(ICommodity commodity, String displayName, int stock, int purchaseOrder, int saleOrder, long price) {
            this(commodity, displayName, stock, purchaseOrder, saleOrder, price, 0);
        }

        public FleetMarketRow(ICommodity commodity, String displayName, int stock, int purchaseOrder, int saleOrder, long price, int missing) {
            this.commodity = commodity;
            String fallback =
                    commodity != null && commodity.getCargoJsonName() != null ? commodity.getCargoJsonName() : "";
            this.displayName = displayName != null && !displayName.isBlank() ? displayName : fallback;
            this.stock = stock;
            this.purchaseOrder = purchaseOrder;
            this.saleOrder = saleOrder;
            this.price = price;
            this.missing = missing;
        }

        public FleetMarketRow withStock(int newStock) {
            return new FleetMarketRow(commodity, displayName, newStock, purchaseOrder, saleOrder, price, missing);
        }

        public FleetMarketRow withMissing(int newMissing) {
            return new FleetMarketRow(commodity, displayName, stock, purchaseOrder, saleOrder, price, newMissing);
        }

        public ICommodity getCommodity() {
            return commodity;
        }

        /** Clé de fusion avec les ressources chantier (même logique qu’avant, dérivée du {@link ICommodity}). */
        public String getCommodityKey() {
            return ColonisationCommodityKeys.mergeKey(commodity);
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getStock() {
            return stock;
        }

        public int getPurchaseOrder() {
            return purchaseOrder;
        }

        public int getSaleOrder() {
            return saleOrder;
        }

        public long getPrice() {
            return price;
        }

        /** Prix unitaire (Cr/t) auquel le Fleet Carrier achète la commodité (offre d’achat), ou 0. */
        public long getCarrierPurchaseBidPerTonCr() {
            return purchaseOrder > 0 ? price : 0L;
        }

        public int getMissing() {
            return missing;
        }
    }

    public static final class ConstructionSiteRow {
        private final String starSystem;
        private final long marketId;
        private final String siteLabel;
        private final double progress;
        private final ConstructionStatus status;

        public ConstructionSiteRow(String starSystem, long marketId, String siteLabel, double progress, ConstructionStatus status) {
            this.starSystem = starSystem;
            this.marketId = marketId;
            this.siteLabel = siteLabel;
            this.progress = progress;
            this.status = status;
        }

        public String getStarSystem() {
            return starSystem;
        }

        public long getMarketId() {
            return marketId;
        }

        public String getSiteLabel() {
            return siteLabel;
        }

        public ConstructionStatus getConstructionStatus() {
            return status;
        }

        public String getProgressLabel() {
            double p = progress;
            if (p >= 0 && p <= 1.0) {
                p = p * 100.0;
            }
            return String.format("%.1f %%", p);
        }

    }

}
