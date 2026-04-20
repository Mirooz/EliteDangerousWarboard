package be.mirooz.elitedangerous.dashboard.view.colonisation;

import be.mirooz.elitedangerous.commons.lib.models.commodities.ICommodity;
import be.mirooz.elitedangerous.backend.generated.model.MatchedCommodityNearbyExport;
import be.mirooz.elitedangerous.backend.generated.model.NearbyExportsBestStationResult;
import be.mirooz.elitedangerous.dashboard.model.colonisation.CarrierTradeOrderEntry;
import be.mirooz.elitedangerous.dashboard.model.colonisation.ColonisationArchitectSystem;
import be.mirooz.elitedangerous.dashboard.model.colonisation.ColonisationConstruction;
import be.mirooz.elitedangerous.dashboard.model.colonisation.ColonisationDockEntry;
import be.mirooz.elitedangerous.dashboard.model.colonisation.ConstructionResource;
import be.mirooz.elitedangerous.dashboard.model.colonisation.ConstructionStatus;
import be.mirooz.elitedangerous.dashboard.model.registries.commander.CommanderShip;
import be.mirooz.elitedangerous.dashboard.model.registries.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.model.registries.fleetcarrier.CarrierStatus;
import be.mirooz.elitedangerous.dashboard.service.CarrierTradeService;
import be.mirooz.elitedangerous.dashboard.service.ColonisationService;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.service.MiningService;
import be.mirooz.elitedangerous.dashboard.service.PreferencesService;
import be.mirooz.elitedangerous.dashboard.service.listeners.CargoEventNotificationService;
import be.mirooz.elitedangerous.dashboard.view.common.DialogComponent;
import be.mirooz.elitedangerous.dashboard.view.common.managers.CopyClipboardManager;
import be.mirooz.elitedangerous.dashboard.view.common.managers.PopupManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.ResourceBundle;
/**
 * Onglet colonisation + fleet carrier : chantiers architecte, chantier courant, ressources, suggestions d’achat dans le détail.
 */
public class ColonisationPanelController implements Initializable {

    @FXML
    private Button refreshButton;
    @FXML
    private Button buildStationButton;
    @FXML
    private Button updateTradeStationButton;
    @FXML
    private SplitPane colonisationSplitPane;
    @FXML
    private Label statusLabel;
    @FXML
    private Label constructionsTitleLabel;
    @FXML
    private VBox architectSystemsContainer;
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
    private Label commanderCurrentCargoTitleLabel;
    @FXML
    private Button commanderCollapseButton;
    @FXML
    private VBox commanderCollapsibleContent;
    @FXML
    private ProgressBar commanderCargoProgressBar;
    @FXML
    private Label commanderCargoUsedLabel;
    @FXML
    private GridPane commanderCargoGrid;
    @FXML
    private Button findColonisableSystemsButton;
    @FXML
    private Label constructionDetailTitleLabel;
    @FXML
    private VBox constructionDetailContent;

    private final ColonisationService colonisationService = ColonisationService.getInstance();
    private final PreferencesService preferencesService = PreferencesService.getInstance();
    private final CarrierTradeService carrierTradeService = CarrierTradeService.getInstance();
    private final LocalizationService localizationService = LocalizationService.getInstance();
    private final CopyClipboardManager copyClipboardManager = CopyClipboardManager.getInstance();
    private final PopupManager popupManager = PopupManager.getInstance();
    private final MiningService miningService = MiningService.getInstance();
    private final CommanderStatus commanderStatus = CommanderStatus.getInstance();
    private final CargoEventNotificationService cargoEventNotificationService = CargoEventNotificationService.getInstance();
    private final CargoEventNotificationService.CargoEventInterface commanderCargoListener = this::refreshCommanderCargoPanel;

    /** Dernières stations d’achat affichées dans le détail (vidée au changement de sélection / actualisation). */
    private List<NearbyExportsBestStationResult> suggestedBuyStations = List.of();

    /** État déplié par nom de système (persiste entre actualisations). */
    private final Map<String, Boolean> architectSystemExpanded = new HashMap<>();

    private ConstructionSiteRow selectedConstructionRow;

    private boolean fleetPanelCollapsed;
    private boolean commanderPanelCollapsed;

    /** Requête API « stations d’achat » en cours (build ou mise à jour). */
    private boolean suggestBuyStationsRequestInProgress;

