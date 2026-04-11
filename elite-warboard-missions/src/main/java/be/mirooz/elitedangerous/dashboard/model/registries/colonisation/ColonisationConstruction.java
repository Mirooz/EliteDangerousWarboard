package be.mirooz.elitedangerous.dashboard.model.registries.colonisation;

import lombok.Value;

import java.util.List;

/**
 * Données du journal {@code ColonisationConstructionDepot} pour un {@link ColonisationDockEntry#marketId}.
 */
@Value
public class ColonisationConstruction {
    String eventTimestamp;
    double constructionProgress;
    ConstructionStatus status;
    List<ConstructionResource> resourcesRequired;
}
