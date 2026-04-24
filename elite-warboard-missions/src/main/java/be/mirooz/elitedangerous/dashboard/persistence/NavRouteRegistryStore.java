package be.mirooz.elitedangerous.dashboard.persistence;

import be.mirooz.elitedangerous.backend.spansh.ExplorationMode;
import be.mirooz.elitedangerous.dashboard.model.navigation.NavRoute;
import be.mirooz.elitedangerous.dashboard.model.registries.navigation.NavRouteRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;

public class NavRouteRegistryStore implements RegistryStore {

    private final Path file;
    private final ObjectMapper mapper = PolymorphicPersistenceMapper.create();

    public NavRouteRegistryStore(Path file) {
        this.file = file;
    }

    @Override
    public String name() {
        return "nav-route-registry";
    }

    @Override
    public void save() {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Payload p = new Payload();
            p.routes = new HashMap<>();
            p.routes.putAll(NavRouteRegistry.getInstance().snapshotRoutes());
            mapper.writeValue(file.toFile(), p);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot save nav route registry to " + file, e);
        }
    }

    @Override
    public boolean loadIfExists() {
        if (!Files.exists(file)) {
            return false;
        }
        try {
            Payload p = mapper.readValue(file.toFile(), Payload.class);
            if (p != null && p.routes != null) {
                EnumMap<ExplorationMode, NavRoute> routes = new EnumMap<>(ExplorationMode.class);
                routes.putAll(p.routes);
                NavRouteRegistry.getInstance().applyFullPersistedSnapshot(routes);
            }
            return true;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load nav route registry from " + file, e);
        }
    }

    @Override
    public void deleteIfExists() {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot delete nav route registry file " + file, e);
        }
    }

    public static class Payload {
        public HashMap<ExplorationMode, NavRoute> routes;
    }
}
