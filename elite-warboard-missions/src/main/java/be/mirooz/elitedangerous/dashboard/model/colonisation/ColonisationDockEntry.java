package be.mirooz.elitedangerous.dashboard.model.colonisation;

import lombok.Data;

/**
 * Site de colonisation identifié par {@link #marketId} : infos Docked + sous-objet {@link #construction}
 * rempli à chaque {@code ColonisationConstructionDepot} de même MarketID.
 */
@Data
public class ColonisationDockEntry {

    private long marketId;

    /** Événement Docked (station colonisation) */
    private String dockTimestamp;
    private String stationNameRaw;
    private String siteNameLocalised;
    private String stationType;
    private String starSystem;
    private long systemAddress;
    private String stationFactionName;
    private double distFromStarLs;

    /** Dernier ColonisationConstructionDepot reçu pour ce MarketID */
    private ColonisationConstruction construction;
}
