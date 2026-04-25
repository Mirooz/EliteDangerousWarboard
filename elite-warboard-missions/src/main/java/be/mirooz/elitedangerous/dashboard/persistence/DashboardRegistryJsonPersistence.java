package be.mirooz.elitedangerous.dashboard.persistence;

import be.mirooz.elitedangerous.backend.spansh.ExplorationMode;
import be.mirooz.elitedangerous.dashboard.model.commander.Mission;
import be.mirooz.elitedangerous.dashboard.model.exploration.SystemVisited;
import be.mirooz.elitedangerous.dashboard.model.navigation.NavRoute;
import be.mirooz.elitedangerous.dashboard.model.registries.commander.CommanderShip;
import be.mirooz.elitedangerous.dashboard.model.registries.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.model.registries.colonisation.ColonisationRegistry;
import be.mirooz.elitedangerous.dashboard.model.registries.CommodityRegistry;
import be.mirooz.elitedangerous.dashboard.model.registries.combat.DestroyedShipsRegistery;
import be.mirooz.elitedangerous.dashboard.model.registries.combat.MissionsRegistry;
import be.mirooz.elitedangerous.dashboard.model.registries.combat.ShipTargetRegistry;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.ExplorationDataSaleRegistry;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.ExplorationModeRegistry;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.OrganicDataSaleRegistry;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.PlaneteRegistry;
import be.mirooz.elitedangerous.dashboard.model.registries.exploration.SystemVisitedRegistry;
import be.mirooz.elitedangerous.dashboard.model.registries.fleetcarrier.CarrierStatus;
import be.mirooz.elitedangerous.dashboard.model.registries.mining.MiningStatRegistry;
import be.mirooz.elitedangerous.dashboard.model.registries.navigation.NavRouteRegistry;
import be.mirooz.elitedangerous.dashboard.model.registries.navigation.NavRouteTargetRegistry;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Point d'entrée : la liste des {@link RegistryStore} (un fichier JSON chacun).
 * Les stores persistent directement le modèle runtime singleton quand c'est possible.
 */
public final class DashboardRegistryJsonPersistence {

    private static final ObjectMapper JSON = PolymorphicPersistenceMapper.create();

    private DashboardRegistryJsonPersistence() {}

