package be.mirooz.elitedangerous.dashboard.model.registries.commander;

import be.mirooz.elitedangerous.commons.lib.models.commodities.ICommodity;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/** Cargo runtime du vaisseau (non persisté). */
@Data
public class ShipCargo {
    private int currentUsed;
    private final Map<ICommodity, Integer> commodities = new HashMap<>();

    public void addCommodity(ICommodity c, String nameLocalised, int x) {
        currentUsed += x;
        c.setLocalisedName(nameLocalised);
        commodities.merge(c, x, Integer::sum);
    }

    public void removeCommodity(ICommodity c, int x) {
        commodities.computeIfPresent(c, (k, v) -> {
            int removed = Math.min(x, v);
            currentUsed -= removed;
            return (v - removed) > 0 ? v - removed : null;
        });
    }

    public void removeAllCommodity(ICommodity commodity) {
        Integer currentQuantity = commodities.remove(commodity);
        if (currentQuantity != null) {
            currentUsed -= currentQuantity;
            if (currentUsed < 0) {
                currentUsed = 0;
            }
        }
    }

    public ShipCargo copy() {
        ShipCargo copy = new ShipCargo();
        copy.currentUsed = this.currentUsed;
        copy.commodities.putAll(this.commodities);
        return copy;
    }
}
