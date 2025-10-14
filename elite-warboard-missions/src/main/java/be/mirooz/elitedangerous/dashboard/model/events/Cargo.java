package be.mirooz.elitedangerous.dashboard.model.events;

import lombok.Data;
import java.util.List;

@Data
public class Cargo {
    private String timestamp;
    private String event;
    private String vessel;
    private Integer count;
    private List<Inventory> inventory;

    @Data
    public static class Inventory {
        private String name;
        private String nameLocalised;
        private Integer count;
        private Integer stolen;
    }
}