    public static List<RegistryStore> buildRegistryStores(Path baseDir) {
        List<RegistryStore> out = new ArrayList<>();

        // Chargé en premier : les autres JSON (vaisseau, porte-vaisseau, etc.) résolvent des ICommodity via le catalogue.
        out.add(storeClass("commodity-registry", baseDir, CommodityRegistry.class,
                CommodityRegistry::getInstance,
                loaded -> mergeIntoSingleton("commodity-registry", CommodityRegistry.getInstance(), loaded)));
        out.add(storeClass("carrier-status", baseDir, CarrierStatus.class,
                CarrierStatus::getInstance,
                loaded -> mergeIntoSingleton("carrier-status", CarrierStatus.getInstance(), loaded)));
        // Données runtime en singleton : Jackson désérialise un objet temporaire, puis updateValue() fusionne sur getInstance().
        out.add(storeClass("commander-status", baseDir, CommanderStatus.class,
                CommanderStatus::getInstance,
                loaded -> {
                    try {
                        JSON.updateValue(CommanderStatus.getInstance(), loaded);
                    } catch (Exception e) {
                        throw new IllegalStateException("Cannot merge commander-status snapshot", e);
                    }
                }));
        out.add(storeClass("commander-ship", baseDir, CommanderShip.class,
                CommanderShip::getInstance,
                loaded -> {
                    try {
                        JSON.updateValue(CommanderShip.getInstance(), loaded);
                    } catch (Exception e) {
                        throw new IllegalStateException("Cannot merge commander-ship snapshot", e);
                    }
                }));

        out.add(storeClass("exploration-mode", baseDir, ExplorationModeRegistry.class,
                ExplorationModeRegistry::getInstance,
                loaded -> mergeIntoSingleton("exploration-mode", ExplorationModeRegistry.getInstance(), loaded)));
        out.add(storeClass("nav-route-target", baseDir, NavRouteTargetRegistry.class,
                NavRouteTargetRegistry::getInstance,
                loaded -> mergeIntoSingleton("nav-route-target", NavRouteTargetRegistry.getInstance(), loaded)));

        out.add(storeClass("ship-targets", baseDir, ShipTargetRegistry.class,
                ShipTargetRegistry::getInstance,
                loaded -> mergeIntoSingleton("ship-targets", ShipTargetRegistry.getInstance(), loaded)));
        out.add(storeMap("missions", baseDir, LinkedHashMap.class, String.class, Mission.class,
                () -> new LinkedHashMap<>(MissionsRegistry.getInstance().getGlobalMissionMap()),
                MissionsRegistry.getInstance()::applyFullPersistedSnapshot));

        out.add(storeClass("destroyed-ships", baseDir, DestroyedShipsRegistery.class,
                DestroyedShipsRegistery::getInstance,
                loaded -> mergeIntoSingleton("destroyed-ships", DestroyedShipsRegistery.getInstance(), loaded)));

        out.add(storeClass("colonisation-registry", baseDir, ColonisationRegistry.class,
                ColonisationRegistry::getInstance,
                loaded -> mergeIntoSingleton("colonisation-registry", ColonisationRegistry.getInstance(), loaded)));
        out.add(storeClass("planete-registry", baseDir, PlaneteRegistry.class,
                PlaneteRegistry::getInstance,
                loaded -> mergeIntoSingleton("planete-registry", PlaneteRegistry.getInstance(), loaded)));
        out.add(storeMap("system-visited-registry", baseDir, LinkedHashMap.class, String.class, SystemVisited.class,
                () -> new LinkedHashMap<>(SystemVisitedRegistry.getInstance().snapshotSystems()),
                SystemVisitedRegistry.getInstance()::applyFullPersistedSnapshot));
        out.add(storeMap("nav-route-registry", baseDir, HashMap.class, ExplorationMode.class, NavRoute.class,
                () -> new HashMap<>(NavRouteRegistry.getInstance().snapshotRoutes()),
                NavRouteRegistry.getInstance()::applyFullPersistedSnapshot));
        out.add(storeClass("exploration-data-sale-registry", baseDir, ExplorationDataSaleRegistry.class,
                ExplorationDataSaleRegistry::getInstance,
                loaded -> mergeIntoSingleton("exploration-data-sale-registry", ExplorationDataSaleRegistry.getInstance(), loaded)));
        out.add(storeClass("organic-data-sale-registry", baseDir, OrganicDataSaleRegistry.class,
                OrganicDataSaleRegistry::getInstance,
                loaded -> mergeIntoSingleton("organic-data-sale-registry", OrganicDataSaleRegistry.getInstance(), loaded)));
        out.add(storeClass("mining-stat-registry", baseDir, MiningStatRegistry.class,
                MiningStatRegistry::getInstance,
                loaded -> mergeIntoSingleton("mining-stat-registry", MiningStatRegistry.getInstance(), loaded)));

        return out;
    }

    private static <T> RegistryStore storeClass(
            String name, Path baseDir, Class<T> type,
            Supplier<T> snapshot, Consumer<T> restore) {
        return new SnapshotJsonStore<>(
                name, jsonFile(baseDir, name), mapper(), type, snapshot, restore);
    }

    @SuppressWarnings("rawtypes")
    private static <K, V> RegistryStore storeMap(
            String name, Path baseDir, Class<? extends Map> mapClass,
            Class<K> keyClass, Class<V> valueClass,
            Supplier<Map<K, V>> snapshot, Consumer<Map<K, V>> restore) {
        JavaType t = TypeFactory.defaultInstance().constructMapType(mapClass, keyClass, valueClass);
        return new SnapshotJsonStore<>(
                name, jsonFile(baseDir, name), mapper(), t, snapshot, restore);
    }

    private static Path jsonFile(Path baseDir, String storeName) {
        return baseDir.resolve(storeName + ".json");
    }

    private static ObjectMapper mapper() {
        return JSON;
    }

    private static <T> void mergeIntoSingleton(String storeName, T singleton, T loaded) {
        try {
            JSON.updateValue(singleton, loaded);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot merge " + storeName + " snapshot", e);
        }
    }
}
