package be.mirooz.elitedangerous.lib.inara.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NearbyStation {

    private String stationName;
    private String systemName;
    private String distanceLy;
    private String updated;
    private String economy;
    private String government;
    private String allegiance;
}