    /** Proportion verrouillée du premier séparateur (colonne architecte, ~−15 % vs 0,26). */
    private static final double ARCHITECT_SPLIT_DIVIDER_LOCK = 0.221;

    private boolean adjustingArchitectDivider;
    private boolean architectSplitDividerListenerAttached;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        refreshButton.setOnAction(e -> refreshAll());
        buildStationButton.setOnAction(e -> onBuildStation());
        if (updateTradeStationButton != null) {
            updateTradeStationButton.setOnAction(e -> onUpdateTradeStations());
            updateTradeStationButton.setVisible(false);
            updateTradeStationButton.setManaged(false);
        }
        setupColonisationSplitPaneArchitectLock();
        if (findColonisableSystemsButton != null) {
            findColonisableSystemsButton.setOnAction(e -> openFindColonisableSystemsDialog());
        }

        localizationService.addLanguageChangeListener(locale -> applyLocalizedTexts());
        cargoEventNotificationService.addListener(commanderCargoListener);
        applyLocalizedTexts();
        setupFoldableFleetAndCargoPanels();
        refreshAll();
    }

    private void openFindColonisableSystemsDialog() {
        if (findColonisableSystemsButton == null || findColonisableSystemsButton.getScene() == null) {
            return;
        }
        Stage primaryStage = (Stage) findColonisableSystemsButton.getScene().getWindow();
        DialogComponent dialog = new DialogComponent(
                "/fxml/colonisation/ed-colonise-search-dialog.fxml",
                "/css/elite-theme.css",
                localizationService.getString("colonisation.edcolonise.dialog.windowTitle"),
                920,
                720);
        dialog.init(primaryStage);
        dialog.showAndWait();
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
        refreshButton.setText(localizationService.getString("colonisation.refresh"));
        if (findColonisableSystemsButton != null) {
            findColonisableSystemsButton.setText(localizationService.getString("colonisation.findColonisable"));
        }
        buildStationButton.setText(localizationService.getString("colonisation.buildStation"));
        if (updateTradeStationButton != null) {
            updateTradeStationButton.setText(localizationService.getString("colonisation.updateTradeStation"));
        }
        constructionsTitleLabel.setText(localizationService.getString("colonisation.constructions.title"));
        fleetTitleLabel.setText(localizationService.getString("colonisation.fleet.title"));
        commanderCurrentCargoTitleLabel.setText(localizationService.getString("colonisation.fleet.currentCargo"));
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
        colonisationService.tryRestorePersistedBuildingSite();
        long preferredMarketId = 0;
        ColonisationDockEntry cur = colonisationService.getCurrentConstructionSite();
        if (cur != null) {
            preferredMarketId = cur.getMarketId();
        } else if (selectedConstructionRow != null) {
            preferredMarketId = selectedConstructionRow.getMarketId();
        }

        architectSystemsContainer.getChildren().clear();
        ConstructionSiteRow globalMatch = null;

        for (ColonisationArchitectSystem arch : colonisationService.getArchitectSystems()) {
            List<ConstructionSiteRow> items = buildConstructionItems(arch);
            if (items.isEmpty()) {
                continue;
            }
            if (preferredMarketId != 0) {
                for (ConstructionSiteRow r : items) {
                    if (r.getMarketId() == preferredMarketId) {
                        globalMatch = r;
                        break;
                    }
                }
            }
            architectSystemsContainer.getChildren().add(
                    createArchitectSystemBlock(arch.getStarSystem(), items, preferredMarketId));
        }

        if (architectSystemsContainer.getChildren().isEmpty()) {
            Label empty = new Label(localizationService.getString("colonisation.architect.empty"));
            empty.getStyleClass().add("colonisation-detail-label");
            empty.setWrapText(true);
            architectSystemsContainer.getChildren().add(empty);
            selectedConstructionRow = null;
        } else {
            selectedConstructionRow = globalMatch;
        }
        refreshConstructionSelectionStyles();
        tryLoadPersistedSuggestionsForSelectedBuilding();
        updateButtonStates();
        refreshConstructionDetailPanel();
    }

    private boolean resolveExpandedState(String starSystem, List<ConstructionSiteRow> items, long preferredMarketId) {
        Boolean saved = architectSystemExpanded.get(starSystem);
        if (saved != null) {
            return saved;
        }
        boolean initial = preferredMarketId != 0
                && items.stream().anyMatch(r -> r.getMarketId() == preferredMarketId);
        architectSystemExpanded.put(starSystem, initial);
        return initial;
    }

    private VBox createArchitectSystemBlock(String starSystem, List<ConstructionSiteRow> items, long preferredMarketId) {
        VBox outer = new VBox(6);
        outer.getStyleClass().add("colonisation-architect-system");

        Label chevron = new Label();
        chevron.getStyleClass().add("colonisation-architect-chevron");
        Label name = new Label(starSystem != null && !starSystem.isBlank() ? starSystem : "—");
        name.getStyleClass().add("colonisation-architect-card-title");
        name.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(name, Priority.ALWAYS);

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("colonisation-architect-header");
        header.getChildren().addAll(chevron, name);

        VBox sitesBox = new VBox(8);
        sitesBox.getStyleClass().add("colonisation-architect-sites");

        ColonisationDockEntry curSite = colonisationService.getCurrentConstructionSite();
        long currentMarketId = curSite != null ? curSite.getMarketId() : 0L;

        for (ConstructionSiteRow row : items) {
            sitesBox.getChildren().add(createConstructionSiteCard(row, currentMarketId));
        }

        final boolean[] expanded = {resolveExpandedState(starSystem, items, preferredMarketId)};
        Runnable updateExpandUi = () -> {
            boolean ex = expanded[0];
            chevron.setText(ex ? "▼" : "▶");
            sitesBox.setVisible(ex);
            sitesBox.setManaged(ex);
            outer.getStyleClass().remove("colonisation-architect-system-expanded");
            if (ex) {
                outer.getStyleClass().add("colonisation-architect-system-expanded");
            }
        };

        header.setOnMouseClicked(e -> {
            expanded[0] = !expanded[0];
            architectSystemExpanded.put(starSystem, expanded[0]);
            updateExpandUi.run();
        });

        updateExpandUi.run();

        outer.getChildren().addAll(header, sitesBox);
        return outer;
    }

    private VBox createConstructionSiteCard(ConstructionSiteRow row, long currentMarketId) {
        VBox card = new VBox(4);
        card.getStyleClass().add("colonisation-construction-card");
        card.setUserData(Long.valueOf(row.getMarketId()));

        if (row.getMarketId() == currentMarketId) {
            card.getStyleClass().add("colonisation-construction-card-current");
        }

        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label siteTitle = new Label(row.getSiteLabel());
        siteTitle.getStyleClass().add("colonisation-construction-card-title");
        siteTitle.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(siteTitle, Priority.ALWAYS);
        titleRow.getChildren().add(siteTitle);
        if (row.getMarketId() == currentMarketId) {
            Label badge = new Label(localizationService.getString("colonisation.badge.building"));
            badge.getStyleClass().addAll("colonisation-construction-badge", "colonisation-construction-badge-building");
            titleRow.getChildren().add(badge);
        }

        Label progressLine = new Label(localizationService.getString("colonisation.col.progress") + ": " + row.getProgressLabel());
        progressLine.getStyleClass().add("colonisation-construction-line");
        Label statusLine = new Label(localizationService.getString("colonisation.col.status") + ": "
                + localizedConstructionStatus(row.getConstructionStatus()));
        statusLine.getStyleClass().add("colonisation-construction-line");
        Label marketLine = new Label(localizationService.getString("colonisation.col.marketId") + ": " + row.getMarketId());
        marketLine.getStyleClass().add("colonisation-construction-line");

        card.getChildren().addAll(titleRow, progressLine, statusLine, marketLine);

        card.setOnMouseClicked(e -> {
            e.consume();
            selectConstructionRow(row);
        });

        return card;
    }

    private void selectConstructionRow(ConstructionSiteRow row) {
        clearSuggestedBuyStations();
        selectedConstructionRow = row;
        refreshConstructionSelectionStyles();
        tryLoadPersistedSuggestionsForSelectedBuilding();
        updateButtonStates();
        refreshConstructionDetailPanel();
    }

    private void forEachConstructionCard(java.util.function.Consumer<VBox> consumer) {
        for (Node n : architectSystemsContainer.getChildren()) {
            if (!(n instanceof VBox outer)) {
                continue;
            }
            if (outer.getChildren().size() < 2) {
                continue;
            }
            if (!(outer.getChildren().get(1) instanceof VBox sites)) {
                continue;
            }
            for (Node c : sites.getChildren()) {
                if (c instanceof VBox card) {
                    consumer.accept(card);
                }
            }
        }
    }

    private void refreshConstructionSelectionStyles() {
        forEachConstructionCard(card -> card.getStyleClass().remove("colonisation-construction-card-selected"));
        if (selectedConstructionRow == null) {
            return;
        }
        long mid = selectedConstructionRow.getMarketId();
        forEachConstructionCard(card -> {
            Object ud = card.getUserData();
            if (ud instanceof Long lid && lid == mid) {
                card.getStyleClass().add("colonisation-construction-card-selected");
            }
        });
    }

    private void refreshConstructionDetailPanel() {
        constructionDetailContent.getChildren().clear();
        constructionDetailTitleLabel.setText(localizationService.getString("colonisation.detail.title"));
        if (selectedConstructionRow == null) {
            Label ph = new Label(localizationService.getString("colonisation.detail.placeholder"));
            ph.getStyleClass().add("colonisation-detail-placeholder");
            ph.setWrapText(true);
            ph.setMaxWidth(Double.MAX_VALUE);
            constructionDetailContent.getChildren().add(ph);
            return;
        }
        ColonisationDockEntry dock = findDockEntry(selectedConstructionRow.getMarketId());
        if (dock == null) {
            Label ph = new Label(localizationService.getString("colonisation.detail.placeholder"));
            ph.getStyleClass().add("colonisation-detail-placeholder");
            ph.setWrapText(true);
            constructionDetailContent.getChildren().add(ph);
            return;
        }

        String headline = firstNonBlank(dock.getSiteNameLocalised(), dock.getStationNameRaw(), "—");
        constructionDetailTitleLabel.setText(headline);

        appendKv(constructionDetailContent, "colonisation.col.system", dock.getStarSystem());
        appendKv(constructionDetailContent, "colonisation.col.site", dock.getSiteNameLocalised());
        if (dock.getDistFromStarLs() > 0 && Double.isFinite(dock.getDistFromStarLs())) {
            appendKv(constructionDetailContent, "colonisation.detail.distLs", String.format("%.0f", dock.getDistFromStarLs()));
        }

        ColonisationConstruction c = dock.getConstruction();
        if (c != null) {
            HBox cons = new HBox(20);
            cons.getStyleClass().add("colonisation-detail-construction-inline");
            Label prog = new Label(localizationService.getString("colonisation.col.progress") + ": "
                    + formatProgress(c.getConstructionProgress()));
            prog.getStyleClass().add("colonisation-detail-resource-meta");
            Label st = new Label(localizationService.getString("colonisation.col.status") + ": "
                    + localizedConstructionStatus(c.getStatus()));
            st.getStyleClass().add("colonisation-detail-resource-meta");
            cons.getChildren().addAll(prog, st);
            constructionDetailContent.getChildren().add(cons);

            if (c.getResourcesRequired() != null && !c.getResourcesRequired().isEmpty()) {
                addConstructionResourcesSection(constructionDetailContent, c);
            }
        } else {
            clearSuggestedBuyStations();
        }
    }

    private void clearSuggestedBuyStations() {
        suggestedBuyStations = List.of();
    }

    private void setupColonisationSplitPaneArchitectLock() {
        if (colonisationSplitPane == null) {
            return;
        }
        colonisationSplitPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                Platform.runLater(this::ensureArchitectSplitDividerStaysFixed);
            }
        });
        if (colonisationSplitPane.getScene() != null) {
            Platform.runLater(this::ensureArchitectSplitDividerStaysFixed);
        }
    }

    private void ensureArchitectSplitDividerStaysFixed() {
        if (colonisationSplitPane == null || colonisationSplitPane.getDividers().size() < 2) {
            return;
        }
        var d0 = colonisationSplitPane.getDividers().get(0);
        var d1 = colonisationSplitPane.getDividers().get(1);
        if (!architectSplitDividerListenerAttached) {
            architectSplitDividerListenerAttached = true;
            d0.positionProperty().addListener((obs, ov, nv) -> {
                if (adjustingArchitectDivider) {
                    return;
                }
                double v = nv.doubleValue();
                if (Math.abs(v - ARCHITECT_SPLIT_DIVIDER_LOCK) > 0.002) {
                    adjustingArchitectDivider = true;
                    double p1 = d1.getPosition();
                    colonisationSplitPane.setDividerPositions(ARCHITECT_SPLIT_DIVIDER_LOCK, p1);
                    adjustingArchitectDivider = false;
                }
            });
        }
        adjustingArchitectDivider = true;
        colonisationSplitPane.setDividerPositions(ARCHITECT_SPLIT_DIVIDER_LOCK, d1.getPosition());
        adjustingArchitectDivider = false;
    }

    private boolean isSelectedRowCurrentBuildingSite() {
        if (selectedConstructionRow == null) {
            return false;
        }
        ColonisationDockEntry cur = colonisationService.getCurrentConstructionSite();
        return cur != null && selectedConstructionRow.getMarketId() == cur.getMarketId();
    }

    private void tryLoadPersistedSuggestionsForSelectedBuilding() {
        if (!isSelectedRowCurrentBuildingSite()) {
            return;
        }
        ColonisationDockEntry cur = colonisationService.getCurrentConstructionSite();
        if (cur == null) {
            return;
        }
        preferencesService.loadColonisationSuggestedBuyStationsIfMatches(cur.getMarketId()).ifPresent(list -> {
            suggestedBuyStations = list;
        });
    }

    /**
     * Liste « ressources à fournir » : mêmes lignes x/y que d’habitude ; si des stations d’achat sont connues,
     * les commodités encore requises correspondant à un marché sont regroupées dans un encadré par station.
     */
    private void addConstructionResourcesSection(VBox parent, ColonisationConstruction c) {
        List<ConstructionResource> req = c.getResourcesRequired();
        if (req == null || req.isEmpty()) {
            return;
        }
        appendSectionTitle(parent, "colonisation.detail.section.resources");
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
        String n = firstNonBlank(r.getNameLocalised(), r.getName(), "?");
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
        fleetSummaryBox.getChildren().clear();
        fleetCargoBarBox.getChildren().clear();
        fleetMarketGrid.getChildren().clear();
        CarrierStatus cs = carrierTradeService.getCarrierStatus();
        if (!cs.isCarrierStatsInitialized()) {
            Label empty = new Label(localizationService.getString("colonisation.fleet.notInitialized"));
            empty.getStyleClass().add("colonisation-detail-placeholder");
            empty.setWrapText(true);
            empty.setMaxWidth(Double.MAX_VALUE);
            fleetSummaryBox.getChildren().add(empty);
            refreshCommanderCargoPanel();
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
        Label bm = new Label(localizationService.getString("colonisation.fleet.blackMarket") + ": "
                + (cs.isBlackMarket()
                ? localizationService.getString("colonisation.fleet.yes")
                : localizationService.getString("colonisation.fleet.no")));
        bm.getStyleClass().add("colonisation-fleet-inline-dim");
        fleetSummaryBox.getChildren().addAll(line1, bm);

        refreshFleetCargoBar(cs);
        refreshFleetMarketGrid(cs);
        refreshCommanderCargoPanel();
    }

    /**
     * Cargo du commandant (vaisseau) : même source que l’onglet minage ({@link MiningService#getCargo} → journal / état vaisseau).
     */
    private void refreshCommanderCargoPanel() {
        Platform.runLater(() -> {
            if (commanderCargoGrid == null || commanderCargoProgressBar == null || commanderCargoUsedLabel == null) {
                return;
            }
            commanderCargoGrid.getChildren().clear();
            CommanderShip ship = commanderStatus.getShip();
            CommanderShip.ShipCargo cargo = miningService.getCargo();
            if (ship == null || cargo == null) {
                commanderCargoProgressBar.setProgress(0);
                commanderCargoUsedLabel.setText("—");
                Label ph = placeholderLabel("colonisation.fleet.commanderCargoNoShip");
                commanderCargoGrid.add(ph, 0, 0, 2, 1);
                return;
            }
            int cap = Math.max(miningService.getCurrentCargoCapacity(), 0);
            int used = Math.max(0, cargo.getCurrentUsed());
            commanderCargoUsedLabel.setText(cap > 0 ? used + " / " + cap : Integer.toString(used));
            commanderCargoProgressBar.setProgress(cap > 0 ? Math.min(1.0, (double) used / (double) cap) : 0);

            if (cargo.getCommodities().isEmpty()) {
                Label ph = placeholderLabel("colonisation.fleet.commanderCargoEmpty");
                commanderCargoGrid.add(ph, 0, 0, 2, 1);
                return;
            }
            List<Entry<ICommodity, Integer>> rows = new ArrayList<>(cargo.getCommodities().entrySet());
            rows.sort(Comparator.comparing(e -> e.getKey().getVisibleName(), String.CASE_INSENSITIVE_ORDER));
            int row = 0;
            for (Entry<ICommodity, Integer> e : rows) {
                Label name = new Label(e.getKey().getVisibleName());
                name.getStyleClass().add("colonisation-commander-cargo-name");
                name.setMaxWidth(Double.MAX_VALUE);
                name.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);
                Label qty = new Label(Integer.toString(e.getValue()));
                qty.getStyleClass().add("colonisation-commander-cargo-qty");
                commanderCargoGrid.add(name, 0, row);
                commanderCargoGrid.add(qty, 1, row);
                row++;
            }
        });
    }

    private Label placeholderLabel(String messageKey) {
        Label ph = new Label(localizationService.getString(messageKey));
        ph.getStyleClass().add("colonisation-detail-placeholder");
        ph.setWrapText(true);
        ph.setMaxWidth(Double.MAX_VALUE);
        return ph;
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
        List<FleetMarketRow> rows = buildFleetMarketRows(cs);
        addFleetMarketHeaderRow();
        if (rows.isEmpty()) {
            Label empty = new Label(localizationService.getString("colonisation.fleet.marketEmpty"));
            empty.getStyleClass().add("cargo-mineral-null-price");
            empty.setWrapText(true);
            fleetMarketGrid.add(empty, 0, 1, 5, 1);
            return;
        }
        int row = 1;
        for (FleetMarketRow r : rows) {
            Label name = new Label(r.getDisplayName().toUpperCase(Locale.ROOT));
            name.getStyleClass().add("cargo-mineral-name");
            Label stock = new Label(Integer.toString(r.getStock()));
            stock.getStyleClass().add(r.getStock() > 0 ? "cargo-mineral-quantity" : "cargo-mineral-null-price");
            Label buy = new Label(Integer.toString(r.getPurchaseOrder()));
            buy.getStyleClass().add(r.getPurchaseOrder() != 0 ? "cargo-mineral-unit-price" : "cargo-mineral-null-price");
            Label sell = new Label(Integer.toString(r.getSaleOrder()));
            sell.getStyleClass().add(r.getSaleOrder() != 0 ? "cargo-mineral-station-price" : "cargo-mineral-null-price");
            Label price = new Label(r.getPrice() > 0 ? Long.toString(r.getPrice()) : "—");
            price.getStyleClass().add(r.getPrice() > 0 ? "cargo-mineral-total-price" : "cargo-mineral-null-price");
            fleetMarketGrid.add(name, 0, row);
            fleetMarketGrid.add(stock, 1, row);
            fleetMarketGrid.add(buy, 2, row);
            fleetMarketGrid.add(sell, 3, row);
            fleetMarketGrid.add(price, 4, row);
            row++;
        }
    }

    private void addFleetMarketHeaderRow() {
        fleetMarketGrid.add(fleetMarketHeaderLabel("colonisation.fleet.col.commodity"), 0, 0);
        fleetMarketGrid.add(fleetMarketHeaderLabel("colonisation.fleet.col.stock"), 1, 0);
        fleetMarketGrid.add(fleetMarketHeaderLabel("colonisation.fleet.col.buyOrder"), 2, 0);
        fleetMarketGrid.add(fleetMarketHeaderLabel("colonisation.fleet.col.saleOrder"), 3, 0);
        fleetMarketGrid.add(fleetMarketHeaderLabel("colonisation.fleet.col.price"), 4, 0);
    }

    private Label fleetMarketHeaderLabel(String messageKey) {
        Label l = new Label(localizationService.getString(messageKey).toUpperCase(Locale.ROOT));
        l.getStyleClass().add("cargo-column-header");
        return l;
    }

    private List<FleetMarketRow> buildFleetMarketRows(CarrierStatus cs) {
        Map<String, Integer> stocks = cs.getStocksByCommodity();
        Map<String, FleetMarketRow> acc = new LinkedHashMap<>();

        for (CarrierTradeOrderEntry e : cs.getActiveTransactions()) {
            if (e == null) {
                continue;
            }
            if (CarrierStatus.isFleetStockExcludedDrone(e.getCommodity(), e.getCommodityLocalised())) {
                continue;
            }
            String nk = cs.canonicalCommodityKey(e);
            if (nk.isBlank() || "__UNKNOWN_COMMODITY__".equalsIgnoreCase(nk)) {
                continue;
            }
            int stockVal = Math.max(e.getStock(), cs.physicalStockForCanonicalKey(nk));
            if (stockVal <= 0 && e.getPurchaseOrder() == 0 && e.getSaleOrder() == 0) {
                continue;
            }
            String display = firstNonBlank(e.getCommodityLocalised(), e.getCommodity(), nk);
            acc.put(nk, new FleetMarketRow(display, stockVal, e.getPurchaseOrder(), e.getSaleOrder(), e.getPrice()));
        }

        if (stocks != null) {
            for (Map.Entry<String, Integer> en : stocks.entrySet()) {
                int st = en.getValue() == null ? 0 : en.getValue();
                if (st <= 0) {
                    continue;
                }
                String nk = en.getKey() != null ? en.getKey().trim().toLowerCase(Locale.ROOT) : "";
                if (nk.isBlank()) {
                    continue;
                }
                if (CarrierStatus.isFleetStockExcludedDrone(nk, "")) {
                    continue;
                }
                if (acc.containsKey(nk)) {
                    FleetMarketRow old = acc.get(nk);
                    acc.put(nk, old.withStock(Math.max(old.getStock(), st)));
                } else {
                    acc.put(nk, new FleetMarketRow(cs.displayLabelForStockKey(nk), st, 0, 0, 0L));
                }
            }
        }

        List<FleetMarketRow> out = new ArrayList<>(acc.values());
        out.sort(Comparator.comparing(FleetMarketRow::getDisplayName, String.CASE_INSENSITIVE_ORDER));
        return out;
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
                List<NearbyExportsBestStationResult> list = colonisationService.suggestBuyStationsForDock(dockRef);
                int n = list.size();
                Platform.runLater(() -> {
                    if (selectedConstructionRow == null || selectedConstructionRow.getMarketId() != requestMarketId) {
                        suggestBuyStationsRequestInProgress = false;
                        updateButtonStates();
                        return;
                    }
                    suggestedBuyStations = List.copyOf(list);
                    preferencesService.persistColonisationSuggestedBuyStations(requestMarketId, list);
                    refreshConstructionDetailPanel();
                    if (n == 0) {
                        statusLabel.setText(localizationService.getString("colonisation.buy.noStations"));
                    } else {
                        statusLabel.setText(localizationService.getString("colonisation.buy.resultCount", n));
                    }
                    suggestBuyStationsRequestInProgress = false;
                    updateButtonStates();
                });
            } catch (IOException e) {
                Platform.runLater(() -> {
                    if (selectedConstructionRow != null && selectedConstructionRow.getMarketId() == requestMarketId) {
                        suggestedBuyStations = List.of();
                        preferencesService.persistColonisationSuggestedBuyStations(requestMarketId, List.of());
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
        if (buildStationButton == null) {
            return;
        }
        boolean noSelection = selectedConstructionRow == null;
        boolean isSelectedBuilding = isSelectedRowCurrentBuildingSite();
        boolean busy = suggestBuyStationsRequestInProgress;
        buildStationButton.setDisable(noSelection || isSelectedBuilding || busy);
        if (updateTradeStationButton != null) {
            updateTradeStationButton.setVisible(isSelectedBuilding);
            updateTradeStationButton.setManaged(isSelectedBuilding);
            updateTradeStationButton.setDisable(!isSelectedBuilding || busy);
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
        private final String displayName;
        private final int stock;
        private final int purchaseOrder;
        private final int saleOrder;
        private final long price;

        public FleetMarketRow(String displayName, int stock, int purchaseOrder, int saleOrder, long price) {
            this.displayName = displayName != null ? displayName : "";
            this.stock = stock;
            this.purchaseOrder = purchaseOrder;
            this.saleOrder = saleOrder;
            this.price = price;
        }

        public FleetMarketRow withStock(int newStock) {
            return new FleetMarketRow(displayName, newStock, purchaseOrder, saleOrder, price);
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
