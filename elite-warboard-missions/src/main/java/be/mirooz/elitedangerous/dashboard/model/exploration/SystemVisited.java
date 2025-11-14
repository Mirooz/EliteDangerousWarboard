package be.mirooz.elitedangerous.dashboard.model.exploration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Représente un système visité et vendu.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemVisited {
    private String systemName;
    private int numBodies;
    private boolean sold; // Indique si les données du système ont été vendues
}

