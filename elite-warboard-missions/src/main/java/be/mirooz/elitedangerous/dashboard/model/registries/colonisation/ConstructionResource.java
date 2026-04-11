package be.mirooz.elitedangerous.dashboard.model.registries.colonisation;

import lombok.Value;

@Value
public class ConstructionResource {
    String nameLocalised;
    int requiredAmount;
    int providedAmount;
    long payment;
}
