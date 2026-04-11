package be.mirooz.elitedangerous.dashboard.model.registries.colonisation;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Value;

/**
 * Une ligne de {@code ResourcesRequired} dans {@code ColonisationConstructionDepot} (journal Elite Dangerous).
 *
 * @see <a href="https://doc.elitedangereuse.fr/Colonisation/">Documentation journal — Colonisation</a>
 */
@Value
public class ConstructionResource {

    /** {@code Name} — identifiant / chaîne symbolique (ex. {@code $aluminium_name;}) */
    String name;

    /** {@code Name_Localised} — libellé affiché dans la langue du jeu */
    String nameLocalised;

    /** {@code RequiredAmount} */
    int requiredAmount;

    /** {@code ProvidedAmount} */
    int providedAmount;

    /** {@code Payment} (crédits par tonne livrée, selon le journal) */
    long payment;

    /**
     * @param row objet JSON d’une entrée de {@code ResourcesRequired} (non null)
     */
    public static ConstructionResource fromResourcesRequiredRow(JsonNode row) {
        return new ConstructionResource(
                row.path("Name").asText(""),
                row.path("Name_Localised").asText(""),
                row.path("RequiredAmount").asInt(),
                row.path("ProvidedAmount").asInt(),
                row.path("Payment").asLong()
        );
    }
}
