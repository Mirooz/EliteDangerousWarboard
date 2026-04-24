package be.mirooz.elitedangerous.dashboard.persistence;

import be.mirooz.elitedangerous.dashboard.model.exploration.ExplorationDataOnHold;
import be.mirooz.elitedangerous.dashboard.model.exploration.ExplorationDataSale;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.ExplorationDataSaleRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ExplorationDataSaleRegistryStore implements RegistryStore {

    private final Path file;
    private final ObjectMapper mapper = PolymorphicPersistenceMapper.create();

    public ExplorationDataSaleRegistryStore(Path file) {
        this.file = file;
    }

    @Override
    public String name() {
        return "exploration-data-sale-registry";
    }

    @Override
    public void save() {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            ExplorationDataSaleRegistry reg = ExplorationDataSaleRegistry.getInstance();
            Payload p = new Payload();
            p.sales = new ArrayList<>(reg.snapshotSales());
            p.currentSale = reg.getCurrentSale();
            p.explorationDataOnHold = reg.getExplorationDataOnHold();
            mapper.writeValue(file.toFile(), p);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot save exploration data sale registry to " + file, e);
        }
    }

    @Override
    public boolean loadIfExists() {
        if (!Files.exists(file)) {
            return false;
        }
        try {
            Payload p = mapper.readValue(file.toFile(), Payload.class);
            ExplorationDataSaleRegistry.getInstance().applyFullPersistedSnapshot(
                    p.sales, p.currentSale, p.explorationDataOnHold);
            return true;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load exploration data sale registry from " + file, e);
        }
    }

    @Override
    public void deleteIfExists() {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot delete exploration data sale registry file " + file, e);
        }
    }

    public static class Payload {
        public List<ExplorationDataSale> sales;
        public ExplorationDataSale currentSale;
        public ExplorationDataOnHold explorationDataOnHold;
    }
}
