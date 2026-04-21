package be.mirooz.elitedangerous.commons.lib.models.commodities;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Commodité carrier / CAPI non reconnue par {@link ICommodityFactory} (pas dans le registre ni minéraux / limpets).
 * Clé stable = identifiant interne en minuscules ou repli sur le libellé.
 */
public final class CarrierUnresolvedCommodity implements ICommodity {

    private final String cargoJsonName;
    private final String inaraName;

    public CarrierUnresolvedCommodity(String cargoJsonName, String displayLabel) {
        this.cargoJsonName =
                cargoJsonName == null ? "" : cargoJsonName.trim().toLowerCase(Locale.ROOT);
        this.inaraName =
                displayLabel != null && !displayLabel.isBlank() ? displayLabel : this.cargoJsonName;
    }

    @Override
    public String getInaraId() {
        return null;
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
    public CommodityCategory getInaraCommodityCategory() {
        return CommodityCategory.UNKNOWN;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CarrierUnresolvedCommodity that)) return false;
        return cargoJsonName.equals(that.cargoJsonName);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(cargoJsonName);
    }

    @Override
    public String toString() {
        return inaraName;
    }
}
