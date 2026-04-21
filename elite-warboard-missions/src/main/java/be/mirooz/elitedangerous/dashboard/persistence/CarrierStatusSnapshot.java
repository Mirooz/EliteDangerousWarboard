package be.mirooz.elitedangerous.dashboard.persistence;

import be.mirooz.elitedangerous.backend.generated.model.CapiFleetCarrierProxyResponse;
import be.mirooz.elitedangerous.commons.lib.models.commodities.CarrierCommodityResolver;
import be.mirooz.elitedangerous.commons.lib.models.commodities.ICommodity;
import be.mirooz.elitedangerous.dashboard.model.colonisation.CarrierTradeOrderEntry;
import be.mirooz.elitedangerous.dashboard.model.fleetcarrier.CarrierPosition;
import be.mirooz.elitedangerous.dashboard.model.registries.fleetcarrier.CarrierStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarrierStatusSnapshot {

    private long carrierId;
    private String carrierType;
    private String callsign;
    private String name;
    private int totalCapacity;
    private boolean blackMarket;
    private boolean carrierStatsInitialized;
    private long balance;
    private int fuel;
    private String operationalState;
    private String positionStarSystem;
    private long positionBodyId;
    private Instant lastModifiedTime;

    @Builder.Default
    private List<StockSnapshot> stocks = new ArrayList<>();

    @Builder.Default
    private List<OrderSnapshot> orders = new ArrayList<>();

    public static CarrierStatusSnapshot fromRuntime(CarrierStatus status) {
        CarrierPosition position = status.getPosition();

        List<StockSnapshot> stocks = status.getStocksByCommodity().entrySet().stream()
                .filter(e -> e.getKey() != null && e.getValue() != null && e.getValue() > 0)
                .map(e -> StockSnapshot.builder()
                        .cargoJsonName(e.getKey().getCargoJsonName())
                        .inaraName(e.getKey().getInaraName())
                        .quantity(e.getValue())
                        .build())
                .toList();

        List<OrderSnapshot> orders = status.getActiveTransactions().stream()
                .filter(o -> o != null && o.getCommodity() != null && !o.isCancelTrade())
                .map(o -> OrderSnapshot.builder()
                        .timestamp(o.getTimestamp())
                        .carrierId(o.getCarrierId())
                        .carrierType(o.getCarrierType())
                        .blackMarket(o.isBlackMarket())
                        .cargoJsonName(o.getCommodity().getCargoJsonName())
                        .inaraName(o.getCommodity().getInaraName())
                        .purchaseOrder(o.getPurchaseOrder())
                        .saleOrder(o.getSaleOrder())
                        .cancelTrade(o.isCancelTrade())
                        .price(o.getPrice())
                        .stock(o.getStock())
                        .build())
                .toList();

        return CarrierStatusSnapshot.builder()
                .carrierId(status.getCarrierId())
                .carrierType(status.getCarrierType())
                .callsign(status.getCallsign())
                .name(status.getName())
                .totalCapacity(status.getTotalCapacity())
                .blackMarket(status.isBlackMarket())
                .carrierStatsInitialized(status.isCarrierStatsInitialized())
                .balance(status.getBalance())
                .fuel(status.getFuel())
                .operationalState(status.getOperationalState())
                .positionStarSystem(position != null ? nvl(position.getStarSystem()) : "")
                .positionBodyId(position != null ? position.getBodyId() : -1L)
                .lastModifiedTime(status.getLastModifiedTime())
                .stocks(stocks)
                .orders(orders)
                .build();
    }

    public void restore() {
        Map<ICommodity, Integer> restoredStocks = new LinkedHashMap<>();
        for (StockSnapshot stock : stocks) {
            if (stock == null || stock.getQuantity() <= 0) {
                continue;
            }
            ICommodity commodity = CarrierCommodityResolver.resolve(
                    nvl(stock.getCargoJsonName()),
                    nvl(stock.getInaraName())
            );
            if (commodity != null) {
                restoredStocks.put(commodity, stock.getQuantity());
            }
        }

        List<CarrierTradeOrderEntry> restoredOrders = new ArrayList<>();
        for (OrderSnapshot order : orders) {
            if (order == null) {
                continue;
            }
            ICommodity commodity = CarrierCommodityResolver.resolve(
                    nvl(order.getCargoJsonName()),
                    nvl(order.getInaraName())
            );
            if (commodity == null) {
                continue;
            }

            restoredOrders.add(new CarrierTradeOrderEntry(
                    order.getTimestamp(),
                    order.getCarrierId(),
                    order.getCarrierType(),
                    order.isBlackMarket(),
                    commodity,
                    order.getPurchaseOrder(),
                    order.getSaleOrder(),
                    order.isCancelTrade(),
                    order.getPrice(),
                    order.getStock()
            ));
        }

        CarrierStatus.getInstance().applyFullPersistedSnapshot(
                carrierId,
                nvl(carrierType),
                nvl(callsign),
                nvl(name),
                totalCapacity,
                blackMarket,
                carrierStatsInitialized,
                balance,
                fuel,
                nvl(operationalState),
                nvl(positionStarSystem),
                positionBodyId,
                lastModifiedTime,
                restoredStocks,
                restoredOrders
        );
    }

    private static String nvl(String value) {
        return value != null ? value : "";
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockSnapshot {
        private String cargoJsonName;
        private String inaraName;
        private int quantity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderSnapshot {
        private String timestamp;
        private long carrierId;
        private String carrierType;
        private boolean blackMarket;
        private String cargoJsonName;
        private String inaraName;
        private int purchaseOrder;
        private int saleOrder;
        private boolean cancelTrade;
        private long price;
        private int stock;
    }
}