package be.mirooz.elitedangerous.dashboard.model.colonisation;

import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Un système stellaire où le commandant suit la colonisation (architecte), avec les sites par {@link ColonisationDockEntry#getMarketId()}.
 */
@ToString
public class ColonisationArchitectSystem {

    @Getter
    private final String starSystem;

    @Getter
    private long systemAddress;

    private final Map<Long, ColonisationDockEntry> sitesByMarketId = new LinkedHashMap<>();
    private Long firstConstructionMarketId;

    /** Premier {@code MarketID} ayant reçu un dépôt de chantier (journal), ou {@code null}. */
    public Long getFirstConstructionMarketId() {
        return firstConstructionMarketId;
    }

    public ColonisationArchitectSystem(String starSystem) {
        this.starSystem = starSystem != null ? starSystem : "";
    }

    void mergeSystemAddress(long address) {
        if (address != 0L) {
            this.systemAddress = address;
        }
    }

    /**
     * @return {@code true} si l’entrée dock a été ajoutée pour ce {@code MarketID}
     */
    public boolean addDockIfAbsent(ColonisationDockEntry dockEntry) {
        if (dockEntry == null) {
            return false;
        }
        long id = dockEntry.getMarketId();
        if (sitesByMarketId.containsKey(id)) {
            return false;
        }
        sitesByMarketId.put(id, dockEntry);
        mergeSystemAddress(dockEntry.getSystemAddress());
        return true;
    }

    public void applyConstructionForMarket(long marketId,
                                           ColonisationConstruction construction,
                                           String starSystemResolved,
                                           Long bodyIdResolved) {
        ColonisationDockEntry entry = sitesByMarketId.computeIfAbsent(marketId, k -> {
            ColonisationDockEntry e = new ColonisationDockEntry();
            e.setMarketId(marketId);
            return e;
        });
        entry.setConstruction(construction);
        if (starSystemResolved != null && !starSystemResolved.isBlank()) {
            entry.setStarSystem(starSystemResolved);
        }
        if (bodyIdResolved != null && bodyIdResolved >= 0) {
            entry.setBodyId(bodyIdResolved);
        }
        if (firstConstructionMarketId == null) {
            firstConstructionMarketId = marketId;
        }
        entry.setFirstStation(firstConstructionMarketId == marketId);
    }

    public ColonisationDockEntry getSiteByMarketId(long marketId) {
        return sitesByMarketId.get(marketId);
    }

    /** Tous les sites connus pour ce système (ordre d’insertion des {@code MarketID}). */
    public List<ColonisationDockEntry> getSites() {
        return Collections.unmodifiableList(new ArrayList<>(sitesByMarketId.values()));
    }

    /** Derniers dépôts de chantier dont le statut est encore {@link ConstructionStatus#IN_PROGRESS}. */
    public List<ColonisationConstruction> getConstructionsInProgress() {
        List<ColonisationConstruction> out = new ArrayList<>();
        for (ColonisationDockEntry site : sitesByMarketId.values()) {
            ColonisationConstruction c = site.getConstruction();
            if (c != null && c.getStatus() == ConstructionStatus.IN_PROGRESS) {
                out.add(c);
            }
        }
        return Collections.unmodifiableList(out);
    }
}
