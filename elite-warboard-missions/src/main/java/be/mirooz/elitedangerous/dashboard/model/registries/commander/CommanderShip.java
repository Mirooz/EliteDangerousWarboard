package be.mirooz.elitedangerous.dashboard.model.registries.commander;

import be.mirooz.elitedangerous.commons.lib.models.commodities.ICommodity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

/**
 * Vaisseau commandant (singleton : une seule instance runtime, alignée sur le
 * modèle d’exploitation en solo).
 * <p>
 * L’identité vaisseau (nom, autonomie, etc.) vient des snapshots JSON ; le cargo
 * n’est pas persisté dans le même fichier. Les champs de persistance s’appliquent
 * via {@link PersistenceFile#applyToSingleton()}.
 */
@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CommanderShip {

    private static final CommanderShip INSTANCE = new CommanderShip();

    public static CommanderShip getInstance() {
        return INSTANCE;
    }

    private String ship;
    private ShipCargo shipCargo = new ShipCargo();
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

    /**
     * DTO JSON pour {@code commander-ship.json} (déclaré dans
     * {@link be.mirooz.elitedangerous.dashboard.persistence.DashboardRegistryJsonPersistence}).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class PersistenceFile {
        private String ship;
        private int maxCapacity;
        private double maxRange;

        public static PersistenceFile fromRuntime(CommanderShip runtime) {
            if (runtime == null) {
                return PersistenceFile.builder().build();
            }
            return PersistenceFile.builder()
                    .ship(runtime.getShip())
                    .maxCapacity(runtime.getMaxCapacity())
                    .maxRange(runtime.getMaxRange())
                    .build();
        }

        public void restore() {
            synchronized (getInstance()) {
                CommanderShip s = getInstance();
                s.setShip(ship);
                s.setMaxCapacity(maxCapacity);
                s.setMaxRange(maxRange);
            }
        }
    }
}
