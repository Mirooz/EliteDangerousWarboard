package be.mirooz.elitedangerous.dashboard.view.fleetcarrier;

import be.mirooz.elitedangerous.commons.lib.models.commodities.ColonisationCommodityKeys;
import be.mirooz.elitedangerous.commons.lib.models.commodities.CommodityCategory;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.view.common.managers.PopupManager;
import be.mirooz.elitedangerous.dashboard.view.common.overlay.OverlayLockChrome;
import be.mirooz.elitedangerous.dashboard.view.common.overlay.OverlayPassthroughSupport;
import be.mirooz.elitedangerous.dashboard.service.PreferencesService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Fenêtre flottante « toujours au-dessus » : commodités groupées par catégorie (comme la grille colonisation),
 * sans ligne d’en-têtes de colonnes ni bandeau résumé (mêmes données et couleurs « marché optimal »).
 * La colonne des noms garde une largeur minimaire fixe ; l’espacement horizontal entre les autres colonnes
 * se resserre automatiquement quand la fenêtre rétrécit (pas de curseur horizontal en bas).
 */
public class FleetCarrierOverlayComponent {

    /** Largeur minimale de la colonne « nom » (px) — ne rétrécit pas quand l’overlay est réduit. */
    private static final double FLEET_OVERLAY_NAME_COL_MIN_WIDTH = 260.0;
    private static final double FLEET_OVERLAY_NUM_COL_MIN_WIDTH = 24.0;
    private static final double FLEET_OVERLAY_HGAP_MAX = 10.0;
    private static final double FLEET_OVERLAY_HGAP_MIN = -40.0;

    public static final double MIN_OPACITY = 0.01;
    public static final int MIN_WIDTH_OVERLAY = 420;
    public static final int MIN_HEIGHT_OVERLAY = 220;

    private static final String OVERLAY_WIDTH_KEY = "overlay.fleet_carrier.width";
    private static final String OVERLAY_HEIGHT_KEY = "overlay.fleet_carrier.height";
    private static final String OVERLAY_OPACITY_KEY = "overlay.fleet_carrier.opacity";
    private static final String OVERLAY_X_KEY = "overlay.fleet_carrier.x";
    private static final String OVERLAY_Y_KEY = "overlay.fleet_carrier.y";
    private static final String OVERLAY_TEXT_SCALE_KEY = "overlay.fleet_carrier.text_scale";

    private final PreferencesService preferencesService = PreferencesService.getInstance();
    private final LocalizationService localizationService = LocalizationService.getInstance();
    private final PopupManager popupManager = PopupManager.getInstance();

    private Stage overlayStage;
    private double overlayOpacity;
    private Label resizeHandle;
    private Slider opacitySlider;
    private double textScale = 1.0;
    private StackPane stackPane;
    private Supplier<FleetCarrierOverlaySnapshot> dataSupplier;
    private Runnable onOverlayClosed;
    private final OverlayPassthroughSupport passthrough = new OverlayPassthroughSupport();

    public void setOnOverlayClosed(Runnable onOverlayClosed) {
        this.onOverlayClosed = onOverlayClosed;
    }

    /**
     * Quand activé : la scène ignore les clics (souris « à travers » le contenu). Déverrouiller pour déplacer ou utiliser les réglages.
     */
    public void setClickThroughLocked(boolean locked) {
        passthrough.setClickThroughLocked(locked, overlayStage, stackPane);
        refreshLockChrome();
    }

    private void refreshLockChrome() {
        OverlayLockChrome.apply(passthrough.isClickThroughLocked(), stackPane,
                resizeHandle, opacitySlider);
    }

