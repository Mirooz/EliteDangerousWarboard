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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Déclaration centralisée : un fichier JSON / entrée = un {@link SnapshotJsonStore} +
 * DTOs (classes internes). La liste est construite par {@link #buildRegistryStores(Path)}.
 */
public final class DashboardRegistryJsonPersistence {

    private static final ObjectMapper JSON_SIMPLE = PolymorphicPersistenceMapper.createSimple();
    private static final ObjectMapper JSON_POLYMORPHIC = PolymorphicPersistenceMapper.create();

    private static final TypeReference<LinkedHashMap<String, Mission>> MISSIONS = new TypeReference<>() {};
    private static final TypeReference<LinkedHashMap<String, ShipTarget>> SHIP_TARGETS = new TypeReference<>() {};
    private static final TypeReference<LinkedHashMap<String, SystemVisited>> SYSTEMS_VISITED = new TypeReference<>() {};
    private static final TypeReference<List<ProspectedAsteroidFile>> PROSPECTED = new TypeReference<>() {};
    /** Racine JSON = map (comme missions / ship-targets), pas d'enveloppe {@code { "routes": ... }}. */
    private static final TypeReference<HashMap<ExplorationMode, NavRoute>> NAV_ROUTES = new TypeReference<>() {};

    private DashboardRegistryJsonPersistence() {}

    public static List<RegistryStore> buildRegistryStores(Path baseDir) {
        List<RegistryStore> out = new ArrayList<>();

        out.add(storeClass("carrier-status", baseDir, false, CarrierStatusSnapshot.class,
                () -> CarrierStatusSnapshot.fromRuntime(CarrierStatus.getInstance()),
                CarrierStatusSnapshot::restore));
        out.add(storeClass("commander-status", baseDir, false, CommanderStatusSnapshot.class,
                () -> CommanderStatusSnapshot.fromRuntime(CommanderStatus.getInstance()),
                CommanderStatusSnapshot::restore));

        out.add(storeClass("exploration-mode", baseDir, false, ExplorationModeFile.class,
                ExplorationModeFile::fromRegistry, ExplorationModeFile::applyToRegistry));
        out.add(storeClass("nav-route-target", baseDir, false, NavRouteTargetFile.class,
                NavRouteTargetFile::fromRegistry, NavRouteTargetFile::applyToRegistry));

        out.add(storeRef("ship-targets", baseDir, false, SHIP_TARGETS,
                () -> new LinkedHashMap<>(ShipTargetRegistry.getInstance().getAll()),
                ShipTargetRegistry.getInstance()::applyFullPersistedSnapshot));
        out.add(storeRef("prospected-asteroids", baseDir, false, PROSPECTED,
                ProspectedAsteroidFile::listFromRegistry, ProspectedAsteroidFile::applyListToRegistry));
        out.add(storeRef("missions", baseDir, false, MISSIONS,
                () -> new LinkedHashMap<>(MissionsRegistry.getInstance().getGlobalMissionMap()),
                MissionsRegistry.getInstance()::applyFullPersistedSnapshot));

        out.add(storeClass("destroyed-ships", baseDir, false, DestroyedShipsFile.class,
                DestroyedShipsFile::fromRuntime, DestroyedShipsFile::apply));

        out.add(storeClass("colonisation-registry", baseDir, true, ColonisationRegistryFile.class,
                ColonisationRegistryFile::fromRegistry, ColonisationRegistryFile::applyToRegistry));
        out.add(storeClass("planete-registry", baseDir, true, PlaneteRegistryFile.class,
                PlaneteRegistryFile::fromRegistry, PlaneteRegistryFile::applyToRegistry));
        out.add(storeRef("system-visited-registry", baseDir, true, SYSTEMS_VISITED,
                () -> new LinkedHashMap<>(SystemVisitedRegistry.getInstance().snapshotSystems()),
                SystemVisitedRegistry.getInstance()::applyFullPersistedSnapshot));
        out.add(storeRef("nav-route-registry", baseDir, true, NAV_ROUTES,
                () -> new HashMap<>(NavRouteRegistry.getInstance().snapshotRoutes()),
                NavRouteRegistry.getInstance()::applyFullPersistedSnapshot));
        out.add(storeClass("exploration-data-sale-registry", baseDir, true, ExplorationDataSaleRegistryFile.class,
                ExplorationDataSaleRegistryFile::fromRegistry, ExplorationDataSaleRegistryFile::applyToRegistry));
        out.add(storeClass("organic-data-sale-registry", baseDir, true, OrganicDataSaleRegistryFile.class,
                OrganicDataSaleRegistryFile::fromRegistry, OrganicDataSaleRegistryFile::applyToRegistry));
        out.add(storeClass("mining-stat-registry", baseDir, true, MiningStatRegistryFile.class,
                MiningStatRegistryFile::fromRegistry, MiningStatRegistryFile::applyToRegistry));

        return out;
    }

    private static <T> RegistryStore storeClass(
            String name, Path baseDir, boolean polymorphic, Class<T> type,
            Supplier<T> snapshot, Consumer<T> restore) {
        return new SnapshotJsonStore<>(
                name, jsonFile(baseDir, name), mapper(polymorphic), type, snapshot, restore);
    }

    private static <T> RegistryStore storeRef(
            String name, Path baseDir, boolean polymorphic, TypeReference<T> ref,
            Supplier<T> snapshot, Consumer<T> restore) {
        return new SnapshotJsonStore<>(
                name, jsonFile(baseDir, name), mapper(polymorphic), ref, snapshot, restore);
    }

    private static Path jsonFile(Path baseDir, String storeName) {
        return baseDir.resolve(storeName + ".json");
    }

    private static ObjectMapper mapper(boolean polymorphic) {
        return polymorphic ? JSON_POLYMORPHIC : JSON_SIMPLE;
    }

    // -------------------------------------------------------------------------
    // DTO racine + sérialisation (logique de capture / restauration sur le DTO)
    // -------------------------------------------------------------------------

    public static final class ColonisationRegistryFile {
        public LinkedHashMap<String, ColonisationArchitectSystem> architectByStarSystem;
        public LinkedHashSet<String> beaconDeployedSystems;
        public Long currentConstructionMarketId;

        public static ColonisationRegistryFile fromRegistry() {
            ColonisationRegistry reg = ColonisationRegistry.getInstance();
            ColonisationRegistryFile f = new ColonisationRegistryFile();
            f.architectByStarSystem = reg.snapshotArchitectByStarSystem();
            f.beaconDeployedSystems = reg.snapshotBeaconDeployedSystems();
            f.currentConstructionMarketId = reg.getCurrentConstructionMarketId();
            return f;
        }

        public void applyToRegistry() {
            ColonisationRegistry.getInstance().applyFullPersistedSnapshot(
                    architectByStarSystem, beaconDeployedSystems, currentConstructionMarketId);
        }
    }

    public static final class PlaneteRegistryFile {
        public LinkedHashMap<Integer, ACelesteBody> planetesMap;
        public String currentStarSystem;

        public static PlaneteRegistryFile fromRegistry() {
            PlaneteRegistry reg = PlaneteRegistry.getInstance();
            PlaneteRegistryFile f = new PlaneteRegistryFile();
            f.planetesMap = new LinkedHashMap<>(reg.snapshotPlanetesMap());
            f.currentStarSystem = reg.getCurrentStarSystem();
            return f;
        }

        public void applyToRegistry() {
            PlaneteRegistry.getInstance().applyFullPersistedSnapshot(planetesMap, currentStarSystem);
        }
    }

    public static final class ExplorationDataSaleRegistryFile {
        public List<ExplorationDataSale> sales;
        public ExplorationDataSale currentSale;
        public ExplorationDataOnHold explorationDataOnHold;

        public static ExplorationDataSaleRegistryFile fromRegistry() {
            ExplorationDataSaleRegistry reg = ExplorationDataSaleRegistry.getInstance();
            ExplorationDataSaleRegistryFile f = new ExplorationDataSaleRegistryFile();
            f.sales = new ArrayList<>(reg.snapshotSales());
            f.currentSale = reg.getCurrentSale();
            f.explorationDataOnHold = reg.getExplorationDataOnHold();
            return f;
        }

        public void applyToRegistry() {
            ExplorationDataSaleRegistry.getInstance().applyFullPersistedSnapshot(
                    sales, currentSale, explorationDataOnHold);
        }
    }

    public static final class OrganicDataSaleRegistryFile {
        public List<OrganicDataSale> sales;
        public OrganicDataOnHold currentOrganicDataOnHold;

        public static OrganicDataSaleRegistryFile fromRegistry() {
            OrganicDataSaleRegistry reg = OrganicDataSaleRegistry.getInstance();
            OrganicDataSaleRegistryFile f = new OrganicDataSaleRegistryFile();
            f.sales = new ArrayList<>(reg.snapshotSales());
            f.currentOrganicDataOnHold = reg.getCurrentOrganicDataOnHold();
            return f;
        }

        public void applyToRegistry() {
            OrganicDataSaleRegistry.getInstance().applyFullPersistedSnapshot(
                    sales, currentOrganicDataOnHold);
        }
    }

    public static final class MiningStatRegistryFile {
        public List<MiningStat> miningStats;
        public MiningStat currentMiningSession;

        public static MiningStatRegistryFile fromRegistry() {
            MiningStatRegistry reg = MiningStatRegistry.getInstance();
            MiningStatRegistryFile f = new MiningStatRegistryFile();
            f.miningStats = new ArrayList<>(reg.snapshotMiningStats());
            f.currentMiningSession = reg.snapshotCurrentMiningSession();
            return f;
        }

        public void applyToRegistry() {
            MiningStatRegistry.getInstance().applyFullPersistedSnapshot(
                    miningStats, currentMiningSession);
        }
    }

    static final class ExplorationModeFile {
        @JsonProperty
        public ExplorationMode mode;

        @JsonCreator
        ExplorationModeFile() {}

        ExplorationModeFile(ExplorationMode mode) {
            this.mode = mode;
        }

        static ExplorationModeFile fromRegistry() {
            return new ExplorationModeFile(ExplorationModeRegistry.getInstance().getCurrentMode());
        }

        void applyToRegistry() {
            if (mode != null) {
                ExplorationModeRegistry.getInstance().setCurrentMode(mode);
            }
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

        static NavRouteTargetFile fromRegistry() {
            return new NavRouteTargetFile(
                    NavRouteTargetRegistry.getInstance().getRemainingJumpsInRoute());
        }

        void applyToRegistry() {
            NavRouteTargetRegistry.getInstance().setRemainingJumpsInRoute(remainingJumpsInRoute);
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
                    restored, bountyPerFaction, combatBondPerFaction,
                    totalBountyEarned, totalConflictBounty
            );
        }
    }

    public static final class DestroyedShipEntry {
        public String type;
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

        static List<ProspectedAsteroidFile> listFromRegistry() {
            Deque<ProspectedAsteroid> all = ProspectedAsteroidRegistry.getInstance().getAll();
            List<ProspectedAsteroidFile> list = new ArrayList<>(all.size());
            for (ProspectedAsteroid a : all) {
                list.add(fromRuntime(a));
            }
            return list;
        }

        static void applyListToRegistry(List<ProspectedAsteroidFile> snapshots) {
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
                    // Nouvel enum / renommé
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
