package be.mirooz.elitedangerous.dashboard.model.targetpanel;

import be.mirooz.elitedangerous.dashboard.model.enums.TargetType;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class TargetFactionStats {
    private final TargetType cible;
    private final String targetFaction;
    private final Map<String, SourceFactionStats> sources = new HashMap<>();

    public void addSource(SourceFactionStats source) {
        sources.merge(
                source.getSourceFaction(),
                source,
                (oldVal, newVal) -> new SourceFactionStats(
                        oldVal.getSourceFaction(),
                        oldVal.getKills() + newVal.getKills()
                )
        );
    }

    public int getTotalKills() {
        return sources.values().stream().mapToInt(SourceFactionStats::getKills).sum();
    }
}
