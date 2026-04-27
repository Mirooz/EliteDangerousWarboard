package be.mirooz.elitedangerous.dashboard.view.colonisation;

import be.mirooz.elitedangerous.backend.edcolonise.EdColoniseStarSystemSearchQuery;
import be.mirooz.elitedangerous.backend.generated.model.EdColoniseColonisedSystemRef;
import be.mirooz.elitedangerous.backend.generated.model.EdColoniseStarSystemSearchResult;
import be.mirooz.elitedangerous.backend.generated.model.EdColoniseSystemCounts;
import be.mirooz.elitedangerous.dashboard.service.EdColoniseService;
import be.mirooz.elitedangerous.dashboard.service.SpanshSystemVisitedService;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.view.exploration.SystemVisualViewComponent;
import be.mirooz.elitedangerous.dashboard.view.common.managers.CopyClipboardManager;
import be.mirooz.elitedangerous.dashboard.view.common.managers.PopupManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import java.net.URL;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Dialogue de recherche de systèmes colonisables (backend ED Colonise).
 */
public class EdColoniseSearchDialogController implements Initializable {

    private static final int MAX_NEIGHBORS_SHOWN = 3;

    @FXML
    private Label dialogTitleLabel;
    @FXML
    private Label dialogSubtitleLabel;
    @FXML
    private Label descriptionLabel;
    @FXML
    private Label minLandablesLabel;
    @FXML
    private Spinner<Integer> minLandablesSpinner;
    @FXML
    private Label minRingsLabel;
    @FXML
    private Spinner<Integer> minRingsSpinner;
    @FXML
    private Button moreFiltersButton;
    @FXML
    private Label maxDistanceSolLabel;
    @FXML
    private Spinner<Integer> maxDistanceSolSpinner;
    @FXML
    private Button searchButton;
    @FXML
    private Button closeButton;
    @FXML
    private VBox resultsBox;
    @FXML
    private ProgressIndicator loadingIndicator;
    @FXML
    private Label errorLabel;
    @FXML
    private StackPane dialogPopupLayer;

    private EdColoniseSearchAdvancedSnapshot advancedSnapshot = EdColoniseSearchAdvancedSnapshot.defaults();

    private final EdColoniseService edColoniseService = EdColoniseService.getInstance();
    private final SpanshSystemVisitedService spanshSystemVisitedService = SpanshSystemVisitedService.getInstance();
    private final LocalizationService localizationService = LocalizationService.getInstance();
    private final CopyClipboardManager copyClipboardManager = CopyClipboardManager.getInstance();
    private final PopupManager popupManager = PopupManager.getInstance();

