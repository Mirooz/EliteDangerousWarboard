package be.mirooz.elitedangerous.dashboard.model.colonisation;

import lombok.Value;

/**
 * Ressource chantier avec quantité restant à fournir (requis − déjà fourni, plafonné à 0).
 */
@Value
public class ConstructionResourceRemaining {

    String name;
    String nameLocalised;
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
                resource.getName(),
                resource.getNameLocalised(),
                resource.getRequiredAmount(),
                resource.getProvidedAmount(),
                remaining,
                resource.getPayment());
    }
}
