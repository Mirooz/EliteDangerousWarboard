package be.mirooz.elitedangerous.dashboard.persistence;

import be.mirooz.elitedangerous.commons.lib.models.commodities.CarrierUnresolvedCommodity;
import be.mirooz.elitedangerous.commons.lib.models.commodities.ICommodity;
import be.mirooz.elitedangerous.commons.lib.models.commodities.RegistryCommodity;
import be.mirooz.elitedangerous.commons.lib.models.commodities.LimpetType;
import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.Mineral;
import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.MineralType;
import be.mirooz.elitedangerous.commons.lib.models.commodities.minerals.UnknownMineral;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Mixins Jackson pour typer explicitement les interfaces provenant de la librairie commons.
 */
public final class PersistencePolymorphicMixins {

    private PersistencePolymorphicMixins() {}

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = RegistryCommodity.class, name = "registryCommodity"),
            @JsonSubTypes.Type(value = CarrierUnresolvedCommodity.class, name = "carrierUnresolvedCommodity"),
            @JsonSubTypes.Type(value = LimpetType.class, name = "limpetType"),
            @JsonSubTypes.Type(value = MineralType.class, name = "mineralType"),
            @JsonSubTypes.Type(value = UnknownMineral.class, name = "unknownMineral")
    })
    public interface ICommodityMixin {}

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = MineralType.class, name = "mineralType"),
            @JsonSubTypes.Type(value = UnknownMineral.class, name = "unknownMineral")
    })
    public interface MineralMixin {}

    public static void registerOn(com.fasterxml.jackson.databind.ObjectMapper mapper) {
        mapper.addMixIn(ICommodity.class, ICommodityMixin.class);
        mapper.addMixIn(Mineral.class, MineralMixin.class);
    }
}
