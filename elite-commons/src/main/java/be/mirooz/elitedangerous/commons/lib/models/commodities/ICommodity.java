package be.mirooz.elitedangerous.commons.lib.models.commodities;

import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.MineralType;

import static be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.MineralType.LOW_TEMPERATURE_DIAMONDS;

/**
 * Interface représentant une commodité dans Elite Dangerous
 */
public interface ICommodity {
    String getInaraId();

    String getInaraName();

    default String getVisibleName() {
        if (this.equals(LOW_TEMPERATURE_DIAMONDS)) {
            return "LTD";
        }
        if (getInaraName() != null)
            return getInaraName().toUpperCase();
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
     * Enum représentant les types de commodités disponibles
     */
    enum CommodityType {
        MINERAL,
        LIMPET,
        OTHER
    }
}
