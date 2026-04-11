package be.mirooz.elitedangerous.dashboard.model.registries.colonisation;

import lombok.Value;

/**
 * Amarrage sur un site de colonisation (Docked avec StationName contenant $EXT_PANEL_ColonisationShip).
 */
@Value
public class ColonisationDockEntry {
    String timestamp;
    String stationNameRaw;
    String siteNameLocalised;
    String stationType;
    String starSystem;
    long systemAddress;
    long marketId;
    String stationFactionName;
    double distFromStarLs;
}
