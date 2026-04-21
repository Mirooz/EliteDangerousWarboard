package be.mirooz.elitedangerous.dashboard.model.colonisation;

import be.mirooz.elitedangerous.commons.lib.models.commodities.CarrierCommodityResolver;
import be.mirooz.elitedangerous.commons.lib.models.commodities.ICommodity;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Value;

/**
 * Une ligne de {@code ResourcesRequired} dans {@code ColonisationConstructionDepot} (journal Elite Dangerous).
 *
 * @see <a href="https://doc.elitedangereuse.fr/Colonisation/">Documentation journal — Colonisation</a>
 */
@Value
public class ConstructionResource {

    /** Résolution {@link ICommodity} (journal {@code Name} / {@code Name_Localised} via {@link CarrierCommodityResolver}). */
    ICommodity commodity;

    /** {@code RequiredAmount} */
    int requiredAmount;

    /** {@code ProvidedAmount} */
    int providedAmount;

    /** {@code Payment} (crédits par tonne livrée, selon le journal) */
    long payment;

    /** Libellé UI : titre lisible, sinon nom visible / identifiant cargo. */
    public String displayLabel() {
        return displayLabel(commodity);
    }

    public static String displayLabel(ICommodity c) {
        if (c == null) {
            return "?";
        }
        String t = c.getTitleName();
        if (t != null && !t.isBlank()) {
            return t;
        }
        if (c.getVisibleName() != null && !c.getVisibleName().isBlank()) {
            return c.getVisibleName();
        }
        String cargo = c.getCargoJsonName();
        return cargo != null && !cargo.isBlank() ? cargo : "?";
    }

    /**
     * @param row objet JSON d’une entrée de {@code ResourcesRequired} (non null)
     */
    public static ConstructionResource fromResourcesRequiredRow(JsonNode row) {
        String name = row.path("Name").asText("");
        String nameLocalised = row.path("Name_Localised").asText("");
        ICommodity commodity = CarrierCommodityResolver.resolve(name, nameLocalised);
        if (name.contains("cmmcomposite")){
            System.out.println("here");
        }
        commodity.setLocalisedName(nameLocalised);
        return new ConstructionResource(
                commodity,
                row.path("RequiredAmount").asInt(),
                row.path("ProvidedAmount").asInt(),
                row.path("Payment").asLong()
        );
    }
}