    private Stage dialogPopupRegisteredStage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        localizationService.addLanguageChangeListener(locale -> applyLocalizedTexts());
        applyLocalizedTexts();
        if (dialogPopupLayer != null) {
            dialogPopupLayer.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene == null) {
                    return;
                }
                Runnable reg = this::registerDialogPopupHostIfNeeded;
                newScene.windowProperty().addListener((wObs, ow, nw) -> {
                    if (nw != null) {
                        Platform.runLater(reg);
                    }
                });
                Platform.runLater(reg);
            });
            Platform.runLater(this::registerDialogPopupHostIfNeeded);
        }
    }

    private void registerDialogPopupHostIfNeeded() {
        if (dialogPopupLayer == null) {
            return;
        }
        Scene scene = dialogPopupLayer.getScene();
        if (scene == null || !(scene.getWindow() instanceof Stage stage)) {
            return;
        }
        if (dialogPopupRegisteredStage == stage) {
            return;
        }
        if (dialogPopupRegisteredStage != null) {
            popupManager.unregisterContainer(dialogPopupRegisteredStage);
        }
        popupManager.registerContainer(stage, dialogPopupLayer);
        dialogPopupRegisteredStage = stage;
        stage.setOnHidden(e -> {
            popupManager.unregisterContainer(stage);
            if (dialogPopupRegisteredStage == stage) {
                dialogPopupRegisteredStage = null;
            }
        });
    }

    private void applyLocalizedTexts() {
        dialogTitleLabel.setText(localizationService.getString("colonisation.edcolonise.dialog.title"));
        dialogSubtitleLabel.setText(localizationService.getString("colonisation.edcolonise.dialog.subtitle"));
        descriptionLabel.setText(localizationService.getString("colonisation.edcolonise.dialog.description"));
        if (minLandablesLabel != null) {
            minLandablesLabel.setText(localizationService.getString("colonisation.edcolonise.metric.landables"));
        }
        if (minRingsLabel != null) {
            minRingsLabel.setText(localizationService.getString("colonisation.edcolonise.metric.rings"));
        }
        maxDistanceSolLabel.setText(localizationService.getString("colonisation.edcolonise.field.maxDistanceSolShort"));
        searchButton.setText(localizationService.getString("colonisation.edcolonise.search"));
        closeButton.setText(localizationService.getString("colonisation.edcolonise.close"));
        if (moreFiltersButton != null) {
            moreFiltersButton.setText(localizationService.getString("colonisation.edcolonise.moreFilters"));
        }
    }

    @FXML
    private void onMoreFilters() {
        Window owner = moreFiltersButton != null && moreFiltersButton.getScene() != null
                ? moreFiltersButton.getScene().getWindow()
                : null;
        EdColoniseAdvancedFiltersPopup.show(owner, localizationService, advancedSnapshot,
                snap -> advancedSnapshot = snap);
    }

    @FXML
    private void onSearch() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        int minLand = minLandablesSpinner != null ? minLandablesSpinner.getValue() : 0;
        int minRing = minRingsSpinner != null ? minRingsSpinner.getValue() : 0;
        int maxLy = maxDistanceSolSpinner != null ? maxDistanceSolSpinner.getValue() : 800;
        EdColoniseStarSystemSearchQuery params = EdColoniseSearchFilterForm.mergeMainAndAdvanced(
                minLand, minRing, maxLy, advancedSnapshot);
        searching(true);
        Thread t = new Thread(() -> {
            try {
                var response = edColoniseService.searchColonisableStarSystems(params);
                List<EdColoniseStarSystemSearchResult> results = response.getResults() != null
                        ? response.getResults()
                        : List.of();
                Platform.runLater(() -> {
                    renderResults(results);
                    searching(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    searching(false);
                    errorLabel.setText(localizationService.getString("colonisation.edcolonise.error") + " " + e.getMessage());
                    errorLabel.setVisible(true);
                    errorLabel.setManaged(true);
                });
            }
        }, "ed-colonise-search");
        t.setDaemon(true);
        t.start();
    }

    private void searching(boolean busy) {
        searchButton.setDisable(busy);
        loadingIndicator.setVisible(busy);
        if (busy) {
            resultsBox.getChildren().clear();
        }
    }

    private void renderResults(List<EdColoniseStarSystemSearchResult> results) {
        resultsBox.getChildren().clear();
        if (results.isEmpty()) {
            Label empty = new Label(localizationService.getString("colonisation.edcolonise.noResults"));
            empty.getStyleClass().add("colonisation-detail-placeholder");
            empty.setWrapText(true);
            resultsBox.getChildren().add(empty);
            return;
        }
        for (EdColoniseStarSystemSearchResult r : results) {
            resultsBox.getChildren().add(buildResultCard(r));
        }
    }

    private BorderPane buildResultCard(EdColoniseStarSystemSearchResult r) {
        BorderPane card = new BorderPane();
        card.getStyleClass().add("colonisation-buy-station-wrap");
        card.setMaxWidth(Double.MAX_VALUE);

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(8);
        grid.setMaxWidth(Double.MAX_VALUE);

        ColumnConstraints leftCol = new ColumnConstraints();
        leftCol.setHgrow(Priority.ALWAYS);
        ColumnConstraints rightCol = new ColumnConstraints();
        rightCol.setPrefWidth(240);
        grid.getColumnConstraints().addAll(leftCol, rightCol);

        VBox left = buildLeftColumn(r);
        VBox right = buildRightColumn(r);
        GridPane.setVgrow(left, Priority.ALWAYS);
        GridPane.setVgrow(right, Priority.ALWAYS);
        grid.add(left, 0, 0);
        grid.add(right, 1, 0);

        card.setCenter(grid);

        String lastUp = formatLastUpdate(r.getLastUpdate());
        if (!lastUp.isBlank()) {
            HBox bottom = new HBox();
            bottom.setAlignment(Pos.CENTER_RIGHT);
            bottom.setPadding(new Insets(6, 4, 2, 4));
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

    private VBox buildLeftColumn(EdColoniseStarSystemSearchResult r) {
        VBox col = new VBox(8);
        col.setMaxWidth(Double.MAX_VALUE);

        String name = r.getSystemName() != null ? r.getSystemName() : "—";
        col.getChildren().add(createCopyablePrimaryNameLabel(name));

        VBox stats = new VBox(4);
        if (r.getDistanceToSol() != null) {
            Label d = new Label(localizationService.getString("colonisation.edcolonise.result.distanceSol", r.getDistanceToSol()));
            d.getStyleClass().add("colonisation-detail-label");
            d.setWrapText(true);
            stats.getChildren().add(d);
        }
        Integer bodies = sumPlanetaryBodies(r.getSystemCounts());
        if (bodies != null) {
            Label b = new Label(localizationService.getString("colonisation.edcolonise.result.bodyCount", bodies));
            b.getStyleClass().add("colonisation-detail-label");
            b.setWrapText(true);
            stats.getChildren().add(b);
        }
        Integer ringBodies = ringBodyCount(r);
        if (ringBodies != null) {
            Label rg = new Label(localizationService.getString("colonisation.edcolonise.result.ringPlanets", ringBodies));
            rg.getStyleClass().add("colonisation-detail-label");
            rg.setWrapText(true);
            stats.getChildren().add(rg);
        }
        if (!stats.getChildren().isEmpty()) {
            col.getChildren().add(stats);
        }

        Button details = new Button(localizationService.getString("colonisation.edcolonise.details"));
        details.getStyleClass().add("elite-nav-button");
        details.setOnAction(e -> showDetails(r));
        HBox row = new HBox(details);
        row.setAlignment(Pos.CENTER_LEFT);
        col.getChildren().add(row);
        return col;
    }

    private VBox buildRightColumn(EdColoniseStarSystemSearchResult r) {
        VBox col = new VBox(6);
        col.setFillWidth(false);
        col.setMaxWidth(Double.MAX_VALUE);

        Label neighTitle = new Label(localizationService.getString("colonisation.edcolonise.neighbors.title"));
        neighTitle.getStyleClass().add("colonisation-section-subtitle");
        neighTitle.setWrapText(true);
        col.getChildren().add(neighTitle);

        List<EdColoniseColonisedSystemRef> neighbors = r.getColonisedSystems();
        if (neighbors == null || neighbors.isEmpty()) {
            Label none = new Label(localizationService.getString("colonisation.edcolonise.neighbors.none"));
            none.getStyleClass().add("colonisation-detail-placeholder");
            none.setWrapText(true);
            col.getChildren().add(none);
            return col;
        }
        int shown = Math.min(MAX_NEIGHBORS_SHOWN, neighbors.size());
        for (int i = 0; i < shown; i++) {
            EdColoniseColonisedSystemRef ref = neighbors.get(i);
            String sys = ref.getSystemName() != null ? ref.getSystemName() : "—";
            Label sysLine = createCopyableNeighborLabel(sys);
            sysLine.setWrapText(true);
            col.getChildren().add(sysLine);
        }
        int others = neighbors.size() - MAX_NEIGHBORS_SHOWN;
        if (others > 0) {
            String moreText = others == 1
                    ? localizationService.getString("colonisation.edcolonise.neighbors.otherOne")
                    : localizationService.getString("colonisation.edcolonise.neighbors.others", others);
            Label more = new Label(moreText);
            more.getStyleClass().add("colonisation-buy-station-meta-dim");
            more.setWrapText(true);
            col.getChildren().add(more);
        }
        return col;
    }

    private void copyToClipboardWithStandardPopup(Label sourceLabel, String text, MouseEvent click) {
        if (text == null || text.isBlank() || sourceLabel.getScene() == null) {
            return;
        }
        Window win = sourceLabel.getScene().getWindow();
        if (win == null) {
            return;
        }
        copyClipboardManager.copyToClipboard(text);
        popupManager.showPopup(localizationService.getString("system.copied"), click.getSceneX(), click.getSceneY(), win);
    }

    private Label createCopyablePrimaryNameLabel(String text) {
        return createCopyableNameLabel(text, "colonisation-buy-station-name");
    }

    private Label createCopyableNeighborLabel(String text) {
        return createCopyableNameLabel(text, "colonisation-buy-station-system");
    }

    private Label createCopyableNameLabel(String text, String nameStyleClass) {
        Label l = new Label(text);
        l.getStyleClass().addAll(nameStyleClass, "colonisation-buy-copy-target");
        l.setCursor(Cursor.HAND);
        Tooltip tip = new Tooltip(localizationService.getString("colonisation.buy.tooltipCopySystem"));
        tip.setShowDelay(Duration.millis(350));
        Tooltip.install(l, tip);
        l.setOnMouseClicked(e -> copyToClipboardWithStandardPopup(l, text, e));
        return l;
    }

    private void showDetails(EdColoniseStarSystemSearchResult r) {
        String systemName = r != null ? r.getSystemName() : null;
        if (systemName == null || systemName.isBlank()) {
            return;
        }
        final Long systemId64 = r.getSystemID();
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/exploration/system-visual-view.fxml"));
                Parent content = loader.load();
                SystemVisualViewComponent controller = loader.getController();
                controller.setBodiesListPanelVisible(false);
                controller.setExplorationValueIndicatorsSuppressed(true);
                controller.setPendingSystemTitle(systemName);
                controller.showSpanshLoadingPlaceholder();

                Scene scene = new Scene(content, 1380, 840);
                scene.getStylesheets().add(getClass().getResource("/css/elite-theme.css").toExternalForm());

                Stage detailStage = new Stage();
                detailStage.setTitle(localizationService.getString("colonisation.edcolonise.details.title") + " - " + systemName);
                detailStage.setScene(scene);
                detailStage.setResizable(true);
                if (closeButton != null && closeButton.getScene() != null) {
                    detailStage.initOwner(closeButton.getScene().getWindow());
                }
                detailStage.show();

                Thread t = new Thread(() -> {
                    try {
                        var visited = spanshSystemVisitedService.fetchSystemVisited(systemName, systemId64);
                        Platform.runLater(() -> {
                            if (!detailStage.isShowing()) {
                                return;
                            }
                            controller.displaySystem(visited, true);
                            detailStage.setTitle(localizationService.getString("colonisation.edcolonise.details.title")
                                    + " - " + (visited != null && visited.getSystemName() != null
                                    ? visited.getSystemName() : systemName));
                        });
                    } catch (Exception ex) {
                        String message = "Spansh details fetch failed for system '"
                                + systemName
                                + "': " + ex.getMessage();
                        System.err.println(message);
                        Platform.runLater(detailStage::close);
                    }
                }, "spansh-system-details");
                t.setDaemon(true);
                t.start();
            } catch (Exception ex) {
                System.err.println("Unable to open exploration detail window: " + ex.getMessage());
            }
        });
    }

    private String formatLastUpdate(OffsetDateTime t) {
        if (t == null) {
            return "";
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withLocale(localizationService.getCurrentLocale());
        return t.atZoneSameInstant(ZoneId.systemDefault()).format(fmt);
    }

    /**
     * Somme des types de corps « planétaires » (hors étoiles compactes / autres étoiles).
     */
    private static Integer sumPlanetaryBodies(EdColoniseSystemCounts c) {
        if (c == null) {
            return null;
        }
        int s = 0;
        boolean any = false;
        Integer[] parts = {
                c.getIcyBodyCount(),
                c.getOrganicCount(),
                c.getGasGiantCount(),
                c.getRockBodyCount(),
                c.getEarthLikeCount(),
                c.getMetalRichCount(),
                c.getWaterWorldCount(),
                c.getGeologicalsCount(),
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

    private static Integer ringBodyCount(EdColoniseStarSystemSearchResult r) {
        EdColoniseSystemCounts c = r.getSystemCounts();
        if (c != null && c.getRingCount() != null) {
            return c.getRingCount();
        }
        if (r.getRings() != null) {
            return r.getRings().size();
        }
        return null;
    }

    @FXML
    private void onClose() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
}
