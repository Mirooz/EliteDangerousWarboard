package be.mirooz.elitedangerous.dashboard.model.colonisation.construction;

import java.util.Map;

public class Structure {

    public String category;
    public String type;
    public String name;

    public Map<String, Integer> cost;
    public Map<String, Integer> earning;

    public Map<String, Integer> estimatedCargo;

    public Stats stats;
    public Economy economy;
    public Population population;

    public int getValueForTier(int tier) {
        int value = 0;

        if (cost != null) {
            value += cost.getOrDefault("t" + tier, 0);
        }

        if (earning != null) {
            value += earning.getOrDefault("t" + tier, 0);
        }

        return value;
    }
}