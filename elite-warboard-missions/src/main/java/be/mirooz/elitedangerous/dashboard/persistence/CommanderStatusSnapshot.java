package be.mirooz.elitedangerous.dashboard.persistence;

import be.mirooz.elitedangerous.commons.lib.models.commodities.CarrierCommodityResolver;
import be.mirooz.elitedangerous.commons.lib.models.commodities.ICommodity;
import be.mirooz.elitedangerous.dashboard.model.registries.commander.CommanderShip;
import be.mirooz.elitedangerous.dashboard.model.registries.commander.CommanderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Snapshot plat de {@link CommanderStatus} + son {@link CommanderShip} embarqué.
 *
 * <p>Le ship et son cargo sont aplati ici pour éviter toute dépendance Jackson ↔ interface
 * {@code ICommodity} : on stocke {@code cargoJsonName}+{@code inaraName} et on résout à la
 * restauration via {@link CarrierCommodityResolver}.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommanderStatusSnapshot {

    private String currentStarSystem;
    private String currentStationName;
    private String currentBodyName;
    private Long currentBodyId;
    private String commanderName;
    private String fid;
    private Boolean isOnline;
    private boolean isOnFoot;
    private Long currentSystemAddress;
    private double[] currentStarPos;
    private String gameVersion;
    private String gameBuild;
    private Boolean horizons;
    private Boolean odyssey;

    private ShipSnapshot ship;

    public static CommanderStatusSnapshot fromRuntime(CommanderStatus status) {
        return CommanderStatusSnapshot.builder()
                .currentStarSystem(status.getCurrentStarSystem())
                .currentStationName(status.getCurrentStationName())
                .currentBodyName(status.getCurrentBodyName())
                .currentBodyId(status.getCurrentBodyId())
                .commanderName(status.getCommanderName())
                .fid(status.getFID())
                .isOnline(status.getIsOnline())
                .isOnFoot(status.isOnFoot())
                .currentSystemAddress(status.getCurrentSystemAddress())
                .currentStarPos(status.getCurrentStarPos())
                .gameVersion(status.getGameVersion())
                .gameBuild(status.getGameBuild())
                .horizons(status.getHorizons())
                .odyssey(status.getOdyssey())
                .ship(ShipSnapshot.fromRuntime(status.getShip()))
                .build();
    }

    public void restore() {
        CommanderStatus.getInstance().applyFullPersistedSnapshot(
                currentStarSystem,
                currentStationName,
                currentBodyName,
                currentBodyId,
                commanderName,
                fid,
                isOnline,
                isOnFoot,
                currentSystemAddress,
                currentStarPos,
                gameVersion,
                gameBuild,
                horizons,
                odyssey,
                ship != null ? ship.toRuntime() : null
        );
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShipSnapshot {
        private String ship;
        private int maxCapacity;
        private double maxRange;
        private int currentUsed;
        @Builder.Default
        private List<CargoEntry> cargo = new ArrayList<>();

        public static ShipSnapshot fromRuntime(CommanderShip runtime) {
            if (runtime == null) return null;
            List<CargoEntry> entries = new ArrayList<>();
            CommanderShip.ShipCargo shipCargo = runtime.getShipCargo();
            if (shipCargo != null && shipCargo.getCommodities() != null) {
                shipCargo.getCommodities().forEach((c, qty) -> {
                    if (c == null || qty == null || qty <= 0) return;
                    entries.add(CargoEntry.builder()
                            .cargoJsonName(c.getCargoJsonName())
                            .inaraName(c.getInaraName())
                            .quantity(qty)
                            .build());
                });
            }
            return ShipSnapshot.builder()
                    .ship(runtime.getShip())
                    .maxCapacity(runtime.getMaxCapacity())
                    .maxRange(runtime.getMaxRange())
                    .currentUsed(shipCargo != null ? shipCargo.getCurrentUsed() : 0)
                    .cargo(entries)
                    .build();
        }

        public CommanderShip toRuntime() {
            CommanderShip.ShipCargo shipCargo = new CommanderShip.ShipCargo();
            if (cargo != null) {
                for (CargoEntry entry : cargo) {
                    if (entry == null || entry.quantity <= 0) continue;
                    ICommodity commodity = CarrierCommodityResolver.resolve(
                            entry.cargoJsonName, entry.inaraName);
                    if (commodity != null) {
                        shipCargo.addCommodity(commodity, null, entry.quantity);
                    }
                }
            }
            // On re-synchronise le currentUsed au cas où des commodities n'auraient pas été
            // résolues (pour éviter une UI incohérente par rapport aux lignes chargées).
            if (shipCargo.getCurrentUsed() != currentUsed && !shipCargo.getCommodities().isEmpty()) {
                // on garde le total calculé réellement — plus sûr que la valeur persistée
            }
            return CommanderShip.builder()
                    .ship(ship)
                    .shipCargo(shipCargo)
                    .jsonShipCargo(shipCargo.copy())
                    .maxCapacity(maxCapacity)
                    .maxRange(maxRange)
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CargoEntry {
        private String cargoJsonName;
        private String inaraName;
        private int quantity;
    }
}
