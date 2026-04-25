package be.mirooz.elitedangerous.dashboard.persistence;

import be.mirooz.elitedangerous.dashboard.model.exploration.OrganicDataOnHold;
import be.mirooz.elitedangerous.dashboard.model.exploration.OrganicDataSale;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.OrganicDataSaleRegistry;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class OrganicDataSaleRegistryStore extends SnapshotJsonStore<OrganicDataSaleRegistryStore.Payload> {

    public OrganicDataSaleRegistryStore(Path file) {
        super(
                "organic-data-sale-registry",
                file,
                PolymorphicPersistenceMapper.create(),
                Payload.class,
                OrganicDataSaleRegistryStore::buildSnapshot,
                OrganicDataSaleRegistryStore::restoreSnapshot
        );
    }

    private static Payload buildSnapshot() {
        OrganicDataSaleRegistry reg = OrganicDataSaleRegistry.getInstance();
        Payload p = new Payload();
        p.sales = new ArrayList<>(reg.snapshotSales());
        p.currentOrganicDataOnHold = reg.getCurrentOrganicDataOnHold();
        return p;
    }

    private static void restoreSnapshot(Payload p) {
        OrganicDataSaleRegistry.getInstance().applyFullPersistedSnapshot(
                p.sales, p.currentOrganicDataOnHold);
    }

    public static class Payload {
        public List<OrganicDataSale> sales;
        public OrganicDataOnHold currentOrganicDataOnHold;
    }
}
