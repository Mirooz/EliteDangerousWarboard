package be.mirooz.elitedangerous.commons.lib.models.commodities;

/**
 * Interface représentant une commodité dans Elite Dangerous
 */
public interface ICommodity {
    String getInaraId();
    String getInaraName();
    String getEdToolName() ;
    /**
     * Retourne le nom utilisé dans le cargo JSON
     * @return Le nom cargo JSON (ex: "benitoite", "drones")
     */
    String getCargoJsonName();
    
    /**
     * Retourne le type de commodité
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
