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
 * Point d'entrée : la liste des {@link RegistryStore} (un fichier JSON chacun). Les DTOs
 * racine sont en général des classes internes {@code PersistenceFile} sur chaque
 * {@code *Registry} (y compris {@link CommodityRegistry.PersistenceFile},
 * {@link CarrierStatus.PersistenceFile}, {@link CommanderStatus.PersistenceFile} ;
 * vaisseau : {@link CommanderShip.PersistenceFile} dans {@code commander-ship.json}).
 */
public final class DashboardRegistryJsonPersistence {

    private static final ObjectMapper JSON_SIMPLE = PolymorphicPersistenceMapper.createSimple();
    private static final ObjectMapper JSON_POLYMORPHIC = PolymorphicPersistenceMapper.create();

    private DashboardRegistryJsonPersistence() {}

    public static List<RegistryStore> buildRegistryStores(Path baseDir) {
        List<RegistryStore> out = new ArrayList<>();

        // Chargé en premier : les autres JSON (vaisseau, porte-vaisseau, etc.) résolvent des ICommodity via le catalogue.
        out.add(storeClass("commodity-registry", baseDir, false, CommodityRegistry.PersistenceFile.class,
                () -> CommodityRegistry.PersistenceFile.fromRuntime(CommodityRegistry.getInstance()),
                CommodityRegistry.PersistenceFile::restore));
        out.add(storeClass("carrier-status", baseDir, false, CarrierStatus.PersistenceFile.class,
                () -> CarrierStatus.PersistenceFile.fromRuntime(CarrierStatus.getInstance()),
                CarrierStatus.PersistenceFile::restore));
        out.add(storeClass("commander-status", baseDir, false, CommanderStatus.PersistenceFile.class,
                () -> CommanderStatus.PersistenceFile.fromRuntime(CommanderStatus.getInstance()),
                CommanderStatus.PersistenceFile::restore));
        out.add(storeClass("commander-ship", baseDir, false, CommanderShip.PersistenceFile.class,
                () -> CommanderShip.PersistenceFile.fromRuntime(CommanderShip.getInstance()),
                CommanderShip.PersistenceFile::restore));

        out.add(storeClass("exploration-mode", baseDir, false, ExplorationModeRegistry.PersistenceFile.class,
                () -> ExplorationModeRegistry.PersistenceFile.fromRuntime(ExplorationModeRegistry.getInstance()),
                ExplorationModeRegistry.PersistenceFile::restore));
        out.add(storeClass("nav-route-target", baseDir, false, NavRouteTargetRegistry.PersistenceFile.class,
                () -> NavRouteTargetRegistry.PersistenceFile.fromRuntime(NavRouteTargetRegistry.getInstance()),
                NavRouteTargetRegistry.PersistenceFile::restore));

        out.add(storeClass("ship-targets", baseDir, false, ShipTargetRegistry.PersistenceFile.class,
                () -> ShipTargetRegistry.PersistenceFile.fromRuntime(ShipTargetRegistry.getInstance()),
                ShipTargetRegistry.PersistenceFile::restore));
        out.add(storeMap("missions", baseDir, false, LinkedHashMap.class, String.class, Mission.class,
                () -> new LinkedHashMap<>(MissionsRegistry.getInstance().getGlobalMissionMap()),
                MissionsRegistry.getInstance()::applyFullPersistedSnapshot));

        out.add(storeClass("destroyed-ships", baseDir, false, DestroyedShipsRegistery.PersistenceFile.class,
                () -> DestroyedShipsRegistery.PersistenceFile.fromRuntime(DestroyedShipsRegistery.getInstance()),
                DestroyedShipsRegistery.PersistenceFile::restore));

        out.add(storeClass("colonisation-registry", baseDir, true, ColonisationRegistry.PersistenceFile.class,
                () -> ColonisationRegistry.PersistenceFile.fromRuntime(ColonisationRegistry.getInstance()),
                ColonisationRegistry.PersistenceFile::restore));
        out.add(storeClass("planete-registry", baseDir, true, PlaneteRegistry.PersistenceFile.class,
                () -> PlaneteRegistry.PersistenceFile.fromRuntime(PlaneteRegistry.getInstance()),
                PlaneteRegistry.PersistenceFile::restore));
        out.add(storeMap("system-visited-registry", baseDir, true, LinkedHashMap.class, String.class, SystemVisited.class,
                () -> new LinkedHashMap<>(SystemVisitedRegistry.getInstance().snapshotSystems()),
                SystemVisitedRegistry.getInstance()::applyFullPersistedSnapshot));
        out.add(storeMap("nav-route-registry", baseDir, true, HashMap.class, ExplorationMode.class, NavRoute.class,
                () -> new HashMap<>(NavRouteRegistry.getInstance().snapshotRoutes()),
                NavRouteRegistry.getInstance()::applyFullPersistedSnapshot));
        out.add(storeClass("exploration-data-sale-registry", baseDir, true, ExplorationDataSaleRegistry.PersistenceFile.class,
                () -> ExplorationDataSaleRegistry.PersistenceFile.fromRuntime(ExplorationDataSaleRegistry.getInstance()),
                ExplorationDataSaleRegistry.PersistenceFile::restore));
        out.add(storeClass("organic-data-sale-registry", baseDir, true, OrganicDataSaleRegistry.PersistenceFile.class,
                () -> OrganicDataSaleRegistry.PersistenceFile.fromRuntime(OrganicDataSaleRegistry.getInstance()),
                OrganicDataSaleRegistry.PersistenceFile::restore));
        out.add(storeClass("mining-stat-registry", baseDir, true, MiningStatRegistry.PersistenceFile.class,
                () -> MiningStatRegistry.PersistenceFile.fromRuntime(MiningStatRegistry.getInstance()),
                MiningStatRegistry.PersistenceFile::restore));

        return out;
    }

    private static <T> RegistryStore storeClass(
            String name, Path baseDir, boolean polymorphic, Class<T> type,
            Supplier<T> snapshot, Consumer<T> restore) {
        return new SnapshotJsonStore<>(
                name, jsonFile(baseDir, name), mapper(polymorphic), type, snapshot, restore);
    }

    @SuppressWarnings("rawtypes")
    private static <K, V> RegistryStore storeMap(
            String name, Path baseDir, boolean polymorphic, Class<? extends Map> mapClass,
            Class<K> keyClass, Class<V> valueClass,
            Supplier<Map<K, V>> snapshot, Consumer<Map<K, V>> restore) {
        JavaType t = TypeFactory.defaultInstance().constructMapType(mapClass, keyClass, valueClass);
        return new SnapshotJsonStore<>(
                name, jsonFile(baseDir, name), mapper(polymorphic), t, snapshot, restore);
    }

    private static Path jsonFile(Path baseDir, String storeName) {
        return baseDir.resolve(storeName + ".json");
    }

    private static ObjectMapper mapper(boolean polymorphic) {
        return polymorphic ? JSON_POLYMORPHIC : JSON_SIMPLE;
    }
}
