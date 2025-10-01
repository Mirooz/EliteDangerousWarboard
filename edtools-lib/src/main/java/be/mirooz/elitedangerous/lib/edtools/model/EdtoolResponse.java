// src/main/java/be/mirooz/elitedangerous/edtools/model/PveResult.java
package be.mirooz.elitedangerous.lib.edtools.model;

import java.util.List;

public class EdtoolResponse {
    private final String referenceSystem;
    private final int maxDistanceLy;
    private final int minSourcesPerTarget;
    private final List<MassacreSystem> rows;

    public EdtoolResponse(String referenceSystem, int maxDistanceLy, int minSourcesPerTarget, List<MassacreSystem> rows) {
        this.referenceSystem = referenceSystem;
        this.maxDistanceLy = maxDistanceLy;
        this.minSourcesPerTarget = minSourcesPerTarget;
        this.rows = rows;
    }

    public String getReferenceSystem() { return referenceSystem; }
    public int getMaxDistanceLy() { return maxDistanceLy; }
    public int getMinSourcesPerTarget() { return minSourcesPerTarget; }
    public List<MassacreSystem> getRows() { return rows; }
}