    public FleetCarrierOverlayComponent() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (overlayStage != null && overlayStage.isShowing()) {
                saveOverlayPreferences();
            }
        }));
    }

    /**
     * Ouvre l'overlay ou le ferme s'il est déjà visible (même logique que le prospecteur).
     */
    public void toggleOverlay(Supplier<FleetCarrierOverlaySnapshot> dataSupplier) {
        this.dataSupplier = dataSupplier;
        if (overlayStage != null && overlayStage.isShowing()) {
            disposeOverlayStage();
            return;
        }
        FleetCarrierOverlaySnapshot snap = dataSupplier != null ? dataSupplier.get() : null;
        createOverlayStage(snap);
    }

    public void closeOverlay() {
        if (overlayStage != null && overlayStage.isShowing()) {
            disposeOverlayStage();
        }
    }

    public boolean isShowing() {
        return overlayStage != null && overlayStage.isShowing();
    }

    /** Rafraîchit le tableau si l'overlay est ouvert (appelé depuis le panneau colonisation). */
    public void refreshIfShowing() {
        if (!isShowing() || dataSupplier == null || stackPane == null) {
            return;
        }
        FleetCarrierOverlaySnapshot snap = dataSupplier.get();
        replaceContentVBox(buildContentVBox(snap));
        Platform.runLater(this::applyFleetOverlayGridHgapFromStage);
    }

    private void replaceContentVBox(VBox newCard) {
        if (stackPane.getChildren().isEmpty()) {
            return;
        }
        javafx.scene.Node oldCard = stackPane.getChildren().get(0);
        cleanupNode(oldCard);
        stackPane.getChildren().remove(oldCard);
        stackPane.getChildren().add(0, newCard);
        applyTextScaleToNode(newCard, textScale);
        passthrough.reapplyLockedState(overlayStage, stackPane);
        refreshLockChrome();
    }

    private void createOverlayStage(FleetCarrierOverlaySnapshot initialSnapshot) {
        overlayStage = new Stage();
        overlayStage.initStyle(StageStyle.TRANSPARENT);
        overlayStage.setAlwaysOnTop(true);
        overlayStage.setTitle(localizationService.getString("colonisation.fleet.overlay.windowTitle"));
        overlayStage.setResizable(true);
        overlayStage.setMinWidth(MIN_WIDTH_OVERLAY);
        overlayStage.setMinHeight(MIN_HEIGHT_OVERLAY);

        restoreOverlayPreferences();

        VBox mirrorCard = buildContentVBox(initialSnapshot);
        resizeHandle = createResizeHandle();
        opacitySlider = createOpacitySlider();

        stackPane = new StackPane();
        stackPane.getChildren().addAll(mirrorCard, resizeHandle, opacitySlider);
        StackPane.setAlignment(resizeHandle, Pos.BOTTOM_RIGHT);
        StackPane.setAlignment(opacitySlider, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(opacitySlider, new Insets(0, 30, 0, 0));

        opacitySlider.setMouseTransparent(false);
        resizeHandle.setMouseTransparent(false);

        Scene scene = new Scene(stackPane);
        scene.setFill(Color.TRANSPARENT);
        overlayStage.setScene(scene);
        overlayStage.setOpacity(1.0);

        scene.getStylesheets().add(getClass().getResource("/css/elite-theme.css").toExternalForm());
        popupManager.registerContainer(overlayStage, stackPane);
        stackPane.getStyleClass().add("overlay-root");

        updatePaneStyle(overlayOpacity, stackPane);
        applyTextScaleToNode(mirrorCard, textScale);
        setupOpacitySliderListener();
        setupInteractions();
        refreshLockChrome();

        overlayStage.widthProperty().addListener((obs, o, n) -> applyFleetOverlayGridHgapFromStage());
        overlayStage.show();
        Platform.runLater(this::applyFleetOverlayGridHgapFromStage);
        overlayStage.setOnCloseRequest(event -> disposeOverlayStage());
    }

    private void disposeOverlayStage() {
        if (overlayStage == null) {
            return;
        }
        saveOverlayPreferences();
        popupManager.unregisterContainer(overlayStage);
        Stage stage = overlayStage;
        StackPane pane = stackPane;
        overlayStage = null;
        stackPane = null;
        passthrough.disposeForClose(stage, pane);
        if (onOverlayClosed != null) {
            onOverlayClosed.run();
        }
        stage.close();
    }

    private VBox buildContentVBox(FleetCarrierOverlaySnapshot snap) {
        VBox root = new VBox(0);
        root.setPadding(new Insets(8, 10, 12, 10));
        root.getStyleClass().addAll("mirror-overlay", "colonisation-fleet-tab-wrap");
        root.setMaxWidth(Double.MAX_VALUE);

        if (snap == null || !snap.carrierStatsInitialized()) {
            Label empty = new Label(localizationService.getString("colonisation.fleet.notInitialized"));
            empty.getStyleClass().add("colonisation-detail-placeholder");
            empty.setWrapText(true);
            root.getChildren().add(empty);
            VBox.setVgrow(empty, Priority.ALWAYS);
            return root;
        }

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.getStyleClass().addAll("cargo-minerals-scroll", "colonisation-fleet-market-scroll", "fleet-carrier-overlay-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox inner = new VBox(4);
        inner.setMaxWidth(Double.MAX_VALUE);
        inner.getStyleClass().add("colonisation-fleet-scroll-inner");
        GridPane grid = new GridPane();
        grid.getStyleClass().addAll("cargo-minerals-grid", "colonisation-fleet-market-grid");
        for (int i = 0; i < 6; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            if (i == 0) {
                cc.setMinWidth(FLEET_OVERLAY_NAME_COL_MIN_WIDTH);
                cc.setHgrow(Priority.ALWAYS);
            } else {
                cc.setMinWidth(FLEET_OVERLAY_NUM_COL_MIN_WIDTH);
                cc.setHgrow(Priority.NEVER);
            }
            grid.getColumnConstraints().add(cc);
        }
        fillMarketGrid(grid, snap);
        inner.getChildren().add(grid);
        scroll.setContent(inner);
        scroll.viewportBoundsProperty().addListener((obs, ov, nv) -> adjustFleetGridHgap(scroll, grid));
        Platform.runLater(() -> adjustFleetGridHgap(scroll, grid));
        root.getChildren().add(scroll);
        return root;
    }

    private void fillMarketGrid(GridPane grid, FleetCarrierOverlaySnapshot snap) {
        List<FleetCarrierMarketRow> rows = snap.rows();
        Map<String, Integer> shipByKey = snap.shipStockByMergeKey() != null ? snap.shipStockByMergeKey() : Map.of();
        Map<String, Color> highlightByKey = snap.routeHighlightByMergeKey() != null ? snap.routeHighlightByMergeKey() : Map.of();
        if (rows == null || rows.isEmpty()) {
            Label empty = new Label(localizationService.getString("colonisation.fleet.marketEmpty"));
            empty.getStyleClass().add("cargo-mineral-null-price");
            empty.setWrapText(true);
            grid.add(empty, 0, 0, 6, 1);
            return;
        }
        int row = 0;
        CommodityCategory lastCategory = null;
        for (FleetCarrierMarketRow r : rows) {
            CommodityCategory cat = FleetCarrierMarketTableSupport.rowCategory(r);
            if (lastCategory != cat) {
                Label catHead = new Label(localizationService.getString("colonisation.fleet.commodityCategory." + cat.name()));
                catHead.getStyleClass().add("colonisation-fleet-commodity-category");
                catHead.setMaxWidth(Double.MAX_VALUE);
                grid.add(catHead, 0, row, 6, 1);
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
                priceText = FleetCarrierMarketTableSupport.formatCreditsThousandsDots(r.getCarrierPurchaseBidPerTonCr());
            }
            Label price = new Label(priceText);
            price.getStyleClass().add(po > 0 ? "cargo-mineral-total-price" : "cargo-mineral-null-price");
            Label stock = new Label(Integer.toString(r.getStock()));
            stock.getStyleClass().add(r.getStock() > 0 ? "cargo-mineral-quantity" : "cargo-mineral-null-price");
            Label missing = new Label(r.getMissing() > 0 ? Integer.toString(r.getMissing()) : "—");
            missing.getStyleClass().add(r.getMissing() > 0 ? "cargo-mineral-unit-price" : "cargo-mineral-null-price");
            applyRowRouteHighlight(r, highlightByKey, name, shipL, stock, missing, buyOrder, price);
            grid.add(name, 0, row);
            grid.add(shipL, 1, row);
            grid.add(stock, 2, row);
            grid.add(missing, 3, row);
            grid.add(buyOrder, 4, row);
            grid.add(price, 5, row);
            row++;
        }
    }

    /** Même logique que {@code ColonisationPanelController#fleetRouteColorForRow} (couleurs marché optimal). */
    private static Color routeColorForRow(FleetCarrierMarketRow r, Map<String, Color> highlightByKey) {
        if (r == null || r.getMissing() <= 0) {
            return null;
        }
        String mk = r.getCommodityKey();
        Color c = highlightByKey.get(mk);
        if (c != null) {
            return c;
        }
        if (r.getCommodity() != null) {
            String cargo = r.getCommodity().getCargoJsonName();
            if (cargo != null && !cargo.isBlank()) {
                c = highlightByKey.get(ColonisationCommodityKeys.normalizeMergeKey(cargo));
            }
        }
        return c;
    }

    private static void clearRowRouteHighlight(Label name, Label shipL, Label stock, Label missing, Label buyOrder, Label price) {
        for (Label cell : List.of(name, shipL, stock, missing, buyOrder, price)) {
            cell.setBackground(Background.EMPTY);
        }
        name.setStyle(null);
    }

    /** Aligné sur {@code ColonisationPanelController#applyFleetCarrierMarketRowRouteHighlight}. */
    private static void applyRowRouteHighlight(
            FleetCarrierMarketRow r,
            Map<String, Color> highlightByKey,
            Label name,
            Label shipL,
            Label stock,
            Label missing,
            Label buyOrder,
            Label price) {
        Color c = routeColorForRow(r, highlightByKey);
        if (c == null) {
            clearRowRouteHighlight(name, shipL, stock, missing, buyOrder, price);
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

    private Label createResizeHandle() {
        Label h = new Label("⤡");
        h.getStyleClass().add("resize-handle");
        h.setStyle("-fx-text-fill: gold;-fx-font-size: 36px; -fx-font-weight: bold; -fx-alignment: center;");
        h.setOpacity(0.0);
        return h;
    }

    private Slider createOpacitySlider() {
        Slider slider = new Slider(MIN_OPACITY, 1.0, overlayOpacity);
        slider.setOrientation(javafx.geometry.Orientation.VERTICAL);
        slider.setPrefWidth(12);
        slider.setPrefHeight(120);
        slider.setOpacity(0.0);
        slider.getStyleClass().add("opacity-slider");
        slider.setMajorTickUnit(0.2);
        slider.setMinorTickCount(1);
        slider.setShowTickLabels(false);
        slider.setShowTickMarks(false);
        slider.setSnapToTicks(false);
        return slider;
    }

    private void setupOpacitySliderListener() {
        opacitySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double opacity = Math.max(newVal.doubleValue(), MIN_OPACITY);
            updatePaneStyle(opacity, stackPane);
            overlayOpacity = opacity;
        });
    }

    private void applyFleetOverlayGridHgapFromStage() {
        if (stackPane == null || stackPane.getChildren().isEmpty()) {
            return;
        }
        ScrollGridRef ref = findScrollGrid(stackPane.getChildren().get(0));
        if (ref != null) {
            adjustFleetGridHgap(ref.scroll(), ref.grid());
        }
    }

    private record ScrollGridRef(ScrollPane scroll, GridPane grid) {}

    private static ScrollGridRef findScrollGrid(Node root) {
        if (!(root instanceof VBox vbox)) {
            return null;
        }
        for (Node child : vbox.getChildren()) {
            if (child instanceof ScrollPane sp && sp.getContent() instanceof VBox inner) {
                for (Node innerChild : inner.getChildren()) {
                    if (innerChild instanceof GridPane g) {
                        return new ScrollGridRef(sp, g);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Répartit l’espace horizontal : colonne nom au moins la largeur min configurée,
     * le reste pour les 5 colonnes numériques ; le {@code hgap} absorbe la contrainte de largeur.
     */
    private static void adjustFleetGridHgap(ScrollPane scroll, GridPane grid) {
        double vw = scroll.getViewportBounds().getWidth();
        if (vw <= 1 || Double.isNaN(vw)) {
            return;
        }
        double pad = 28;
        double numericMins = 5 * FLEET_OVERLAY_NUM_COL_MIN_WIDTH;
        double fixed = pad + FLEET_OVERLAY_NAME_COL_MIN_WIDTH + numericMins;
        double slack = vw - fixed;
        double hg = slack / 5.0;
        hg = Math.max(FLEET_OVERLAY_HGAP_MIN, Math.min(FLEET_OVERLAY_HGAP_MAX, hg));
        grid.setHgap(hg);
    }

    private void updatePaneStyle(double opacity, StackPane pane) {
        double stackPaneOpacity = Math.max(MIN_OPACITY, opacity);
        overlayOpacity = stackPaneOpacity;
        String style = String.format(Locale.US, "-fx-background-color: rgba(0, 0, 0, %.2f);", stackPaneOpacity);
        pane.setStyle(style);
    }

    private void applyTextScaleToNode(javafx.scene.Node node, double scale) {
        if (node instanceof Label label) {
            String scalePart = String.format(Locale.ENGLISH, "-fx-font-size: %.1fem;", scale);
            String existing = label.getStyle();
            if (existing != null && !existing.isBlank()) {
                label.setStyle(existing + " " + scalePart);
            } else {
                label.setStyle(scalePart);
            }
        } else if (node instanceof javafx.scene.layout.Pane pane) {
            List<javafx.scene.Node> snapshot = List.copyOf(pane.getChildren());
            for (javafx.scene.Node child : snapshot) {
                applyTextScaleToNode(child, scale);
            }
        }
    }

    private void cleanupNode(javafx.scene.Node node) {
        if (node == null) {
            return;
        }
        if (node instanceof javafx.scene.Parent parent) {
            List<javafx.scene.Node> children = List.copyOf(parent.getChildrenUnmodifiable());
            for (javafx.scene.Node child : children) {
                cleanupNode(child);
            }
            if (parent instanceof javafx.scene.layout.Pane p) {
                p.getChildren().clear();
            }
        }
        javafx.scene.Parent par = node.getParent();
        if (par instanceof javafx.scene.layout.Pane pp) {
            pp.getChildren().remove(node);
        }
        node.setStyle(null);
        node.setUserData(null);
    }

    private void setupInteractions() {
        final double[] offset = new double[2];
        final double[] resizeOffset = new double[2];
        final boolean[] isResizing = {false};
        final boolean[] armWindowMove = {false};

        Scene scene = overlayStage.getScene();
        /*
         * Filtres en phase capture : reçus avant le ScrollPane / la grille, pour pouvoir déplacer la fenêtre
         * en cliquant-glissant au milieu du tableau (consume sur DRAG pour éviter que le scroll prenne le geste).
         */
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (passthrough.isClickThroughLocked()) {
                return;
            }
            if (e.getButton() != MouseButton.PRIMARY) {
                return;
            }
            armWindowMove[0] = false;
            Node pick = pickNode(e);
            if (isInsideSliderOrScrollBar(pick)) {
                return;
            }
            double sceneWidth = scene.getWidth();
            double sceneHeight = scene.getHeight();
            double mx = e.getSceneX();
            double my = e.getSceneY();
            if (mx >= sceneWidth - 25 && my >= sceneHeight - 25) {
                isResizing[0] = true;
                resizeOffset[0] = e.getScreenX();
                resizeOffset[1] = e.getScreenY();
            } else {
                isResizing[0] = false;
                armWindowMove[0] = true;
                offset[0] = e.getScreenX() - overlayStage.getX();
                offset[1] = e.getScreenY() - overlayStage.getY();
            }
        });
        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            if (passthrough.isClickThroughLocked()) {
                return;
            }
            if (e.getButton() != MouseButton.PRIMARY) {
                return;
            }
            Node pick = pickNode(e);
            if (isInsideSliderOrScrollBar(pick)) {
                return;
            }
            if (isResizing[0]) {
                double deltaX = e.getScreenX() - resizeOffset[0];
                double deltaY = e.getScreenY() - resizeOffset[1];
                double newWidth = overlayStage.getWidth() + deltaX;
                double newHeight = overlayStage.getHeight() + deltaY;
                if (newWidth >= overlayStage.getMinWidth()) {
                    overlayStage.setWidth(newWidth);
                }
                if (newHeight >= overlayStage.getMinHeight()) {
                    overlayStage.setHeight(newHeight);
                }
                resizeOffset[0] = e.getScreenX();
                resizeOffset[1] = e.getScreenY();
                e.consume();
            } else if (armWindowMove[0]) {
                overlayStage.setX(e.getScreenX() - offset[0]);
                overlayStage.setY(e.getScreenY() - offset[1]);
                e.consume();
            }
        });
        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            if (passthrough.isClickThroughLocked()) {
                return;
            }
            isResizing[0] = false;
            armWindowMove[0] = false;
        });
        scene.addEventFilter(MouseEvent.MOUSE_MOVED, e -> {
            if (passthrough.isClickThroughLocked()) {
                return;
            }
            double sceneWidth = scene.getWidth();
            double sceneHeight = scene.getHeight();
            if (e.getSceneX() >= sceneWidth - 25 && e.getSceneY() >= sceneHeight - 25) {
                scene.setCursor(javafx.scene.Cursor.SE_RESIZE);
            } else {
                scene.setCursor(javafx.scene.Cursor.DEFAULT);
            }
        });
    }

    private static Node pickNode(MouseEvent e) {
        if (e.getPickResult() != null && e.getPickResult().getIntersectedNode() != null) {
            return e.getPickResult().getIntersectedNode();
        }
        if (e.getTarget() instanceof Node n) {
            return n;
        }
        return null;
    }

    private static boolean isInsideSliderOrScrollBar(Node node) {
        for (Node n = node; n != null; n = n.getParent()) {
            if (n instanceof ScrollBar || n instanceof Slider) {
                return true;
            }
        }
        return false;
    }

    private void restoreOverlayPreferences() {
        String savedWidthStr = preferencesService.getPreference(OVERLAY_WIDTH_KEY, "520");
        String savedHeightStr = preferencesService.getPreference(OVERLAY_HEIGHT_KEY, "420");
        String savedOpacityStr = preferencesService.getPreference(OVERLAY_OPACITY_KEY, "0.92");
        String savedXStr = preferencesService.getPreference(OVERLAY_X_KEY, "120");
        String savedYStr = preferencesService.getPreference(OVERLAY_Y_KEY, "120");
        String savedTextScaleStr = preferencesService.getPreference(OVERLAY_TEXT_SCALE_KEY, "1.0");
        double savedWidth = Double.parseDouble(savedWidthStr);
        double savedHeight = Double.parseDouble(savedHeightStr);
        double savedX = Double.parseDouble(savedXStr);
        double savedY = Double.parseDouble(savedYStr);
        overlayOpacity = Double.parseDouble(savedOpacityStr);
        textScale = Double.parseDouble(savedTextScaleStr);
        overlayStage.setWidth(Math.max(savedWidth, overlayStage.getMinWidth()));
        overlayStage.setHeight(Math.max(savedHeight, overlayStage.getMinHeight()));
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double finalX = Math.max(0, Math.min(savedX, screenBounds.getWidth() - overlayStage.getWidth()));
        double finalY = Math.max(0, Math.min(savedY, screenBounds.getHeight() - overlayStage.getHeight()));
        overlayStage.setX(finalX);
        overlayStage.setY(finalY);
    }

    private void saveOverlayPreferences() {
        if (overlayStage != null && overlayStage.isShowing()) {
            preferencesService.setPreference(OVERLAY_WIDTH_KEY, String.valueOf((int) overlayStage.getWidth()));
            preferencesService.setPreference(OVERLAY_HEIGHT_KEY, String.valueOf((int) overlayStage.getHeight()));
            preferencesService.setPreference(OVERLAY_OPACITY_KEY, String.valueOf(overlayOpacity));
            preferencesService.setPreference(OVERLAY_X_KEY, String.valueOf((int) overlayStage.getX()));
            preferencesService.setPreference(OVERLAY_Y_KEY, String.valueOf((int) overlayStage.getY()));
            preferencesService.setPreference(OVERLAY_TEXT_SCALE_KEY, String.valueOf(textScale));
        }
    }
}
