package be.mirooz.elitedangerous.dashboard.model.registries.commander;

import be.mirooz.elitedangerous.commons.lib.models.commodities.ICommodity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Vaisseau commandant (singleton : une seule instance runtime, alignée sur le
 * modèle d’exploitation en solo).
 */
@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CommanderShip {

    private static final CommanderShip INSTANCE = new CommanderShip();

    public static CommanderShip getInstance() {
        return INSTANCE;
    }

    private String ship;
    @JsonIgnore
    private ShipCargo shipCargo = new ShipCargo();
    @JsonIgnore
    private ShipCargo jsonShipCargo = new ShipCargo();
    private int maxCapacity;
    private double maxRange;

    public void addCommodity(ICommodity commodity, int number) {
        shipCargo.addCommodity(commodity, null, number);
    }

    public void addCommodity(ICommodity commodity) {
        shipCargo.addCommodity(commodity, null, 1);
    }

    public void removeCommodity(ICommodity commodity) {
        shipCargo.removeCommodity(commodity, 1);
    }

    public void removeCommodity(ICommodity commodity, int number) {
        shipCargo.removeCommodity(commodity, number);
    }

    public void removeAllCommodity(ICommodity commodity) {
        shipCargo.removeAllCommodity(commodity);
    }

    public void setCurrentUsed(int x) {
        shipCargo.setCurrentUsed(x);
    }

    public boolean isEmpty() {
        return shipCargo.getCurrentUsed() == 0;
    }

    public void resetCargo() {
        shipCargo.setCurrentUsed(0);
        shipCargo.getCommodities().clear();
    }

}
