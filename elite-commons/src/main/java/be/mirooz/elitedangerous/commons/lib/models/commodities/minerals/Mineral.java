package be.mirooz.elitedangerous.commons.lib.models.commodities.minerals;

import be.mirooz.elitedangerous.commons.lib.models.commodities.ICommodity;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javafx.beans.property.IntegerProperty;

/**
 * Interface représentant un minéral dans Elite Dangerous
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = MineralType.class, name = "mineralType"),
        @JsonSubTypes.Type(value = UnknownMineral.class, name = "unknownMineral")
})
public interface Mineral extends ICommodity {
    
    /**
     * Retourne le type de minéral
     * @return Le type de minéral
     */
    MiningMethod getMiningMethod();

    String getMiningRefinedName();


    int getPrice();
    IntegerProperty getPriceProperty();
    void setPrice(int price);

    /**
     * Indique si la valeur de ce minéral doit être considérée comme négligeable (trash value)
     * Par défaut: false
     */
    boolean isTrashValue();
    void setTrashValue(boolean trashValue);
}