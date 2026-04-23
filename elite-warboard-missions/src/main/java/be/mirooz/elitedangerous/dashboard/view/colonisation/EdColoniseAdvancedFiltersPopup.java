package be.mirooz.elitedangerous.dashboard.view.colonisation;

import be.mirooz.elitedangerous.backend.edcolonise.EdColoniseSearchMetricSlots;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import be.mirooz.elitedangerous.dashboard.view.common.DialogComponent;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Filtres avancés ED Colonise : même type de fenêtre que la configuration ({@link DialogComponent}).
 */
public final class EdColoniseAdvancedFiltersPopup {

    /** Largeur / hauteur du dialogue : compact, sans marge vide sous le formulaire. */
    private static final double DIALOG_WIDTH = 840;
    private static final double DIALOG_HEIGHT = 660;
    /** Hauteur de la zone défilante : évite d’étirer le ScrollPane sur toute la fenêtre (bande vide). */
    private static final double SCROLL_VIEWPORT_PREF_HEIGHT = 480;

    private static final String HOTSPOT_NONE_VALUE = "";

    private EdColoniseAdvancedFiltersPopup() {
    }

    /**
     * Ouvre le dialogue modal (transparent, non redimensionnable, {@code showAndWait} comme les settings).
     */
    public static void show(
            Window owner,
            LocalizationService localizationService,
            EdColoniseSearchAdvancedSnapshot initial,
            Consumer<EdColoniseSearchAdvancedSnapshot> onOk) {
        EdColoniseSearchAdvancedSnapshot base = initial != null ? initial : EdColoniseSearchAdvancedSnapshot.defaults();

        String windowTitle = localizationService.getString("colonisation.edcolonise.advancedFilters.title");

        TextField referenceField = new TextField(base.referenceSystem());
        referenceField.setMaxWidth(Double.MAX_VALUE);
        referenceField.getStyleClass().addAll("elite-textfield", "edcolonise-advanced-field");

        ComboBox<String> sortCombo = new ComboBox<>();
        sortCombo.getStyleClass().addAll("elite-combobox", "edcolonise-advanced-field");
        sortCombo.setMaxWidth(Double.MAX_VALUE);
        EdColoniseSearchFilterForm.setupSortOrderCombo(sortCombo, localizationService);
        if (base.sortOrder() != null && sortCombo.getItems().contains(base.sortOrder())) {
            sortCombo.setValue(base.sortOrder());
        } else {
            sortCombo.setValue(EdColoniseSearchFilterForm.SORT_API_VALUES[0]);
        }

        TextField factionField = new TextField(base.factionName());
        factionField.setMaxWidth(Double.MAX_VALUE);
        factionField.getStyleClass().addAll("elite-textfield", "edcolonise-advanced-field");

        ComboBox<String> hotspotCombo = new ComboBox<>();
        hotspotCombo.setMaxWidth(Double.MAX_VALUE);
        hotspotCombo.getStyleClass().addAll("elite-combobox", "edcolonise-advanced-field");
        setupHotspotCombo(hotspotCombo, localizationService);
        applyHotspotInitialValue(hotspotCombo, base.hotspotTypes());

        HBox metricsHost = new HBox();
        metricsHost.setSpacing(14);
        metricsHost.setAlignment(Pos.TOP_LEFT);
        EdColoniseSearchFilterForm form = new EdColoniseSearchFilterForm(metricsHost);
        form.buildMetricsGrid(localizationService, EdColoniseSearchMetricSlots.ALL_EXCEPT_LANDABLES_RINGS, true);
        form.applyMinValuesFromSnapshot(base);

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.getStyleClass().addAll("colonisation-architect-scroll", "colonisation-detail-scroll");
        scroll.setPrefViewportHeight(SCROLL_VIEWPORT_PREF_HEIGHT);
        scroll.setMinViewportHeight(120);

        VBox formBody = new VBox(12);
        formBody.getStyleClass().addAll("config-panel", "edcolonise-advanced-form-panel");
        formBody.setPadding(new Insets(10, 12, 12, 12));
        formBody.setMaxWidth(800);
        formBody.getChildren().addAll(
                labeledRow(localizationService, "colonisation.edcolonise.field.referenceSystem", referenceField),
                labeledRow(localizationService, "colonisation.edcolonise.field.sortOrder", sortCombo),
                labeledRow(localizationService, "colonisation.edcolonise.field.faction", factionField),
                labeledRow(localizationService, "colonisation.edcolonise.field.hotspots", hotspotCombo),
                metricsHost
        );
        VBox.setVgrow(metricsHost, Priority.SOMETIMES);
        scroll.setContent(formBody);

        Button okButton = new Button(localizationService.getString("colonisation.edcolonise.advancedFilters.ok"));
        okButton.getStyleClass().add("elite-button");
        Button resetButton = new Button(localizationService.getString("colonisation.edcolonise.advancedFilters.reset"));
        resetButton.getStyleClass().add("elite-nav-button");
        Runnable resetFormToDefaults = () -> {
            EdColoniseSearchAdvancedSnapshot d = EdColoniseSearchAdvancedSnapshot.defaults();
            referenceField.setText(d.referenceSystem());
            String sort = d.sortOrder();
            if (sort != null && sortCombo.getItems().contains(sort)) {
                sortCombo.setValue(sort);
            } else {
                sortCombo.setValue(EdColoniseSearchFilterForm.SORT_API_VALUES[0]);
            }
            factionField.setText(d.factionName());
            hotspotCombo.setValue(HOTSPOT_NONE_VALUE);
            form.applyMinValuesFromSnapshot(d);
        };
        resetButton.setOnAction(e -> resetFormToDefaults.run());

        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_LEFT);
        buttons.setPadding(new Insets(12, 0, 0, 0));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        buttons.getChildren().addAll(resetButton, spacer, okButton);

