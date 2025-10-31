package be.mirooz.elitedangerous.dashboard.model.events;

import lombok.Data;
import java.util.List;

@Data
public class Loadout {
    private String timestamp;
    private String event;
    private String ship;
    private Integer shipID;
    private String shipName;
    private String shipIdent;
    private Long hullValue;
    private Long modulesValue;
    private Double hullHealth;
    private Double unladenMass;
    private Integer cargoCapacity;
    private Double maxJumpRange;
    private FuelCapacity fuelCapacity;
    private Long rebuy;
    private List<Module> modules;

    @Data
    public static class Engineering {
        private String engineer;
        private Integer engineerID;
        private Integer blueprintID;
        private String blueprintName;
        private Integer level;
        private Double quality;
        private List<Modifier> modifiers;
    }

    @Data
    public static class FuelCapacity {
        private Double main;
        private Double reserve;
    }

    @Data
    public static class Modifier {
        private String label;
        private Double value;
        private Double originalValue;
        private Integer lessIsGood;
    }

    @Data
    public static class Module {
        private String slot;
        private String item;
        private Boolean on;
        private Integer priority;
        private Integer ammoInClip;
        private Integer ammoInHopper;
        private Double health;
        private Long value;
        private Engineering engineering;
    }
}
