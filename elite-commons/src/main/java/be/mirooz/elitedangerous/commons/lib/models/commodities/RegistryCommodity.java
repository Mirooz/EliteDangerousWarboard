package be.mirooz.elitedangerous.commons.lib.models.commodities;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;
import java.util.Optional;

/**
 * Commodité issue du fichier {@code commodities/ardent-inara-registry.json}.
 * Les minéraux et limpets connus en enum restent prioritaires dans {@link ICommodityFactory}.
 */
public final class RegistryCommodity implements ICommodity {

    @Setter
    @Getter
    private String localisedName;
    private final String cargoJsonName;
    private final String inaraId;
    private final String inaraName;
    private final CommodityCategory inaraCategory;

    public RegistryCommodity(
            String cargoJsonName,
            String inaraId,
            String inaraName,
            CommodityCategory inaraCategory) {
        this.cargoJsonName = Objects.requireNonNull(cargoJsonName, "cargoJsonName").toLowerCase();
        this.inaraId = inaraId;
        this.inaraName = inaraName != null ? inaraName : cargoJsonName;
        this.inaraCategory = inaraCategory != null ? inaraCategory : CommodityCategory.UNKNOWN;
    }

    @Override
    public String getInaraId() {
        return inaraId;
    }

    @Override
    public String getInaraName() {
        return inaraName;
    }

    @Override
    public String getEdToolName() {
        return null;
    }

    @Override
    public String getCargoJsonName() {
        return cargoJsonName;
    }

    @Override
    public CommodityType getCommodityType() {
        return CommodityType.OTHER;
    }

    @Override
    public Optional<CommodityCategory> getInaraCommodityCategory() {
        return Optional.of(inaraCategory);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RegistryCommodity that)) return false;
        return cargoJsonName.equals(that.cargoJsonName);
    }

    @Override
    public int hashCode() {
        return cargoJsonName.hashCode();
    }

    @Override
    public String toString() {
        return inaraName;
    }
}
