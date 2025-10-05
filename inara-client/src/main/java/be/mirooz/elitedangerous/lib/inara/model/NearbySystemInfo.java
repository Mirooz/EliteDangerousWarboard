package be.mirooz.elitedangerous.lib.inara.model;

import lombok.Builder;
import lombok.Data;

/**
 * Informations extraites de la page "Nearest Stations" d'Inara.
 */
@Data
@Builder
public class NearbySystemInfo {
    private String stationName;
    private String systemName;
    private String economy;
    private String government;
    private String allegiance;
    private Double distanceLy;
    private Integer distanceLs;
}
