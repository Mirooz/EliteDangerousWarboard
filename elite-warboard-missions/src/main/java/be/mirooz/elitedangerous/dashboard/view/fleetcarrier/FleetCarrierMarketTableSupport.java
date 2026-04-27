package be.mirooz.elitedangerous.dashboard.view.fleetcarrier;

import be.mirooz.elitedangerous.commons.lib.models.commodities.CarrierCommodityResolver;
import be.mirooz.elitedangerous.commons.lib.models.commodities.ColonisationCommodityKeys;
import be.mirooz.elitedangerous.commons.lib.models.commodities.CommodityCategory;
import be.mirooz.elitedangerous.commons.lib.models.commodities.ICommodity;
import be.mirooz.elitedangerous.dashboard.model.colonisation.CarrierTradeOrderEntry;
import be.mirooz.elitedangerous.dashboard.model.registries.commander.ShipCargo;
import be.mirooz.elitedangerous.dashboard.model.registries.fleetcarrier.CarrierStatus;
import be.mirooz.elitedangerous.dashboard.service.LocalizationService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Construction et tri des lignes du tableau Fleet Carrier (partagé colonisation + overlay).
 */
public final class FleetCarrierMarketTableSupport {

    private FleetCarrierMarketTableSupport() {
    }

    public static CommodityCategory rowCategory(FleetCarrierMarketRow r) {
        if (r == null || r.getCommodity() == null) {
            return CommodityCategory.UNKNOWN;
        }
        return r.getCommodity().getInaraCommodityCategory();
    }

    public static String formatCreditsThousandsDots(long amount) {
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

    public static Map<String, Integer> shipStockByMergeKey(ShipCargo cargo) {
        Map<String, Integer> out = new LinkedHashMap<>();
        if (cargo == null || cargo.getCommodities() == null) {
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

    public static List<FleetCarrierMarketRow> buildMarketRows(CarrierStatus cs) {
        Map<ICommodity, Integer> stocks = cs.getStocksByCommodity();
        Map<String, FleetCarrierMarketRow> acc = new LinkedHashMap<>();

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
            String display = cs.displayLabel(nk);
            if (display == null || display.isBlank() || "?".equals(display)) {
                display = firstNonBlank(
                        nk.getTitleName(),
                        nk.getVisibleName(),
                        nk.getCargoJsonName());
            }
            long listedPerTonCr = (e.getPurchaseOrder() > 0 || e.getSaleOrder() > 0) ? e.getPrice() : 0L;
            String rowKey = ColonisationCommodityKeys.mergeKey(nk);
            acc.put(rowKey, new FleetCarrierMarketRow(nk, display, stockVal, e.getPurchaseOrder(), e.getSaleOrder(), listedPerTonCr));
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
                    FleetCarrierMarketRow old = acc.get(nk);
                    acc.put(nk, old.withStock(Math.max(old.getStock(), st)));
                } else {
                    acc.put(nk, new FleetCarrierMarketRow(comm, cs.displayLabel(comm), st, 0, 0, 0L));
                }
            }
        }

        List<FleetCarrierMarketRow> out = new ArrayList<>(acc.values());
        return out;
    }

    /**
     * Fusionne le marché FC avec les tonnages « restants chantier » ({@code missingByCommodity} = requis − fourni).
     * Puis calcule le manquant affiché / marché optimal : {@code max(0, remaining − stock_carrier − stock_cargo_commandant)}.
     *
     * @param shipStockByMergeKey tonnes dans le cargo du commandant par clé de fusion ; vide ou {@code null} → 0.
     */
    public static List<FleetCarrierMarketRow> buildMergedRows(
            CarrierStatus cs,
            Map<String, Integer> missingByCommodity,
            Map<String, String> missingDisplayByCommodity,
            LocalizationService localizationService,
            Map<String, Integer> shipStockByMergeKey) {
        List<FleetCarrierMarketRow> base = buildMarketRows(cs);
        Map<String, FleetCarrierMarketRow> byCommodity = new LinkedHashMap<>();
        for (FleetCarrierMarketRow r : base) {
            byCommodity.put(r.getCommodityKey(), r);
        }
        if (missingByCommodity != null) {
            for (Map.Entry<String, Integer> e : missingByCommodity.entrySet()) {
                String k = e.getKey();
                int missing = e.getValue();
                FleetCarrierMarketRow existing = byCommodity.get(k);
                if (existing != null) {
                    byCommodity.put(k, existing.withMissing(missing));
                } else {
                    String display = missingDisplayByCommodity != null
                            ? missingDisplayByCommodity.getOrDefault(k, k)
                            : k;
                    ICommodity onlyMissing = CarrierCommodityResolver.resolve(k, display);
                    byCommodity.put(k, new FleetCarrierMarketRow(onlyMissing, display, 0, 0, 0, 0L, missing));
                }
            }
        }
        Map<String, Integer> ship = shipStockByMergeKey != null ? shipStockByMergeKey : Map.of();
        List<FleetCarrierMarketRow> out = new ArrayList<>(byCommodity.size());
        for (FleetCarrierMarketRow r : byCommodity.values()) {
            String k = r.getCommodityKey();
            String constructionLabel = missingDisplayByCommodity != null ? missingDisplayByCommodity.get(k) : null;
            FleetCarrierMarketRow row = r;
            if (constructionLabel != null && !constructionLabel.isBlank()) {
                row = row.withDisplayName(constructionLabel);
            }
            int shipTons = ship.getOrDefault(k, 0);
            int effectiveMissing = Math.max(0, row.getMissing() - row.getStock() - shipTons);
            out.add(row.withMissing(effectiveMissing));
        }
        sortRows(out, localizationService);
        return out;
    }

    public static void sortRows(List<FleetCarrierMarketRow> rows, LocalizationService localizationService) {
        Comparator<FleetCarrierMarketRow> cmp = Comparator
                .comparing((FleetCarrierMarketRow r) -> categorySortKey(r, localizationService), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(FleetCarrierMarketRow::getDisplayName, String.CASE_INSENSITIVE_ORDER);
        rows.sort(cmp);
    }

    private static String categorySortKey(FleetCarrierMarketRow r, LocalizationService localizationService) {
        if (localizationService == null) {
            return rowCategory(r).name();
        }
        return localizationService.getString("colonisation.fleet.commodityCategory." + rowCategory(r).name());
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
}
