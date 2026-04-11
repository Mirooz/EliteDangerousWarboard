package be.mirooz.elitedangerous.dashboard.model.registries.colonisation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Synchronized;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * Historique récent des ordres d'achat / vente du Fleet Carrier (événement CarrierTradeOrder).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CarrierTradeOrderRegistry {

    private static final int MAX_ENTRIES = 50;

    private static final CarrierTradeOrderRegistry INSTANCE = new CarrierTradeOrderRegistry();

    private final Deque<CarrierTradeOrderEntry> recent = new ArrayDeque<>();

    public static CarrierTradeOrderRegistry getInstance() {
        return INSTANCE;
    }

    @Synchronized
    public void record(CarrierTradeOrderEntry entry) {
        if (entry == null) {
            return;
        }
        recent.addLast(entry);
        while (recent.size() > MAX_ENTRIES) {
            recent.removeFirst();
        }
    }

    @Synchronized
    public List<CarrierTradeOrderEntry> getRecentOrders() {
        return Collections.unmodifiableList(new ArrayList<>(recent));
    }
}
