package be.mirooz.elitedangerous.dashboard.persistence;

import be.mirooz.elitedangerous.dashboard.model.exploration.OrganicDataOnHold;
import be.mirooz.elitedangerous.dashboard.model.exploration.OrganicDataSale;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.OrganicDataSaleRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class OrganicDataSaleRegistryStore implements RegistryStore {

    private final Path file;
    private final ObjectMapper mapper = PolymorphicPersistenceMapper.create();

    public OrganicDataSaleRegistryStore(Path file) {
        this.file = file;
    }

    @Override
    public String name() {
        return "organic-data-sale-registry";
    }

    @Override
    public void save() {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            OrganicDataSaleRegistry reg = OrganicDataSaleRegistry.getInstance();
            Payload p = new Payload();
            p.sales = new ArrayList<>(reg.snapshotSales());
            p.currentOrganicDataOnHold = reg.getCurrentOrganicDataOnHold();
            mapper.writeValue(file.toFile(), p);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot save organic data sale registry to " + file, e);
        }
    }

    @Override
    public boolean loadIfExists() {
        if (!Files.exists(file)) {
            return false;
        }
        try {
            Payload p = mapper.readValue(file.toFile(), Payload.class);
            OrganicDataSaleRegistry.getInstance().applyFullPersistedSnapshot(
                    p.sales, p.currentOrganicDataOnHold);
            return true;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load organic data sale registry from " + file, e);
        }
    }

    @Override
    public void deleteIfExists() {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot delete organic data sale registry file " + file, e);
        }
    }

    public static class Payload {
        public List<OrganicDataSale> sales;
        public OrganicDataOnHold currentOrganicDataOnHold;
    }
}
