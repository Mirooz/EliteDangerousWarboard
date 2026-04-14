package be.mirooz.elitedangerous.dashboard.model.registries.colonisation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Synchronized;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Registre des ordres d'achat / vente du Fleet Carrier (événement CarrierTradeOrder),
 * indexé par le nom localisé de la commodity.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CarrierTradeOrderRegistry {

    private static final CarrierTradeOrderRegistry INSTANCE = new CarrierTradeOrderRegistry();

    private final Map<String, CarrierTradeOrderEntry> ordersByCommodity = new LinkedHashMap<>();

    public static CarrierTradeOrderRegistry getInstance() {
        return INSTANCE;
    }

    @Synchronized
    public void record(CarrierTradeOrderEntry entry) {
        if (entry == null) {
            return;
        }
        String commodityKey = normalizeCommodityKey(entry.getCommodityLocalised());
        if (entry.isCancelTrade()) {
            ordersByCommodity.remove(commodityKey);
            return;
        }
        ordersByCommodity.put(commodityKey, entry);
    }

    @Synchronized
    public Map<String, CarrierTradeOrderEntry> getOrdersByCommodity() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(ordersByCommodity));
    }

    @Synchronized
    public List<String> getPurchaseOrder() {
        return ordersByCommodity.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().getPurchaseOrder() != 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    @Synchronized
    public List<String> getSaleOrder() {
        return ordersByCommodity.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().getSaleOrder() != 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private String normalizeCommodityKey(String commodityLocalised) {
        if (commodityLocalised == null || commodityLocalised.isBlank()) {
            return "__UNKNOWN_COMMODITY__";
        }
        return commodityLocalised;
    }
}
