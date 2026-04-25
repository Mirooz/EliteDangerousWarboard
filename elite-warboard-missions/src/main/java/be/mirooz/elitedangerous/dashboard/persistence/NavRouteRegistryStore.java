package be.mirooz.elitedangerous.dashboard.persistence;

import be.mirooz.elitedangerous.backend.spansh.ExplorationMode;
import be.mirooz.elitedangerous.dashboard.model.navigation.NavRoute;
import be.mirooz.elitedangerous.dashboard.model.registries.navigation.NavRouteRegistry;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;

public class NavRouteRegistryStore extends SnapshotJsonStore<NavRouteRegistryStore.Payload> {

    public NavRouteRegistryStore(Path file) {
        super(
                "nav-route-registry",
                file,
                PolymorphicPersistenceMapper.create(),
                Payload.class,
                NavRouteRegistryStore::buildSnapshot,
                NavRouteRegistryStore::restoreSnapshot
        );
    }

    private static Payload buildSnapshot() {
        Payload p = new Payload();
        p.routes = new HashMap<>();
        p.routes.putAll(NavRouteRegistry.getInstance().snapshotRoutes());
        return p;
    }

    private static void restoreSnapshot(Payload p) {
        if (p != null && p.routes != null) {
            EnumMap<ExplorationMode, NavRoute> routes = new EnumMap<>(ExplorationMode.class);
            routes.putAll(p.routes);
            NavRouteRegistry.getInstance().applyFullPersistedSnapshot(routes);
        }
    }

    public static class Payload {
        public HashMap<ExplorationMode, NavRoute> routes;
    }
}
