package be.mirooz.elitedangerous.dashboard.model.events;

import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.Mineral;
import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.MineralType;
import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.UnknownMineral;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProspectedAsteroid {

    private String timestamp;
    private String event;

    @JsonSerialize(contentUsing = ProspectedAsteroidMaterialSerializer.class)
    @JsonDeserialize(contentUsing = ProspectedAsteroidMaterialDeserializer.class)
    private List<Material> materials;

    private String motherlodeMaterial;

    @JsonProperty("coreMineralName")
    @JsonDeserialize(using = LenientMineralTypeDeserializer.class)
    private MineralType coreMineral;

    private String content;
    private String contentLocalised;
    private Double remaining;
    private boolean cracked = false;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Material {
        /** Résolution catalogue ED ; null si seul {@link #unregisteredName} est connu. */
        private MineralType mineral;
        /** Nom brut du journal quand le minéral n'est pas mappé sur {@link MineralType}. */
        private String unregisteredName;
        private String nameLocalised;
        private Double proportion;

        /**
         * Vue “runtime” pour prix / affichage (connu = enum, inconnu = {@link UnknownMineral}).
         * Les champs stockés restent concrets ({@code mineral} + {@code unregisteredName}).
         */
        public Mineral toMineral() {
            if (mineral != null) {
                return mineral;
            }
            if (unregisteredName != null && !unregisteredName.isBlank()) {
                return new UnknownMineral(unregisteredName);
            }
            return null;
        }
    }
}
