package be.mirooz.elitedangerous.dashboard.view.colonisation;

import be.mirooz.elitedangerous.backend.edcolonise.EdColoniseSearchMetricSlots;
import be.mirooz.elitedangerous.backend.edcolonise.EdColoniseStarSystemSearchQuery;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Grille des filtres corps ED Colonise (min seul ou min+max) + tri + fusion avec les trois filtres principaux.
 */
public final class EdColoniseSearchFilterForm {

    /** Valeurs API {@code sortOrder} (sans espaces), ordre d’affichage du combo. */
    public static final String[] SORT_API_VALUES = {
            "SystemValue", "MostWalkableBodies", "MostHotspots", "DistanceToSol"
    };

    private static final Set<String> SORT_API_VALUE_SET = Set.copyOf(Arrays.asList(SORT_API_VALUES));

    private final HBox metricsRowHost;
    private final List<EdColoniseStarSystemSearchQuery.BodyMinMaxSlot> displayedSlots = new ArrayList<>();
    private final List<Label> metricRowLabels = new ArrayList<>();
    private final List<Spinner<Integer>> metricMinSpinners = new ArrayList<>();
    private final List<Spinner<Integer>> metricMaxSpinners = new ArrayList<>();

    public EdColoniseSearchFilterForm(HBox metricsRowHost) {
        this.metricsRowHost = metricsRowHost;
    }

    /**
     * @param minOnly si {@code true}, un seul spinner « min » par ligne ; le max API reste le gabarit par défaut.
     */
    public void buildMetricsGrid(
            LocalizationService localizationService,
            EdColoniseStarSystemSearchQuery.BodyMinMaxSlot[] slots,
            boolean minOnly) {
        if (metricsRowHost == null) {
            return;
        }
        metricsRowHost.getChildren().clear();
        displayedSlots.clear();
        metricRowLabels.clear();
        metricMinSpinners.clear();
        metricMaxSpinners.clear();

        int[] templ = EdColoniseStarSystemSearchQuery.DEFAULT_MAX_TEMPLATE;
        int half = (slots.length + 1) / 2;

        VBox leftCol = new VBox(8);
        VBox rightCol = new VBox(8);
        leftCol.setFillWidth(true);
        rightCol.setFillWidth(true);

        for (int i = 0; i < slots.length; i++) {
            EdColoniseStarSystemSearchQuery.BodyMinMaxSlot slot = slots[i];
            displayedSlots.add(slot);
            int defMax = templ[slot.maxIndex];
            Spinner<Integer> minS = createIntSpinner(0, defMax, 0);
            metricMinSpinners.add(minS);

            Label name = new Label(localizationService.getString("colonisation.edcolonise.metric." + slot.metricI18nSuffix));
            name.getStyleClass().add("config-label");
            name.setMinWidth(minOnly ? 200 : 170);
            name.setWrapText(true);
            metricRowLabels.add(name);

            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            if (minOnly) {
                minS.getStyleClass().add("edcolonise-advanced-spinner");
                minS.setPrefWidth(102);
                Label geq = new Label("≥");
                geq.getStyleClass().add("edcolonise-filter-geq");
                geq.setMinWidth(22);
                row.getChildren().addAll(name, geq, minS);
            } else {
                Label minTag = new Label(localizationService.getString("colonisation.edcolonise.rangeMin"));
                minTag.getStyleClass().add("colonisation-detail-placeholder");
                Label maxTag = new Label(localizationService.getString("colonisation.edcolonise.rangeMax"));
                maxTag.getStyleClass().add("colonisation-detail-placeholder");
                Spinner<Integer> maxS = createIntSpinner(0, defMax, defMax);
                metricMaxSpinners.add(maxS);
                row.getChildren().addAll(name, minTag, minS, maxTag, maxS);
            }
            HBox.setHgrow(name, Priority.SOMETIMES);
            if (i < half) {
                leftCol.getChildren().add(row);
            } else {
                rightCol.getChildren().add(row);
            }
        }

        HBox columns = new HBox(24);
        columns.setAlignment(Pos.TOP_LEFT);
        columns.getChildren().addAll(leftCol, rightCol);
        metricsRowHost.getChildren().add(columns);
    }

    /** Grille complète min+max (hors usage actuel). */
    public void buildMetricsGrid(LocalizationService localizationService) {
        buildMetricsGrid(localizationService, EdColoniseSearchMetricSlots.ALL, false);
    }

    public void refreshMetricRowLabels(LocalizationService localizationService) {
        for (int i = 0; i < metricRowLabels.size() && i < displayedSlots.size(); i++) {
            EdColoniseStarSystemSearchQuery.BodyMinMaxSlot slot = displayedSlots.get(i);
            metricRowLabels.get(i).setText(
                    localizationService.getString("colonisation.edcolonise.metric." + slot.metricI18nSuffix));
        }
    }

    public void applyMinValuesFromSnapshot(EdColoniseSearchAdvancedSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        int[] src = snapshot.bodyMinExtras();
        for (int i = 0; i < displayedSlots.size() && i < metricMinSpinners.size(); i++) {
            int idx = displayedSlots.get(i).minIndex;
            if (idx >= 0 && idx < src.length) {
                metricMinSpinners.get(i).getValueFactory().setValue(src[idx]);
            }
        }
    }

