package be.mirooz.elitedangerous.dashboard.model.targetpanel;

public class SourceFactionStats {
    private final String sourceFaction;
    private final int kills;

    public SourceFactionStats(String sourceFaction, int kills) {
        this.sourceFaction = sourceFaction;
        this.kills = kills;
    }

    public String getSourceFaction() { return sourceFaction; }
    public int getKills() { return kills; }
}