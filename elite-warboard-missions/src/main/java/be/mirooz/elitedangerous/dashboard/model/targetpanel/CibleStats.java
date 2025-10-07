package be.mirooz.elitedangerous.dashboard.model.targetpanel;

import be.mirooz.elitedangerous.dashboard.model.enums.TargetType;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class CibleStats {
    private final TargetType cible;
    private final Map<String, TargetFactionStats> factions = new HashMap<>();

    public TargetFactionStats getOrCreateFaction(String targetFaction) {
        return factions.computeIfAbsent(targetFaction, f -> new TargetFactionStats(cible, targetFaction));
    }

}
