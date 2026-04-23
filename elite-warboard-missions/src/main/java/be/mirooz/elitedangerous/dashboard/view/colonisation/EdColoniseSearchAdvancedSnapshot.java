package be.mirooz.elitedangerous.dashboard.view.colonisation;

import java.util.Arrays;

/**
 * Paramètres ED Colonise hors les trois filtres principaux (min atterrissables, min anneaux, distance max).
 * Les min par type de corps (hors landables / anneaux) sont stockés par index API {@code min*} (0..17).
 */
public record EdColoniseSearchAdvancedSnapshot(
        String referenceSystem,
        String sortOrder,
        String factionName,
        String hotspotTypes,
        int[] bodyMinExtras) {

    public EdColoniseSearchAdvancedSnapshot {
        bodyMinExtras = bodyMinExtras == null ? new int[18] : Arrays.copyOf(bodyMinExtras, 18);
    }

    public static EdColoniseSearchAdvancedSnapshot defaults() {
        return new EdColoniseSearchAdvancedSnapshot("", "SystemValue", "", "", new int[18]);
    }
}
