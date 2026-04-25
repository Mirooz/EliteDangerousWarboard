package be.mirooz.elitedangerous.dashboard.persistence;

import be.mirooz.elitedangerous.backend.spansh.ExplorationMode;
import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.MineralType;
import be.mirooz.elitedangerous.dashboard.model.colonisation.ColonisationArchitectSystem;
import be.mirooz.elitedangerous.dashboard.model.commander.Mission;
import be.mirooz.elitedangerous.dashboard.model.registries.commander.CommanderStatus;
import be.mirooz.elitedangerous.dashboard.model.events.ProspectedAsteroid;
import be.mirooz.elitedangerous.dashboard.model.exploration.ACelesteBody;
import be.mirooz.elitedangerous.dashboard.model.exploration.ExplorationDataOnHold;
import be.mirooz.elitedangerous.dashboard.model.exploration.ExplorationDataSale;
import be.mirooz.elitedangerous.dashboard.model.exploration.OrganicDataOnHold;
import be.mirooz.elitedangerous.dashboard.model.exploration.OrganicDataSale;
import be.mirooz.elitedangerous.dashboard.model.exploration.SystemVisited;
import be.mirooz.elitedangerous.dashboard.model.mining.MiningStat;
import be.mirooz.elitedangerous.dashboard.model.navigation.NavRoute;
import be.mirooz.elitedangerous.dashboard.model.registries.colonisation.ColonisationRegistry;
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
import be.mirooz.elitedangerous.dashboard.model.registries.mining.ProspectedAsteroidRegistry;
import be.mirooz.elitedangerous.dashboard.model.registries.navigation.NavRouteRegistry;
import be.mirooz.elitedangerous.dashboard.model.registries.navigation.NavRouteTargetRegistry;
import be.mirooz.elitedangerous.dashboard.model.ships.DestroyedBountyShip;
import be.mirooz.elitedangerous.dashboard.model.ships.DestroyedConflictShip;
import be.mirooz.elitedangerous.dashboard.model.ships.DestroyedShip;
import be.mirooz.elitedangerous.dashboard.model.ships.Reward;
import be.mirooz.elitedangerous.dashboard.model.ships.ShipTarget;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Déclaration centralisée : un fichier JSON / entrée = une ligne
 * de {@link SnapshotJsonStore} + DTOs de forme disque (plus de classes {@code *Store}).
 */
public final class DashboardRegistryJsonPersistence {

    private static final ObjectMapper JSON_SIMPLE = PolymorphicPersistenceMapper.createSimple();
    private static final ObjectMapper JSON_POLYMORPHIC = PolymorphicPersistenceMapper.create();

    private static final TypeReference<LinkedHashMap<String, Mission>> MISSIONS =
            new TypeReference<>() {};
    private static final TypeReference<LinkedHashMap<String, ShipTarget>> SHIP_TARGETS =
            new TypeReference<>() {};
    private static final TypeReference<LinkedHashMap<String, SystemVisited>> SYSTEMS_VISITED =
            new TypeReference<>() {};
    private static final TypeReference<List<ProspectedAsteroidFile>> PROSPECTED =
            new TypeReference<>() {};

    private DashboardRegistryJsonPersistence() {}

