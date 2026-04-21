package be.mirooz.elitedangerous.dashboard.model.colonisation;

import be.mirooz.elitedangerous.commons.lib.models.commodities.ICommodity;
import lombok.Value;

/**
 * Ressource chantier avec quantité restant à fournir (requis − déjà fourni, plafonné à 0).
 */
@Value
public class ConstructionResourceRemaining {

    ICommodity commodity;
    int requiredAmount;
    int providedAmount;
    int remainingAmount;
    long payment;

    public static ConstructionResourceRemaining from(ConstructionResource resource) {
        if (resource == null) {
            throw new IllegalArgumentException("resource");
        }
        int remaining = Math.max(0, resource.getRequiredAmount() - resource.getProvidedAmount());
        return new ConstructionResourceRemaining(
                resource.getCommodity(),
                resource.getRequiredAmount(),
                resource.getProvidedAmount(),
                remaining,
                resource.getPayment());
    }
}
