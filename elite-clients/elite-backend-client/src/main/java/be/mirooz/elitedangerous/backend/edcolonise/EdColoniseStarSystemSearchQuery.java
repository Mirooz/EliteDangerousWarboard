package be.mirooz.elitedangerous.backend.edcolonise;

import java.util.Arrays;

/**
 * Paramètres complets pour {@code GET /api/edcolonise/star-systems} (recherche ED Colonise).
 * Les tableaux {@code max19} / {@code min18} suivent l’ordre des paramètres OpenAPI du client généré.
 */
public final class EdColoniseStarSystemSearchQuery {

    public static final int MAX_DISTANCE_TO_SOL_CAP = 2770;

    /**
     * Gabarit des plafonds « max » (ordre OpenAPI), aligné StarSystemSearch.
     */
    public static final int[] DEFAULT_MAX_TEMPLATE = {
            3, 2, 2770, 1, 15, 29, 26, 64, 61, 8, 2, 20, 15, 26, 48, 16, 60, 4, 1
    };

    private static final int IDX_MAX_DISTANCE = 2;
    private static final int IDX_MIN_LANDABLES = 7;
    private static final int IDX_MIN_RINGS = 12;

    private final String systemName;
    private final String factionName;
    private final String hotspotTypes;
    private final String sortOrder;
    private final int pageNo;
    private final int resultsPerPage;
    private final int[] max19;
    private final int[] min18;

    private EdColoniseStarSystemSearchQuery(
            String systemName,
            String factionName,
            String hotspotTypes,
            String sortOrder,
            int pageNo,
            int resultsPerPage,
            int[] max19,
            int[] min18) {
        this.systemName = systemName;
        this.factionName = factionName;
        this.hotspotTypes = hotspotTypes;
        this.sortOrder = sortOrder;
        this.pageNo = pageNo;
        this.resultsPerPage = resultsPerPage;
        this.max19 = Arrays.copyOf(max19, 19);
        this.min18 = Arrays.copyOf(min18, 18);
    }

    /**
     * Requête minimale (onglet colonisation) : distance max, min atterrissables, min anneaux ; autres champs au gabarit par défaut.
     */
    public static EdColoniseStarSystemSearchQuery withSimpleFilters(int maxDistLy, int minLandables, int minRings) {
        int[] max = DEFAULT_MAX_TEMPLATE.clone();
        max[IDX_MAX_DISTANCE] = Math.min(MAX_DISTANCE_TO_SOL_CAP, Math.max(1, maxDistLy));
        int[] min = new int[18];
        min[IDX_MIN_LANDABLES] = Math.max(0, minLandables);
        min[IDX_MIN_RINGS] = Math.max(0, minRings);
        return new EdColoniseStarSystemSearchQuery(
                null,
                null,
                null,
                "SystemValue",
                1,
                10,
                max,
                min);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String systemName() {
        return systemName;
    }

    public String factionName() {
        return factionName;
    }

    public String hotspotTypes() {
        return hotspotTypes;
    }

    public String sortOrder() {
        return sortOrder;
    }

    public int pageNo() {
        return pageNo;
    }

    public int resultsPerPage() {
        return resultsPerPage;
    }

    public int[] maxValues() {
        return Arrays.copyOf(max19, max19.length);
    }

    public int[] minValues() {
        return Arrays.copyOf(min18, min18.length);
    }

    /**
     * Paire (index min 0..17, index max 0..18) pour un filtre corps (ex. landables → 7, 8).
     * {@code metricI18nSuffix} : suffixe pour {@code colonisation.edcolonise.metric.<suffix>} (UI).
     */
    public static final class BodyMinMaxSlot {
        public final int minIndex;
        public final int maxIndex;
        public final String metricI18nSuffix;

        public BodyMinMaxSlot(int minIndex, int maxIndex, String metricI18nSuffix) {
            this.minIndex = minIndex;
            this.maxIndex = maxIndex;
            this.metricI18nSuffix = metricI18nSuffix;
        }
    }

    public static final class Builder {
        private String systemName;
        private String factionName;
        private String hotspotTypes;
        private String sortOrder = "SystemValue";
        private int pageNo = 1;
        private int resultsPerPage = 10;
        private final int[] max19 = DEFAULT_MAX_TEMPLATE.clone();
        private final int[] min18 = new int[18];

        private Builder() {
        }

        public Builder systemName(String v) {
            this.systemName = v;
            return this;
        }

        public Builder factionName(String v) {
            this.factionName = v;
            return this;
        }

        public Builder hotspotTypes(String v) {
            this.hotspotTypes = v;
            return this;
        }

        public Builder sortOrder(String v) {
            if (v != null && !v.isBlank()) {
                this.sortOrder = v.trim().replaceAll("\\s+", "");
            }
            return this;
        }

        public Builder pageNo(int pageNo) {
            this.pageNo = Math.max(1, pageNo);
            return this;
        }

        public Builder resultsPerPage(int n) {
            this.resultsPerPage = Math.max(1, Math.min(100, n));
            return this;
        }

        public Builder maxDistanceToSolLy(int ly) {
            max19[IDX_MAX_DISTANCE] = Math.min(MAX_DISTANCE_TO_SOL_CAP, Math.max(1, ly));
            return this;
        }

        public Builder minLandables(int v) {
            min18[IDX_MIN_LANDABLES] = Math.max(0, v);
            return this;
        }

        public Builder minRings(int v) {
            min18[IDX_MIN_RINGS] = Math.max(0, v);
            return this;
        }

        public Builder slot(EdColoniseStarSystemSearchQuery.BodyMinMaxSlot slot, int minV, int maxV) {
            int lo = Math.max(0, minV);
            int hi = Math.max(0, maxV);
            if (lo > hi) {
                int t = lo;
                lo = hi;
                hi = t;
            }
            min18[slot.minIndex] = lo;
            max19[slot.maxIndex] = hi;
            return this;
        }

        public EdColoniseStarSystemSearchQuery build() {
            normalizePairs();
            String sys = trimOrNull(systemName);
            String fac = trimOrNull(factionName);
            String hot = trimOrNull(hotspotTypes);
            return new EdColoniseStarSystemSearchQuery(
                    sys,
                    fac,
                    hot,
                    sortOrder != null && !sortOrder.isBlank()
                            ? sortOrder.trim().replaceAll("\\s+", "")
                            : "SystemValue",
                    pageNo,
                    resultsPerPage,
                    max19,
                    min18);
        }

        private void normalizePairs() {
            BodyMinMaxSlot[] slots = EdColoniseSearchMetricSlots.ALL;
            for (BodyMinMaxSlot s : slots) {
                int lo = min18[s.minIndex];
                int hi = max19[s.maxIndex];
                if (lo > hi) {
                    min18[s.minIndex] = hi;
                    max19[s.maxIndex] = lo;
                }
            }
            max19[IDX_MAX_DISTANCE] = Math.min(MAX_DISTANCE_TO_SOL_CAP, Math.max(1, max19[IDX_MAX_DISTANCE]));
        }

        private static String trimOrNull(String s) {
            if (s == null) {
                return null;
            }
            String t = s.trim();
            return t.isEmpty() ? null : t;
        }
    }

}
