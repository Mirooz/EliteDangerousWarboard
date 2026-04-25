package be.mirooz.elitedangerous.commons.lib.models.commodities;

import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.MineralType;
import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.UnknownMineral;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Optional;

import static be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.MineralType.LOW_TEMPERATURE_DIAMONDS;

/**
 * Interface représentant une commodité dans Elite Dangerous
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = RegistryCommodity.class, name = "registryCommodity"),
        @JsonSubTypes.Type(value = CarrierUnresolvedCommodity.class, name = "carrierUnresolvedCommodity"),
        @JsonSubTypes.Type(value = LimpetType.class, name = "limpetType"),
        @JsonSubTypes.Type(value = MineralType.class, name = "mineralType"),
        @JsonSubTypes.Type(value = UnknownMineral.class, name = "unknownMineral")
})
public interface ICommodity {
    String getInaraId();

    String getInaraName();
    default String getLocalisedName() {
        return null;
    }
    default void setLocalisedName(String localisedName) {
    }
    default String getVisibleName() {
        if (this.equals(LOW_TEMPERATURE_DIAMONDS)) {
            return "LTD";
        }
        if (getLocalisedName() != null){
            return getLocalisedName();
        }
        if (getInaraName() != null)
            return getInaraName();
        return null;
    }

    default String getTitleName() {
        if (this.equals(LOW_TEMPERATURE_DIAMONDS)) {
            return "LTD";
        }
        String input = getVisibleName();
        if (input == null || input.isEmpty()) {
            return input;
        }

        String[] words = input.toLowerCase().split("\\s+");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }

        return result.toString().trim();
    }

    String getEdToolName();

    /**
     * Retourne le nom utilisé dans le cargo JSON
     *
     * @return Le nom cargo JSON (ex: "benitoite", "drones")
     */
    String getCargoJsonName();

    /**
     * Retourne le type de commodité
     *
     * @return Le type de commodité
     */
    CommodityType getCommodityType();

    /**
     * Catégorie Inara lorsque la commodité provient du registre Ardent/Inara ; vide pour les enums (minéraux, limpets).
     */
    default CommodityCategory getInaraCommodityCategory() {
        return null;
    }
    default void setInaraCommodityCategory(CommodityCategory category) {

    }


    /**
     * Enum représentant les types de commodités disponibles
     */
    enum CommodityType {
        MINERAL,
        LIMPET,
        OTHER
    }
}