    public static List<RegistryStore> buildRegistryStores(Path baseDir) {
        List<RegistryStore> out = new ArrayList<>();

        out.add(new SnapshotJsonStore<>(
                "carrier-status", baseDir.resolve("carrier-status.json"), JSON_SIMPLE,
                CarrierStatusSnapshot.class,
                () -> CarrierStatusSnapshot.fromRuntime(CarrierStatus.getInstance()),
                CarrierStatusSnapshot::restore));

        out.add(new SnapshotJsonStore<>(
                "commander-status", baseDir.resolve("commander-status.json"), JSON_SIMPLE,
                CommanderStatusSnapshot.class,
                () -> CommanderStatusSnapshot.fromRuntime(CommanderStatus.getInstance()),
                CommanderStatusSnapshot::restore));

        out.add(new SnapshotJsonStore<>(
                "exploration-mode", baseDir.resolve("exploration-mode.json"), JSON_SIMPLE,
                ExplorationModeFile.class,
                () -> new ExplorationModeFile(ExplorationModeRegistry.getInstance().getCurrentMode()),
                f -> {
                    if (f != null && f.mode != null) {
                        ExplorationModeRegistry.getInstance().setCurrentMode(f.mode);
                    }
                }));

        out.add(new SnapshotJsonStore<>(
                "nav-route-target", baseDir.resolve("nav-route-target.json"), JSON_SIMPLE,
                NavRouteTargetFile.class,
                () -> new NavRouteTargetFile(NavRouteTargetRegistry.getInstance()
                        .getRemainingJumpsInRoute()),
                f -> {
                    if (f != null) {
                        NavRouteTargetRegistry.getInstance().setRemainingJumpsInRoute(
                                f.remainingJumpsInRoute);
                    }
                }));

        out.add(new SnapshotJsonStore<>(
                "ship-targets", baseDir.resolve("ship-targets.json"), JSON_SIMPLE,
                SHIP_TARGETS,
                () -> new LinkedHashMap<>(ShipTargetRegistry.getInstance().getAll()),
                ShipTargetRegistry.getInstance()::applyFullPersistedSnapshot));

        out.add(new SnapshotJsonStore<>(
                "prospected-asteroids", baseDir.resolve("prospected-asteroids.json"), JSON_SIMPLE,
                PROSPECTED,
                DashboardRegistryJsonPersistence::prospectedBuild,
                DashboardRegistryJsonPersistence::prospectedRestore));

        out.add(new SnapshotJsonStore<>(
                "missions", baseDir.resolve("missions.json"), JSON_SIMPLE,
                MISSIONS,
                () -> new LinkedHashMap<>(
                        MissionsRegistry.getInstance().getGlobalMissionMap()),
                MissionsRegistry.getInstance()::applyFullPersistedSnapshot));

        out.add(new SnapshotJsonStore<>(
                "destroyed-ships", baseDir.resolve("destroyed-ships.json"), JSON_SIMPLE,
                DestroyedShipsFile.class,
                DestroyedShipsFile::fromRuntime,
                DestroyedShipsFile::apply));

        out.add(new SnapshotJsonStore<>(
                "colonisation-registry", baseDir.resolve("colonisation-registry.json"), JSON_POLYMORPHIC,
                ColonisationRegistryFile.class,
                DashboardRegistryJsonPersistence::colonisationBuild,
                DashboardRegistryJsonPersistence::colonisationRestore));

        out.add(new SnapshotJsonStore<>(
                "planete-registry", baseDir.resolve("planete-registry.json"), JSON_POLYMORPHIC,
                PlaneteRegistryFile.class,
                DashboardRegistryJsonPersistence::planeteBuild,
                DashboardRegistryJsonPersistence::planeteRestore));

        out.add(new SnapshotJsonStore<>(
                "system-visited-registry", baseDir.resolve("system-visited-registry.json"), JSON_POLYMORPHIC,
                SYSTEMS_VISITED,
                () -> new LinkedHashMap<>(
                        SystemVisitedRegistry.getInstance().snapshotSystems()),
                SystemVisitedRegistry.getInstance()::applyFullPersistedSnapshot));

        out.add(new SnapshotJsonStore<>(
                "nav-route-registry", baseDir.resolve("nav-route-registry.json"), JSON_POLYMORPHIC,
                NavRouteRegistryFile.class,
                DashboardRegistryJsonPersistence::navRouteBuild,
                DashboardRegistryJsonPersistence::navRouteRestore));

        out.add(new SnapshotJsonStore<>(
                "exploration-data-sale-registry",
                baseDir.resolve("exploration-data-sale-registry.json"), JSON_POLYMORPHIC,
                ExplorationDataSaleRegistryFile.class,
                DashboardRegistryJsonPersistence::explorationDataSaleBuild,
                DashboardRegistryJsonPersistence::explorationDataSaleRestore));

        out.add(new SnapshotJsonStore<>(
                "organic-data-sale-registry",
                baseDir.resolve("organic-data-sale-registry.json"), JSON_POLYMORPHIC,
                OrganicDataSaleRegistryFile.class,
                DashboardRegistryJsonPersistence::organicDataSaleBuild,
                DashboardRegistryJsonPersistence::organicDataSaleRestore));

        out.add(new SnapshotJsonStore<>(
                "mining-stat-registry", baseDir.resolve("mining-stat-registry.json"), JSON_POLYMORPHIC,
                MiningStatRegistryFile.class,
                DashboardRegistryJsonPersistence::miningStatBuild,
                DashboardRegistryJsonPersistence::miningStatRestore));

        return out;
    }

