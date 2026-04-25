package be.mirooz.elitedangerous.dashboard.persistence;

import be.mirooz.elitedangerous.dashboard.model.exploration.ExplorationDataOnHold;
import be.mirooz.elitedangerous.dashboard.model.exploration.ExplorationDataSale;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.ExplorationDataSaleRegistry;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ExplorationDataSaleRegistryStore extends SnapshotJsonStore<ExplorationDataSaleRegistryStore.Payload> {

    public ExplorationDataSaleRegistryStore(Path file) {
        super(
                "exploration-data-sale-registry",
                file,
                PolymorphicPersistenceMapper.create(),
                Payload.class,
                ExplorationDataSaleRegistryStore::buildSnapshot,
                ExplorationDataSaleRegistryStore::restoreSnapshot
        );
    }

    private static Payload buildSnapshot() {
        ExplorationDataSaleRegistry reg = ExplorationDataSaleRegistry.getInstance();
        Payload p = new Payload();
        p.sales = new ArrayList<>(reg.snapshotSales());
        p.currentSale = reg.getCurrentSale();
        p.explorationDataOnHold = reg.getExplorationDataOnHold();
        return p;
    }

    private static void restoreSnapshot(Payload p) {
        ExplorationDataSaleRegistry.getInstance().applyFullPersistedSnapshot(
                p.sales, p.currentSale, p.explorationDataOnHold);
    }

    public static class Payload {
        public List<ExplorationDataSale> sales;
        public ExplorationDataSale currentSale;
        public ExplorationDataOnHold explorationDataOnHold;
    }
}
