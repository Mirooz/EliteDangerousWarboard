package be.mirooz.elitedangerous.dashboard.model;

import java.util.List;

public class TargetFactionStats {
    private final String targetFaction;
    private final int totalKills;
    private final List<SourceFactionStats> sources;

    public TargetFactionStats(String targetFaction, int totalKills, List<SourceFactionStats> sources) {
        this.targetFaction = targetFaction;
        this.totalKills = totalKills;
        this.sources = sources;
    }

    public String getTargetFaction() { return targetFaction; }
    public int getTotalKills() { return totalKills; }
    public List<SourceFactionStats> getSources() { return sources; }
}