    // -------- colonisation / planete / nav routes / sales / mining --------

    private static ColonisationRegistryFile colonisationBuild() {
        ColonisationRegistry reg = ColonisationRegistry.getInstance();
        ColonisationRegistryFile f = new ColonisationRegistryFile();
        f.architectByStarSystem = reg.snapshotArchitectByStarSystem();
        f.beaconDeployedSystems = reg.snapshotBeaconDeployedSystems();
        f.currentConstructionMarketId = reg.getCurrentConstructionMarketId();
        return f;
    }

    private static void colonisationRestore(ColonisationRegistryFile p) {
        ColonisationRegistry.getInstance().applyFullPersistedSnapshot(
                p.architectByStarSystem, p.beaconDeployedSystems, p.currentConstructionMarketId);
    }

    private static PlaneteRegistryFile planeteBuild() {
        PlaneteRegistry reg = PlaneteRegistry.getInstance();
        PlaneteRegistryFile f = new PlaneteRegistryFile();
        f.planetesMap = new LinkedHashMap<>(reg.snapshotPlanetesMap());
        f.currentStarSystem = reg.getCurrentStarSystem();
        return f;
    }

    private static void planeteRestore(PlaneteRegistryFile p) {
        PlaneteRegistry.getInstance().applyFullPersistedSnapshot(p.planetesMap, p.currentStarSystem);
    }

    private static NavRouteRegistryFile navRouteBuild() {
        NavRouteRegistryFile f = new NavRouteRegistryFile();
        f.routes = new HashMap<>();
        f.routes.putAll(NavRouteRegistry.getInstance().snapshotRoutes());
        return f;
    }

    private static void navRouteRestore(NavRouteRegistryFile p) {
        if (p != null && p.routes != null) {
            EnumMap<ExplorationMode, NavRoute> routes = new EnumMap<>(ExplorationMode.class);
            routes.putAll(p.routes);
            NavRouteRegistry.getInstance().applyFullPersistedSnapshot(routes);
        }
    }

    private static ExplorationDataSaleRegistryFile explorationDataSaleBuild() {
        ExplorationDataSaleRegistry reg = ExplorationDataSaleRegistry.getInstance();
        ExplorationDataSaleRegistryFile f = new ExplorationDataSaleRegistryFile();
        f.sales = new ArrayList<>(reg.snapshotSales());
        f.currentSale = reg.getCurrentSale();
        f.explorationDataOnHold = reg.getExplorationDataOnHold();
        return f;
    }

    private static void explorationDataSaleRestore(ExplorationDataSaleRegistryFile p) {
        ExplorationDataSaleRegistry.getInstance().applyFullPersistedSnapshot(
                p.sales, p.currentSale, p.explorationDataOnHold);
    }

    private static OrganicDataSaleRegistryFile organicDataSaleBuild() {
        OrganicDataSaleRegistry reg = OrganicDataSaleRegistry.getInstance();
        OrganicDataSaleRegistryFile f = new OrganicDataSaleRegistryFile();
        f.sales = new ArrayList<>(reg.snapshotSales());
        f.currentOrganicDataOnHold = reg.getCurrentOrganicDataOnHold();
        return f;
    }

    private static void organicDataSaleRestore(OrganicDataSaleRegistryFile p) {
        OrganicDataSaleRegistry.getInstance().applyFullPersistedSnapshot(
                p.sales, p.currentOrganicDataOnHold);
    }

    private static MiningStatRegistryFile miningStatBuild() {
        MiningStatRegistry reg = MiningStatRegistry.getInstance();
        MiningStatRegistryFile f = new MiningStatRegistryFile();
        f.miningStats = new ArrayList<>(reg.snapshotMiningStats());
        f.currentMiningSession = reg.snapshotCurrentMiningSession();
        return f;
    }

    private static void miningStatRestore(MiningStatRegistryFile p) {
        MiningStatRegistry.getInstance().applyFullPersistedSnapshot(
                p.miningStats, p.currentMiningSession);
    }

    // -------- prospected asteroids --------

    private static List<ProspectedAsteroidFile> prospectedBuild() {
        Deque<ProspectedAsteroid> all = ProspectedAsteroidRegistry.getInstance().getAll();
        List<ProspectedAsteroidFile> list = new ArrayList<>(all.size());
        for (ProspectedAsteroid a : all) {
            list.add(ProspectedAsteroidFile.fromRuntime(a));
        }
        return list;
    }