        Label headerTitle = new Label(windowTitle);
        headerTitle.getStyleClass().add("config-title");
        headerTitle.setWrapText(true);
        HBox header = new HBox(headerTitle);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("config-header");

        VBox inner = new VBox(10);
        inner.getStyleClass().add("config-content");
        inner.setPadding(new Insets(8, 10, 6, 10));
        inner.getChildren().addAll(header, scroll, buttons);

        StackPane outer = new StackPane(inner);
        outer.getStyleClass().addAll("config-dialog", "edcolonise-advanced-filters-root");

        DialogComponent dialog = new DialogComponent(outer, "/css/elite-theme.css", windowTitle, DIALOG_WIDTH, DIALOG_HEIGHT);
        dialog.init(owner);

        Stage stage = dialog.getStage();
        Runnable close = stage::close;

        okButton.setOnAction(e -> {
            EdColoniseSearchAdvancedSnapshot snap = form.captureAdvancedSnapshot(
                    referenceField,
                    sortCombo,
                    factionField,
                    () -> hotspotValueForApi(hotspotCombo));
            if (onOk != null) {
                onOk.accept(snap);
            }
            close.run();
        });

        var scene = stage.getScene();
        if (scene != null) {
            scene.addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
                if (ev.getCode() == KeyCode.ESCAPE) {
                    close.run();
                    ev.consume();
                }
            });
        }

        stage.setOnShown(e -> Platform.runLater(() -> {
            stage.requestFocus();
            referenceField.requestFocus();
            if (!referenceField.getText().isEmpty()) {
                referenceField.selectAll();
            }
        }));

        dialog.showAndWait();
    }

    private static void setupHotspotCombo(ComboBox<String> combo, LocalizationService localizationService) {
        ObservableList<String> items = FXCollections.observableArrayList();
        items.add(HOTSPOT_NONE_VALUE);
        items.addAll(EdColoniseHotspotTypes.ALL_SORTED);
        combo.setItems(items);
        combo.setValue(HOTSPOT_NONE_VALUE);
        combo.setCellFactory(lv -> createHotspotListCell(localizationService));
        combo.setButtonCell(createHotspotListCell(localizationService));
    }

    private static ListCell<String> createHotspotListCell(LocalizationService localizationService) {
        return new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else if (HOTSPOT_NONE_VALUE.equals(item)) {
                    setText(localizationService.getString("colonisation.edcolonise.hotspot.none"));
                } else {
                    setText(item);
                }
            }
        };
    }

    private static void applyHotspotInitialValue(ComboBox<String> combo, String initialCsv) {
        Set<String> parsed = EdColoniseHotspotTypes.parseCsvToDisplayNames(initialCsv);
        if (parsed.isEmpty()) {
            combo.setValue(HOTSPOT_NONE_VALUE);
            return;
        }
        List<String> sorted = new ArrayList<>(parsed);
        sorted.sort(String.CASE_INSENSITIVE_ORDER);
        String pick = sorted.get(0);
        if (combo.getItems().contains(pick)) {
            combo.setValue(pick);
        } else {
            combo.setValue(HOTSPOT_NONE_VALUE);
        }
    }

    private static String hotspotValueForApi(ComboBox<String> combo) {
        String v = combo != null ? combo.getValue() : null;
        if (v == null || HOTSPOT_NONE_VALUE.equals(v) || v.isBlank()) {
            return "";
        }
        return EdColoniseHotspotTypes.serializeForApi(List.of(v));
    }

    private static HBox labeledRow(LocalizationService loc, String labelKey, javafx.scene.Node control) {
        Label label = new Label(loc.getString(labelKey));
        label.getStyleClass().add("config-label");
        label.setMinWidth(150);
        label.setPrefWidth(150);
        label.setMaxWidth(200);
        label.setWrapText(true);
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(control, Priority.ALWAYS);
        row.getChildren().addAll(label, control);
        return row;
    }
}
