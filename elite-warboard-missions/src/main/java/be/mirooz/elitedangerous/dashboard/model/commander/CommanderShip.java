package be.mirooz.elitedangerous.dashboard.model.commander;

import be.mirooz.elitedangerous.commons.lib.models.commodities.ICommodity;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
public class CommanderShip {
    private String ship;
    private ShipCargo shipCargo;
    private double maxRange;
    public void addCommodity(ICommodity commodity,int number){
        this.getShipCargo().addCommodity(commodity,number);
    }
    public void addCommodity(ICommodity commodity){
        this.getShipCargo().addCommodity(commodity,1);
    }
    public void removeCommodity(ICommodity commodity){
        this.getShipCargo().removeCommodity(commodity,1);
    }
    public void removeCommodity(ICommodity commodity, int number){
        this.getShipCargo().removeCommodity(commodity,number);
    }
    public void removeAllCommodity(ICommodity commodity){
        this.getShipCargo().removeAllCommodity(commodity);
    }
    public void setCurrentUsed(int x){
        this.getShipCargo().currentUsed =x;
    }
    public boolean isEmpty(){
        return this.getShipCargo().currentUsed ==0;
    }
    public void resetCargo(){
        this.getShipCargo().currentUsed=0;
        this.getShipCargo().commodities.clear();
    }

    @Data
    public static class ShipCargo {
        private int maxCapacity;
        private int currentUsed;
        private final Map<ICommodity, Integer> commodities = new HashMap<>();

        public ShipCargo(int maxCapacity){
            this.maxCapacity = maxCapacity;
        }
        private void addCommodity(ICommodity c, int x) {
            currentUsed+=x;
            commodities.merge(c, x, Integer::sum);
        }
        private void removeCommodity(ICommodity c, int x) {
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
                if (currentUsed < 0) currentUsed = 0; // sécurité anti négatif
            }
        }
        public ShipCargo copy(int newMaxCapacity) {
            ShipCargo copy = new ShipCargo(newMaxCapacity);
            copy.currentUsed = this.currentUsed;
            copy.commodities.putAll(this.commodities);
            return copy;
        }
    }

}