    /**
     * @param hotspotTypesSerialized chaîne API hotspots (ex. depuis cases à cocher), sans espaces entre les noms
     */
    public EdColoniseSearchAdvancedSnapshot captureAdvancedSnapshot(
            TextField referenceSystemField,
            ComboBox<String> sortOrderCombo,
            TextField factionField,
            Supplier<String> hotspotTypesSerialized) {
        int[] mins = new int[18];
        for (int i = 0; i < displayedSlots.size() && i < metricMinSpinners.size(); i++) {
            int idx = displayedSlots.get(i).minIndex;
            if (idx >= 0 && idx < mins.length) {
                mins[idx] = Math.max(0, metricMinSpinners.get(i).getValue());
            }
        }
        String ref = referenceSystemField != null ? referenceSystemField.getText() : "";
        String sort = normalizeApiSortOrder(sortOrderCombo != null ? sortOrderCombo.getValue() : null);
        String fac = factionField != null ? factionField.getText() : "";
        String hot = hotspotTypesSerialized != null ? hotspotTypesSerialized.get() : "";
        return new EdColoniseSearchAdvancedSnapshot(ref, sort, fac, hot, mins);
    }

    /**
     * Requête complète : trois filtres principaux + snapshot fenêtre avancée (max corps = gabarit API).
     */
    public static EdColoniseStarSystemSearchQuery mergeMainAndAdvanced(
            int minLandables,
            int minRings,
            int maxDistLy,
            EdColoniseSearchAdvancedSnapshot advanced) {
        int distLy = Math.min(EdColoniseStarSystemSearchQuery.MAX_DISTANCE_TO_SOL_CAP, Math.max(1, maxDistLy));
        EdColoniseSearchAdvancedSnapshot adv = advanced != null ? advanced : EdColoniseSearchAdvancedSnapshot.defaults();
        int[] extras = adv.bodyMinExtras();

        EdColoniseStarSystemSearchQuery.Builder b = EdColoniseStarSystemSearchQuery.builder()
                .systemName(adv.referenceSystem())
                .factionName(adv.factionName())
                .hotspotTypes(adv.hotspotTypes())
                .sortOrder(normalizeApiSortOrder(adv.sortOrder()))
                .maxDistanceToSolLy(distLy)
                .minLandables(Math.max(0, minLandables))
                .minRings(Math.max(0, minRings));

        int[] templ = Arrays.copyOf(EdColoniseStarSystemSearchQuery.DEFAULT_MAX_TEMPLATE, 19);
        templ[2] = distLy;

        for (EdColoniseStarSystemSearchQuery.BodyMinMaxSlot slot : EdColoniseSearchMetricSlots.ALL) {
            int lo;
            if (slot == EdColoniseSearchMetricSlots.LANDABLES) {
                lo = Math.max(0, minLandables);
            } else if (slot == EdColoniseSearchMetricSlots.RINGS) {
                lo = Math.max(0, minRings);
            } else {
                lo = Math.max(0, extras[slot.minIndex]);
            }
            int hi = templ[slot.maxIndex];
            b.slot(slot, lo, hi);
        }
        return b.build();
    }

    public static void setupSortOrderCombo(ComboBox<String> sortOrderCombo, LocalizationService localizationService) {
        if (sortOrderCombo == null) {
            return;
        }
        sortOrderCombo.getItems().setAll(SORT_API_VALUES);
        if (sortOrderCombo.getValue() == null || !sortOrderCombo.getItems().contains(sortOrderCombo.getValue())) {
            sortOrderCombo.setValue(SORT_API_VALUES[0]);
        }
        sortOrderCombo.setCellFactory(lv -> createSortOrderCell(localizationService));
        sortOrderCombo.setButtonCell(createSortOrderCell(localizationService));
        refreshSortOrderComboLabels(sortOrderCombo, localizationService);
    }

    public static void refreshSortOrderComboLabels(ComboBox<String> sortOrderCombo, LocalizationService localizationService) {
        if (sortOrderCombo == null) {
            return;
        }
        sortOrderCombo.setButtonCell(createSortOrderCell(localizationService));
    }

    private static ListCell<String> createSortOrderCell(LocalizationService localizationService) {
        return new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(localizationService.getString("colonisation.edcolonise.sort." + item));
                }
            }
        };
    }

    /** Tri API : sans espaces ; valeur inconnue (ex. ancien {@code Name}) → {@code SystemValue}. */
    public static String normalizeApiSortOrder(String value) {
        if (value == null || value.isBlank()) {
            return "SystemValue";
        }
        String v = value.trim().replaceAll("\\s+", "");
        if (SORT_API_VALUE_SET.contains(v)) {
            return v;
        }
        return "SystemValue";
    }

    private static Spinner<Integer> createIntSpinner(int min, int max, int initial) {
        Spinner<Integer> s = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, initial));
        s.setEditable(true);
        s.setPrefWidth(88);
        return s;
    }
}