    private static void prospectedRestore(List<ProspectedAsteroidFile> snapshots) {
        List<ProspectedAsteroid> restored = new ArrayList<>();
        if (snapshots != null) {
            for (ProspectedAsteroidFile s : snapshots) {
                ProspectedAsteroid p = s.toRuntime();
                if (p != null) {
                    restored.add(p);
                }
            }
        }
        ProspectedAsteroidRegistry.getInstance().applyFullPersistedSnapshot(restored);
    }

    // -------- fichiers DTO (racine JSON) --------

    public static final class ColonisationRegistryFile {
        public LinkedHashMap<String, ColonisationArchitectSystem> architectByStarSystem;
        public LinkedHashSet<String> beaconDeployedSystems;
        public Long currentConstructionMarketId;
    }

    public static final class PlaneteRegistryFile {
        public LinkedHashMap<Integer, ACelesteBody> planetesMap;
        public String currentStarSystem;
    }

    public static final class NavRouteRegistryFile {
        public HashMap<ExplorationMode, NavRoute> routes;
    }

    public static final class ExplorationDataSaleRegistryFile {
        public List<ExplorationDataSale> sales;
        public ExplorationDataSale currentSale;
        public ExplorationDataOnHold explorationDataOnHold;
    }

    public static final class OrganicDataSaleRegistryFile {
        public List<OrganicDataSale> sales;
        public OrganicDataOnHold currentOrganicDataOnHold;
    }

    public static final class MiningStatRegistryFile {
        public List<MiningStat> miningStats;
        public MiningStat currentMiningSession;
    }

    static final class ExplorationModeFile {
        @JsonProperty
        public ExplorationMode mode;

        @JsonCreator
        ExplorationModeFile() {}

        ExplorationModeFile(ExplorationMode mode) {
            this.mode = mode;
        }
    }

    static final class NavRouteTargetFile {
        @JsonProperty
        public int remainingJumpsInRoute;

        @JsonCreator
        NavRouteTargetFile() {}

        NavRouteTargetFile(int value) {
            this.remainingJumpsInRoute = value;
        }
    }

    public static final class DestroyedShipsFile {
        public List<DestroyedShipEntry> ships = new ArrayList<>();
        public Map<String, Integer> bountyPerFaction = new HashMap<>();
        public Map<String, Integer> combatBondPerFaction = new HashMap<>();
        public int totalBountyEarned;
        public int totalConflictBounty;

        @JsonCreator
        public DestroyedShipsFile() {}

        static DestroyedShipsFile fromRuntime() {
            DestroyedShipsRegistery reg = DestroyedShipsRegistery.getInstance();
            DestroyedShipsFile p = new DestroyedShipsFile();
            for (DestroyedShip s : reg.getDestroyedShips()) {
                p.ships.add(DestroyedShipEntry.fromRuntime(s));
            }
            p.bountyPerFaction.putAll(reg.getBountyPerFaction());
            p.combatBondPerFaction.putAll(reg.getCombatBondPerFaction());
            p.totalBountyEarned = reg.getTotalBountyEarned();
            p.totalConflictBounty = reg.getTotalConflictBounty();
            return p;
        }

        void apply() {
            List<DestroyedShip> restored = new ArrayList<>();
            if (ships != null) {
                for (DestroyedShipEntry e : ships) {
                    DestroyedShip ship = e.toRuntime();
                    if (ship != null) {
                        restored.add(ship);
                    }
                }
            }
            DestroyedShipsRegistery.getInstance().applyFullPersistedSnapshot(
                    restored,
                    bountyPerFaction,
                    combatBondPerFaction,
                    totalBountyEarned,
                    totalConflictBounty
            );
        }
    }

    public static final class DestroyedShipEntry {
        public String type; // "BOUNTY" ou "CONFLICT"
        public String shipName;
        public String pilotName;
        public String faction;
        public String bountyFaction;
        public List<Reward> rewards;
        public int totalBountyReward;
        public LocalDateTime destroyedTime;

        @JsonCreator
        public DestroyedShipEntry() {}

