package be.mirooz.elitedangerous.dashboard.model.exploration;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Représente une vente complète de données d'exploration.
 * Accumule toutes les ventes MultiSellExplorationData jusqu'à l'événement Undocked.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExplorationDataOnHold implements ExplorationData {
    /**
     * {@link ConcurrentHashMap} : la sauvegarde JSON et les événements journal (Scan, etc.) peuvent
     * s'exécuter en parallèle ; un {@link java.util.HashMap} provoque des {@code ConcurrentModificationException}.
     */
    @JsonDeserialize(as = ConcurrentHashMap.class)
    @Builder.Default
    private Map<String, SystemVisited> systemsVisitedMap = new ConcurrentHashMap<>();
    private String startTimeStamp;


    @Override
    public long getTotalEarnings() {
        if (systemsVisitedMap == null || systemsVisitedMap.isEmpty()) {
            return 0;
        }

        return systemsVisitedMap.values().stream()
                .flatMap(system -> system.getCelesteBodies().stream())
                .mapToLong(ACelesteBody::computeBodyValue)
                .sum();
    }
    @Override
    public List<SystemVisited> getSystemsVisited(){
        return systemsVisitedMap.values().stream().toList();
    }

    @Override
    public String getEndTimeStamp() {
        return "";
    }

    /**
     * Remplace {@code HashMap}/{@code LinkedHashMap} par un {@link ConcurrentHashMap} si besoin
     * (ex. après {@code ObjectMapper#updateValue} ou données anciennes en mémoire).
     */
    public static void ensureSystemsVisitedMapIsConcurrent(ExplorationDataOnHold onHold) {
        if (onHold == null) {
            return;
        }
        Map<String, SystemVisited> m = onHold.getSystemsVisitedMap();
        if (m != null && !(m instanceof ConcurrentHashMap)) {
            onHold.setSystemsVisitedMap(new ConcurrentHashMap<>(m));
        }
    }
}

