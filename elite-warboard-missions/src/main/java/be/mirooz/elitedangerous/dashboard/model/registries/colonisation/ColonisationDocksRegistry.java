package be.mirooz.elitedangerous.dashboard.model.registries.colonisation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Synchronized;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Entrées {@link ColonisationDockEntry} par MarketID (dock + {@link ColonisationConstruction}).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ColonisationDocksRegistry {

    private static final ColonisationDocksRegistry INSTANCE = new ColonisationDocksRegistry();

    private final Map<Long, ColonisationDockEntry> entriesByMarketId = new LinkedHashMap<>();

    public static ColonisationDocksRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Enregistre l’entrée dock uniquement si aucune entrée n’existe déjà pour ce {@link ColonisationDockEntry#getMarketId()}.
     *
     * @return {@code true} si l’entrée a été ajoutée
     */
    @Synchronized
    public boolean addDockIfAbsent(ColonisationDockEntry dockEntry) {
        if (dockEntry == null) {
            return false;
        }
        long id = dockEntry.getMarketId();
        if (entriesByMarketId.containsKey(id)) {
            return false;
        }
        entriesByMarketId.put(id, dockEntry);
        return true;
    }

    @Synchronized
    public void updateConstruction(long marketId, ColonisationConstruction construction) {
        ColonisationDockEntry entry = entriesByMarketId.computeIfAbsent(marketId, k -> {
            ColonisationDockEntry e = new ColonisationDockEntry();
            e.setMarketId(marketId);
            return e;
        });
        entry.setConstruction(construction);
    }

    @Synchronized
    public List<ColonisationDockEntry> getDockEntries() {
        return Collections.unmodifiableList(new ArrayList<>(entriesByMarketId.values()));
    }

    @Synchronized
    public void clear() {
        entriesByMarketId.clear();
    }
}
