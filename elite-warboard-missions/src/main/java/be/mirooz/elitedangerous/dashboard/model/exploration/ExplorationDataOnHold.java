package be.mirooz.elitedangerous.dashboard.model.exploration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Représente une vente complète de données d'exploration.
 * Accumule toutes les ventes MultiSellExplorationData jusqu'à l'événement Undocked.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExplorationDataOnHold implements ExplorationData {
    @Builder.Default
    private Map<String,SystemVisited> systemsVisitedMap = new HashMap<>();
    private long totalEarnings; // Somme de tous les BaseValue


    @Override
    public List<SystemVisited> getSystemsVisited(){
        return systemsVisitedMap.values().stream().toList();
    }
    @Override
    public String getStartTimeStamp() {
        return "";
    }

    @Override
    public String getEndTimeStamp() {
        return "";
    }
}

