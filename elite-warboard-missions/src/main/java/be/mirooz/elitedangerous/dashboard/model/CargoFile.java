package be.mirooz.elitedangerous.dashboard.model;

import lombok.Data;
import java.util.List;

@Data
public class CargoFile {
    private String timestamp;
    private String event;
    private String vessel;
    private Integer count;
    private List<CargoItem> inventory;

    @Data
    public static class CargoItem {
        private String name;
        private String nameLocalised;
        private Integer count;
        private Integer stolen;
        private String missionID;
        private String ownerID;
        private String ownerName;
        private String ownerType;
        private String category;
        private String categoryLocalised;
        private String subcategory;
        private String subcategoryLocalised;
        private Double value;
        private String rarity;
        private String rarityLocalised;
        private String commodity;
        private String commodityLocalised;
        private Integer legal;
        private String type;
        private String typeLocalised;
    }
}

