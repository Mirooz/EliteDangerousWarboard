package be.mirooz.elitedangerous.dashboard.model.events;

import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.CoreMineralType;
import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.Mineral;
import lombok.Data;
import java.util.List;

@Data
public class ProspectedAsteroid {
    private String timestamp;
    private String event;
    private List<Material> materials;
    private String motherlodeMaterial;
    private CoreMineralType coreMineral;
    private String content;
    private String contentLocalised;
    private Double remaining;

    @Data
    public static class Material {
        private Mineral name;
        private String nameLocalised;
        private Double proportion;
    }
}