        static DestroyedShipEntry fromRuntime(DestroyedShip s) {
            DestroyedShipEntry e = new DestroyedShipEntry();
            if (s instanceof DestroyedBountyShip) {
                e.type = "BOUNTY";
            } else if (s instanceof DestroyedConflictShip) {
                e.type = "CONFLICT";
            } else {
                e.type = "UNKNOWN";
            }
            e.shipName = s.getShipName();
            e.pilotName = s.getPilotName();
            e.faction = s.getFaction();
            e.bountyFaction = s.getBountyFaction();
            e.rewards = s.getRewards();
            e.totalBountyReward = s.getTotalBountyReward();
            e.destroyedTime = s.getDestroyedTime();
            return e;
        }

        DestroyedShip toRuntime() {
            if ("BOUNTY".equals(type)) {
                return DestroyedBountyShip.builder()
                        .shipName(shipName).pilotName(pilotName).faction(faction)
                        .bountyFaction(bountyFaction).rewards(rewards)
                        .totalBountyReward(totalBountyReward).destroyedTime(destroyedTime)
                        .build();
            }
            if ("CONFLICT".equals(type)) {
                return DestroyedConflictShip.builder()
                        .shipName(shipName).pilotName(pilotName).faction(faction)
                        .bountyFaction(bountyFaction).rewards(rewards)
                        .totalBountyReward(totalBountyReward).destroyedTime(destroyedTime)
                        .build();
            }
            return null;
        }
    }

    public static final class ProspectedAsteroidFile {
        public String timestamp;
        public String event;
        public String motherlodeMaterial;
        public String coreMineralName;
        public String content;
        public String contentLocalised;
        public Double remaining;
        public boolean cracked;
        public List<MaterialRow> materials;

        @JsonCreator
        public ProspectedAsteroidFile() {}

        static ProspectedAsteroidFile fromRuntime(ProspectedAsteroid a) {
            ProspectedAsteroidFile s = new ProspectedAsteroidFile();
            s.timestamp = a.getTimestamp();
            s.event = a.getEvent();
            s.motherlodeMaterial = a.getMotherlodeMaterial();
            s.coreMineralName = a.getCoreMineral() != null ? a.getCoreMineral().name() : null;
            s.content = a.getContent();
            s.contentLocalised = a.getContentLocalised();
            s.remaining = a.getRemaining();
            s.cracked = a.isCracked();
            s.materials = new ArrayList<>();
            if (a.getMaterials() != null) {
                for (ProspectedAsteroid.Material m : a.getMaterials()) {
                    s.materials.add(MaterialRow.fromRuntime(m));
                }
            }
            return s;
        }

        ProspectedAsteroid toRuntime() {
            ProspectedAsteroid p = new ProspectedAsteroid();
            p.setTimestamp(timestamp);
            p.setEvent(event);
            p.setMotherlodeMaterial(motherlodeMaterial);
            if (coreMineralName != null) {
                try {
                    p.setCoreMineral(MineralType.valueOf(coreMineralName));
                } catch (IllegalArgumentException ignored) {
                    // Nouvel enum / renommé → on ignore silencieusement
                }
            }
            p.setContent(content);
            p.setContentLocalised(contentLocalised);
            p.setRemaining(remaining);
            p.setCracked(cracked);
            List<ProspectedAsteroid.Material> mats = new ArrayList<>();
            if (materials != null) {
                for (MaterialRow ms : materials) {
                    ProspectedAsteroid.Material m = ms.toRuntime();
                    if (m != null) {
                        mats.add(m);
                    }
                }
            }
            p.setMaterials(mats);
            return p;
        }
    }

    public static final class MaterialRow {
        @JsonProperty("name")
        public String mineralName;
        public String nameLocalised;
        public Double proportion;

        @JsonCreator
        public MaterialRow() {}

        static MaterialRow fromRuntime(ProspectedAsteroid.Material m) {
            MaterialRow s = new MaterialRow();
            s.mineralName = m.getName() instanceof MineralType mt ? mt.name() : null;
            s.nameLocalised = m.getNameLocalised();
            s.proportion = m.getProportion();
            return s;
        }

        ProspectedAsteroid.Material toRuntime() {
            ProspectedAsteroid.Material m = new ProspectedAsteroid.Material();
            if (mineralName != null) {
                try {
                    m.setName(MineralType.valueOf(mineralName));
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
            m.setNameLocalised(nameLocalised);
            m.setProportion(proportion);
            return m;
        }
    }
}
