package be.mirooz.elitedangerous.dashboard.model.exploration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Représente un système découvert vendu dans une transaction MultiSellExplorationData.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscoveredSystem {
    private String systemName;
    private int numBodies;
}

