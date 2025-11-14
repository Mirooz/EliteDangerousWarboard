package be.mirooz.elitedangerous.dashboard.model.exploration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;

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
    private boolean firstDiscover;
    private String firstVisitedTime;
    private String lastVisitedTime;
    @Builder.Default
    private int numberVisited=1;
    private boolean sold; // Indique si les données du système ont été vendues
    @Builder.Default
    private Collection<AbstractCelesteBody> celesteBodies = new ArrayList<>();
}

