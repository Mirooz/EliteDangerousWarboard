package be.mirooz.elitedangerous.dashboard.model.registries.colonisation;

import be.mirooz.elitedangerous.dashboard.model.colonisation.ColonisationArchitectSystem;
import be.mirooz.elitedangerous.dashboard.model.colonisation.ColonisationConstruction;
import be.mirooz.elitedangerous.dashboard.model.colonisation.ColonisationDockEntry;
import be.mirooz.elitedangerous.dashboard.model.colonisation.ConstructionResourceRemaining;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Synchronized;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Registre colonisation : systèmes « architecte » (ordre d’apparition dans le journal), chacun avec ses sites
 * {@link ColonisationDockEntry} par {@code MarketID} et progression de chantier.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ColonisationRegistry {

    private static final ColonisationRegistry INSTANCE = new ColonisationRegistry();

    /** Clé = nom de système journal ; ordre = première activité colonisation sur ce système. */
    private final LinkedHashMap<String, ColonisationArchitectSystem> architectByStarSystem = new LinkedHashMap<>();

    /**
     * Systèmes pour lesquels {@code ColonisationBeaconDeployed} a été reçu, dans l’ordre chronologique de réception
     * (une nouvelle réception pour un même système le déplace en fin de collection). Tant qu’un système n’y figure pas,
     * les événements {@code ColonisationConstructionDepot} associés sont ignorés.
     */
    private final LinkedHashSet<String> beaconDeployedSystems = new LinkedHashSet<>();

    /**
     * Site (même instance que dans {@link ColonisationArchitectSystem}) dont la {@link ColonisationDockEntry#getConstruction()}
     * est considérée comme chantier courant ; mis à jour quand le journal pousse un nouveau dépôt sur ce {@code MarketID}.
     */
    private ColonisationDockEntry currentConstruction;

    public static ColonisationRegistry getInstance() {
        return INSTANCE;
    }

    private ColonisationArchitectSystem ensureArchitectSystem(String starSystem) {
        if (starSystem == null || starSystem.isBlank()) {
            return null;
        }
        return architectByStarSystem.computeIfAbsent(starSystem, ColonisationArchitectSystem::new);
    }

    /** Enregistre le système comme projet architecte (balise déployée). */
    @Synchronized
    public void recordArchitectBeaconDeployed(String starSystem) {
        if (starSystem == null || starSystem.isBlank()) {
            return;
        }
        // Réinsertion pour que la dernière réception positionne le système en fin de collection.
        beaconDeployedSystems.remove(starSystem);
        beaconDeployedSystems.add(starSystem);
        ensureArchitectSystem(starSystem);
    }

    /** {@code true} si {@code ColonisationBeaconDeployed} a déjà été reçu pour ce système. */
    @Synchronized
    public boolean isBeaconDeployed(String starSystem) {
        if (starSystem == null || starSystem.isBlank()) {
            return false;
        }
        return beaconDeployedSystems.contains(starSystem);
    }

    /**
     * Ordre chronologique de réception des {@code ColonisationBeaconDeployed} (plus ancien en tête, plus récent en queue).
     * La collection retournée est une copie immuable.
     */
    @Synchronized
    public List<String> getBeaconDeploymentOrder() {
        return List.copyOf(new ArrayList<>(beaconDeployedSystems));
    }

    /** Liste ordonnée des systèmes architecte (noms). */
    @Synchronized
    public List<String> getArchitectStarSystems() {
        return List.copyOf(new ArrayList<>(architectByStarSystem.keySet()));
    }

    /** Liste ordonnée des systèmes avec sites et chantiers associés. */
    @Synchronized
    public List<ColonisationArchitectSystem> getArchitectSystems() {
        return List.copyOf(new ArrayList<>(architectByStarSystem.values()));
    }

    /**
     * Met à jour le chantier pour ce {@code MarketID} dans le bucket du système indiqué.
     */
    @Synchronized
    public void applyConstructionDepot(long marketId,
                                       ColonisationConstruction construction,
                                       String starSystem,
                                       Long bodyId) {
        if (!isBeaconDeployed(starSystem)) {
            return;
        }
        ColonisationArchitectSystem sys = ensureArchitectSystem(starSystem);
        if (sys == null) {
            return;
        }
        sys.applyConstructionForMarket(marketId, construction, starSystem, bodyId);
    }

    /**
     * Pointe le chantier courant sur le site de ce {@code MarketID} (référence partagée avec la map interne).
     */
    @Synchronized
    public void setCurrentConstructionByMarketId(long marketId) {
        currentConstruction = findDockEntryByMarketId(marketId);
    }

    @Synchronized
    public ColonisationDockEntry getCurrentConstructionSite() {
        return currentConstruction;
    }

    @Synchronized
    public ColonisationConstruction getCurrentConstruction() {
        return currentConstruction != null ? currentConstruction.getConstruction() : null;
    }

    /**
     * Ressources du chantier courant avec quantités restantes à livrer.
     */
    @Synchronized
    public List<ConstructionResourceRemaining> getActualConstruction() {
        ColonisationConstruction c = currentConstruction != null ? currentConstruction.getConstruction() : null;
        if (c == null || c.getResourcesRequired() == null) {
            return List.of();
        }
        List<ConstructionResourceRemaining> out = new ArrayList<>();
        for (var row : c.getResourcesRequired()) {
            out.add(ConstructionResourceRemaining.from(row));
        }
        return Collections.unmodifiableList(out);
    }

    private ColonisationDockEntry findDockEntryByMarketId(long marketId) {
        for (ColonisationArchitectSystem s : architectByStarSystem.values()) {
            ColonisationDockEntry e = s.getSiteByMarketId(marketId);
            if (e != null) {
                return e;
            }
        }
        return null;
    }

    /** Système architecte contenant ce {@code MarketID}, ou {@code null}. */
    @Synchronized
    public ColonisationArchitectSystem findArchitectSystemContaining(long marketId) {
        for (ColonisationArchitectSystem s : architectByStarSystem.values()) {
            if (s.getSiteByMarketId(marketId) != null) {
                return s;
            }
        }
        return null;
    }

    /**
     * Enregistre l’entrée dock pour le système de l’entrée, uniquement si ce {@code MarketID} n’existe pas encore dans ce système.
     *
     * @return {@code true} si l’entrée a été ajoutée
     */
    @Synchronized
    public boolean addDockIfAbsent(ColonisationDockEntry dockEntry) {
        if (dockEntry == null) {
            return false;
        }
        String sys = dockEntry.getStarSystem();
        ColonisationArchitectSystem bucket = ensureArchitectSystem(sys);
        if (bucket == null) {
            return false;
        }
        return bucket.addDockIfAbsent(dockEntry);
    }

    /** Tous les sites (tous systèmes), ordre : systèmes puis {@code MarketID} dans chaque système. */
    @Synchronized
    public List<ColonisationDockEntry> getDockEntries() {
        List<ColonisationDockEntry> all = new ArrayList<>();
        for (ColonisationArchitectSystem s : architectByStarSystem.values()) {
            all.addAll(s.getSites());
        }
        return Collections.unmodifiableList(all);
    }

    @Synchronized
    public void clear() {
        architectByStarSystem.clear();
        beaconDeployedSystems.clear();
        currentConstruction = null;
    }
}
